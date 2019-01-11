package com.uwaterloo.iqc.kms.apigateway;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;



//@Profile("secure")
//@Configuration
//@EnableResourceServer
//class SecureResourceConfiguration extends ResourceServerConfigurerAdapter {

//@Override
//public void configure(HttpSecurity http) throws Exception {
///	http.antMatcher("/api/**").authorizeRequests()
///	 .anyRequest().authenticated();
//}
//}