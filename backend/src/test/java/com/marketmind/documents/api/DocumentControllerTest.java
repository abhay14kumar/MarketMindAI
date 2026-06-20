package com.marketmind.documents.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.documents.application.DocumentService;
import com.marketmind.documents.infrastructure.MockDocumentCatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID DOCUMENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");
    private static final UUID SOURCE_ID =
            UUID.fromString("51000000-0000-0000-0000-000000000001");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        DocumentService service = new DocumentService(new MockDocumentCatalog(), clock);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentController(service, new DocumentApiMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$[0].sourceCode").value("NSE"))
                .andExpect(jsonPath("$[0].documentType").value("ANNUAL_REPORT"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void shouldGetDocumentById() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.reportingPeriod").value("FY2025-26"));
    }

    @Test
    void shouldReturnNotFoundForUnknownDocument() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/documents/{id}",
                        UUID.fromString("53000000-0000-0000-0000-000000000099")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldQueueMockDownloadJob() throws Exception {
        DownloadDocumentRequest request = new DownloadDocumentRequest(
                DOCUMENT_ID,
                SOURCE_ID,
                "https://example.invalid/annual-report.pdf",
                3);

        mockMvc.perform(post("/api/v1/documents/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attemptCount").value(0))
                .andExpect(jsonPath("$.maxAttempts").value(3));
    }

    @Test
    void shouldRejectNonHttpsDownloadUrl() throws Exception {
        DownloadDocumentRequest request = new DownloadDocumentRequest(
                null,
                SOURCE_ID,
                "http://example.invalid/annual-report.pdf",
                3);

        mockMvc.perform(post("/api/v1/documents/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldListDownloadJobs() throws Exception {
        mockMvc.perform(get("/api/v1/documents/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].sourceId").value(SOURCE_ID.toString()));
    }
}
