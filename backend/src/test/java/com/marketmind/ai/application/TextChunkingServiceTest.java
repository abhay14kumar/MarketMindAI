package com.marketmind.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TextChunkingServiceTest {

    @Test
    void shouldCreateOrderedOverlappingNonEmptyChunks() {
        TextChunkingService service = new TextChunkingService(
                new RagProperties(40, 10, 5, "test"),
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC));
        ExtractedDocument document = new ExtractedDocument(
                UUID.randomUUID(), UUID.randomUUID(), "Annual Report",
                "Revenue increased strongly. Operating margin improved. "
                        + "Risk disclosures remained unchanged. Cash flow was positive.");

        var chunks = service.chunk(document);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(chunk -> chunk.chunkIndex())
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.chunkText()).isNotBlank();
            assertThat(chunk.characterCount()).isLessThanOrEqualTo(40);
            assertThat(chunk.tokenCount()).isPositive();
        });
    }

    @Test
    void shouldSkipEmptyText() {
        TextChunkingService service = new TextChunkingService(
                new RagProperties(1000, 200, 5, "test"), Clock.systemUTC());

        assertThat(service.chunk(new ExtractedDocument(
                UUID.randomUUID(), UUID.randomUUID(), "Empty", "  \n "))).isEmpty();
    }
}
