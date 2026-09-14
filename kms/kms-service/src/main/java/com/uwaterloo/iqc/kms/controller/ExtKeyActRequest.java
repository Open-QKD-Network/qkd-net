package com.uwaterloo.iqc.kms.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ExtKeyActRequest {

    @JsonProperty("key_id_container")
    private List<ExtKeyIdContainer> keyIdContainer;

    @JsonProperty("ack_status")
    private String ackStatus;

    @JsonProperty("initiator_sae_id")
    private String initiatorSaeId;

    @JsonProperty("target_sae_ids")
    private List<String> targetSaeIds;

    private String message;

    private Map<String, Object> extension;

    public List<ExtKeyIdContainer> getKeyIdContainer() {
        return keyIdContainer;
    }

    public void setKeyIdContainer(List<ExtKeyIdContainer> keyIdContainer) {
        this.keyIdContainer = keyIdContainer;
    }

    public String getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(String ackStatus) {
        this.ackStatus = ackStatus;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getExtension() {
        return extension;
    }

    public void setExtension(Map<String, Object> extension) {
        this.extension = extension;
    }
}
