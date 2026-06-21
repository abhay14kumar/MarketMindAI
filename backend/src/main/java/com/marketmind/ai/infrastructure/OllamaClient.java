package com.marketmind.ai.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.ai.application.ChatClient;
import com.marketmind.ai.application.EmbeddingClient;
import com.marketmind.common.exception.ErrorCode;

import org.springframework.stereotype.Component;

@Component
public class OllamaClient implements EmbeddingClient, ChatClient {

    private final AiInfrastructureProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaClient(
            AiInfrastructureProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.ollama().timeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        JsonNode response = post("/api/embed", Map.of(
                "model", properties.ollama().embeddingModel(),
                "input", text));
        JsonNode values = response.path("embeddings").path(0);
        if (!values.isArray() || values.isEmpty()) {
            throw ollamaFailure("Ollama returned no embedding vector.");
        }
        return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(JsonNode::doubleValue)
                .toList();
    }

    @Override
    public String answer(String question, String groundedContext) {
        String system = """
                You are MarketMind AI. Answer only from the supplied document context.
                If the context does not support an answer, say "Insufficient context."
                Do not invent facts, prices, recommendations, or citations.
                Do not provide buy or sell instructions.
                """;
        JsonNode response = post("/api/chat", Map.of(
                "model", properties.ollama().chatModel(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content",
                                "Context:\n" + groundedContext + "\n\nQuestion:\n" + question))));
        String content = response.path("message").path("content").asText("");
        if (content.isBlank()) {
            throw ollamaFailure("Ollama returned an empty answer.");
        }
        return content;
    }

    private JsonNode post(String path, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(trim(properties.ollama().baseUrl()) + path))
                    .timeout(Duration.ofSeconds(properties.ollama().timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw ollamaFailure(
                        "Ollama returned HTTP " + response.statusCode() + ".");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw ollamaFailure("Ollama request was interrupted.", exception);
        } catch (IOException exception) {
            throw ollamaFailure("Ollama is unavailable.", exception);
        }
    }

    private AiInfrastructureException ollamaFailure(String message) {
        return new AiInfrastructureException(ErrorCode.OLLAMA_FAILURE, message);
    }

    private AiInfrastructureException ollamaFailure(
            String message,
            Throwable cause) {
        return new AiInfrastructureException(
                ErrorCode.OLLAMA_FAILURE, message, cause);
    }

    private String trim(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
