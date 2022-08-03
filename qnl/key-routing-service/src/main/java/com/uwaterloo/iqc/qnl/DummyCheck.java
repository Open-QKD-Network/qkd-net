package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.cqp.remote.*;

import java.util.Timer;
import java.util.TimerTask;

public class DummyCheck extends TimerTask{
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private QKDLinkConfig cfg;

    DummyCheck(QKDLinkConfig cfg)
    {
        this.cfg = cfg;
    }

    public void run()
    {
        GrpcClient client = new GrpcClient();
        String localAddress = cfg.localSiteAgentUrl.split(":")[0];
        int localPort = Integer.parseInt(cfg.localSiteAgentUrl.split(":")[1]);
        String remoteAddress = cfg.remoteSiteAgentUrl.split(":")[0];
        int remotePort = Integer.parseInt(cfg.remoteSiteAgentUrl.split(":")[1]);
        Site localSite = client.getSiteDetails(localAddress, localPort);
        Site remoteSite = client.getSiteDetails(remoteAddress, remotePort);
        //if(localSite.getDevicesCount() == 0)
        LOGGER.info("The local device ID is: " + cfg.localQKDDeviceId + " number of devices on this site agent: " + localSite.getDevicesCount());
        //if(remoteSite.getDevicesCount() == 0)
        LOGGER.info("The remote device ID is: " + cfg.remoteQKDDeviceId + " number of devices on this site agent: " + remoteSite.getDevicesCount());
    }
}