package com.uwaterloo.iqc.qnl;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.uwaterloo.iqc.qnl.qll.QLLFileReader;
import com.uwaterloo.iqc.qnl.qll.QLLReader;

public class QNLConfiguration {

    private String configLoc;
    private RouteConfig routeCfg;
    private QNLConfig config;
    private Map<String, QLLReader> qllClientMap = new HashMap<String, QLLReader>();
    private Map<String, OTPKey> otpKeyMap =  new HashMap<String, OTPKey>();

    public QNLConfiguration() {
        loadConfig(null);
    }

    public QNLConfiguration(String instanceId) {
        loadConfig(instanceId);
    }

    private void loadConfig(String instanceId) {
        try {
            if (instanceId != null)
                configLoc = System.getProperty("user.home") + "/.qkd" + instanceId + "/qnl/config.yaml";
            else
                configLoc = System.getProperty("user.home") + "/.qkd/qnl/config.yaml";
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            config = mapper.readValue(new File(configLoc), QNLConfig.class);
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(config.getRouteConfigLoc()));
            routeCfg = gson.fromJson(reader, RouteConfig.class);
            reader.close();
            createQLLClients();
            createOTPKeys();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public String getConfigLoc() {
        return configLoc;
    }

    public RouteConfig getRouteConfig() {
        return routeCfg;
    }

    public QNLConfig getConfig() {
        return config;
    }

    public QLLReader getQLLReader(String id) {
        return qllClientMap.get(id);
    }

    public OTPKey getOTPKey(String id) {
        return otpKeyMap.get(id);
    }

    private void createOTPKeys() {
        for (String k : routeCfg.adjacent.keySet())
            otpKeyMap.put(k, new OTPKey(this, k));
    }

    private void createQLLClients() {
        for (String k : routeCfg.adjacent.keySet()) {
            qllClientMap.put(k, new QLLFileReader(k, config));
        }
    }
}

