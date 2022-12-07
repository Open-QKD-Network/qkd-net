package com.uwaterloo.iqc.qnl;

public class QKDLinkConfig {
    public String localQKDDeviceId;
    public String remoteQKDDeviceId;
    public String localSiteAgentUrl;
    public String remoteSiteAgentUrl;

    public String toString() {
        return "LocalId=" + localQKDDeviceId +
               ",RemoteId=" + remoteQKDDeviceId +
               ",LocalSiteAgent=" + localSiteAgentUrl +
               ",RemoteSiteAgent=" + remoteSiteAgentUrl;
    }
}
