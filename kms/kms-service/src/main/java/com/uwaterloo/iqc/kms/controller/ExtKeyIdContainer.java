package com.uwaterloo.iqc.kms.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ExtKeyIdContainer {

    @JsonProperty("key_id")
    private String keyId;

    private Map<String, Object> extension;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Map<String, Object> getExtension() {
        return extension;
    }

    public void setExtension(Map<String, Object> extension) {
        this.extension = extension;
    }
}