package com.marketmind.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.ai.domain.AiAnswerStatus;
import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.ai.domain.DocumentEmbeddingJob;

import org.junit.jupiter.api.Test;

class RagQuestionAnswerServiceTest {

    @Test
    void shouldReturnGroundedAnswerWithCitation() {
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        TestRepository repository = new TestRepository();
        VectorStore vectorStore = new StubVectorStore(List.of(new VectorSearchResult(
                documentId, chunkId, 2,
                "Revenue increased by ten percent during the reporting period.",
                "Annual Report", 0.82)));
        RagQuestionAnswerService service = service(
                repository, vectorStore, (question, context) -> "Revenue increased by ten percent.");

        AiQuestionAnswer answer = service.ask(
                "How did revenue change?", documentId, 5);

        assertThat(answer.status()).isEqualTo(AiAnswerStatus.SUCCESS);
        assertThat(answer.answer()).contains("not financial advice");
        assertThat(answer.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.chunkId()).isEqualTo(chunkId);
            assertThat(citation.chunkIndex()).isEqualTo(2);
        });
        assertThat(answer.confidenceScore()).isEqualByComparingTo("0.8200");
    }

    @Test
    void shouldReturnInsufficientContextWithoutCallingChatModel() {
        TestRepository repository = new TestRepository();
        RagQuestionAnswerService service = service(
                repository,
                new StubVectorStore(List.of()),
                (question, context) -> {
                    throw new AssertionError("Chat model must not run without context.");
                });

        AiQuestionAnswer answer = service.ask("Unknown question", null, 5);

        assertThat(answer.status()).isEqualTo(AiAnswerStatus.INSUFFICIENT_CONTEXT);
        assertThat(answer.citations()).isEmpty();
    }

    private RagQuestionAnswerService service(
            TestRepository repository,
            VectorStore vectorStore,
            ChatClient chatClient) {
        return new RagQuestionAnswerService(
                repository, text -> List.of(0.1, 0.2), vectorStore, chatClient,
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC));
    }

    private record StubVectorStore(List<VectorSearchResult> results) implements VectorStore {
        @Override public void ensureCollection(int vectorSize) { }
        @Override public UUID upsert(DocumentChunk chunk, String title, List<Double> vector) { return UUID.randomUUID(); }
        @Override public List<VectorSearchResult> search(List<Double> vector, UUID documentId, int topK) { return results; }
    }

    private static final class TestRepository implements RagRepository {
        private final List<AiQuestionAnswer> answers = new ArrayList<>();
        @Override public Optional<ExtractedDocument> findCompletedExtraction(UUID documentId) { return Optional.empty(); }
        @Override public void replaceChunks(UUID documentVersionId, List<DocumentChunk> chunks) { }
        @Override public DocumentChunk updateVectorReference(UUID chunkId, String collectionName, UUID pointId) { throw new UnsupportedOperationException(); }
        @Override public List<DocumentChunk> findChunks(UUID documentId) { return List.of(); }
        @Override public DocumentEmbeddingJob saveEmbeddingJob(DocumentEmbeddingJob job) { return job; }
        @Override public AiQuestionAnswer saveAnswer(AiQuestionAnswer answer) { answers.add(answer); return answer; }
        @Override public List<AiQuestionAnswer> findRecentAnswers(int limit) { return List.copyOf(answers); }
    }
}
