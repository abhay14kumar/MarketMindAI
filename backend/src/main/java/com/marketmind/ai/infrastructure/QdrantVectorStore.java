package com.marketmind.ai.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.ai.application.RagProperties;
import com.marketmind.ai.application.VectorSearchResult;
import com.marketmind.ai.application.VectorStore;
import com.marketmind.ai.domain.DocumentChunk;
import com.marketmind.common.exception.ErrorCode;

import org.springframework.stereotype.Component;

@Component
public class QdrantVectorStore implements VectorStore {

    private final AiInfrastructureProperties properties;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QdrantVectorStore(
            AiInfrastructureProperties properties,
            RagProperties ragProperties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.qdrant().timeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public void ensureCollection(int vectorSize) {
        String path = "/collections/" + ragProperties.collectionName();
        HttpResponse<String> existing = send("GET", path, null);
        if (existing.statusCode() == 200) {
            return;
        }
        if (existing.statusCode() != 404) {
            throw failure(existing, "inspect collection");
        }
        HttpResponse<String> created = send("PUT", path, Map.of(
                "vectors", Map.of("size", vectorSize, "distance", "Cosine")));
        if (created.statusCode() < 200 || created.statusCode() >= 300) {
            throw failure(created, "create collection");
        }
    }

    @Override
    public UUID upsert(DocumentChunk chunk, String title, List<Double> vector) {
        UUID pointId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", chunk.documentId().toString());
        payload.put("documentVersionId", chunk.documentVersionId().toString());
        payload.put("chunkId", chunk.id().toString());
        payload.put("chunkIndex", chunk.chunkIndex());
        payload.put("chunkText", chunk.chunkText());
        payload.put("title", title);
        HttpResponse<String> response = send(
                "PUT",
                "/collections/" + ragProperties.collectionName() + "/points?wait=true",
                Map.of("points", List.of(Map.of(
                        "id", pointId.toString(),
                        "vector", vector,
                        "payload", payload))));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw failure(response, "upsert vector");
        }
        return pointId;
    }

    @Override
    public List<VectorSearchResult> search(
            List<Double> vector,
            UUID documentId,
            int topK) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", vector);
        body.put("limit", topK);
        body.put("with_payload", true);
        if (documentId != null) {
            body.put("filter", Map.of("must", List.of(Map.of(
                    "key", "documentId",
                    "match", Map.of("value", documentId.toString())))));
        }
        HttpResponse<String> response = send(
                "POST",
                "/collections/" + ragProperties.collectionName() + "/points/query",
                body);
        if (response.statusCode() == 404) {
            return List.of();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw failure(response, "query vectors");
        }
        try {
            JsonNode result = objectMapper.readTree(response.body()).path("result");
            JsonNode points = result.has("points") ? result.path("points") : result;
            List<VectorSearchResult> matches = new ArrayList<>();
            if (points.isArray()) {
                for (JsonNode point : points) {
                    JsonNode payload = point.path("payload");
                    matches.add(new VectorSearchResult(
                            UUID.fromString(payload.path("documentId").asText()),
                            UUID.fromString(payload.path("chunkId").asText()),
                            payload.path("chunkIndex").asInt(),
                            payload.path("chunkText").asText(),
                            payload.path("title").asText(),
                            point.path("score").asDouble()));
                }
            }
            return List.copyOf(matches);
        } catch (IOException | IllegalArgumentException exception) {
            throw qdrantFailure(
                    "Qdrant returned an invalid query response.", exception);
        }
    }

    private HttpResponse<String> send(String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(
                            URI.create(trim(properties.qdrant().baseUrl()) + path))
                    .timeout(Duration.ofSeconds(properties.qdrant().timeoutSeconds()))
                    .header("Accept", "application/json");
            if (properties.qdrant().apiKey() != null
                    && !properties.qdrant().apiKey().isBlank()) {
                builder.header("api-key", properties.qdrant().apiKey());
            }
            if (body != null) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(body)));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw qdrantFailure("Qdrant request was interrupted.", exception);
        } catch (IOException exception) {
            throw qdrantFailure("Qdrant is unavailable.", exception);
        }
    }

    private AiInfrastructureException failure(
            HttpResponse<String> response,
            String operation) {
        return qdrantFailure("Unable to " + operation + " in Qdrant (HTTP "
                + response.statusCode() + ").");
    }

    private AiInfrastructureException qdrantFailure(String message) {
        return new AiInfrastructureException(ErrorCode.QDRANT_FAILURE, message);
    }

    private AiInfrastructureException qdrantFailure(
            String message,
            Throwable cause) {
        return new AiInfrastructureException(
                ErrorCode.QDRANT_FAILURE, message, cause);
    }

    private String trim(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
