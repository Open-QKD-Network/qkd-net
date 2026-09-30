package com.uwaterloo.iqc.kms.component;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Configuration
public class PolicyEngine {

    @Bean
    public PolicyEngine policy() {
        PolicyEngine p = new  PolicyEngine();
        return  p;
    }

    public boolean check() {
        return true;
    }

    public String getETSI020BorderNode() {
        // Read from etsi20-border.conf
        String filePath = System.getProperty("user.home") + "/.qkd/kms/etsi020-border.conf";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return reader.readLine();
        } catch (Exception io) {
            return "";
        }
    }
}