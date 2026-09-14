package com.uwaterloo.iqc.kms.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtKeyRequest {

    private List<ExtKeyInfo> keys;

    @JsonProperty("initiator_sae_id")
    private String initiatorSaeId;

    @JsonProperty("target_sae_ids")
    private List<String> targetSaeIds;

    @JsonProperty("ack_callback_url")
    private String ackCallbackUrl;

    public List<ExtKeyInfo> getKeys() {
        return keys;
    }

    public void setKeys(List<ExtKeyInfo> keys) {
        this.keys = keys;
    }

    public String getInitiatorSaeId() {
        return initiatorSaeId;
    }

    public void setInitiatorSaeId(String initiatorSaeId) {
        this.initiatorSaeId = initiatorSaeId;
    }

    public List<String> getTargetSaeIds() {
        return targetSaeIds;
    }

    public void setTargetSaeIds(List<String> targetSaeIds) {
        this.targetSaeIds = targetSaeIds;
    }

    public String getAckCallbackUrl() {
        return ackCallbackUrl;
    }

    public void setAckCallbackUrl(String ackCallbackUrl) {
        this.ackCallbackUrl = ackCallbackUrl;
    }
}