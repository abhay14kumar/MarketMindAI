package com.marketmind.marketdata.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.marketdata.application.PortfolioPriceInput;
import com.marketmind.marketdata.application.PriceFeedRepository;
import com.marketmind.marketdata.application.PriceFeedService;
import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.dto.ManualPriceRequest;
import com.marketmind.marketdata.mapper.PriceFeedMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PriceFeedControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PriceFeedService service = new PriceFeedService(
                new InMemoryRepository(),
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PriceFeedController(service, null, new PriceFeedMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldRecordManualPrice() throws Exception {
        ManualPriceRequest request = new ManualPriceRequest(
                "INFY", Exchange.NSE,
                new BigDecimal("1600"), new BigDecimal("1580"),
                com.marketmind.marketdata.domain.PriceSource.MANUAL);

        mockMvc.perform(post("/api/v1/market/prices/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("INFY"))
                .andExpect(jsonPath("$.lastPrice").value(1600));
    }

    private static final class InMemoryRepository implements PriceFeedRepository {

        private final List<MarketInstrument> instruments = new ArrayList<>();

        @Override
        public MarketInstrument saveInstrument(MarketInstrument instrument) {
            instruments.add(instrument);
            return instrument;
        }

        @Override
        public Optional<MarketInstrument> findInstrument(String symbol, Exchange exchange) {
            return instruments.stream().filter(item -> item.symbol().equals(symbol)).findFirst();
        }

        @Override
        public List<MarketInstrument> findInstruments() {
            return List.copyOf(instruments);
        }

        @Override
        public PriceSnapshot saveSnapshot(PriceSnapshot snapshot, UUID feedJobId) {
            return snapshot;
        }

        @Override
        public Optional<PriceSnapshot> findLatest(String symbol) {
            return Optional.empty();
        }

        @Override
        public List<PriceSnapshot> findAllLatest() {
            return List.of();
        }

        @Override
        public PriceFeedJob saveJob(PriceFeedJob job) {
            return job;
        }

        @Override
        public Optional<PriceFeedJob> findLatestProviderJob() {
            return Optional.empty();
        }

        @Override
        public List<PortfolioPriceInput> findPortfolioPriceInputs() {
            return List.of();
        }
    }
}
