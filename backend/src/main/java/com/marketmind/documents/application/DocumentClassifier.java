package com.marketmind.documents.application;

import com.marketmind.documents.domain.DocumentType;

public interface DocumentClassifier {

    DocumentType classify(ClassificationRequest request);

    record ClassificationRequest(String title, String sourceUrl, String contentType) {
    }
}
