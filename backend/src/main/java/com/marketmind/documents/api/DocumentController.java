package com.marketmind.documents.api;

import java.util.List;
import java.util.UUID;

import com.marketmind.documents.application.DocumentDownloadService;
import com.marketmind.documents.application.DocumentService;
import com.marketmind.documents.dto.DocumentDownloadResponse;
import com.marketmind.documents.dto.DocumentResponse;
import com.marketmind.documents.dto.DocumentVersionResponse;
import com.marketmind.documents.dto.DownloadDocumentRequest;
import com.marketmind.documents.dto.DownloadJobResponse;
import com.marketmind.documents.dto.PageResponse;
import com.marketmind.documents.mapper.DocumentMapper;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Acquisition", description = "Acquire and inspect official company documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentDownloadService documentDownloadService;
    private final DocumentMapper mapper;

    public DocumentController(
            DocumentService documentService,
            DocumentDownloadService documentDownloadService,
            DocumentMapper mapper) {
        this.documentService = documentService;
        this.documentDownloadService = documentDownloadService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            summary = "List documents",
            description = "Returns acquired document metadata ordered by publication date.")
    @ApiResponse(
            responseCode = "200",
            description = "Documents returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<DocumentResponse> getDocuments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toDocumentPage(documentService.getDocuments(page, size));
    }

    @PostMapping("/download")
    @Operation(
            summary = "Download document",
            description = "Downloads, checksums, stores, and versions a generic HTTP/HTTPS document.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Document downloaded and stored",
                content = @Content(schema = @Schema(
                        implementation = DocumentDownloadResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid or unsupported URL",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Duplicate document checksum",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "413",
                description = "Document exceeds configured size limit",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "502",
                description = "Remote document download failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "504",
                description = "Remote document download timed out",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DocumentDownloadResponse> download(
            @Valid @RequestBody DownloadDocumentRequest request) {
        DocumentDownloadResponse response = mapper.toResponse(
                documentDownloadService.download(mapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Returns document metadata by identifier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Document not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DocumentResponse getDocument(@PathVariable UUID id) {
        return mapper.toResponse(documentService.getDocument(id));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List download jobs", description = "Returns document-download jobs.")
    @ApiResponse(
            responseCode = "200",
            description = "Download jobs returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<DownloadJobResponse> getJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toJobPage(documentService.getDownloadJobs(page, size));
    }

    @GetMapping("/versions/{documentId}")
    @Operation(
            summary = "List document versions",
            description = "Returns immutable versions for an acquired document.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Document versions returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Document not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<DocumentVersionResponse> getVersions(@PathVariable UUID documentId) {
        return documentDownloadService.getVersions(documentId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
