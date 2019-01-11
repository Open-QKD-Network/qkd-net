package com.uwaterloo.iqc.registrationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class RegistrationServiceApplication {
	private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(RegistrationServiceApplication.class, args);
        logger.info("INFO: Starting registration-service ...");
    }
}
