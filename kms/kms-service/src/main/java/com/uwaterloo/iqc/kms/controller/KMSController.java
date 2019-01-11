package com.uwaterloo.iqc.kms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    void printKey(Key k, boolean isNew) {
        String str = isNew ? "New Key:" : "Peer Key:";
        logger.info(str);
        logger.info(k.toString());
    }
}
