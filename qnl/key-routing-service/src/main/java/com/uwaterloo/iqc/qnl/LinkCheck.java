package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cqp.remote.*;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class LinkCheck extends TimerTask{
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private ISiteAgentServer siteAgentServer;
    private GrpcClient client = new GrpcClient();
    private String siteAgentAddress;
    private int siteAgentPort;

    public LinkCheck(String siteAgentAddress, int siteAgentPort, ISiteAgentServer siteAgentServer)
    {
        this.siteAgentAddress = siteAgentAddress;
        this.siteAgentPort = siteAgentPort;
        this.siteAgentServer = siteAgentServer;
    }

    public void run()
    {
        String dummyDriverAddress;
        int dummyDriverPort;
        Site site = client.getSiteDetails(siteAgentAddress, siteAgentPort);
        if(site.getDevicesCount() > 0){
            for(int index = 0; index < site.getDevicesCount(); ++index){
                dummyDriverAddress = site.getDevices(index).getControlAddress().split(":")[0];
                dummyDriverPort = Integer.parseInt(site.getDevices(index).getControlAddress().split(":")[1]);
                if(!client.getLinkStatus(dummyDriverAddress, dummyDriverPort)){ // unregistering time
                    LOGGER.info("Inside the if condition, device should have been removed from devices.");
                    siteAgentServer.removeDevice(site.getDevices(index).getConfig().getId());
                }
            }
        }
    }
}