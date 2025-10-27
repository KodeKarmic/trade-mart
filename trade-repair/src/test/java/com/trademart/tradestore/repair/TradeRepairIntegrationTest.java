package com.trademart.tradestore.repair;

import com.trademart.tradestore.repair.dto.FailedTrade;
import com.trademart.tradestore.repair.repository.FailedTradeRepository;
import com.trademart.tradestore.repair.service.TradeRepairService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

@Testcontainers
@SpringBootTest
class TradeRepairIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:6.0.8");

    static WireMockServer wireMock = new WireMockServer(0);
    static {
        // Start WireMock early so its port is available when DynamicPropertySource runs
        wireMock.start();
    }

    @Autowired
    FailedTradeRepository repository;

    @Autowired
    TradeRepairService service;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // WireMock is started in the static initializer, so port is available here
        registry.add("spring.data.mongodb.uri", () -> mongo.getReplicaSetUrl());
        registry.add("trade.store.resubmit.url", () -> "http://localhost:" + wireMock.port() + "/trades");
    }

    @AfterAll
    static void stopWiremock() {
        if (wireMock != null) wireMock.stop();
    }

    @Test
    void endToEnd_resubmit_uses_max_version_and_posts_updated_trade() throws Exception {
        // arrange: prepare a failed trade in Mongo
        FailedTrade ft = new FailedTrade();
        ft.setId("it-1");
        ft.setTradeId("IT-1");
        ft.setPayload("{\"tradeId\":\"IT-1\", \"price\":123.45}");
        repository.save(ft);

        // stub max-version endpoint to return 7
        String maxPath = "/trades/IT-1/max-version";
        wireMock.stubFor(get(urlEqualTo(maxPath)).willReturn(aResponse().withStatus(200).withBody("7")));

        // stub post resubmit endpoint
        String postPath = "/trades";
        wireMock.stubFor(post(urlEqualTo(postPath)).willReturn(aResponse().withStatus(201)));

        // act
        boolean ok = service.resubmit(ft.getId());

    // assert
    assert(ok);
    // configure static client to talk to the running WireMock instance then verify
    com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMock.port());
    verify(postRequestedFor(urlEqualTo(postPath)).withRequestBody(equalToJson("{\"tradeId\":\"IT-1\", \"price\":123.45, \"version\":8}")));
    assert(repository.findById(ft.getId()).isEmpty());
    }
}
