package com.uwaterloo.iqc.qnl;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uwaterloo.iqc.qnl.qll.QLLFileReaderWriter;
import com.uwaterloo.iqc.qnl.qll.QLLReader;

import com.uwaterloo.iqc.qnl.qll.cqptoolkit.server.*;

public class QNLConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(QNLConfiguration.class);

    private String configLoc;
    private RouteConfig routeCfg;
    private QNLConfig config;
    private Map<String, QKDLinkConfig> qkdLinkCfgMap = new HashMap<String, QKDLinkConfig>();
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

            JsonReader routeReader = new JsonReader(new FileReader(config.getRouteConfigLoc()));
            routeCfg = gson.fromJson(routeReader, RouteConfig.class);
            routeReader.close();

            for (String siteID: routeCfg.adjacent.keySet()) {
                LOGGER.info("Parsing file: " + config.getQKDLinkConfigLoc(siteID));
                JsonReader qkdLinkReader = new JsonReader(new FileReader(config.getQKDLinkConfigLoc(siteID)));
                qkdLinkCfgMap.put(siteID, (QKDLinkConfig) gson.fromJson(qkdLinkReader, QKDLinkConfig.class));
                qkdLinkReader.close();
            }

            createQLLClients();
        } catch(Exception e) {
            e.printStackTrace();
            LOGGER.info("Failed to load/parse JSON files, please check the files: " + config.getRouteConfigLoc() + " and qkdlink files and to make sure they are valid JSON.");
        }
    }

    public String getConfigLoc() {
        return configLoc;
    }

    public RouteConfig getRouteConfig() {
        return routeCfg;
    }

    public QKDLinkConfig getQKDLinkConfig(String id) {
        return qkdLinkCfgMap.get(id);
    }

    public Map<String, QKDLinkConfig> getQKDLinkConfigMap() {
        return qkdLinkCfgMap;
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

    public void createOTPKeys(KeyTransferServer server) {
        for (String k : routeCfg.adjacent.keySet()) {
            OTPKey key = new OTPKey(this, k);
            server.setListener(key);
            otpKeyMap.put(k, key);
        }
    }

    private void createQLLClients() {
        for (String k : routeCfg.adjacent.keySet()) {
            qllClientMap.put(k, new QLLFileReaderWriter(k, config));
        }
    }
}

