package com.marketmind.documents.mapper;

import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.dto.DocumentTextExtractionResponse;

import org.springframework.stereotype.Component;

@Component
public class DocumentTextExtractionMapper {

    public DocumentTextExtractionResponse toResponse(DocumentTextExtraction extraction) {
        return new DocumentTextExtractionResponse(
                extraction.id(),
                extraction.documentId(),
                extraction.documentVersionId(),
                extraction.extractionStatus(),
                extraction.extractedText(),
                extraction.pageCount(),
                extraction.characterCount(),
                extraction.errorMessage(),
                extraction.extractedAt(),
                extraction.createdAt());
    }
}
