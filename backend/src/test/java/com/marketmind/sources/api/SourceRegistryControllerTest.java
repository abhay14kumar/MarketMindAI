package com.marketmind.sources.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.sources.application.SourceRegistryService;
import com.marketmind.sources.application.SourceValidationService;
import com.marketmind.sources.domain.AuthenticationType;
import com.marketmind.sources.domain.CapabilityType;
import com.marketmind.sources.domain.RefreshFrequency;
import com.marketmind.sources.domain.SourceStatus;
import com.marketmind.sources.domain.SourceType;
import com.marketmind.sources.domain.CapabilityStatus;
import com.marketmind.sources.dto.SourceRegistryRequest;
import com.marketmind.sources.infrastructure.InMemorySourceRegistryRepository;
import com.marketmind.sources.mapper.SourceRegistryMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SourceRegistryControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID NSE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        InMemorySourceRegistryRepository repository = new InMemorySourceRegistryRepository();
        SourceRegistryService service = new SourceRegistryService(repository, clock);
        SourceValidationService validationService = new SourceValidationService(
                repository,
                sourceUrl -> new com.marketmind.sources.application.ReachabilityChecker
                        .ReachabilityResult(true, 200, 125, "Reachable."),
                sourceUrl -> new com.marketmind.sources.application.RobotsTxtChecker
                        .RobotsTxtResult(true, 200, "robots.txt available."),
                sourceUrl -> new com.marketmind.sources.application.PdfCapabilityChecker
                        .PdfCapabilityResult(CapabilityStatus.UNKNOWN, "No sample PDF."),
                clock);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SourceRegistryController(
                                service,
                                validationService,
                                new SourceRegistryMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListDefaultSources() throws Exception {
        mockMvc.perform(get("/api/v1/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(9))
                .andExpect(jsonPath("$.totalElements").value(9));
    }

    @Test
    void shouldGetSourceById() throws Exception {
        mockMvc.perform(get("/api/v1/sources/{id}", NSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NSE"))
                .andExpect(jsonPath("$.authenticationType").value("SESSION"));
    }

    @Test
    void shouldCreateAndUpdateSource() throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("COMPANY_IR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMPANY_IR"))
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/api/v1/sources/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("COMPANY_IR_UPDATED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMPANY_IR_UPDATED"));
    }

    @Test
    void shouldRunSourceValidation() throws Exception {
        mockMvc.perform(post("/api/v1/sources/{id}/validate", NSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.robotsTxtAvailable").value(true))
                .andExpect(jsonPath("$.pdfCapabilityStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.latencyMs").isNumber());
    }

    @Test
    void shouldListHealthAndCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/sources/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));
        mockMvc.perform(get("/api/v1/sources/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    void shouldDeleteSource() throws Exception {
        mockMvc.perform(delete("/api/v1/sources/{id}", NSE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectInvalidSource() throws Exception {
        SourceRegistryRequest invalid = new SourceRegistryRequest(
                "",
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                "http://insecure.example.com",
                null,
                null,
                null,
                Set.of(),
                true,
                0,
                null);

        mockMvc.perform(post("/api/v1/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private SourceRegistryRequest request(String code) {
        return new SourceRegistryRequest(
                code,
                "Company Investor Relations",
                "Example Company",
                "Company IR source.",
                SourceType.COMPANY_INVESTOR_RELATIONS,
                SourceStatus.ACTIVE,
                AuthenticationType.NONE,
                RefreshFrequency.DAILY,
                "https://example.com",
                "https://example.com/robots.txt",
                "https://example.com/investors",
                null,
                Set.of(CapabilityType.INVESTOR_RELATIONS_DOCUMENTS),
                true,
                50,
                new java.math.BigDecimal("0.9000"));
    }
}
