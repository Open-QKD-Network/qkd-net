package com.uwaterloo.iqc.qnl.qll.cqptoolkit.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.cqp.remote.ControlDetails;
import com.cqp.remote.DeviceID;
import com.cqp.remote.Hop;
import com.cqp.remote.HopPair;
import com.cqp.remote.IDeviceGrpc;
import com.cqp.remote.ISiteAgentGrpc;
import com.cqp.remote.LocalSettings;
import com.cqp.remote.PhysicalPath;
import com.cqp.remote.RawKeys;
import com.cqp.remote.SessionDetailsTo;
import com.cqp.remote.Site;
import com.cqp.remote.ISiteAgentGrpc.ISiteAgentImplBase;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class ISiteAgentServer { // wrapper class for start() stop() functionality
    private static Logger LOGGER = LoggerFactory.getLogger(ISiteAgentServer.class);

    private final int port;
    private final Server server;

    public ISiteAgent siteAgent;

    public ISiteAgentServer(String address, int port) throws IOException {
        this.port = port;
        this.server = ServerBuilder.forPort(this.port).addService(new ISiteAgent(address + ":" + port)).build();
    }

    public void start() throws IOException {
        this.server.start();
        LOGGER.info("start() success");
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private static class ISiteAgent extends ISiteAgentImplBase {

        private final String url; // Note: the cpp version uses a SiteAgentConfig grpc message. see Site.proto
                                    // and SiteAgent constructor in cqptoolkit for more info
        private Set<ControlDetails> devices = new HashSet<ControlDetails>();

        private ArrayList<Byte> keyData = new ArrayList<Byte>();

        ISiteAgent(String url) {
            LOGGER.info("Creating site agent with url " + url);
            this.url = url;
        }

        private void prepHop(final String localDevice) {
            Thread t = new Thread() {
                public void run() {
                    LOGGER.info("Starting key reader thread");
                    // TODO: possibly make getting address and port less hacky?
                    String address = localDevice.split(":")[0];
                    int port = Integer.parseInt(localDevice.split(":")[1]);
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                        .usePlaintext()
                        .build();
                    IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);
                    // initial key is garbage value - not used
                    // see DummyQKD::setinitialkey in cqptoolkit
                    Iterator<RawKeys> keys = stub.waitForSession(LocalSettings.newBuilder()
                                                                            .setInitialKey(ByteString.copyFromUtf8("garbage value initial key"))
                                                                            .build());
                    while(keys.hasNext()) {
                        RawKeys key = keys.next();
                        List<ByteString> byteStrList = key.getKeyDataList();
                        for(ByteString byteStr : byteStrList) {
                            LOGGER.info("got bytes: " + byteStr.toStringUtf8());
                            // TODO: find more elegant way to add List<ByteString> to arraylist
                            byte[] byteArr = byteStr.toByteArray();
                            for(byte b : byteArr) {
                                keyData.add(b);
                            }
                        }
                    }
                }
            };
            t.start();
        }

        private void startNode(HopPair hop, final PhysicalPath path) {
            if(hop.getFirst().getSite() == url) {
                LOGGER.info("Starting Alice node with url " + url);
                prepHop(hop.getFirst().getDeviceAddress());
                // TODO: possibly make getting address and port less hacky?
                String address = hop.getSecond().getSite().split(":")[0];
                int port = Integer.parseInt(hop.getSecond().getSite().split(":")[1]);
                ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                    .usePlaintext()
                    .build();
                ISiteAgentGrpc.ISiteAgentBlockingStub stub = ISiteAgentGrpc.newBlockingStub(channel);
                stub.startNode(path);
            } else if(hop.getSecond().getSite() == url) {
                LOGGER.info("Starting Bob node with url " + url);
                prepHop(hop.getSecond().getDeviceAddress());
                // TODO: possibly make getting address and port less hacky?
                String address = hop.getSecond().getDeviceAddress().split(":")[0];
                int port = Integer.parseInt(hop.getSecond().getDeviceAddress().split(":")[1]);
                ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                    .usePlaintext()
                    .build();
                IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);
                stub.runSession(SessionDetailsTo.newBuilder()
                                            .setPeerAddress(hop.getFirst().getDeviceAddress())
                                            .setDetails(hop.getParams())
                                            .build());
            } else {
                LOGGER.error(url + " doesn't match either side of hop " + hop);
            }
        }

        @Override
        public void startNode(PhysicalPath path, StreamObserver<Empty> responseObserver) {
            List<HopPair> hops = path.getHopsList();
            for(HopPair hop : hops) {
                startNode(hop, path);
            }
        }

        @Override
        public void endKeyExchange(PhysicalPath path, StreamObserver<Empty> responseObserver) {

        }

        @Override
        public void getSiteDetails(Empty empty, StreamObserver<Site> responseObserver) {
            LOGGER.info("Getting site details, size " + devices.size());
            Site response = Site.newBuilder()
            .setUrl(url)
            .addAllDevices(devices)
            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        
        @Override
        public void registerDevice(ControlDetails details, StreamObserver<Empty> responseObserver) {
            LOGGER.info("Registering device with control address " + details.getControlAddress());
            devices.add(details);
        }

        @Override
        public void unregisterDevice(DeviceID deviceID, StreamObserver<Empty> responseObserver) {

        }
    }

}
