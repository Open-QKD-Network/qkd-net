package com.uwaterloo.iqc.kms.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtKeyInfo {
     @JsonProperty("key_id")
    private String keyId;
    private String value;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}