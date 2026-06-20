package com.marketmind.documents.api;

import java.util.List;

import com.marketmind.documents.application.DocumentService;
import com.marketmind.documents.dto.CreateDocumentSourceRequest;
import com.marketmind.documents.dto.DocumentSourceResponse;
import com.marketmind.documents.mapper.DocumentMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sources")
@Tag(name = "Document Sources", description = "Manage official document acquisition sources")
public class DocumentSourceController {

    private final DocumentService documentService;
    private final DocumentMapper mapper;

    public DocumentSourceController(DocumentService documentService, DocumentMapper mapper) {
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "List sources", description = "Returns configured mock document sources.")
    @ApiResponse(responseCode = "200", description = "Document sources returned")
    public List<DocumentSourceResponse> getSources() {
        return documentService.getSources().stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @Operation(
            summary = "Create source",
            description = "Registers a mock document source without connecting to it.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document source created"),
        @ApiResponse(
                responseCode = "409",
                description = "Source code already exists",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DocumentSourceResponse> createSource(
            @Valid @RequestBody CreateDocumentSourceRequest request) {
        DocumentSourceResponse response = mapper.toResponse(
                documentService.createSource(mapper.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
