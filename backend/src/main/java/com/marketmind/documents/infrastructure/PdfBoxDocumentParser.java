package com.marketmind.documents.infrastructure;

import java.io.IOException;
import java.util.Map;

import com.marketmind.documents.application.Parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfBoxDocumentParser implements Parser {

    @Override
    public ParseResult parse(ParseRequest request) {
        if (!isPdf(request.contentType())) {
            throw new UnsupportedDocumentFormatException(
                    "Only application/pdf documents are supported.");
        }
        try (var document = Loader.loadPDF(request.content())) {
            String text = new PDFTextStripper().getText(document);
            String normalized = text == null ? "" : text.strip();
            return new ParseResult(
                    normalized,
                    Map.of("pageCount", Integer.toString(document.getNumberOfPages())));
        } catch (IOException exception) {
            throw new DocumentParsingException("Unable to parse the PDF document.", exception);
        }
    }

    private boolean isPdf(String contentType) {
        return contentType != null
                && contentType.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("application/pdf");
    }
}
