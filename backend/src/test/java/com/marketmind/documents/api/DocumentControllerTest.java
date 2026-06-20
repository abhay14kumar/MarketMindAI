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
import com.marketmind.documents.domain.SourceType;
import com.marketmind.documents.dto.CreateDocumentSourceRequest;
import com.marketmind.documents.dto.DownloadDocumentRequest;
import com.marketmind.documents.infrastructure.MockDocumentCatalog;
import com.marketmind.documents.mapper.DocumentMapper;

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
    private static final UUID FAILED_JOB_ID =
            UUID.fromString("55000000-0000-0000-0000-000000000002");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        DocumentService service = new DocumentService(new MockDocumentCatalog(), clock);
        DocumentMapper mapper = new DocumentMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentController(service, mapper),
                        new DocumentSourceController(service, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListPaginatedDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.content[0].sourceCode").value("NSE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetDocumentById() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportingPeriod").value("FY2025-26"));
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
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.attemptCount").value(0));
    }

    @Test
    void shouldRetryFailedDownloadJob() throws Exception {
        mockMvc.perform(post("/api/v1/documents/retry/{jobId}", FAILED_JOB_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.retryOfJobId").value(FAILED_JOB_ID.toString()));
    }

    @Test
    void shouldListPaginatedDownloadJobs() throws Exception {
        mockMvc.perform(get("/api/v1/documents/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
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
    void shouldListSources() throws Exception {
        mockMvc.perform(get("/api/v1/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NSE"))
                .andExpect(jsonPath("$[0].sourceType").value("EXCHANGE"));
    }

    @Test
    void shouldCreateSource() throws Exception {
        CreateDocumentSourceRequest request = new CreateDocumentSourceRequest(
                "SEBI",
                "Securities and Exchange Board of India",
                SourceType.REGULATOR,
                "https://www.sebi.gov.in",
                true);

        mockMvc.perform(post("/api/v1/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SEBI"))
                .andExpect(jsonPath("$.sourceType").value("REGULATOR"));
    }
}
