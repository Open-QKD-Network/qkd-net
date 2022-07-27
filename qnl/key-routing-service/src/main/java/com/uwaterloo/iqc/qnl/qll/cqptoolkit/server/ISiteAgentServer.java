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
import com.cqp.remote.Key;
import com.cqp.remote.KeyTransferGrpc;
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

    public interface ISiteAgentServerListener {
        public void onDeviceRegistered(String deviceID);
    }

    static ISiteAgentServerListener myListener_ = null; // this is static only because the registerDevice is in a nested class

    public void setMySiteAgentListener(ISiteAgentServerListener listener) {
        this.myListener_ = listener;
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

        private String findDeviceAddress(String deviceId) {
            for(ControlDetails cd : devices) {
                if(cd.getConfig().getId().equals(deviceId)) {
                    return cd.getControlAddress();
                }
            }
            return null;
        }

        private ManagedChannel splitAndGetChannel(String addr) {
            // TODO: possibly make getting address and port less hacky?
            String address = addr.split(":")[0];
            int port = Integer.parseInt(addr.split(":")[1]);
            return ManagedChannelBuilder.forAddress(address, port)
                .usePlaintext()
                .build();
        }

        private void prepHop(final String localDeviceUrl, final String localDeviceId) {
            Thread t = new Thread() {
                public void run() {
                    LOGGER.info("Starting key reader thread");
                    LOGGER.info("localDeviceUrl is " + localDeviceUrl);
                    LOGGER.info("localDeviceId is " + localDeviceId);
                    ManagedChannel channel = splitAndGetChannel(localDeviceUrl);
                    IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);

		    //Key Transfer Stub Setup
                    //TODO get these automatically
                    String keyTransferHost = "localhost";
                    int keyTransferPort = 50051;
                    int keysSent = 1;

                    int qllBlockSz = 4096;
                    int maxKeyBlocks = 10;

                    ManagedChannel keyTransferChannel =
                        ManagedChannelBuilder
                        .forAddress(keyTransferHost, keyTransferPort)
                        .usePlaintext()
                        .build();

                    KeyTransferGrpc.KeyTransferBlockingStub keyTransferStub = KeyTransferGrpc.newBlockingStub(keyTransferChannel);


                    // initial key is garbage value - not used
                    // see DummyQKD::setinitialkey in cqptoolkit
                    LOGGER.info("starting waitForSession for stub " + localDeviceUrl);
                    Iterator<RawKeys> keys = stub.waitForSession(LocalSettings.newBuilder()
                                                                            .setInitialKey(ByteString.copyFromUtf8("garbage value initial key"))
                                                                            .build());
                    LOGGER.info("ended waitForSession for stub " + localDeviceUrl);

                    while(keys.hasNext()) {
                        RawKeys key = keys.next();
                        List<ByteString> byteStrList = key.getKeyDataList();
                        for(ByteString byteStr : byteStrList) {
                            LOGGER.info("got key bytes: " + byteStr.toStringUtf8());
                            // TODO: find more elegant way to add List<ByteString> to arraylist
                            byte[] byteArr = byteStr.toByteArray();
                            for(byte b : byteArr) {
                                keyData.add(b);
                            }

                            Key keyMessage =
                                Key.newBuilder()
                                    .setKey(byteStr)
                                    .setSeqID(keysSent)
                                    .setLocalID(localDeviceId)
                                .build();
                            keysSent += 1;

                            keyTransferStub.onKeyFromCQP(keyMessage);
                        }
                    }
                }
            };
            t.start();
        }

        private void startNode(int hopIndex, PhysicalPath path) {
            HopPair hop = path.getHops(hopIndex);
            if(hop.getFirst().getSite().equals(url)) {
                LOGGER.info("Starting Alice node with url " + url);
                // device ID is necessary since we don't know if device address is set for us
                String deviceAddress = findDeviceAddress(hop.getFirst().getDeviceId());
                if(deviceAddress == null) {
                    LOGGER.error("Device with id " + hop.getFirst().getDeviceId() + " not found on site agent "
                                    + url + ". Unable to start key reading thread. Fatal.");
                }
                prepHop(deviceAddress, hop.getFirst().getDeviceId()); // starts key reader thread
                ManagedChannel channel = splitAndGetChannel(hop.getSecond().getSite());
                ISiteAgentGrpc.ISiteAgentBlockingStub stub = ISiteAgentGrpc.newBlockingStub(channel);
                // set device address for left/first side
                // TODO: find a less hacky way to edit a grpc object
                // TODO: frankenstein url correct way to do this?
                String thisDeviceAddr = deviceAddress;
                PhysicalPath newPath = PhysicalPath.newBuilder(path).setHops(hopIndex,
                                        HopPair.newBuilder(hop).setFirst(
                                            Hop.newBuilder(hop.getFirst()).setDeviceAddress(thisDeviceAddr))).build();
                stub.startNode(newPath);
            } else if(hop.getSecond().getSite().equals(url)) {
                LOGGER.info("Starting Bob node with url " + url);
                // device ID is necessary since we don't know if device address is set for us
                String deviceAddress = findDeviceAddress(hop.getSecond().getDeviceId());
                if(deviceAddress == null) {
                    LOGGER.error("Device with id " + hop.getSecond().getDeviceId() + " not found on site agent "
                                    + url + ". Unable to start key reading thread. Fatal.");
                }
                prepHop(deviceAddress, hop.getSecond().getDeviceId()); // starts key reader thread
                ManagedChannel channel = splitAndGetChannel(deviceAddress);
                IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);
                LOGGER.info("Peer address has length " + hop.getFirst().getDeviceAddress().length() + " and message " + hop.getFirst().getDeviceAddress());
                stub.runSession(SessionDetailsTo.newBuilder()
                                            // getDeviceAddress() works here because we always start on left/first side
                                            // and we set deviceaddress when doing that side
                                            .setPeerAddress(hop.getFirst().getDeviceAddress())
                                            .setDetails(hop.getParams())
                                            .build());
            } else {
                LOGGER.error(url + " doesn't match either side of hop " + hop);
            }
        }

        @Override
        public void startNode(PhysicalPath path, StreamObserver<Empty> responseObserver) {
            LOGGER.info("startNode starting on " + url);
            for(int i = 0; i < path.getHopsCount(); i++) {
                startNode(i, path);
            }
        }

        @Override
        public void endKeyExchange(PhysicalPath path, StreamObserver<Empty> responseObserver) {
            // TODO: do this
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
            if(ISiteAgentServer.myListener_ != null){
                ISiteAgentServer.myListener_.onDeviceRegistered(details.getConfig().getId());
            }
        }

        @Override
        public void unregisterDevice(DeviceID deviceID, StreamObserver<Empty> responseObserver) {
            LOGGER.info("Unregistering device with device ID " + deviceID.getId());
            for(ControlDetails cd : devices) {
                if(cd.getConfig().getId().equals(deviceID)) {
                    devices.remove(cd);
                    return;
                }
            }
            LOGGER.info("Could not find device with id " + deviceID.getId());
        }
    }
    
}
