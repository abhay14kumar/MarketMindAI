package com.marketmind.company.adapter.in.web;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

import com.marketmind.company.application.dto.CompanyDTO;
import com.marketmind.company.application.dto.CompanyPageDTO;
import com.marketmind.company.application.model.CompanyPageQuery;
import com.marketmind.company.application.model.CompanyPageQuery.SortDirection;
import com.marketmind.company.application.service.CompanyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Company Master", description = "Manage listed company master data")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "List companies", description = "Returns a paginated and sorted company list.")
    @ApiResponse(responseCode = "200", description = "Companies returned")
    public CompanyPageDTO findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "companyName") String sort,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String direction) {
        return companyService.findAll(toPageQuery(page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company", description = "Returns a company by its identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Company not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CompanyDTO findById(@PathVariable UUID id) {
        return companyService.findById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search companies", description = "Searches company names case-insensitively.")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    public CompanyPageDTO search(
            @RequestParam @NotBlank @Size(max = 255) String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "companyName") String sort,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String direction) {
        return companyService.searchByName(name, toPageQuery(page, size, sort, direction));
    }

    @PostMapping
    @Operation(summary = "Create company", description = "Creates a company master record.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Company created"),
        @ApiResponse(
                responseCode = "409",
                description = "ISIN or exchange symbol already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CompanyDTO> create(@Valid @RequestBody CompanyDTO request) {
        CompanyDTO created = companyService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace company", description = "Replaces mutable company master fields.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Company updated"),
        @ApiResponse(
                responseCode = "404",
                description = "Company not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "ISIN or exchange symbol already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CompanyDTO update(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyDTO request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete company", description = "Deletes an unreferenced company master record.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Company deleted"),
        @ApiResponse(
                responseCode = "404",
                description = "Company not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Company is referenced by other records",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CompanyPageQuery toPageQuery(int page, int size, String sort, String direction) {
        return new CompanyPageQuery(page, size, sort, parseDirection(direction));
    }

    private SortDirection parseDirection(String direction) {
        try {
            return SortDirection.valueOf(direction.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.", exception);
        }
    }
}
