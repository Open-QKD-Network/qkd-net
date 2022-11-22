package com.uwaterloo.iqc.qnl;

import java.util.Map;

import com.uwaterloo.iqc.qnl.lsrp.LSRPRouter;

public class RouteConfig {
    public class Route {
        public String address;
        public KMEConfig kme;
    }

    public class KMEConfig {
        public String name;
        public String address;
        public String remote_sae;
        public String client_cert;
        public String client_key;
        public String client_key_password;
        public String server_cert;
    }

    public Map<String, Route> adjacent;
    //non-adjacent -  key:siteid of non-adjacent node,
    //value:siteid of adjacent node through which the key
    //is reachable
    public Map <String, String> nonAdjacent;

    private LSRPRouter lsrpRouter;

    public boolean isIdAdjacent(String key) {
        return adjacent.containsKey(key);
    }

    public String getAdjacentId(String key) {
        /*if (adjacent.containsKey(key))
            return key;
        else
            return nonAdjacent.get(key);*/
        return this.lsrpRouter.getNextHop(key);
    }

    public String getOtherAdjacentId(String key) {
        for (String k : adjacent.keySet()) {
            if (!k.equals(key))
                return k;
        }
        return null;
    }

    public String[] getAdjacentIdAddress(String key) {
        if (adjacent.containsKey(key)) {
            return adjacent.get(key).address.split(":");
        }

        return null;
    }

    public KMEConfig getAdjacentIdKME(String key) {
        if (adjacent.containsKey(key)) {
            return adjacent.get(key).kme;
        }

        return null;
    }

    public void setLSRPRouter(LSRPRouter router) {
        this.lsrpRouter = router;
    }
}
