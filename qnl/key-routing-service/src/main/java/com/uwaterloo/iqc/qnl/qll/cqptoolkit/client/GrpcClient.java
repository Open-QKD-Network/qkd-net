package com.uwaterloo.iqc.qnl.qll.cqptoolkit.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.cqp.remote.*;

public class GrpcClient {
    private static Logger LOGGER = LoggerFactory.getLogger(GrpcClient.class);

    public GrpcClient() {
    }

    public void getSiteDetails(String address, int port) {
        try {
            LOGGER.info("getSiteDetails " + address + ":" + port);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
                .usePlaintext()
                .build();
            ISiteAgentGrpc.ISiteAgentBlockingStub stub = ISiteAgentGrpc.newBlockingStub(channel);
            Site site = stub.getSiteDetails(com.google.protobuf.Empty.getDefaultInstance());
            int numberOfDevices = site.getDevicesCount();
            LOGGER.info("GRPC/C/SiteAgent::GetSiteDetails(), url=" + site.getUrl() +
                ", number of devices:" + numberOfDevices);
            if (numberOfDevices > 0) {
                ControlDetails cd = site.getDevices(0);
                DeviceConfig dc = cd.getConfig();
                LOGGER.info("First device's id is " + dc.getId());
            }
        
            channel.shutdown();
        } catch (Exception e) {
            LOGGER.info("getSiteDetails throws exception: " + e);
        }
    }

    public void startNode(String aliceUrl, String bobUrl) {
    }
}