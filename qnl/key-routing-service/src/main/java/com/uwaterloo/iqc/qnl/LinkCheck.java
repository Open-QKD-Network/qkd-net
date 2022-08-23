package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cqp.remote.*;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

public class LinkCheck extends TimerTask{
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private ISiteAgentServer siteAgentServer;
    private GrpcClient client = new GrpcClient();
    private String siteAgentAddress;
    private int siteAgentPort;
    private TimerWrapper timers;
    private boolean nodeStarted;
    private QNLConfiguration qConfig;


    public LinkCheck(String siteAgentAddress, int siteAgentPort, ISiteAgentServer siteAgentServer, TimerWrapper timers, boolean nodeStarted, QNLConfiguration qConfig)
    {
        this.siteAgentAddress = siteAgentAddress;
        this.siteAgentPort = siteAgentPort;
        this.siteAgentServer = siteAgentServer;
        this.timers = timers;
        this.nodeStarted = nodeStarted;
        this.qConfig = qConfig;
    }

    private int findIndex(Site site, String deviceID){
        for(int i = 0; i < site.getDevicesCount(); ++i){
            if(site.getDevices(i).getConfig().getId().equals(deviceID)){
                return i;
            }
        }
        return -1;
    }

    public void run()
    {
        String deviceID;
        String dummyDriverAddress;
        int dummyDriverPort;
        String remoteDeviceID;
        String remoteSiteAgentAddress;
        int remoteSiteAgentPort;
        String remoteDummyDriverAddress;
        int remoteDummyDriverPort;
        int remoteIndex;
        Site localSite = client.getSiteDetails(siteAgentAddress, siteAgentPort);
        Site remoteSite;

        if(localSite.getDevicesCount() > 0){
            LOGGER.info("the amount of devices on our site is: " + localSite.getDevicesCount());
            for(int index = 0; index < localSite.getDevicesCount(); ++index){
                
                dummyDriverAddress = localSite.getDevices(index).getControlAddress().split(":")[0];
                dummyDriverPort = Integer.parseInt(localSite.getDevices(index).getControlAddress().split(":")[1]);
                deviceID = localSite.getDevices(index).getConfig().getId();
                remoteDeviceID = deviceID.substring(0, 4);

                if(deviceID.charAt(4) < deviceID.charAt(2)){  // alice moment

                    remoteDeviceID += deviceID.charAt(2);
                    QKDLinkConfig cfg = qConfig.getQKDLinkConfig(Character.toString(deviceID.charAt(2))); // for example, "B"
                    remoteSiteAgentAddress = cfg.remoteSiteAgentUrl.split(":")[0];
                    remoteSiteAgentPort = Integer.parseInt(cfg.remoteSiteAgentUrl.split(":")[1]);
                    remoteSite = client.getSiteDetails(remoteSiteAgentAddress, remoteSiteAgentPort);
              
                    remoteIndex = findIndex(remoteSite, remoteDeviceID);
                    if(remoteIndex == -1){
                         LOGGER.info("this remote dummy driver was not registered yet.");
                        return;
                    }
                    remoteDummyDriverAddress = remoteSite.getDevices(remoteIndex).getControlAddress().split(":")[0];
                    remoteDummyDriverPort = Integer.parseInt(remoteSite.getDevices(remoteIndex).getControlAddress().split(":")[1]);

                    LOGGER.info("mmkay, remote info: " + remoteDummyDriverAddress + " and port " + remoteDummyDriverPort);

                    if(client.getLinkStatus(dummyDriverAddress, dummyDriverPort)){ // alice is up
                        if(client.getLinkStatus(remoteDummyDriverAddress, remoteDummyDriverPort) && !nodeStarted){
                            //startNode here.
                            LOGGER.info("we're going to start!");
                            timers.addTimer(deviceID); // this is because even if there was a timer for this connection earlier, it has been removed by now.
                            timers.getTimer(deviceID).schedule(new WaitForConnect(cfg, timers.getTimer(deviceID)), 0);
                            nodeStarted = true;  
                        }
                    }
                    else if(siteAgentServer.deviceExists(deviceID)){
                        // this means that the link is down, but the dummy driver hasn't been removed yet from our set of devices
                        // also since we are alice, we will kill the startNode associated with us.
                        LOGGER.info("bye bye alice, from alice!");
                        timers.removeTimer(deviceID);
                        siteAgentServer.removeDevice(deviceID);
                    }

                    if(!client.getLinkStatus(remoteDummyDriverAddress, remoteDummyDriverPort)){ // bob is down
                        // since bob is down, we still need to kill the startNode associated with us.
                        // we will not remove the device, since bob's dummy driver is not registered on our site (doesn't exist in our set)
                        LOGGER.info("bye bye bob, from alice!");
                        timers.removeTimer(deviceID);
                        nodeStarted = false;
                    }
                    else{
                        LOGGER.info("bob is currently up for alice.");
                    }
                }
                else{ //bob moment
                    if(!client.getLinkStatus(dummyDriverAddress, dummyDriverPort)){ // bob is down
                        LOGGER.info("bye bye bob, from bob!");
                        siteAgentServer.removeDevice(deviceID);
                    }
                }
                /*if(!client.getLinkStatus(dummyDriverAddress, dummyDriverPort)){ // unregistering time
                    LOGGER.info("Inside the if condition, device should have been removed from devices.");
                    siteAgentServer.removeDevice(deviceID);
                }*/
            }
        }
    }
}