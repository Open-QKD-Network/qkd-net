package com.uwaterloo.iqc.qllsim;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.Scanner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class KeyGenPeer {
    public static void main(String[] args) {
    	Config cfg = ConfigFactory.load();
    	// Creating environment
    	ActorSystem system = ActorSystem.create("KeyGenRemotePeer", cfg);
        // KeyGenPeer actor
        ActorRef kgp = system.actorOf(Props.create(KeyGenPeerActor.class));
        // Send a KEYGEN_BOOT message 
        boolean initiator = cfg.getBoolean("qll.site.initiator");
        
        if (initiator) {
        	Scanner scanner = new Scanner(System.in);
        	System.out.print("Press enter 1 to start key generation: ");
        	int start = scanner.nextInt();
        	if (start == 1) {
        		System.out.print("Starting key generation ...");
        		long interval = cfg.getLong("qll.site.keygeninterval") * 1000;
        		for (;;) {
        			System.out.print("Generating keys ...");
        			kgp.tell("KEYGEN_BOOT", ActorRef.noSender());
        			try {
        				Thread.sleep(interval);
        			} catch(Exception e) {}        			
        		}
        	}
        	scanner.close();
        }        
    }
}
