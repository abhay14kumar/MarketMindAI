package com.marketmind.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;
import com.marketmind.ai.domain.EmbeddingJobStatus;

import org.junit.jupiter.api.Test;

class DocumentEmbeddingServiceTest {

    @Test
    void shouldMarkJobPartialWhenOneChunkEmbeddingFails() {
        UUID documentId = UUID.randomUUID();
        TestRepository repository = new TestRepository(new ExtractedDocument(
                documentId, UUID.randomUUID(), "Report",
                "First material section with facts. Second material section with risks."));
        RagProperties properties = new RagProperties(35, 5, 5, "test_collection");
        Clock clock = Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC);
        TextChunkingService chunking = new TextChunkingService(properties, clock);
        EmbeddingClient embeddings = text -> {
            if (text.contains("Second")) {
                throw new IllegalStateException("Synthetic embedding failure.");
            }
            return List.of(0.1, 0.2, 0.3);
        };
        DocumentEmbeddingService service = new DocumentEmbeddingService(
                repository, chunking, embeddings, new TestVectorStore(),
                properties, clock);

        DocumentEmbeddingJob job = service.embed(documentId);

        assertThat(job.status()).isEqualTo(EmbeddingJobStatus.PARTIAL);
        assertThat(job.embeddedChunks()).isPositive();
        assertThat(job.failedChunks()).isPositive();
    }

    private static final class TestVectorStore implements VectorStore {
        @Override public void ensureCollection(int vectorSize) { }
        @Override public UUID upsert(DocumentChunk chunk, String title, List<Double> vector) { return UUID.randomUUID(); }
        @Override public List<VectorSearchResult> search(List<Double> vector, UUID documentId, int topK) { return List.of(); }
    }

    private static final class TestRepository implements RagRepository {
        private final ExtractedDocument document;
        private final List<DocumentChunk> chunks = new ArrayList<>();
        private TestRepository(ExtractedDocument document) { this.document = document; }
        @Override public Optional<ExtractedDocument> findCompletedExtraction(UUID documentId) { return Optional.of(document); }
        @Override public void replaceChunks(UUID documentVersionId, List<DocumentChunk> values) { chunks.clear(); chunks.addAll(values); }
        @Override public DocumentChunk updateVectorReference(UUID chunkId, String collectionName, UUID pointId) {
            return chunks.stream().filter(chunk -> chunk.id().equals(chunkId)).findFirst().orElseThrow();
        }
        @Override public List<DocumentChunk> findChunks(UUID documentId) { return List.copyOf(chunks); }
        @Override public DocumentEmbeddingJob saveEmbeddingJob(DocumentEmbeddingJob job) { return job; }
        @Override public AiQuestionAnswer saveAnswer(AiQuestionAnswer answer) { return answer; }
        @Override public List<AiQuestionAnswer> findRecentAnswers(int limit) { return List.of(); }
    }
}
