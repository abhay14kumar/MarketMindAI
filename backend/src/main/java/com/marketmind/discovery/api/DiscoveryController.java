package com.marketmind.discovery.api;

import java.util.UUID;

import com.marketmind.discovery.application.DiscoveryDocumentFilter;
import com.marketmind.discovery.application.DiscoveryService;
import com.marketmind.discovery.domain.DiscoveredDocumentStatus;
import com.marketmind.discovery.domain.DiscoveredDocumentType;
import com.marketmind.discovery.domain.DiscoverySourceType;
import com.marketmind.discovery.dto.DiscoveredDocumentResponse;
import com.marketmind.discovery.dto.DiscoveryJobDetailResponse;
import com.marketmind.discovery.dto.DiscoveryJobResponse;
import com.marketmind.discovery.dto.DiscoveryRunRequest;
import com.marketmind.discovery.dto.PageResponse;
import com.marketmind.discovery.mapper.DiscoveryMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery")
@Tag(
        name = "Document Discovery",
        description = "Discover and classify official document links without downloading them")
public class DiscoveryController {

    private final DiscoveryService service;
    private final DiscoveryMapper mapper;

    public DiscoveryController(DiscoveryService service, DiscoveryMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/run")
    @Operation(summary = "Run document-link discovery for one source")
    public DiscoveryJobResponse run(@Valid @RequestBody DiscoveryRunRequest request) {
        return mapper.toResponse(service.run(mapper.toCommand(request)).job());
    }

    @GetMapping("/jobs")
    @Operation(summary = "List discovery jobs")
    public PageResponse<DiscoveryJobResponse> getJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toJobPage(service.getJobs(page, size));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get a discovery job with source-run details")
    public DiscoveryJobDetailResponse getJob(@PathVariable UUID id) {
        return mapper.toResponse(service.getJob(id));
    }

    @GetMapping("/documents")
    @Operation(summary = "List discovered documents with optional filters")
    public PageResponse<DiscoveredDocumentResponse> getDocuments(
            @RequestParam(required = false) DiscoveredDocumentStatus status,
            @RequestParam(required = false) DiscoverySourceType sourceType,
            @RequestParam(required = false) String companySymbol,
            @RequestParam(required = false) DiscoveredDocumentType documentType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toDocumentPage(service.getDocuments(
                new DiscoveryDocumentFilter(
                        status, sourceType, companySymbol, documentType),
                page,
                size));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get discovered document metadata")
    public DiscoveredDocumentResponse getDocument(@PathVariable UUID id) {
        return mapper.toResponse(service.getDocument(id));
    }

    @PostMapping("/documents/{id}/ignore")
    @Operation(summary = "Mark a discovered document as ignored")
    public DiscoveredDocumentResponse ignore(@PathVariable UUID id) {
        return mapper.toResponse(service.ignore(id));
    }

    @PostMapping("/documents/{id}/mark-existing")
    @Operation(summary = "Mark a discovered document as already existing")
    public DiscoveredDocumentResponse markExisting(@PathVariable UUID id) {
        return mapper.toResponse(service.markExisting(id));
    }
}
