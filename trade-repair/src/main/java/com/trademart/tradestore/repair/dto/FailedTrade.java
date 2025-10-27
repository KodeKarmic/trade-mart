package com.trademart.tradestore.repair.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "failed_trades")
public class FailedTrade {
    @Id
    private String id;
    private String tradeId;
    private String reason;
    private String payload;

    public FailedTrade() {}

    public FailedTrade(String id, String tradeId, String reason, String payload) {
        this.id = id;
        this.tradeId = tradeId;
        this.reason = reason;
        this.payload = payload;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
