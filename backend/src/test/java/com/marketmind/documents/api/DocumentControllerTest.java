package com.marketmind.documents.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.documents.application.DocumentDownloadService;
import com.marketmind.documents.application.DocumentService;
import com.marketmind.documents.application.Downloader;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.documents.domain.SourceType;
import com.marketmind.documents.dto.CreateDocumentSourceRequest;
import com.marketmind.documents.dto.DownloadDocumentRequest;
import com.marketmind.documents.infrastructure.DefaultVersionManager;
import com.marketmind.documents.infrastructure.DocumentDownloadProperties;
import com.marketmind.documents.infrastructure.DocumentStorageProperties;
import com.marketmind.documents.infrastructure.InMemoryDocumentCatalog;
import com.marketmind.documents.infrastructure.LocalFileStorageProvider;
import com.marketmind.documents.infrastructure.Sha256ChecksumService;
import com.marketmind.documents.mapper.DocumentMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID DOCUMENT_ID =
            UUID.fromString("53000000-0000-0000-0000-000000000001");

    @TempDir
    Path storageRoot;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        InMemoryDocumentCatalog catalog = new InMemoryDocumentCatalog();
        DocumentService documentService = new DocumentService(catalog, clock);
        DocumentDownloadService downloadService = new DocumentDownloadService(
                catalog,
                new StubDownloader(),
                new LocalFileStorageProvider(new DocumentStorageProperties(storageRoot)),
                new Sha256ChecksumService(),
                new DefaultVersionManager(catalog, clock),
                new DocumentDownloadProperties(30, 1),
                documentId -> {
                },
                clock);
        DocumentMapper mapper = new DocumentMapper();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentController(documentService, downloadService, mapper),
                        new DocumentSourceController(documentService, mapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListPaginatedDocuments() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldGetDocumentById() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportingPeriod").value("FY2025-26"));
    }

    @Test
    void shouldExecuteDownloadPipelineThroughApi() throws Exception {
        mockMvc.perform(post("/api/v1/documents/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.job.status").value("COMPLETED"))
                .andExpect(jsonPath("$.document.documentType").value("ANNUAL_REPORT"))
                .andExpect(jsonPath("$.document.fiscalYear").value(2026))
                .andExpect(jsonPath("$.version.versionNumber").value(1))
                .andExpect(jsonPath("$.version.checksumSha256").isNotEmpty());
    }

    @Test
    void shouldListDownloadJobs() throws Exception {
        mockMvc.perform(get("/api/v1/documents/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldListDocumentVersions() throws Exception {
        mockMvc.perform(get("/api/v1/documents/versions/{documentId}", DOCUMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(1));
    }

    @Test
    void shouldRejectUnsupportedProtocol() throws Exception {
        DownloadDocumentRequest request = new DownloadDocumentRequest(
                "ftp://example.invalid/report.pdf",
                "Annual Report",
                DocumentType.ANNUAL_REPORT,
                null,
                null,
                2026,
                null);

        mockMvc.perform(post("/api/v1/documents/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.legacyCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldListAndCreateSources() throws Exception {
        mockMvc.perform(get("/api/v1/document-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NSE"));

        CreateDocumentSourceRequest sourceRequest = new CreateDocumentSourceRequest(
                "SEBI",
                "Securities and Exchange Board of India",
                SourceType.REGULATOR,
                "https://www.sebi.gov.in",
                true);
        mockMvc.perform(post("/api/v1/document-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sourceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SEBI"));
    }

    private DownloadDocumentRequest request() {
        return new DownloadDocumentRequest(
                "https://example.invalid/reliance-annual-report.pdf",
                "Reliance Industries Annual Report",
                DocumentType.ANNUAL_REPORT,
                null,
                null,
                2026,
                null);
    }

    private static final class StubDownloader implements Downloader {

        @Override
        public DownloadResult download(DownloadRequest request) {
            byte[] content = "integration-style-document".getBytes(StandardCharsets.UTF_8);
            try {
                Path temporaryFile = Files.createTempFile("api-download-", ".pdf");
                Files.write(temporaryFile, content);
                return new DownloadResult(
                        temporaryFile,
                        "application/pdf",
                        NOW,
                        "reliance-annual-report.pdf",
                        content.length);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
