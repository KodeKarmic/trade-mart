package com.trademart.tradestore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "trades",
    indexes = {
        @Index(name = "idx_trade_tradeid", columnList = "tradeId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_trade_tradeid", columnNames = { "tradeId" })
    }
)
public class TradeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String tradeId;

  @Column(nullable = false)
  private Integer version;

  @Column(precision = 19, scale = 4)
  private BigDecimal price;

  private Integer quantity;

  private LocalDate maturityDate;

  @Enumerated(EnumType.STRING)
  private TradeStatus status;

  private Instant createdAt;

  private Instant updatedAt;

  @Version
  private Integer optLock;

  public TradeEntity() {
  }

  public TradeEntity(
      String tradeId,
      Integer version,
      BigDecimal price,
      Integer quantity,
      LocalDate maturityDate,
      TradeStatus status) {
    this.tradeId = tradeId;
    this.version = version;
    this.price = price;
    this.quantity = quantity;
    this.maturityDate = maturityDate;
    this.status = status;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
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

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public void setMaturityDate(LocalDate maturityDate) {
    this.maturityDate = maturityDate;
  }

  public TradeStatus getStatus() {
    return status;
  }

  public void setStatus(TradeStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Integer getOptLock() {
    return optLock;
  }

  public void setOptLock(Integer optLock) {
    this.optLock = optLock;
  }
}
