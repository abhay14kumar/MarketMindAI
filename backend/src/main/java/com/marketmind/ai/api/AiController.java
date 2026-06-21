package com.marketmind.ai.api;

import java.util.List;
import java.util.UUID;

import com.marketmind.ai.application.DocumentEmbeddingService;
import com.marketmind.ai.application.RagProperties;
import com.marketmind.ai.application.RagQuestionAnswerService;
import com.marketmind.ai.dto.AiAnswerResponse;
import com.marketmind.ai.dto.AiAskRequest;
import com.marketmind.ai.dto.DocumentChunkResponse;
import com.marketmind.ai.dto.EmbeddingJobResponse;
import com.marketmind.ai.mapper.AiMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Research", description = "Local document-grounded RAG workflows")
public class AiController {

    private final DocumentEmbeddingService embeddingService;
    private final RagQuestionAnswerService questionAnswerService;
    private final RagProperties properties;
    private final AiMapper mapper;

    public AiController(
            DocumentEmbeddingService embeddingService,
            RagQuestionAnswerService questionAnswerService,
            RagProperties properties,
            AiMapper mapper) {
        this.embeddingService = embeddingService;
        this.questionAnswerService = questionAnswerService;
        this.properties = properties;
        this.mapper = mapper;
    }

    @PostMapping("/documents/{documentId}/embed")
    @Operation(
            summary = "Chunk and embed extracted document text",
            description = "Uses local Ollama embeddings and stores vectors in Qdrant.")
    @ApiResponse(responseCode = "200", description = "Embedding job completed or partially completed")
    public EmbeddingJobResponse embed(@PathVariable UUID documentId) {
        return mapper.toResponse(embeddingService.embed(documentId));
    }

    @GetMapping("/documents/{documentId}/chunks")
    @Operation(summary = "List persisted chunks for a document")
    public List<DocumentChunkResponse> getChunks(@PathVariable UUID documentId) {
        return embeddingService.getChunks(documentId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/ask")
    @Operation(
            summary = "Ask a document-grounded question",
            description = "Retrieves indexed chunks and asks the local Ollama chat model. "
                    + "Answers include citations and are not financial advice.")
    public AiAnswerResponse ask(@Valid @RequestBody AiAskRequest request) {
        int topK = request.topK() == null ? properties.defaultTopK() : request.topK();
        return mapper.toResponse(questionAnswerService.ask(
                request.question(), request.documentId(), topK));
    }

    @GetMapping("/answers")
    @Operation(summary = "List recent AI question and answer history")
    public List<AiAnswerResponse> getAnswers() {
        return questionAnswerService.getRecentAnswers().stream()
                .map(mapper::toResponse)
                .toList();
    }
}
