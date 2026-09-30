package com.uwaterloo.iqc.kms.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class PolicyEngine {

    private static final Logger logger = LoggerFactory.getLogger(PolicyEngine.class);

    @Bean
    public PolicyEngine policy() {
        PolicyEngine p = new  PolicyEngine();
        return  p;
    }

    public boolean check() {
        return true;
    }

    public String getETSI020BorderNode(String key) {
        // Read from etsi20-border.conf
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String filePath = System.getProperty("user.home") + "/.qkd/kms/etsi020-border.json";
            String jsonContent = Files.readString(Path.of(filePath));
            JsonNode json = objectMapper.readTree(jsonContent);
            if (json == null) {
                logger.error("Can not get key, check the file " + filePath);
                return "";
            }
            String ret = json.get(key).asText();
            logger.info("Local ETSI020 border node for " + key + ", " + ret);
            return ret;
        } catch (Exception e) {
            logger.info("Exception in getETSI020BorderNode for keyv" + key + "Exception:" + e);
            return "";
        }
    }
}