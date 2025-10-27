package com.trademart.tradestore.repair.service;

import com.trademart.tradestore.repair.dto.FailedTrade;
import com.trademart.tradestore.repair.repository.FailedTradeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TradeRepairServiceTest {

    private FailedTradeRepository repository;
    private TradeRepairService service;

    @BeforeEach
    void setUp() {
    repository = mock(FailedTradeRepository.class);
    }

    private void injectResubmitUrl(String url) throws Exception {
        java.lang.reflect.Field urlField = TradeRepairService.class.getDeclaredField("resubmitUrl");
        urlField.setAccessible(true);
        urlField.set(service, url);
    }

    @Test
    void resubmit_sets_version_to_max_plus_one_when_max_exists() throws Exception {
        // prepare failed trade payload without version
        FailedTrade ft = new FailedTrade();
        ft.setId("1");
        ft.setPayload("{\"tradeId\":\"T-1\", \"price\":100.0}");
        when(repository.findById("1")).thenReturn(Optional.of(ft));

        // spy RestTemplate used by the service; create service with injected dependencies
        RestTemplate spyRest = Mockito.spy(new RestTemplate());
        // mock getForEntity for max-version
        doReturn(new ResponseEntity<>("5", HttpStatus.OK)).when(spyRest).getForEntity("http://trade-store:8080/trades/T-1/max-version", String.class);
        // mock postForEntity for resubmit
        doReturn(new ResponseEntity<>("ok", HttpStatus.CREATED)).when(spyRest).postForEntity(eq("http://trade-store:8080/trades"), any(), eq(String.class));

        service = new TradeRepairService(repository, spyRest, "http://trade-store:8080/trades");

        boolean result = service.resubmit("1");
        assertThat(result).isTrue();

        // capture posted body to verify version was set to 6
        ArgumentCaptor<org.springframework.http.HttpEntity> captor = ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(spyRest).postForEntity(eq("http://trade-store:8080/trades"), captor.capture(), eq(String.class));

        String postedJson = (String) captor.getValue().getBody();
        ObjectMapper mapper = new ObjectMapper();
        var map = mapper.readValue(postedJson, java.util.Map.class);
        assertThat(map.get("version")).isEqualTo(6);

        verify(repository).deleteById("1");
    }

    @Test
    void resubmit_defaults_version_to_one_when_no_existing() throws Exception {
        FailedTrade ft = new FailedTrade();
        ft.setId("2");
        ft.setPayload("{\"tradeId\":\"T-2\", \"price\":200.0}");
        when(repository.findById("2")).thenReturn(Optional.of(ft));

        RestTemplate spyRest = Mockito.spy(new RestTemplate());
        // simulate 204 or no content by returning 204
        doReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT)).when(spyRest).getForEntity("http://trade-store:8080/trades/T-2/max-version", String.class);
        doReturn(new ResponseEntity<>("ok", HttpStatus.CREATED)).when(spyRest).postForEntity(eq("http://trade-store:8080/trades"), any(), eq(String.class));

        service = new TradeRepairService(repository, spyRest, "http://trade-store:8080/trades");

        boolean result = service.resubmit("2");
        assertThat(result).isTrue();

        ArgumentCaptor<org.springframework.http.HttpEntity> captor = ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(spyRest).postForEntity(eq("http://trade-store:8080/trades"), captor.capture(), eq(String.class));

        String postedJson = (String) captor.getValue().getBody();
        ObjectMapper mapper = new ObjectMapper();
        var map = mapper.readValue(postedJson, java.util.Map.class);
        assertThat(map.get("version")).isEqualTo(1);

        verify(repository).deleteById("2");
    }

    @Test
    void resubmit_handles_max_version_query_failure_and_uses_payload_version_or_one() throws Exception {
        FailedTrade ft = new FailedTrade();
        ft.setId("3");
        // include an explicit version in payload
        ft.setPayload("{\"tradeId\":\"T-3\", \"version\":2, \"price\":300.0}");
        when(repository.findById("3")).thenReturn(Optional.of(ft));

        RestTemplate spyRest = Mockito.spy(new RestTemplate());
        // simulate getForEntity throwing exception (e.g., network)
        doThrow(new RuntimeException("down")).when(spyRest).getForEntity("http://trade-store:8080/trades/T-3/max-version", String.class);
        doReturn(new ResponseEntity<>("ok", HttpStatus.CREATED)).when(spyRest).postForEntity(eq("http://trade-store:8080/trades"), any(), eq(String.class));

        service = new TradeRepairService(repository, spyRest, "http://trade-store:8080/trades");

        boolean result = service.resubmit("3");
        assertThat(result).isTrue();

        ArgumentCaptor<org.springframework.http.HttpEntity> captor = ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(spyRest).postForEntity(eq("http://trade-store:8080/trades"), captor.capture(), eq(String.class));

        String postedJson = (String) captor.getValue().getBody();
        ObjectMapper mapper = new ObjectMapper();
        var map = mapper.readValue(postedJson, java.util.Map.class);
        // payload had version 2 and we couldn't get max; implementation should preserve provided version (or set 1 if missing)
        assertThat(map.get("version")).isEqualTo(2);

        verify(repository).deleteById("3");
    }
}
