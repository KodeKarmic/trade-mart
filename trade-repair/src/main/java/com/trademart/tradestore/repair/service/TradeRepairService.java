package com.trademart.tradestore.repair.service;

import com.trademart.tradestore.repair.dto.FailedTrade;
import com.trademart.tradestore.repair.repository.FailedTradeRepository;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TradeRepairService {

    // Configurable endpoint to resubmit repaired trades to trade-store.
    private final String resubmitUrl;

    private final RestTemplate rest;
    private final FailedTradeRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public TradeRepairService(FailedTradeRepository repository, RestTemplate rest,
                              @org.springframework.beans.factory.annotation.Value("${trade.store.resubmit.url:http://trade-store:8080/api/trades/resubmit}") String resubmitUrl) {
        this.repository = repository;
        this.rest = rest;
        this.resubmitUrl = resubmitUrl;
    }

    public List<FailedTrade> listFailedTrades() {
        return repository.findAll();
    }

    public FailedTrade getFailed(String id) {
        return repository.findById(id).orElse(null);
    }

    public boolean resubmit(String id) {
        FailedTrade ft = repository.findById(id).orElse(null);
        if (ft == null) return false;
        try {
            // Parse payload to a map so we can adjust the version field
            Map<String, Object> tradeMap = mapper.readValue(ft.getPayload(), new TypeReference<>() {});

            // derive max-version endpoint from the resubmit URL which should end with /trades
            // resulting path: {resubmitUrl}/{tradeId}/max-version
            String tradeId = (tradeMap.get("tradeId") != null) ? tradeMap.get("tradeId").toString() : null;
            if (tradeId != null && !tradeId.isBlank()) {
                String maxVersionUrl = resubmitUrl.endsWith("/") ? resubmitUrl + tradeId + "/max-version" : resubmitUrl + "/" + tradeId + "/max-version";
                try {
                    // Use String.class for robustness against content-type differences, then parse
                    var resp = rest.getForEntity(maxVersionUrl, String.class);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        try {
                            Integer max = Integer.valueOf(resp.getBody().trim());
                            // set version to max + 1
                            tradeMap.put("version", max + 1);
                        } catch (NumberFormatException nfe) {
                            if (!tradeMap.containsKey("version") || tradeMap.get("version") == null) {
                                tradeMap.put("version", 1);
                            }
                        }
                    } else {
                        // no existing version -> if payload lacks a version, default to 1
                        if (!tradeMap.containsKey("version") || tradeMap.get("version") == null) {
                            tradeMap.put("version", 1);
                        }
                    }
                } catch (Exception ex) {
                    // If query fails, fall back to using provided payload version or 1
                    if (!tradeMap.containsKey("version") || tradeMap.get("version") == null) {
                        tradeMap.put("version", 1);
                    }
                }
            } else {
                // no tradeId in payload - cannot determine max version; fallback
                if (!tradeMap.containsKey("version") || tradeMap.get("version") == null) {
                    tradeMap.put("version", 1);
                }
            }

            // serialize updated payload
            String updatedPayload = mapper.writeValueAsString(tradeMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> body = new HttpEntity<>(updatedPayload, headers);

            rest.postForEntity(resubmitUrl, body, String.class);
            // If successful, remove from failed store
            repository.deleteById(id);
            return true;
        } catch (Exception ex) {
            // log and return false - leave it in the failed list for retry
            System.err.println("Resubmit failed for id=" + id + " -> " + ex.getMessage());
            return false;
        }
    }

    // Helper to allow adding/replacing a repaired trade
    public void saveRepaired(FailedTrade repaired) {
        if (repaired != null && repaired.getId() != null) {
            repository.save(repaired);
        }
    }
}
