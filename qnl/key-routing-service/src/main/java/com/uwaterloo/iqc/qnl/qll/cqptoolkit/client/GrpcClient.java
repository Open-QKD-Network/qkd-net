package com.uwaterloo.iqc.qnl.qll.cqptoolkit.client;

import java.util.Iterator;

import com.uwaterloo.iqc.qnl.LinkCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.cqp.remote.*;

public class GrpcClient {
    private static Logger LOGGER = LoggerFactory.getLogger(GrpcClient.class);

    public GrpcClient() {
    }

    public int getLinkStatus(String dummyAddress, int dummyPort)
    {
        int returnValue = -1;
        try{
            ManagedChannel channel = ManagedChannelBuilder.forAddress(dummyAddress, dummyPort)
            .usePlaintext()
            .build();
            IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);
            Iterator<LinkStatus> status = stub.getLinkStatus(com.google.protobuf.Empty.getDefaultInstance());
            if(status.hasNext()){
                returnValue = status.next().getStateValue();
                LOGGER.info("The link is currently up. The current state of the link is: " + returnValue);
            }   
            channel.shutdown();
            return returnValue;
        } catch (Exception e) {
            LOGGER.info("The link is currently down, unregister device if its registered on the siteagent.");
            return returnValue;
        }
    }

    public Site getSiteDetails(String address, int port) {
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
            String deviceId = null;
            if (numberOfDevices > 0) {
                ControlDetails cd = site.getDevices(0);
                DeviceConfig dc = cd.getConfig();
                LOGGER.info("First device's id is " + dc.getId());
                deviceId = dc.getId();
            }
            channel.shutdown();
            return site;
        } catch (Exception e) {
            LOGGER.info("getSiteDetails throws exception: " + e);
            return null;
        }
    }

    public boolean startNode(String aliceUrl,
                            String aliceDeviceId,
                            String bobUrl,
                            String bobDeviceId) {
        Hop alice = Hop.newBuilder()
            .setSite(aliceUrl)
            .setDeviceId(aliceDeviceId)
            .build();

        Hop bob = Hop.newBuilder()
            .setSite(bobUrl)
            .setDeviceId(bobDeviceId)
            .build();

        HopPair hpp = HopPair.newBuilder()
            .setFirst(alice)
            .setSecond(bob)
            .build();

        PhysicalPath pp = PhysicalPath.newBuilder()
            .addHops(hpp)
            .build();

        String[] splits = aliceUrl.split(":");
        if (splits.length != 2) {
            LOGGER.error("Invalid Alice URL. Expected address:port, found " + aliceUrl);
            return false;
        }

        String aliceAddress = splits[0];
        int alicePort;
        try {
            alicePort = Integer.parseInt(splits[1]);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid Alice port in URL. Expected number, found " + splits[1]);
            return false;
        }

        LOGGER.info("GRPC/C/SiteAgent::StartNode between Alice="
                + aliceUrl + ", Bob=" + bobUrl);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(aliceAddress, alicePort)
            .usePlaintext()
            .build();

        ISiteAgentGrpc.ISiteAgentBlockingStub stub = ISiteAgentGrpc.newBlockingStub(channel);
        stub.startNode(pp);

        channel.shutdown();
        return true;
    }

    public boolean endKeyExchange(String aliceAddress,
                            int alicePort,
                            String bobAddress,
                            int bobPort) {
        Site aliceSite = getSiteDetails(aliceAddress, alicePort);
        if (aliceSite == null || aliceSite.getDevicesCount() == 0) {
            LOGGER.error("Fails to get device id for Alice " + aliceAddress + ":" + alicePort);
            return false;
        }

        Site bobSite = getSiteDetails(bobAddress, bobPort);
        if (bobSite == null || bobSite.getDevicesCount() == 0) {
            LOGGER.error("Fails to get device id for Bob " + bobAddress + ":" + bobPort);
            return false;
        }

        String aliceUrl = aliceSite.getUrl();
        String aliceId = aliceSite.getDevices(0).getConfig().getId();

        String bobUrl = bobSite.getUrl();
        String bobId = bobSite.getDevices(0).getConfig().getId();

        Hop alice = Hop.newBuilder()
            .setSite(aliceUrl)
            .setDeviceId(aliceId)
            .build();

        Hop bob = Hop.newBuilder()
            .setSite(bobUrl)
            .setDeviceId(bobId)
            .build();

        HopPair hpp = HopPair.newBuilder()
            .setFirst(alice)
            .setSecond(bob)
            .build();

        PhysicalPath pp = PhysicalPath.newBuilder()
            .addHops(hpp)
            .build();

        LOGGER.info("GRPC/C/SiteAgent::EndKeyExchange between Alice="
                + aliceUrl + ", Bob=" + bobUrl);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(aliceAddress, alicePort)
            .usePlaintext()
            .build();

        ISiteAgentGrpc.ISiteAgentBlockingStub stub = ISiteAgentGrpc.newBlockingStub(channel);
        stub.endKeyExchange(pp);

        channel.shutdown();
        return true;
    }
}
