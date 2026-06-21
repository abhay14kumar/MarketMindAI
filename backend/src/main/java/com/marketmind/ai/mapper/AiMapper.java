package com.marketmind.ai.mapper;

import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.Citation;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.dto.AiAnswerResponse;
import com.marketmind.ai.dto.CitationResponse;
import com.marketmind.ai.dto.DocumentChunkResponse;
import com.marketmind.ai.dto.EmbeddingJobResponse;

import org.springframework.stereotype.Component;

@Component
public class AiMapper {

    public EmbeddingJobResponse toResponse(DocumentEmbeddingJob job) {
        return new EmbeddingJobResponse(
                job.id(), job.documentId(), job.documentVersionId(), job.status(),
                job.totalChunks(), job.embeddedChunks(), job.failedChunks(),
                job.errorMessage(), job.startedAt(), job.completedAt());
    }

    public DocumentChunkResponse toResponse(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.id(), chunk.documentId(), chunk.documentVersionId(),
                chunk.chunkIndex(), chunk.chunkText(), chunk.tokenCount(),
                chunk.characterCount(), chunk.qdrantCollection(),
                chunk.qdrantPointId(), chunk.createdAt());
    }

    public AiAnswerResponse toResponse(AiQuestionAnswer answer) {
        return new AiAnswerResponse(
                answer.id(), answer.question(), answer.answer(), answer.documentId(),
                answer.status(), answer.confidenceScore(),
                answer.citations().stream().map(this::toResponse).toList(),
                answer.createdAt());
    }

    private CitationResponse toResponse(Citation citation) {
        return new CitationResponse(
                citation.documentId(), citation.chunkId(), citation.chunkIndex(),
                citation.snippet());
    }
}
