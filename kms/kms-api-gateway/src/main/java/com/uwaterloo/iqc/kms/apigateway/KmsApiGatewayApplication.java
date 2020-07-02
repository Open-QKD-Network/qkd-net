package com.uwaterloo.iqc.kms.apigateway;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
// @EnableResourceServer
public class KmsApiGatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(KmsApiGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(KmsApiGatewayApplication.class, args);
        logger.info("INFO: Starting kms-apigw ...");
    }
}
