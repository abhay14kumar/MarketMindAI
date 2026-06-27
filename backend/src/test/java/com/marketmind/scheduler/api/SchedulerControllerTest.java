package com.marketmind.scheduler.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.common.exception.GlobalExceptionHandler;
import com.marketmind.scheduler.application.SchedulerService;
import com.marketmind.scheduler.application.SchedulerJobExecutor;
import com.marketmind.scheduler.domain.SchedulerRunStatus;
import com.marketmind.scheduler.domain.SchedulerJobStatus;
import com.marketmind.scheduler.domain.SchedulerType;
import com.marketmind.scheduler.dto.SchedulerJobRequest;
import com.marketmind.scheduler.infrastructure.MockSchedulerRepository;
import com.marketmind.scheduler.mapper.SchedulerMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SchedulerControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-19T12:00:00Z");
    private static final UUID ACTIVE_JOB_ID =
            UUID.fromString("61000000-0000-0000-0000-000000000001");

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SchedulerService service = new SchedulerService(
                new MockSchedulerRepository(),
                job -> new SchedulerJobExecutor.ExecutionResult(
                        SchedulerRunStatus.COMPLETED,
                        "Generic HTML crawler could not discover documents from NSE because "
                                + "the page may be dynamic or protected. NSE-specific crawler is planned.",
                        null,
                        0,
                        0),
                Clock.fixed(NOW, ZoneOffset.UTC));
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new SchedulerController(service, new SchedulerMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void shouldListPaginatedJobs() throws Exception {
        mockMvc.perform(get("/api/v1/scheduler/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldGetJobById() throws Exception {
        mockMvc.perform(get("/api/v1/scheduler/jobs/{id}", ACTIVE_JOB_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NSE Filing Discovery"))
                .andExpect(jsonPath("$.schedulerType").value("NSE_FILINGS"));
    }

    @Test
    void shouldCreateJob() throws Exception {
        mockMvc.perform(post("/api/v1/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("BSE Filing Discovery"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("BSE Filing Discovery"));
    }

    @Test
    void shouldUpdateJob() throws Exception {
        SchedulerJobRequest request = new SchedulerJobRequest(
                "NSE Filing Discovery Updated",
                "Updated scheduler configuration.",
                SchedulerType.NSE_FILINGS,
                SchedulerJobStatus.PAUSED,
                "0 0 * * * *",
                "Asia/Kolkata",
                Map.of());

        mockMvc.perform(put("/api/v1/scheduler/jobs/{id}", ACTIVE_JOB_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTIVE_JOB_ID.toString()))
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void shouldTriggerActiveJobWithMeaningfulMessage() throws Exception {
        mockMvc.perform(post("/api/v1/scheduler/jobs/{id}/trigger", ACTIVE_JOB_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.resultSummary").value(
                        "Generic HTML crawler could not discover documents from NSE because "
                                + "the page may be dynamic or protected. NSE-specific crawler is planned."))
                .andExpect(jsonPath("$.schedulerJobId").value(ACTIVE_JOB_ID.toString()));
    }

    @Test
    void shouldListRuns() throws Exception {
        mockMvc.perform(get("/api/v1/scheduler/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldRejectInvalidRequest() throws Exception {
        SchedulerJobRequest invalid = new SchedulerJobRequest(
                "",
                null,
                SchedulerType.CUSTOM,
                SchedulerJobStatus.ACTIVE,
                "",
                "",
                Map.of());

        mockMvc.perform(post("/api/v1/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.legacyCode").value("VALIDATION_ERROR"));
    }

    private SchedulerJobRequest request(String name) {
        return new SchedulerJobRequest(
                name,
                "Discovers official BSE filings.",
                SchedulerType.BSE_FILINGS,
                SchedulerJobStatus.ACTIVE,
                "0 0/30 * * * *",
                "Asia/Kolkata",
                Map.of("sourceCode", "BSE"));
    }
}
