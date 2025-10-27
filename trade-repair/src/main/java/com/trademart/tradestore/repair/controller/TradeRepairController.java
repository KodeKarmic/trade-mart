package com.trademart.tradestore.repair.controller;

import com.trademart.tradestore.repair.dto.FailedTrade;
import com.trademart.tradestore.repair.service.TradeRepairService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/repair")
public class TradeRepairController {

    private final TradeRepairService service;

    public TradeRepairController(TradeRepairService service) {
        this.service = service;
    }

    @GetMapping("/failed")
    public List<FailedTrade> listFailed() {
        return service.listFailedTrades();
    }

    @GetMapping("/failed/{id}")
    public ResponseEntity<FailedTrade> getFailed(@PathVariable String id) {
        FailedTrade ft = service.getFailed(id);
        if (ft == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ft);
    }

    @PostMapping("/resubmit/{id}")
    public ResponseEntity<?> resubmit(@PathVariable String id) {
        boolean ok = service.resubmit(id);
        if (ok) return ResponseEntity.accepted().build();
        return ResponseEntity.status(502).body("Resubmit failed or trade not found");
    }

    @PostMapping("/repair")
    public ResponseEntity<?> saveRepaired(@RequestBody FailedTrade repaired) {
        service.saveRepaired(repaired);
        return ResponseEntity.ok().build();
    }
}
