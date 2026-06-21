package com.marketmind.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketmind.ai.infrastructure.AiInfrastructureException;
import com.marketmind.common.observability.CorrelationIdFilter;
import com.marketmind.documents.domain.DocumentType;
import com.marketmind.marketdata.application.PriceProviderException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void shouldReturnFieldLevelValidationErrorsAndCorrelationId()
            throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","documentType":"ANNUAL_REPORT"}
                                """)
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER,
                                "validation-123"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(
                        CorrelationIdFilter.CORRELATION_ID_HEADER,
                        "validation-123"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.legacyCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").value("validation-123"))
                .andExpect(jsonPath("$.requestId").value("validation-123"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void shouldIdentifyInvalidEnumInJsonBody() throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"source","documentType":"testing"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("documentType"))
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value 'testing' for documentType. "
                                + "Allowed values are: ANNUAL_REPORT, "
                                + "QUARTERLY_RESULT, EXCHANGE_FILING, "
                                + "CONCALL_TRANSCRIPT, INVESTOR_PRESENTATION, "
                                + "OTHER."));
    }

    @Test
    void shouldIdentifyInvalidEnumRequestParameter() throws Exception {
        mockMvc.perform(get("/errors/enum")
                        .param("documentType", "testing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("documentType"))
                .andExpect(jsonPath("$.detail").value(
                        "Invalid value 'testing' for documentType. "
                                + "Allowed values are: ANNUAL_REPORT, "
                                + "QUARTERLY_RESULT, EXCHANGE_FILING, "
                                + "CONCALL_TRANSCRIPT, INVESTOR_PRESENTATION, "
                                + "OTHER."));
    }

    @Test
    void shouldIdentifyMalformedJson() throws Exception {
        mockMvc.perform(post("/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.legacyCode").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void shouldMapUploadLimitFailure() throws Exception {
        mockMvc.perform(get("/errors/upload"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.legacyCode").value("UPLOAD_SIZE_EXCEEDED"));
    }

    @Test
    void shouldMapQdrantFailureWithoutExposingLowLevelMessage()
            throws Exception {
        mockMvc.perform(get("/errors/qdrant"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("QDRANT_FAILURE"))
                .andExpect(jsonPath("$.detail")
                        .value("The vector search service is temporarily unavailable."));
    }

    @Test
    void shouldMapExternalProviderFailure() throws Exception {
        mockMvc.perform(get("/errors/provider"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_FAILURE"));
    }

    private record ProbeRequest(
            @NotBlank String name,
            DocumentType documentType) {
    }

    @RestController
    @RequestMapping("/errors")
    private static final class ErrorProbeController {

        @PostMapping("/validate")
        ProbeRequest validate(@Valid @RequestBody ProbeRequest request) {
            return request;
        }

        @GetMapping("/enum")
        DocumentType enumParameter(
                @RequestParam DocumentType documentType) {
            return documentType;
        }

        @GetMapping("/upload")
        void upload() {
            throw new MaxUploadSizeExceededException(1024);
        }

        @GetMapping("/qdrant")
        void qdrant() {
            throw new AiInfrastructureException(
                    ErrorCode.QDRANT_FAILURE,
                    "Connection refused at internal endpoint.");
        }

        @GetMapping("/provider")
        void provider() {
            throw new PriceProviderException("Provider timed out.");
        }
    }
}
