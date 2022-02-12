package com.uwaterloo.iqc.qnl.qll.cqptoolkit.server;

import com.uwaterloo.iqc.qnl.QNLConfiguration;
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

    public void setListener(KeyListener k) {
        KeyTransferService.setListener(k);
    }


    private static class KeyTransferService extends KeyTransferGrpc.KeyTransferImplBase {

        private QNLConfiguration qConfig;
        private static KeyListener keyListener;
        private long keys;

        KeyTransferService(QNLConfiguration qConfig) {
            this.qConfig = qConfig;
            this.keys = 0;
        }

        public static void setListener(KeyListener k) {
            keyListener = k;
            keyListener.reset();
        }

        @Override
        public void onKeyFromCQP(Key keyMessage, StreamObserver<Empty> responseObserver) {

            String source = this.qConfig.getConfig().getSiteId();
            String destination = source.equals("A") ? "B" : "A";

            QLLReader qllRdr = this.qConfig.getQLLReader(destination);
            qllRdr.write(keyMessage.getSeqID(), Hex.encodeHexString(keyMessage.getKey().toByteArray()), destination);
            this.keys++;

            Empty reply = Empty.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            if ((this.keys == this.qConfig.getConfig().getQllBlockSz()) && this.keyListener != null) {
                this.keyListener.onKeyGenerated();
            }
        }
    }
}
