package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.cqp.remote.*;

import java.util.Timer;
import java.util.TimerTask;

public class WaitForConnect extends TimerTask { // the purpose of this class is to run startNode
    private static Logger LOGGER = LoggerFactory.getLogger(KeyRouter.class);
    private QKDLinkConfig cfg;
    private Timer timer;

    WaitForConnect(QKDLinkConfig cfg, Timer timer) {
      this.cfg = cfg;
      this.timer = timer;
    }

    public void run()
    {
      boolean found = false;
      GrpcClient client = new GrpcClient();
      String remoteAddress = cfg.remoteSiteAgentUrl.split(":")[0];
      int remotePort = Integer.parseInt(cfg.remoteSiteAgentUrl.split(":")[1]);
      Site site = client.getSiteDetails(remoteAddress, remotePort);

      LOGGER.info("number of devices on remote site currently: " + site.getDevicesCount());
      for(int index = 0; index < site.getDevicesCount(); ++index) { // for (ControlDetails cd : site.getDevices()) could be an option?
        if(site.getDevices(index).getConfig().getId().equals(cfg.remoteQKDDeviceId)){ // if the particular dummy driver exists on the siteAgent
          LOGGER.info("Starting node " + cfg.localQKDDeviceId.charAt(4) + " --> " + cfg.remoteQKDDeviceId.charAt(4));
          client.startNode(cfg.localSiteAgentUrl, cfg.localQKDDeviceId, cfg.remoteSiteAgentUrl, cfg.remoteQKDDeviceId);
          found = true;
        }
      }
      if(!found){
        timer.schedule(new WaitForConnect(cfg, timer), 10000);
        LOGGER.info("waiting for 10 seconds, remote site's dummy driver isn't registered on its site agent yet.");
      }
    }
  }
