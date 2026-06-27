package com.marketmind.sourceintelligence.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.marketmind.sourceintelligence.application.SourceCatalogItem;
import com.marketmind.sourceintelligence.application.SourceCoverageRow;
import com.marketmind.sourceintelligence.application.SourceIntelligenceMetrics;
import com.marketmind.sourceintelligence.application.SourceIntelligenceService;
import com.marketmind.sourceintelligence.application.SourceRefreshResult;
import com.marketmind.sourceintelligence.domain.SourceActivity;
import com.marketmind.sourceintelligence.domain.SourceFormat;

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
@RequestMapping("/api/v1/source-intelligence")
@Tag(
        name = "Source Intelligence",
        description = "Enterprise source catalog, connectors, coverage, activity, and operations")
public class SourceIntelligenceController {

    private final SourceIntelligenceService service;

    public SourceIntelligenceController(SourceIntelligenceService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    @Operation(summary = "List enterprise source catalog")
    public List<SourceCatalogItem> catalog() {
        return service.catalog();
    }

    @GetMapping("/catalog/{sourceId}")
    @Operation(summary = "Get source intelligence detail")
    public SourceCatalogItem source(@PathVariable UUID sourceId) {
        return service.getSource(sourceId);
    }

    @GetMapping("/health")
    @Operation(summary = "List enriched source health")
    public List<SourceCatalogItem> health() {
        return service.catalog();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get source intelligence metrics")
    public SourceIntelligenceMetrics metrics() {
        return service.metrics();
    }

    @GetMapping("/activity")
    @Operation(summary = "List discovery, pipeline, validation, and source activity")
    public List<SourceActivity> activity(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return service.activity(limit);
    }

    @GetMapping("/coverage")
    @Operation(summary = "Get company and document-type coverage matrix")
    public List<SourceCoverageRow> coverage() {
        return service.coverage();
    }

    @GetMapping("/connectors")
    @Operation(summary = "List available source connectors")
    public List<SourceIntelligenceService.ConnectorDescriptor> connectors() {
        return service.connectors();
    }

    @GetMapping("/formats")
    @Operation(summary = "List detectable and supported source formats")
    public Set<SourceFormat> formats() {
        return service.formats();
    }

    @PostMapping("/sources/{sourceId}/validate")
    @Operation(summary = "Validate source and refresh intelligence metadata")
    public SourceCatalogItem validate(@PathVariable UUID sourceId) {
        return service.validate(sourceId);
    }

    @PostMapping("/sources/{sourceId}/refresh")
    @Operation(summary = "Run connector-based source refresh")
    public SourceRefreshResult refresh(@PathVariable UUID sourceId) {
        return service.refresh(sourceId);
    }
}
