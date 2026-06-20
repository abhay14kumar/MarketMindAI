package com.marketmind.marketdata.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.marketdata.application.MarketDataService;
import com.marketmind.marketdata.infrastructure.MockMarketDataProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarketDataControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        MarketDataService service = new MarketDataService(new MockMarketDataProvider(clock), clock);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MarketDataController(service, new MarketDataApiMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnMarketIndexes() throws Exception {
        mockMvc.perform(get("/api/v1/market/indexes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("NIFTY50"))
                .andExpect(jsonPath("$[0].exchange").value("NSE"))
                .andExpect(jsonPath("$[1].symbol").value("SENSEX"));
    }

    @Test
    void shouldReturnNormalizedDailyPrices() throws Exception {
        mockMvc.perform(get("/api/v1/market/prices/reliance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].symbol").value("RELIANCE"))
                .andExpect(jsonPath("$[0].source").value("MARKETMIND_MOCK"));
    }

    @Test
    void shouldRejectInvalidSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/market/prices/INVALID_SYMBOL"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnMarketDataHealth() throws Exception {
        mockMvc.perform(get("/api/v1/market/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.provider").value("MARKETMIND_MOCK"))
                .andExpect(jsonPath("$.mode").value("MOCK"))
                .andExpect(jsonPath("$.asOf").exists());
    }
}
