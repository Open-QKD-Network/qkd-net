package com.uwaterloo.iqc.kms.component;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
