package com.marketmind.ai.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;

public interface RagRepository {

    Optional<ExtractedDocument> findCompletedExtraction(UUID documentId);

    void replaceChunks(UUID documentVersionId, List<DocumentChunk> chunks);

    DocumentChunk updateVectorReference(
            UUID chunkId,
            String collectionName,
            UUID pointId);

    List<DocumentChunk> findChunks(UUID documentId);

    DocumentEmbeddingJob saveEmbeddingJob(DocumentEmbeddingJob job);

    AiQuestionAnswer saveAnswer(AiQuestionAnswer answer);

    List<AiQuestionAnswer> findRecentAnswers(int limit);
}
