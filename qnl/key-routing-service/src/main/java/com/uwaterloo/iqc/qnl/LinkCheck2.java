package com.uwaterloo.iqc.qnl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cqp.remote.*;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.client.GrpcClient;
import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.ISiteAgentServer;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

public class LinkCheck2 extends TimerTask {
    private static Logger LOGGER = LoggerFactory.getLogger(LinkCheck2.class);

    private Timer timer;
    private String mySiteId;
    private String peerSiteId;
    private String myQKDDeviceId;
    private QNLConfiguration qConfig;
    private GrpcClient grpcClient = new GrpcClient();
    private String localSiteAgentAddress;
    private int localSiteAgentPort;
    private ISiteAgentServer localSiteAgent;

    public LinkCheck2(Timer timer,
                    String mySiteId,
                    String peerSiteId,
                    String localSiteAgentAddress,
                    int localSiteAgentPort,
                    ISiteAgentServer localSiteAgent,
                    QNLConfiguration qConfig) {
        this.timer = timer;
        this.mySiteId = mySiteId;
        this.peerSiteId = peerSiteId;
        this.myQKDDeviceId = qConfig.getQKDLinkConfig(peerSiteId).localQKDDeviceId;
        this.localSiteAgent = localSiteAgent;
        this.localSiteAgentAddress = localSiteAgentAddress;
        this.localSiteAgentPort = localSiteAgentPort;
        this.qConfig = qConfig;
        LOGGER.info("New link check for " + this);
    }

    // TimerTask class
    public void run() {

        Site localSite = this.grpcClient.getSiteDetails(this.localSiteAgentAddress, this.localSiteAgentPort);

        if (localSite.getDevicesCount() == 0) {
            restartTimerTask();
            return;
        }
        
        boolean localDeviceRegistered = false;
        for (int index = 0; index < localSite.getDevicesCount(); ++index) {
            String deviceId = localSite.getDevices(index).getConfig().getId();
            LOGGER.info("Locally registered QKD device:" + deviceId + ", this:" + this);
            if (this.myQKDDeviceId.equalsIgnoreCase(deviceId)) {
                localDeviceRegistered = true;
                LOGGER.info("Local QKD device is registered:" + this.myQKDDeviceId);
                QKDLinkConfig link = qConfig.getQKDLinkConfig(this.peerSiteId);
                String remoteDeviceId = link.remoteQKDDeviceId;
                String remoteSiteAgentUrl = link.remoteSiteAgentUrl;
                // check if remote device is registered
                boolean isRemoteRegistered = isRemoteDeviceRegistered(remoteDeviceId, remoteSiteAgentUrl);
                if (isRemoteRegistered) {
                    LOGGER.info("Remote QKD device is registered:" + remoteDeviceId + ", this:" + this);
                    this.grpcClient.startNode(link.localSiteAgentUrl, link.localQKDDeviceId, link.remoteSiteAgentUrl, link.remoteQKDDeviceId);
                    return;
                } else {
                    LOGGER.info("Remote QKD device is NOT registered:" + remoteDeviceId + ", this:" + this);
                    restartTimerTask();
                    return;
                }
            }
        }

        if (!localDeviceRegistered) {
            LOGGER.info("Local QKD device is NOT registered, this:" + this);
            restartTimerTask();
            return;
        }
    }

    private void restartTimerTask() {
        LinkCheck2 again = new LinkCheck2(this.timer,
            this.mySiteId,
            this.peerSiteId,
            this.localSiteAgentAddress,
            this.localSiteAgentPort,
            this.localSiteAgent,
            this.qConfig);
        this.timer.schedule(again, 1000 * 30);
    }

    private boolean isRemoteDeviceRegistered(String remoteDeviceId, String remoteSiteAgentUrl) {

        String remoteSiteAgentAddress;
        int remoteSiteAgentPort = 0;
        remoteSiteAgentAddress = remoteSiteAgentUrl.split(":")[0];

        try {
            remoteSiteAgentPort = Integer.parseInt(remoteSiteAgentUrl.split(":")[1]);
        } catch (NumberFormatException e) {
            LOGGER.info("NumberFormatException for remoteSiteAgentUrl:" + remoteSiteAgentUrl);
            return false;
        }

        Site remoteSite = this.grpcClient.getSiteDetails(remoteSiteAgentAddress, remoteSiteAgentPort);
        for (int index = 0; index < remoteSite.getDevicesCount(); index++) {
            String deviceId = remoteSite.getDevices(index).getConfig().getId();
            LOGGER.info("QKD device on remote site agent:" + deviceId + ", my remote device:" + remoteDeviceId + ", this:" + this);
            if (deviceId.equalsIgnoreCase(remoteDeviceId)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "MyId=" + this.mySiteId + ", link=" + this.qConfig.getQKDLinkConfig(this.peerSiteId);
    }
}