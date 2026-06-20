package com.marketmind.documents.application;

import java.net.URI;

import com.marketmind.documents.domain.SourceType;

public record CreateDocumentSourceCommand(
        String code,
        String name,
        SourceType sourceType,
        URI baseUrl,
        boolean enabled) {
}
