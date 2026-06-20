package com.marketmind.documents.api;

import java.util.List;
import java.util.UUID;

import com.marketmind.documents.application.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Acquisition", description = "Discover and queue company documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentApiMapper mapper;

    public DocumentController(DocumentService documentService, DocumentApiMapper mapper) {
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
            content = @Content(array = @ArraySchema(schema = @Schema(
                    implementation = DocumentResponse.class))))
    public List<DocumentResponse> getDocuments() {
        return documentService.getDocuments().stream().map(mapper::toResponse).toList();
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
            content = @Content(array = @ArraySchema(schema = @Schema(
                    implementation = DownloadJobResponse.class))))
    public List<DownloadJobResponse> getJobs() {
        return documentService.getDownloadJobs().stream().map(mapper::toResponse).toList();
    }
}
