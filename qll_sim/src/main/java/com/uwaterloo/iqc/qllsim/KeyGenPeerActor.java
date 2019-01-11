package com.uwaterloo.iqc.qllsim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Formatter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.uwaterloo.iqc.qllsim.Messages.KeyGenReq;
import com.uwaterloo.iqc.qllsim.Messages.KeyGenResp;

import akka.actor.ActorSelection;
import akka.actor.UntypedAbstractActor;

public class KeyGenPeerActor extends UntypedAbstractActor {
	private Config cfg = ConfigFactory.load();
	String home = System.getProperty("user.home");
	String ip = cfg.getString("qll.site.peer.ip");
	long port = cfg.getLong("qll.site.peer.port");
	String seedPeer = cfg.getString("qll.site.peer.seed");
	String seed = cfg.getString("qll.site.seed");
	String selectionString = "akka.tcp://KeyGenRemotePeer@" + ip + ":" + port + "/user/KeyGenPeerActor";
	String siteId = cfg.getString("qll.site.id");
	String outputLocation = home + cfg.getString("qll.keys.location-sufix");
	long outputSz = cfg.getLong("qll.keys.output-size");
	static long fileCnt = 0L;
	SecureRandom rng;
	SecureRandom rngPeer;
	
	// Getting the other actor
	private ActorSelection selection = getContext().actorSelection(selectionString);
	
    public KeyGenPeerActor() {
    	rng = new SecureRandom();
    	rngPeer = new SecureRandom();
    	rng.setSeed(seed.getBytes());
    	rngPeer.setSeed(seedPeer.getBytes());    	
    }
	
    @Override
    public void onReceive(Object message) throws Exception {
    	String digest;
        if (message.equals("KEYGEN_BOOT")) {        	
        	System.out.println("Recieved KEYGEN_BOOT message by site id :" + siteId); 
            selection.tell(new KeyGenReq(siteId), getSelf());
        } else if (message instanceof KeyGenReq) {
        	KeyGenReq req = (KeyGenReq)message;
        	System.out.println("Recieved key generation request message from site id :" + req.getId());
        	digest = generateKeys(outputLocation + req.getId() + "_" + fileCnt, rng);
        	selection.tell(new KeyGenReq(siteId), getSelf());
        	getSender().tell(new KeyGenResp(siteId, digest, fileCnt), getSelf());
        	++fileCnt;
        } else if (message instanceof KeyGenResp) {
            KeyGenResp resp = (KeyGenResp) message;
            System.out.println("Recieved key generation response message from site id :" + resp.getId());            
            digest = generateKeys(outputLocation + resp.getId() + "_" + resp.getCnt(), rngPeer);
            //compare digest with peer's digest
        }
    }
    
    private String generateKeys(String location, SecureRandom random) {
    	byte[] bytes = new byte[32];
    	byte[] md5digest = null;
    	PrintWriter out = null;
        try {
        	out  = new PrintWriter(new BufferedWriter(new FileWriter(location)));
        	for (int k = 0; k < (int)outputSz; ++k) {
        		random.nextBytes(bytes);
                MessageDigest md = MessageDigest.getInstance("MD5");
                md5digest = md.digest(bytes);
                Formatter formatter = new Formatter();
                for (byte b : bytes) {
                	formatter.format("%02x", b);
                }
                out.println(formatter.toString());
                formatter.close();               
        	}
        } catch(Exception e) {  	  
        } finally {
        	out.close();
        }
        return md5digest != null ? new String(md5digest) : new String();        
    }
}
