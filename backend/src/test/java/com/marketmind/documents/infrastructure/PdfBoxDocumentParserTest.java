package com.marketmind.documents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import com.marketmind.documents.application.Parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfBoxDocumentParserTest {

    private final PdfBoxDocumentParser parser = new PdfBoxDocumentParser();

    @Test
    void shouldExtractTextAndPageCount() throws Exception {
        byte[] pdf = pdfWithText("MarketMind annual report");

        Parser.ParseResult result = parser.parse(
                new Parser.ParseRequest(pdf, "application/pdf"));

        assertThat(result.text()).contains("MarketMind annual report");
        assertThat(result.metadata()).containsEntry("pageCount", "1");
    }

    @Test
    void shouldReturnBlankTextForImageOnlyStylePdfWithoutOcr() throws Exception {
        byte[] pdf;
        try (var document = new PDDocument();
                var output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        Parser.ParseResult result = parser.parse(
                new Parser.ParseRequest(pdf, "application/pdf"));

        assertThat(result.text()).isBlank();
        assertThat(result.metadata()).containsEntry("pageCount", "1");
    }

    private byte[] pdfWithText(String text) throws Exception {
        try (var document = new PDDocument();
                var output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
