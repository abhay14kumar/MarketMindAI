package com.marketmind.pipeline.api;

import java.util.List;
import java.util.UUID;

import com.marketmind.common.observability.CorrelationIdFilter;
import com.marketmind.pipeline.application.PipelineOrchestrator;
import com.marketmind.pipeline.application.PipelineStartCommand;
import com.marketmind.pipeline.dto.PageResponse;
import com.marketmind.pipeline.dto.PipelineEventResponse;
import com.marketmind.pipeline.dto.PipelineJobResponse;
import com.marketmind.pipeline.dto.PipelineMetricsResponse;
import com.marketmind.pipeline.dto.PipelineStartRequest;
import com.marketmind.pipeline.mapper.PipelineOrchestrationMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline")
@Tag(
        name = "Pipeline Orchestration",
        description = "Autonomous discovery-to-AI-ready pipeline jobs")
public class PipelineOrchestrationController {

    private final PipelineOrchestrator orchestrator;
    private final PipelineOrchestrationMapper mapper;

    public PipelineOrchestrationController(
            PipelineOrchestrator orchestrator,
            PipelineOrchestrationMapper mapper) {
        this.orchestrator = orchestrator;
        this.mapper = mapper;
    }

    @PostMapping("/start")
    @Operation(summary = "Start an autonomous pipeline job")
    public ResponseEntity<PipelineJobResponse> start(
            @Valid @RequestBody PipelineStartRequest request,
            @RequestHeader(
                    name = CorrelationIdFilter.CORRELATION_ID_HEADER,
                    required = false) String correlationId) {
        var job = orchestrator.start(new PipelineStartCommand(
                request.discoveredDocumentId(), request.documentId(), correlationId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(job));
    }

    @PostMapping("/jobs/{id}/retry")
    @Operation(summary = "Retry a failed autonomous pipeline job")
    public ResponseEntity<PipelineJobResponse> retry(
            @PathVariable UUID id,
            @RequestHeader(
                    name = CorrelationIdFilter.CORRELATION_ID_HEADER,
                    required = false) String correlationId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(orchestrator.retry(id, correlationId)));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List autonomous pipeline jobs")
    public PageResponse<PipelineJobResponse> jobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toResponse(orchestrator.getJobs(page, size));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get pipeline job, stages, and events")
    public PipelineJobResponse job(@PathVariable UUID id) {
        return mapper.toResponse(orchestrator.getJob(id));
    }

    @GetMapping("/jobs/{id}/events")
    @Operation(summary = "List pipeline job events")
    public List<PipelineEventResponse> events(@PathVariable UUID id) {
        return orchestrator.getEvents(id).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get aggregate pipeline metrics")
    public PipelineMetricsResponse metrics() {
        return mapper.toResponse(orchestrator.getMetrics());
    }
}
