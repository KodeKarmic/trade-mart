package com.trademart.tradestore.mongo;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trade_history")
public class TradeHistory {

  @Id private String id;

  private String tradeId;

  private Integer version;

  private String changeType;

  private Map<String, Object> before;

  private Map<String, Object> after;

  private String actor;

  private Instant timestamp;

  public TradeHistory() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTradeId() {
    return tradeId;
  }

  public void setTradeId(String tradeId) {
    this.tradeId = tradeId;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getChangeType() {
    return changeType;
  }

  public void setChangeType(String changeType) {
    this.changeType = changeType;
  }

  public Map<String, Object> getBefore() {
    return before;
  }

  public void setBefore(Map<String, Object> before) {
    this.before = before;
  }

  public Map<String, Object> getAfter() {
    return after;
  }

  public void setAfter(Map<String, Object> after) {
    this.after = after;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }
}
