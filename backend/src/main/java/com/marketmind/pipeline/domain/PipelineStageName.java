package com.marketmind.pipeline.domain;

public enum PipelineStageName {
    DISCOVERY,
    DOWNLOAD,
    TEXT_EXTRACTION,
    CHUNKING,
    EMBEDDING,
    QDRANT_INDEXING,
    AI_SUMMARY,
    AI_READY
}
