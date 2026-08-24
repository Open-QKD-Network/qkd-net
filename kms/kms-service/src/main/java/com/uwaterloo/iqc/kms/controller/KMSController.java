package com.uwaterloo.iqc.kms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.uwaterloo.iqc.kms.component.Key;
import com.uwaterloo.iqc.kms.component.KeyPoolManager;
import com.uwaterloo.iqc.kms.component.PolicyEngine;

/**
 * API to create a new key and get an already existing key.
 */

@RestController
public class KMSController {

    @Autowired private KeyPoolManager keyPoolMgr;
    @Autowired private PolicyEngine policy;
    private static final Logger logger = LoggerFactory.getLogger(KMSController.class);
    private static final String URL = "http://127.0.0.1:9000/saeid2siteid.json";
    
    /**
     * Request a new key from KMS. KMS generating the key is considered
     * a source site while the site requesting the key is considered a
     * destination site. The determination of which key pool to use is 
     * made by using source and destination site id. Local site id of the KMS
     * is always the source site for this call.
     * 
     * @param name	Id of the KMS site where the new key request is coming from. 
     * @return Key  New key
     */    
    @RequestMapping("/newkey")
    public Key newKey(@RequestParam(value="siteid") String name) {
        Key k;
        if (policy.check()) {
            k = keyPoolMgr.newKey(name);
            printKey(k, true);
        } else {
            k = new Key();
        }
        return k;
    }

    /**
     * Get a key from the supplied blockid at a given index.
     * The pool name/id used to get the key is constructed
     * from the local site id and supplied site id. For this
     * call local site id is considered the destination and supplied
     * site id is the source site. 
     *     
     * @param name	Source site id	
     * @param block	Block id of the key. It is created when a new
     * 				key request is made.	
     * @param index	Index of the key within the block mentioned above.
     * 				It is created when a new key request is made.	
     * @return		Key
     */
    @RequestMapping("/getkey")
    public Key getKey(@RequestParam(value="siteid") String name,
                      @RequestParam(value="blockid") String block,
                      @RequestParam(value="index") long index) {
        Key k;
        if (policy.check()) {
            k = keyPoolMgr.getKey(name, block, index);
            printKey(k, false);
        } else {
            k = new Key();
        }
        return k;
    }

    //https://stackoverflow.com/questions/44839753/returning-json-object-as-response-in-spring-boot
    @RequestMapping("/v1/keys/{slaveSAEID}/enc_keys")
    public String getEncKey(@PathVariable String slaveSAEID) {
        String siteID = getSiteIDFromSAEID(slaveSAEID, URL);
        Key k;
        if (policy.check()) {
	    k = keyPoolMgr.newKey(siteID);
	    printKey(k, true);
        } else {
	    k = new Key();
        }
        StringBuilder sb = new StringBuilder("{\"keys\":[");
        sb.append(k.toJsonString());
        sb.append("]}");
        return sb.toString();
    }

    @RequestMapping("/v1/keys/{slaveSAEID}/dec_keys")
    public String getDecKey(@PathVariable String slaveSAEID,
                            @RequestParam(value="key_ID") String keyID) {
        String siteID = getSiteIDFromSAEID(slaveSAEID, URL);
        Key k;
        if (policy.check()) {
            // KeyID: index-blockId
            int i = keyID.indexOf('-');
            String index = keyID.substring(0, i);
            String blockId = keyID.substring(i + 1);
            logger.info("siteID:" + siteID);
            logger.info("keyID:" + keyID);
            logger.info("index:" + index);
            logger.info("blockId:" + blockId);
	    k = keyPoolMgr.getKey(siteID, blockId, Long.parseLong(index));
	    printKey(k, false);
        } else {
	    k = new Key();
        }
        StringBuilder sb = new StringBuilder("{\"keys\":[");
        sb.append(k.toJsonString());
        sb.append("]}");
        return sb.toString();
    }

    @RequestMapping("/v1/keys/{slaveSAEID}/status")
    public String getStatus(@PathVariable String slaveSAEID) {
        // key blocksize
        // key bytesize
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("\"source_KME_ID\": ");
        sb.append("\"");
        sb.append(keyPoolMgr.getLocalSiteId());
        sb.append("\"");
        sb.append(",");

        sb.append("\"target_KME_ID\": ");
        sb.append("\"");
        sb.append(slaveSAEID);
        sb.append("\"");
        sb.append(",");

        sb.append("\"master_KME_ID\": ");
        sb.append("\"");
        sb.append(keyPoolMgr.getLocalSiteId());
        sb.append("\"");
        sb.append(",");

        sb.append("\"slave_KME_ID\": ");
        sb.append("\"");
        sb.append(slaveSAEID);
        sb.append("\"");
        sb.append(",");

        sb.append("\"key_size\": ");
        sb.append(keyPoolMgr.getKeyByteSize());
        sb.append(",");

        sb.append("\"key_block_size\": ");
        sb.append(keyPoolMgr.getKeyBlockSize());

        sb.append("}");

        return sb.toString();
    }

    void printKey(Key k, boolean isNew) {
        String str = isNew ? "New Key:" : "Peer Key:";
        logger.info(str);
        logger.info(k.toString());
    }

    String getSiteIDFromSAEID(String SAEID, String url) {
        if (SAEID == null) {
            return "A";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.error("HTTP Response:", response.statusCode());
                return null;
            }
            logger.info("JSON result:" + response.body());
            ObjectMapper om = new ObjectMapper();
            JsonNode result = om.readTree(response.body());
            if (result == null) {
                logger.error("Fails to parse JSON:", response.body());
                return null;
            }
            String siteId = result.get(SAEID).asText();
            logger.info("SAEID:" + SAEID + "Site ID:" + siteId);
            return siteId;
        } catch (IOException |InterruptedException e) {
            return null;
        }
    }
}
