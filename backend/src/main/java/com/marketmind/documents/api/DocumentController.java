package com.marketmind.documents.api;

import java.util.UUID;

import com.marketmind.documents.application.DocumentService;
import com.marketmind.documents.dto.DocumentResponse;
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
@Tag(name = "Document Acquisition", description = "Discover and queue company documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper mapper;

    public DocumentController(DocumentService documentService, DocumentMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(
            summary = "List documents",
            description = "Returns mock document metadata ordered by publication date.")
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
            summary = "Queue document download",
            description = "Creates a mock asynchronous download job. No network call is made.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "Download job accepted",
                content = @Content(schema = @Schema(implementation = DownloadJobResponse.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DownloadJobResponse> queueDownload(
            @Valid @RequestBody DownloadDocumentRequest request) {
        DownloadJobResponse response = mapper.toResponse(
                documentService.queueDownload(mapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Returns document metadata by identifier.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Document returned",
                content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Document not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DocumentResponse getDocument(@PathVariable UUID id) {
        return mapper.toResponse(documentService.getDocument(id));
    }

    @GetMapping("/jobs")
    @Operation(
            summary = "List download jobs",
            description = "Returns mock and newly queued document-download jobs.")
    @ApiResponse(
            responseCode = "200",
            description = "Download jobs returned",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    public PageResponse<DownloadJobResponse> getJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return mapper.toJobPage(documentService.getDownloadJobs(page, size));
    }

    @PostMapping("/retry/{jobId}")
    @Operation(
            summary = "Retry failed download",
            description = "Creates a new queued mock job from a failed job. No network call is made.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "Retry job accepted",
                content = @Content(schema = @Schema(implementation = DownloadJobResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Download job not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Download job is not failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DownloadJobResponse> retryDownload(@PathVariable UUID jobId) {
        return ResponseEntity.accepted()
                .body(mapper.toResponse(documentService.retryDownload(jobId)));
    }
}
