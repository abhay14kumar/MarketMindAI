package com.marketmind.documents.application;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import com.marketmind.common.exception.ResourceNotFoundException;
import com.marketmind.documents.domain.Document;
import com.marketmind.documents.domain.DocumentTextExtraction;
import com.marketmind.documents.domain.DocumentVersion;
import com.marketmind.documents.domain.ExtractionStatus;
import com.marketmind.documents.infrastructure.UnsupportedDocumentFormatException;

import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractionService {

    private static final String PDF_MIME_TYPE = "application/pdf";

    private final DocumentCatalog documentCatalog;
    private final DocumentTextExtractionRepository extractionRepository;
    private final StorageProvider storageProvider;
    private final Parser parser;
    private final Clock clock;

    public PdfTextExtractionService(
            DocumentCatalog documentCatalog,
            DocumentTextExtractionRepository extractionRepository,
            StorageProvider storageProvider,
            Parser parser,
            Clock clock) {
        this.documentCatalog = documentCatalog;
        this.extractionRepository = extractionRepository;
        this.storageProvider = storageProvider;
        this.parser = parser;
        this.clock = clock;
    }

    public DocumentTextExtraction extract(UUID documentId) {
        Document document = documentCatalog.findDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + documentId));
        DocumentVersion version = resolveVersion(document);
        Instant startedAt = clock.instant();
        UUID extractionId = UUID.randomUUID();
        extractionRepository.save(new DocumentTextExtraction(
                extractionId, document.id(), version.id(), ExtractionStatus.STARTED,
                null, null, null, null, null, startedAt));

        if (!isPdf(version.mimeType())) {
            return saveFinal(
                    extractionId, document.id(), version.id(),
                    ExtractionStatus.UNSUPPORTED, null, null,
                    "Only PDF text extraction is supported.", startedAt);
        }
        if (version.storageReference() == null || version.storageReference().isBlank()) {
            return saveFinal(
                    extractionId, document.id(), version.id(),
                    ExtractionStatus.FAILED, null, null,
                    "The document version has no stored file reference.", startedAt);
        }

        try (InputStream content = storageProvider.load(version.storageReference())) {
            Parser.ParseResult parsed = parser.parse(new Parser.ParseRequest(
                    content.readAllBytes(), version.mimeType()));
            int pageCount = parsePageCount(parsed);
            String text = parsed.text() == null ? "" : parsed.text().strip();
            if (text.isBlank()) {
                return saveFinal(
                        extractionId, document.id(), version.id(),
                        ExtractionStatus.UNSUPPORTED, "", pageCount,
                        "No extractable text was found. The PDF may be scanned or image-only; OCR is not enabled.",
                        startedAt);
            }
            return saveFinal(
                    extractionId, document.id(), version.id(),
                    ExtractionStatus.COMPLETED, text, pageCount, null, startedAt);
        } catch (UnsupportedDocumentFormatException exception) {
            return saveFinal(
                    extractionId, document.id(), version.id(),
                    ExtractionStatus.UNSUPPORTED, null, null,
                    safeMessage(exception), startedAt);
        } catch (IOException | RuntimeException exception) {
            return saveFinal(
                    extractionId, document.id(), version.id(),
                    ExtractionStatus.FAILED, null, null,
                    safeMessage(exception), startedAt);
        }
    }

    public DocumentTextExtraction getLatest(UUID documentId) {
        if (documentCatalog.findDocumentById(documentId).isEmpty()) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        return extractionRepository.findLatestByDocumentId(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No text extraction exists for document: " + documentId));
    }

    private DocumentVersion resolveVersion(Document document) {
        return documentCatalog.findVersionsByDocumentId(document.id()).stream()
                .filter(version -> document.currentVersionId() == null
                        || version.id().equals(document.currentVersionId()))
                .max(Comparator.comparingInt(DocumentVersion::versionNumber))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stored version exists for document: " + document.id()));
    }

    private DocumentTextExtraction saveFinal(
            UUID id,
            UUID documentId,
            UUID versionId,
            ExtractionStatus status,
            String text,
            Integer pageCount,
            String errorMessage,
            Instant createdAt) {
        Instant extractedAt = clock.instant();
        return extractionRepository.save(new DocumentTextExtraction(
                id, documentId, versionId, status, text, pageCount,
                text == null ? null : (long) text.length(),
                truncate(errorMessage), extractedAt, createdAt));
    }

    private int parsePageCount(Parser.ParseResult parsed) {
        try {
            return Integer.parseInt(parsed.metadata().getOrDefault("pageCount", "0"));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean isPdf(String contentType) {
        return contentType != null
                && contentType.toLowerCase(java.util.Locale.ROOT).startsWith(PDF_MIME_TYPE);
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private String truncate(String value) {
        return value == null || value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
