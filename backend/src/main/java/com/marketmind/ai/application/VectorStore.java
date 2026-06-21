package com.marketmind.ai.application;

import java.util.List;
import java.util.UUID;

import com.marketmind.ai.domain.DocumentChunk;

public interface VectorStore {

    void ensureCollection(int vectorSize);

    UUID upsert(DocumentChunk chunk, String title, List<Double> vector);

    List<VectorSearchResult> search(
            List<Double> vector,
            UUID documentId,
            int topK);
}
