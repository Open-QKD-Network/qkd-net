package com.uwaterloo.iqc.qnl.qll.cqptoolkit.server;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.cqp.remote.ControlDetails;
import com.cqp.remote.DeviceID;
import com.cqp.remote.PhysicalPath;
import com.cqp.remote.Site;
import com.cqp.remote.ISiteAgentGrpc.ISiteAgentImplBase;
import com.google.protobuf.Empty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        ISiteAgent(String url) {
            LOGGER.info("Creating site agent with url " + url);
            this.url = url;
        }

        @Override
        public void startNode(PhysicalPath path, StreamObserver<Empty> responseObserver) {

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
