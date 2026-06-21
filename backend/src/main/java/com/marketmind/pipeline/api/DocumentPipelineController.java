package com.marketmind.pipeline.api;

import java.util.UUID;

import com.marketmind.pipeline.application.DocumentPipelineService;
import com.marketmind.pipeline.dto.PageResponse;
import com.marketmind.pipeline.dto.PipelineRunResponse;
import com.marketmind.pipeline.mapper.DocumentPipelineMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipeline")
@Tag(
        name = "Document Pipeline",
        description = "Automated document extraction and embedding workflows")
public class DocumentPipelineController {

    private final DocumentPipelineService service;
    private final DocumentPipelineMapper mapper;

    public DocumentPipelineController(
            DocumentPipelineService service,
            DocumentPipelineMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/runs")
    @Operation(summary = "List document pipeline runs")
    public PageResponse<PipelineRunResponse> getRuns(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toResponse(service.getRuns(page, size));
    }

    @GetMapping("/runs/{id}")
    @Operation(summary = "Get a document pipeline run and its step timeline")
    public PipelineRunResponse getRun(@PathVariable UUID id) {
        return mapper.toResponse(service.getRun(id));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "Get the latest pipeline run for a document")
    public PipelineRunResponse getDocumentPipeline(
            @PathVariable UUID documentId) {
        return mapper.toResponse(service.getByDocumentId(documentId));
    }

    @PostMapping("/documents/{documentId}/retry")
    @Operation(summary = "Retry failed document pipeline steps")
    public PipelineRunResponse retry(@PathVariable UUID documentId) {
        var run = service.retry(documentId);
        return mapper.toResponse(service.getRun(run.id()));
    }
}
