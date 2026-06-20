package com.marketmind.sources.api;

import java.util.List;
import java.util.UUID;

import com.marketmind.sources.application.SourceRegistryService;
import com.marketmind.sources.dto.PageResponse;
import com.marketmind.sources.dto.SourceCapabilityResponse;
import com.marketmind.sources.dto.SourceHealthResponse;
import com.marketmind.sources.dto.SourceRegistryRequest;
import com.marketmind.sources.dto.SourceRegistryResponse;
import com.marketmind.sources.dto.SourceValidationResponse;
import com.marketmind.sources.mapper.SourceRegistryMapper;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sources")
@Tag(name = "Source Registry", description = "Manage governed financial data sources")
public class SourceRegistryController {

    private final SourceRegistryService service;
    private final SourceRegistryMapper mapper;

    public SourceRegistryController(SourceRegistryService service, SourceRegistryMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List sources", description = "Returns the paginated source registry.")
    @ApiResponse(
            responseCode = "200",
            description = "Sources returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<SourceRegistryResponse> getSources(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toPage(service.getSources(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get source", description = "Returns a source by identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Source returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Source not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SourceRegistryResponse getSource(@PathVariable UUID id) {
        return mapper.toResponse(service.getSource(id));
    }

    @PostMapping
    @Operation(
            summary = "Create source",
            description = "Registers source metadata and capabilities without credentials.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Source created"),
        @ApiResponse(
                responseCode = "409",
                description = "Source code already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<SourceRegistryResponse> createSource(
            @Valid @RequestBody SourceRegistryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.createSource(mapper.toCommand(request))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace source", description = "Replaces mutable source metadata.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Source updated"),
        @ApiResponse(
                responseCode = "404",
                description = "Source not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Source code already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public SourceRegistryResponse updateSource(
            @PathVariable UUID id,
            @Valid @RequestBody SourceRegistryRequest request) {
        return mapper.toResponse(service.updateSource(id, mapper.toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete source", description = "Deletes a source registry entry.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Source deleted"),
        @ApiResponse(
                responseCode = "404",
                description = "Source not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteSource(@PathVariable UUID id) {
        service.deleteSource(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/validate")
    @Operation(
            summary = "Validate source",
            description = "Runs deterministic mock validation without making an external call.")
    @ApiResponse(responseCode = "200", description = "Mock validation completed")
    public SourceValidationResponse validateSource(@PathVariable UUID id) {
        return mapper.toResponse(service.validateSource(id));
    }

    @GetMapping("/health")
    @Operation(summary = "List source health", description = "Returns mock health observations.")
    @ApiResponse(responseCode = "200", description = "Source health returned")
    public List<SourceHealthResponse> getHealth() {
        return service.getHealth().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/capabilities")
    @Operation(
            summary = "List source capabilities",
            description = "Returns supported capabilities across registered sources.")
    @ApiResponse(responseCode = "200", description = "Source capabilities returned")
    public List<SourceCapabilityResponse> getCapabilities() {
        return service.getCapabilities().stream().map(mapper::toResponse).toList();
    }
}
