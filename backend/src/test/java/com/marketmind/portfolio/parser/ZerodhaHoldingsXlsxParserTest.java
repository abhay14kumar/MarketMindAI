package com.marketmind.portfolio.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.marketmind.portfolio.domain.InstrumentType;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ZerodhaHoldingsXlsxParserTest {

    private final ZerodhaHoldingsXlsxParser parser = new ZerodhaHoldingsXlsxParser();

    @Test
    void shouldParseExactZerodhaHeadersAtExcelRowTwentyThree() throws Exception {
        byte[] workbook = zerodhaWorkbook(false);

        PortfolioParseResult result = parser.parse(new ByteArrayInputStream(workbook));

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.errors()).isEmpty();
        assertThat(result.holdings()).singleElement().satisfies(holding -> {
            assertThat(holding.symbol()).isEqualTo("NIFTYBEES");
            assertThat(holding.instrumentType()).isEqualTo(InstrumentType.ETF);
            assertThat(holding.investedValue()).isEqualByComparingTo("2000");
            assertThat(holding.presentValue()).isEqualByComparingTo("2500");
            assertThat(holding.unrealizedPnl()).isEqualByComparingTo("500");
            assertThat(holding.unrealizedPnlPercentage()).isEqualByComparingTo("25");
            assertThat(holding.previousClose()).isEqualByComparingTo("250");
        });
    }

    @Test
    void shouldCollectBadRowsWithoutFailingValidRows() throws Exception {
        PortfolioParseResult result = parser.parse(
                new ByteArrayInputStream(zerodhaWorkbook(true)));

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.holdings()).hasSize(1);
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.rowNumber()).isEqualTo(25);
            assertThat(error.message()).contains("Combined");
            assertThat(error.message()).contains("quantity");
        });
    }

    @Test
    void shouldPreferCombinedSheetOverEquityAndMutualFunds() throws Exception {
        try (var workbook = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            addSheet(workbook, "Equity", 22, "INFY", "Technology", "-", 10, 100, 110);
            addSheet(workbook, "Mutual Funds", 21, "FUND1", "Mutual Fund", "Mutual Fund", 5, 50, 60);
            addSheet(workbook, "Combined", 22, "NIFTYBEES", "ETF", "-", 10, 200, 250);
            workbook.write(output);

            PortfolioParseResult result = parser.parse(
                    new ByteArrayInputStream(output.toByteArray()));

            assertThat(result.holdings()).extracting(ParsedHolding::symbol)
                    .containsExactly("NIFTYBEES");
        }
    }

    @Test
    void shouldParseEquityAndMutualFundsWhenCombinedIsAbsent() throws Exception {
        try (var workbook = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            addSheet(workbook, "Equity", 22, "INFY", "Technology", "-", 10, 100, 110);
            addSheet(workbook, "Mutual Funds", 21, "FUND1", "Mutual Fund", "Mutual Fund", 5, 50, 60);
            workbook.write(output);

            PortfolioParseResult result = parser.parse(
                    new ByteArrayInputStream(output.toByteArray()));

            assertThat(result.holdings()).extracting(ParsedHolding::symbol)
                    .containsExactly("INFY", "FUND1");
        }
    }

    @Test
    void shouldReportDetectedSheetsWhenHeaderCannotBeFound() throws Exception {
        try (var workbook = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            workbook.createSheet("Combined").createRow(22)
                    .createCell(0).setCellValue("Not a holdings header");
            workbook.write(output);

            assertThatThrownBy(() -> parser.parse(
                    new ByteArrayInputStream(output.toByteArray())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Detected sheets: [Combined]")
                    .hasMessageContaining("Detected header rows: none")
                    .hasMessageContaining("first 40 rows");
        }
    }

    private byte[] zerodhaWorkbook(boolean addInvalidRow) throws Exception {
        try (var workbook = new XSSFWorkbook();
                var output = new ByteArrayOutputStream()) {
            var sheet = addSheet(
                    workbook, "Combined", 22, "NIFTYBEES", "ETF", "-", 10, 200, 250);

            // A non-empty summary/footer row without a symbol must be ignored.
            sheet.createRow(24).createCell(1).setCellValue("Portfolio total");

            if (addInvalidRow) {
                var invalid = sheet.createRow(24);
                invalid.createCell(0).setCellValue("INFY");
                invalid.createCell(4).setCellValue("not-a-number");
                invalid.createCell(9).setCellValue(1500);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private org.apache.poi.ss.usermodel.Sheet addSheet(
            XSSFWorkbook workbook,
            String sheetName,
            int headerRowIndex,
            String symbol,
            String sector,
            String instrumentType,
            double quantity,
            double averagePrice,
            double previousClose) {
        var sheet = workbook.createSheet(sheetName);
        var header = sheet.createRow(headerRowIndex);
        String[] headers = {
            "Symbol",
            "ISIN",
            "Sector",
            "Instrument Type",
            "Quantity Available",
            "Quantity Discrepant",
            "Quantity Long Term",
            "Quantity Pledged (Margin)",
            "Quantity Pledged (Loan)",
            "Average Price",
            "Previous Closing Price",
            "Unrealized P&L",
            "Unrealize P&L Pct."
        };
        for (int index = 0; index < headers.length; index++) {
            header.createCell(index).setCellValue(headers[index]);
        }

        var holding = sheet.createRow(headerRowIndex + 1);
        holding.createCell(0).setCellValue(symbol);
        holding.createCell(1).setCellValue("INF204KB14I2");
        holding.createCell(2).setCellValue(sector);
        holding.createCell(3).setCellValue(instrumentType);
        holding.createCell(4).setCellValue(quantity);
        holding.createCell(9).setCellValue(averagePrice);
        holding.createCell(10).setCellValue(previousClose);
        return sheet;
    }
}
