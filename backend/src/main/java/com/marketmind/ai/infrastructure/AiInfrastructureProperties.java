package com.marketmind.ai.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiInfrastructureProperties(
        Ollama ollama,
        Qdrant qdrant) {

    public AiInfrastructureProperties {
        ollama = ollama == null
                ? new Ollama("http://localhost:11434", "nomic-embed-text", "llama3.1", 60)
                : ollama;
        qdrant = qdrant == null
                ? new Qdrant("http://localhost:6333", null, 30)
                : qdrant;
    }

    public record Ollama(
            String baseUrl,
            String embeddingModel,
            String chatModel,
            int timeoutSeconds) {
    }

    public record Qdrant(
            String baseUrl,
            String apiKey,
            int timeoutSeconds) {
    }
}
