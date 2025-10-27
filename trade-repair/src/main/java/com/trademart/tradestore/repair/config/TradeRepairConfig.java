package com.trademart.tradestore.repair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TradeRepairConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
