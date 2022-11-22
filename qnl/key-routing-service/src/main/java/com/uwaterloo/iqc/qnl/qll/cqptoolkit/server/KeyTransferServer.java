package com.uwaterloo.iqc.qnl.qll.cqptoolkit.server;

import com.uwaterloo.iqc.qnl.QNLConfiguration;
import com.uwaterloo.iqc.qnl.qll.QLLFileReaderWriter;
import com.uwaterloo.iqc.qnl.qll.QLLReader;
import com.uwaterloo.iqc.qnl.KeyListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;

import com.cqp.remote.KeyTransferGrpc.KeyTransferImplBase;
import com.cqp.remote.*;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.commons.codec.binary.Hex;

public class KeyTransferServer {
    private static Logger LOGGER = LoggerFactory.getLogger(KeyTransferServer.class);

    private final int port;
    private final Server server;
    
    public KeyTransferServer(QNLConfiguration qConfig) throws IOException {
        this.port = 50051;
        this.server = ServerBuilder.forPort(this.port).addService(new KeyTransferService(qConfig)).build();
    }

    public void start() throws IOException {
        this.server.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    KeyTransferServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        });
        
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void addListener(String id, KeyListener k) {
        KeyTransferService.addListener(id, k);
    }


    private static class KeyTransferService extends KeyTransferGrpc.KeyTransferImplBase {

        private QNLConfiguration qConfig;
        private static Map<String, KeyListener> keyListenerMap;
        private Map<String, Long> keysMap;

        KeyTransferService(QNLConfiguration qConfig) {
            this.qConfig = qConfig;
            this.keysMap = new HashMap();
            keyListenerMap = new Hashtable();
        }

        public static void addListener(String id, KeyListener k) {
            k.reset();
            keyListenerMap.put(id, k);
        }

        private void onKeyFromDriver(Key keyMessage, StreamObserver<Empty> responseObserver) {
            String myID = this.qConfig.getConfig().getSiteId();
            // LOGGER.info("SITEID: " + this.qConfig.getConfig().getSiteId());
            String qkdID = keyMessage.getLocalID();
            // LOGGER.info("QKDID: " + qkdID);

            String[] splits = qkdID.split("_");
            if (splits.length != 3) {
                LOGGER.error("Unexpected QKDID format! Expected format alicesite_bobsite_localsite (ex. A_B_A, A_B_B, or B_C_B), found " + qkdID + ". Key cannot be stored");
                return;
            }

            String peerID = splits[0].equals(myID) ? splits[1] : splits[0];
            // LOGGER.info("PEERID: " + peerID);

            QLLReader qllRdr = this.qConfig.getQLLReader(peerID);
            ((QLLFileReaderWriter)qllRdr).write(keyMessage.getKey().toByteArray());
            if (this.keysMap.containsKey(qkdID)) {
                this.keysMap.put(qkdID, this.keysMap.get(qkdID) + 1);
            } else {
                this.keysMap.put(qkdID, 1L);
            }

            Empty reply = Empty.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            if ((this.keysMap.get(qkdID) == this.qConfig.getConfig().getQllBlockSz()) && keyListenerMap.containsKey(qkdID)) {
                KeyListener keyListener = keyListenerMap.get(qkdID);
                keyListener.onKeyGenerated();
            }
        }

        @Override
        public void onKeyFromCQP(Key keyMessage, StreamObserver<Empty> responseObserver) {
            LOGGER.debug("Received key from CQP.");
            onKeyFromDriver(keyMessage, responseObserver);
        }

        @Override
        public void onKeyFromSatellite(Key keyMessage, StreamObserver<Empty> responseObserver) {
            LOGGER.debug("Received key from satellite.");
            onKeyFromDriver(keyMessage, responseObserver);
        }
    }
}
