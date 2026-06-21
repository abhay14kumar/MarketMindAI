package com.marketmind.ai.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.marketmind.ai.domain.AiAnswerStatus;
import com.marketmind.ai.domain.AiQuestionAnswer;
import com.marketmind.ai.domain.Citation;

import org.springframework.stereotype.Service;

@Service
public class RagQuestionAnswerService {

    public static final String DISCLAIMER =
            "AI answer is based only on indexed documents and is not financial advice.";
    private static final double MINIMUM_CONTEXT_SCORE = 0.15;

    private final RagRepository repository;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Clock clock;

    public RagQuestionAnswerService(
            RagRepository repository,
            EmbeddingClient embeddingClient,
            VectorStore vectorStore,
            ChatClient chatClient,
            Clock clock) {
        this.repository = repository;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.clock = clock;
    }

    public AiQuestionAnswer ask(String question, UUID documentId, int topK) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank.");
        }
        List<VectorSearchResult> matches;
        try {
            matches = vectorStore.search(
                    embeddingClient.embed(question.strip()), documentId, topK);
        } catch (RuntimeException exception) {
            return save(question, documentId, AiAnswerStatus.FAILED,
                    withDisclaimer("The local retrieval service is currently unavailable."),
                    List.of(), BigDecimal.ZERO);
        }
        List<VectorSearchResult> relevant = matches.stream()
                .filter(match -> match.score() >= MINIMUM_CONTEXT_SCORE)
                .toList();
        if (relevant.isEmpty()) {
            return save(question, documentId, AiAnswerStatus.INSUFFICIENT_CONTEXT,
                    withDisclaimer(
                            "Insufficient context was found in the indexed documents."),
                    List.of(),
                    BigDecimal.ZERO);
        }

        String context = buildContext(relevant);
        String generated;
        try {
            generated = chatClient.answer(question.strip(), context);
        } catch (RuntimeException exception) {
            return save(question, documentId, AiAnswerStatus.FAILED,
                    withDisclaimer("The local AI model could not generate an answer."),
                    List.of(),
                    BigDecimal.ZERO);
        }
        List<Citation> citations = relevant.stream()
                .map(match -> new Citation(
                        match.documentId(), match.chunkId(), match.chunkIndex(),
                        snippet(match.chunkText())))
                .toList();
        BigDecimal confidence = BigDecimal.valueOf(
                        relevant.stream().mapToDouble(VectorSearchResult::score)
                                .average().orElse(0))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP);
        return save(question, documentId, AiAnswerStatus.SUCCESS,
                withDisclaimer(generated.strip()), citations, confidence);
    }

    public List<AiQuestionAnswer> getRecentAnswers() {
        return repository.findRecentAnswers(50);
    }

    private AiQuestionAnswer save(
            String question,
            UUID documentId,
            AiAnswerStatus status,
            String answer,
            List<Citation> citations,
            BigDecimal confidence) {
        return repository.saveAnswer(new AiQuestionAnswer(
                UUID.randomUUID(), question.strip(), answer, documentId, status,
                citations, confidence, clock.instant()));
    }

    private String buildContext(List<VectorSearchResult> results) {
        StringBuilder context = new StringBuilder();
        for (VectorSearchResult result : results) {
            context.append("[Document ").append(result.documentId())
                    .append(", Chunk ").append(result.chunkIndex()).append("]\n")
                    .append(result.chunkText()).append("\n\n");
        }
        return context.toString();
    }

    private String snippet(String text) {
        String normalized = text.replaceAll("\\s+", " ").strip();
        return normalized.length() <= 280 ? normalized : normalized.substring(0, 280) + "…";
    }

    private String withDisclaimer(String answer) {
        return answer + "\n\n" + DISCLAIMER;
    }
}
