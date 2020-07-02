package com.uwaterloo.iqc.kmsservice;

import java.util.Formatter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages= {"com.uwaterloo.iqc.kms"})
// @EnableResourceServer
public class KmsServiceApplication {

    public static void main(String[] args) {

        Formatter formatter = new Formatter();
        byte[] bytes = new byte[] {127, 15, 0};
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String hex = formatter.toString();
        //		UserSimple userObject = new UserSimple(
        //			    "Norman",
        //			    "norman@futurestud.io",
        //			    26,
        //			    true
        //			);
        //		Gson gson = new Gson();
        //		String userJson = gson.toJson(userObject);
        SpringApplication.run(KmsServiceApplication.class, args);
    }
    /*
    	@Bean
    	  @LoadBalanced
    	  RestTemplate simpleRestTemplate() {
    	   return new RestTemplate();
    	  }*/

//
//	@Bean(name = "threadPoolExecutor")
//	public Executor getAsyncExecutor() {
//		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//		executor.setCorePoolSize(7);
//		executor.setMaxPoolSize(42);
//		executor.setQueueCapacity(11);
//		executor.setThreadNamePrefix("threadPoolExecutor-");
//		executor.initialize();
//		return executor;
//	}
}
//
//class UserSimple {
//    String name;
//    String email;
//    int age;
//    boolean isDeveloper;
//
//    public UserSimple(String name, String email, int age, boolean dev) {
//    	this.name = name;
//    	this.email = email;
//    	this.age = age;
//    	this.isDeveloper = dev;
//    }
//}
