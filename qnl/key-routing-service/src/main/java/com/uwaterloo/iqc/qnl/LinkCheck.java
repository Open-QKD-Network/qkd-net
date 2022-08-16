package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import com.cqp.remote.*;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class LinkCheck extends TimerTask{
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private String address;
    private int port;

    public LinkCheck(String address, int port)
    {
        this.address = address;
        this.port = port;
    }

    public void run()
    {
        try{
            ManagedChannel channel = ManagedChannelBuilder.forAddress(address, port)
            .usePlaintext()
            .build();
            IDeviceGrpc.IDeviceBlockingStub stub = IDeviceGrpc.newBlockingStub(channel);
            Iterator<LinkStatus> status = stub.getLinkStatus(com.google.protobuf.Empty.getDefaultInstance());
            if(status.hasNext())
                LOGGER.info("This is vital information. The current state of the link is: " + status.next().getStateValue());
            channel.shutdown();
        } catch (Exception e) {
            LOGGER.info("getLinkStatus throws exception " + e);
        }
    }
}