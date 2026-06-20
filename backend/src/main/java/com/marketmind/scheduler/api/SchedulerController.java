package com.marketmind.scheduler.api;

import java.util.UUID;

import com.marketmind.scheduler.application.SchedulerService;
import com.marketmind.scheduler.dto.PageResponse;
import com.marketmind.scheduler.dto.SchedulerJobRequest;
import com.marketmind.scheduler.dto.SchedulerJobResponse;
import com.marketmind.scheduler.dto.SchedulerRunResponse;
import com.marketmind.scheduler.mapper.SchedulerMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduler")
@Tag(name = "Scheduler", description = "Manage ingestion scheduler jobs and execution runs")
public class SchedulerController {

    private final SchedulerService schedulerService;
    private final SchedulerMapper mapper;

    public SchedulerController(SchedulerService schedulerService, SchedulerMapper mapper) {
        this.schedulerService = schedulerService;
        this.mapper = mapper;
    }

    @GetMapping("/jobs")
    @Operation(summary = "List scheduler jobs", description = "Returns paginated mock jobs.")
    @ApiResponse(
            responseCode = "200",
            description = "Scheduler jobs returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<SchedulerJobResponse> getJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toJobPage(schedulerService.getJobs(page, size));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get scheduler job", description = "Returns a scheduler job by identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduler job returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduler job not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SchedulerJobResponse getJob(@PathVariable UUID id) {
        return mapper.toResponse(schedulerService.getJob(id));
    }

    @PostMapping("/jobs")
    @Operation(
            summary = "Create scheduler job",
            description = "Creates scheduler metadata without starting real scheduling.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Scheduler job created"),
        @ApiResponse(
                responseCode = "409",
                description = "Scheduler job name already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SchedulerJobResponse> createJob(
            @Valid @RequestBody SchedulerJobRequest request) {
        SchedulerJobResponse response = mapper.toResponse(
                schedulerService.createJob(mapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/jobs/{id}")
    @Operation(
            summary = "Replace scheduler job",
            description = "Replaces editable scheduler job configuration.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduler job updated"),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduler job not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Scheduler job name already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SchedulerJobResponse updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody SchedulerJobRequest request) {
        return mapper.toResponse(schedulerService.updateJob(id, mapper.toCommand(request)));
    }

    @PostMapping("/jobs/{id}/trigger")
    @Operation(
            summary = "Trigger scheduler job",
            description = "Creates a queued mock run; no external or scheduled work is executed.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Mock scheduler run accepted"),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduler job not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Scheduler job is not active",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SchedulerRunResponse> triggerJob(@PathVariable UUID id) {
        return ResponseEntity.accepted()
                .body(mapper.toResponse(schedulerService.triggerJob(id)));
    }

    @GetMapping("/runs")
    @Operation(summary = "List scheduler runs", description = "Returns paginated mock runs.")
    @ApiResponse(
            responseCode = "200",
            description = "Scheduler runs returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<SchedulerRunResponse> getRuns(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toRunPage(schedulerService.getRuns(page, size));
    }
}
