package com.uwaterloo.iqc.configservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigServiceApplication {
	private static final Logger logger = LoggerFactory.getLogger(ConfigServiceApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
        logger.info("INFO: Starting config-service ...");
    }
}
