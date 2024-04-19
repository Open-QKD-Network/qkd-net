package com.uwaterloo.iqc.qnl.qll;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QLLConfig {
    
    /** IP address of QLL hardware device's ETSI server (KME) */
    private String qllIPaddr;

    /** Port of QLL hardware device's ETSI server (KME) */
    private int qllPort;
    
    // private static Logger LOGGER = LoggerFactory.getLogger(QLLConfig.class);

    public QLLConfig() {}

    public static void main(String[] args) {
        // Load config from a given absolute file path
        // print out ip and port
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            String fileLocation = "/Users/rahultandon/dev/work/iqc/etsi-qkd-net/qnl/qnl-qll-service/src/main/resources/exampleQLLConfig.yaml";
            QLLConfig qllConfig = mapper.readValue(new File(fileLocation), QLLConfig.class);
            System.out.println("IPAddr: " + qllConfig.getQllIPaddr());
            System.out.println("Port: " + qllConfig.getqllPort());
        } catch (java.io.FileNotFoundException e) {
            System.err.println("QLLConfig.yaml not found!");
            e.printStackTrace();
        } catch (Exception e) {
            // [@rahul revisit]: this can have multiple types of exceptions which should be handled indvidually
            // LOGGER.error("Error loading QLL config file: " + e.getMessage());
            System.err.println("Error loading QLL config file: " + e.getMessage());
            e.printStackTrace();
            
        }
    }


    public String getQllIPaddr() {
        return qllIPaddr;
    }

    public void setQllIPaddr(String qllIPaddr) {
        this.qllIPaddr = qllIPaddr;
    }

    public int getqllPort() {
        return qllPort;
    }

    public void setqllPort(int qllPort) {
        this.qllPort = qllPort;
    }

}
