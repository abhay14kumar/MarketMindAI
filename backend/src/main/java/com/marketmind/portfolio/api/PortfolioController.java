package com.marketmind.portfolio.api;

import java.io.IOException;
import java.util.List;

import com.marketmind.portfolio.application.PortfolioService;
import com.marketmind.portfolio.dto.AllocationResponse;
import com.marketmind.portfolio.dto.PageResponse;
import com.marketmind.portfolio.dto.PortfolioHoldingResponse;
import com.marketmind.portfolio.dto.PortfolioImportJobResponse;
import com.marketmind.portfolio.dto.PortfolioImportResponse;
import com.marketmind.portfolio.dto.PortfolioSummaryResponse;
import com.marketmind.portfolio.mapper.PortfolioMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/portfolio")
@Tag(name = "Portfolio Intelligence", description = "Import and analyze Zerodha holdings")
public class PortfolioController {

    private final PortfolioService service;
    private final PortfolioMapper mapper;

    public PortfolioController(PortfolioService service, PortfolioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Import Zerodha holdings",
            description = "Parses an XLSX export in memory, replaces the current holdings, "
                    + "and creates an import job and portfolio snapshot.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Holdings imported"),
        @ApiResponse(
                responseCode = "400",
                description = "The workbook is empty, unsupported, or malformed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PortfolioImportResponse> importPortfolio(
            @Parameter(
                    description = "Zerodha holdings XLSX export",
                    required = true,
                    content = @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            @RequestPart("file") MultipartFile file) throws IOException {
        validateFile(file);
        PortfolioImportResponse response;
        try (var input = file.getInputStream()) {
            response = mapper.toResponse(service.importHoldings(
                    safeFileName(file.getOriginalFilename()),
                    input));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary")
    public PortfolioSummaryResponse getSummary() {
        return mapper.toResponse(service.getSummary());
    }

    @GetMapping("/holdings")
    @Operation(summary = "List portfolio holdings")
    public PageResponse<PortfolioHoldingResponse> getHoldings(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return mapper.toHoldingPage(service.getHoldings(page, size));
    }

    @GetMapping("/allocation/sector")
    @Operation(summary = "Get portfolio allocation by sector")
    public List<AllocationResponse> getSectorAllocation() {
        return service.getSectorAllocation().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/allocation/instrument")
    @Operation(summary = "Get portfolio allocation by instrument type")
    public List<AllocationResponse> getInstrumentAllocation() {
        return service.getInstrumentAllocation().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/import-jobs")
    @Operation(summary = "List portfolio import history")
    public PageResponse<PortfolioImportJobResponse> getImportJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toImportJobPage(service.getImportJobs(page, size));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("An XLSX file is required.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx holdings exports are supported.");
        }
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "zerodha-holdings.xlsx";
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (fileName.isBlank()) {
            return "zerodha-holdings.xlsx";
        }
        return fileName.length() <= 255 ? fileName : fileName.substring(fileName.length() - 255);
    }
}
