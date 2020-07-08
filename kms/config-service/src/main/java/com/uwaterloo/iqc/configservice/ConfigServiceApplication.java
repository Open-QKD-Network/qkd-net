package com.uwaterloo.iqc.configservice;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class ConfigServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceApplication.class);

    public static void main(String[] args) throws IOException {
        Properties kmsProperties = new Properties();
        String kmsString = System.getProperty("user.home") + "/config-repo/kms-service.properties";
        InputStream kmsInputStream = new FileInputStream(kmsString);
        kmsProperties.load(kmsInputStream);
        kmsInputStream.close();
        Properties apiProperties = new Properties();
        String apiString = System.getProperty("user.home") + "/config-repo/kms-apigw.properties";
        InputStream apiInputStream = new FileInputStream(apiString);
        apiProperties.load(apiInputStream);
        apiInputStream.close();
        OutputStream kmsOutputStream = new FileOutputStream(kmsString);
        OutputStream apiOutputStream = new FileOutputStream(apiString);
        String auth = "";
        String kmsConfig = System.getProperty("user.home") + "/.qkd/kms/kms.conf";
        BufferedReader br = new BufferedReader(new FileReader(kmsConfig));
        for (int i = 0; i < 5; ++i) {
            auth = br.readLine();
        }
        if (auth.equals("false")) {
            if (!kmsProperties.containsKey("security.ignored")) {
                kmsProperties.setProperty("security.ignored", "/**");
            }
            if (!apiProperties.containsKey("security.ignored")) {
                apiProperties.setProperty("security.ignored", "/**");
            }
        } else {
            if (kmsProperties.containsKey("security.ignored")) {
                kmsProperties.remove("security.ignored");
            }
            if (apiProperties.containsKey("security.ignored")) {
                apiProperties.remove("security.ignored");
            }
        }
        kmsProperties.store(kmsOutputStream, null);
        apiProperties.store(apiOutputStream, null);
        br.close();
        kmsOutputStream.close();
        apiOutputStream.close();
        SpringApplication.run(ConfigServiceApplication.class, args);
        logger.info("INFO: Starting config-service ...");
    }
}
