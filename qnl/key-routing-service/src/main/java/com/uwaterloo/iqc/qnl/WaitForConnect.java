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
      /***here's what we want to check:
       * 1. if our site id is lexicographically less than our peer site's site id (aka we are alice)
       * 2. if peer's dummy driver is registered on peer's site agent
       * 
       * only after these two checks are complete will we run startNode on the Timer thread.
       * 
       * something like while(!check2){
       *  timer.schedule(new TimerTask(), 10000); //this is not the repeat after period version of timer.schedule
       * }
       * 
       * this way after TimerTask.run() calls startNode and gets it running on the thread, we don't call timer.schedule again
       * since outside (!check2) becomes false. 
       * 
       * check1 requires us to compare the siteIDs.
       * since we have access to ConfigArgs, which has a static qConfig, we have access to both the localSite as well as
       * all remoteSites.
       * 
       * localSite = ConfigArgs.qConfig.getConfig().getSiteId();
       * 
       * for the remoteSites, simply parse through the entrySet of qConfig:
       * 
       * for( Map.Entry<String, QKDLinkConfig> cfgEntry: ConfigArgs.qConfig.getQKDLinkConfigMap().entrySet())
       * 
       * remoteSite = cfgEntry.getKey();
       *
       * we could simply run through all the remoteSites one at a time and call startNode for each, aka a nested loop.
       * 
       * localSite = ...;
       * for(each remoteSite){
       *    if(localSite < remoteSite){
       *      while(!registered){
       *        timer.schedule(...);
       *    }
       *  }
       * }
       * 
       * simply do the following:
       * 
       * String remoteAddress = cfg.remoteSiteAgentUrl.split(":")[0];
       * String remotePort = cfg.remoteSiteAgentUrl.split(":")[1];
       * 
       * Site site = client.getSiteDetails(remoteAddress, remotePort); 
       * 
       * so, for localSite being A and remoteSite being B, we need to check if dummy_a_b_b is registered on B or not.
       * and so on.
       * ***/
      boolean found = false;
      GrpcClient client = new GrpcClient();
      String remoteAddress = cfg.remoteSiteAgentUrl.split(":")[0];
      int remotePort = Integer.parseInt(cfg.remoteSiteAgentUrl.split(":")[1]);
      Site site = client.getSiteDetails(remoteAddress, remotePort);

      LOGGER.info("number of devices on remote site currently: " + site.getDevicesCount());
      for(int index = 0; index < site.getDevicesCount(); ++index) { // for (ControlDetails cd : site.getDevices())
        if(site.getDevices(index).getConfig().getId().equals(cfg.remoteQKDDeviceId)){
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
