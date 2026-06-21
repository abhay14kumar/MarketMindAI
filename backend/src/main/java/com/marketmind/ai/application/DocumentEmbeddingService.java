package com.marketmind.ai.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;

import org.springframework.stereotype.Service;

@Service
public class DocumentEmbeddingService {

    private final RagRepository repository;
    private final TextChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final RagProperties properties;
    private final Clock clock;

    public DocumentEmbeddingService(
            RagRepository repository,
            TextChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            VectorStore vectorStore,
            RagProperties properties,
            Clock clock) {
        this.repository = repository;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.properties = properties;
        this.clock = clock;
    }

    public DocumentEmbeddingJob embed(UUID documentId) {
        ExtractedDocument document = repository.findCompletedExtraction(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Completed extracted text not found for document: " + documentId));
        Instant startedAt = clock.instant();
        UUID jobId = UUID.randomUUID();
        repository.saveEmbeddingJob(new DocumentEmbeddingJob(
                jobId, document.documentId(), document.documentVersionId(),
                EmbeddingJobStatus.STARTED, 0, 0, 0,
                null, startedAt, null, startedAt));

        List<DocumentChunk> chunks = chunkingService.chunk(document);
        repository.replaceChunks(document.documentVersionId(), chunks);
        if (chunks.isEmpty()) {
            return repository.saveEmbeddingJob(new DocumentEmbeddingJob(
                    jobId, document.documentId(), document.documentVersionId(),
                    EmbeddingJobStatus.FAILED, 0, 0, 0,
                    "No non-empty text chunks were produced.",
                    startedAt, clock.instant(), startedAt));
        }

        int embedded = 0;
        List<String> errors = new ArrayList<>();
        boolean collectionReady = false;
        for (DocumentChunk chunk : chunks) {
            try {
                List<Double> vector = embeddingClient.embed(chunk.chunkText());
                if (!collectionReady) {
                    vectorStore.ensureCollection(vector.size());
                    collectionReady = true;
                }
                UUID pointId = vectorStore.upsert(chunk, document.title(), vector);
                repository.updateVectorReference(
                        chunk.id(), properties.collectionName(), pointId);
                embedded++;
            } catch (RuntimeException exception) {
                errors.add("Chunk " + chunk.chunkIndex() + ": " + safeMessage(exception));
            }
        }

        int failed = chunks.size() - embedded;
        EmbeddingJobStatus status = failed == 0
                ? EmbeddingJobStatus.COMPLETED
                : embedded == 0 ? EmbeddingJobStatus.FAILED : EmbeddingJobStatus.PARTIAL;
        return repository.saveEmbeddingJob(new DocumentEmbeddingJob(
                jobId, document.documentId(), document.documentVersionId(),
                status, chunks.size(), embedded, failed,
                truncate(String.join("; ", errors)),
                startedAt, clock.instant(), startedAt));
    }

    public List<DocumentChunk> getChunks(UUID documentId) {
        return repository.findChunks(documentId);
    }

    private String safeMessage(Throwable exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private String truncate(String value) {
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
