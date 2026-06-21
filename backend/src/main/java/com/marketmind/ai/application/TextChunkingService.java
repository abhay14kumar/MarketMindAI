package com.marketmind.ai.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.marketmind.ai.domain.DocumentChunk;

import org.springframework.stereotype.Service;

@Service
public class TextChunkingService {

    private final RagProperties properties;
    private final Clock clock;

    public TextChunkingService(RagProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public List<DocumentChunk> chunk(ExtractedDocument document) {
        String text = document.extractedText() == null
                ? ""
                : document.extractedText().strip();
        if (text.isBlank()) {
            return List.of();
        }
        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int tentativeEnd = Math.min(start + properties.chunkSize(), text.length());
            int end = findBoundary(text, start, tentativeEnd);
            String value = text.substring(start, end).strip();
            if (!value.isBlank()) {
                chunks.add(new DocumentChunk(
                        UUID.randomUUID(), document.documentId(),
                        document.documentVersionId(), index++, value,
                        approximateTokens(value), value.length(),
                        null, null, clock.instant()));
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(start + 1, end - properties.chunkOverlap());
        }
        return List.copyOf(chunks);
    }

    private int findBoundary(String text, int start, int tentativeEnd) {
        if (tentativeEnd >= text.length()) {
            return text.length();
        }
        int minimum = Math.max(start + properties.chunkSize() / 2, start + 1);
        for (int index = tentativeEnd; index >= minimum; index--) {
            char previous = text.charAt(index - 1);
            if (previous == '\n' || previous == '.' || Character.isWhitespace(previous)) {
                return index;
            }
        }
        return tentativeEnd;
    }

    private int approximateTokens(String value) {
        return Math.max(1, (int) Math.ceil(value.length() / 4.0));
    }
}
