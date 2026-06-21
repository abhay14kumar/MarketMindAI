package com.marketmind.documents.api;

import java.util.UUID;

import com.marketmind.documents.application.PdfTextExtractionService;
import com.marketmind.documents.dto.DocumentTextExtractionResponse;
import com.marketmind.documents.mapper.DocumentTextExtractionMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Text Extraction", description = "Extract text from stored PDF versions")
public class DocumentTextExtractionController {

    private final PdfTextExtractionService service;
    private final DocumentTextExtractionMapper mapper;

    public DocumentTextExtractionController(
            PdfTextExtractionService service,
            DocumentTextExtractionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/extract-text")
    @Operation(
            summary = "Extract PDF text",
            description = "Extracts text from the current stored PDF version using PDFBox. "
                    + "Image-only PDFs are marked UNSUPPORTED because OCR is not enabled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Extraction attempt completed"),
        @ApiResponse(
                responseCode = "404",
                description = "Document or stored version not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DocumentTextExtractionResponse extract(@PathVariable UUID id) {
        return mapper.toResponse(service.extract(id));
    }

    @GetMapping("/{id}/extracted-text")
    @Operation(summary = "Get latest extracted text and metadata")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Extraction returned"),
        @ApiResponse(
                responseCode = "404",
                description = "Document or extraction not found",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public DocumentTextExtractionResponse getExtractedText(@PathVariable UUID id) {
        return mapper.toResponse(service.getLatest(id));
    }
}
