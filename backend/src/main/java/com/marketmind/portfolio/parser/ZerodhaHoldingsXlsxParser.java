package com.marketmind.portfolio.parser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.marketmind.portfolio.domain.InstrumentType;
import com.marketmind.portfolio.domain.RowImportError;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ZerodhaHoldingsXlsxParser implements PortfolioFileParser {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int HEADER_SCAN_LIMIT = 40;
    private static final Map<String, Set<String>> HEADER_ALIASES = Map.ofEntries(
            Map.entry("symbol", Set.of("symbol", "trading symbol", "tradingsymbol")),
            Map.entry("isin", Set.of("isin")),
            Map.entry("companyName", Set.of("company", "company name", "security name", "name")),
            Map.entry("sector", Set.of("sector")),
            Map.entry("instrumentType", Set.of("instrument type")),
            Map.entry("quantity", Set.of("quantity available", "quantity")),
            Map.entry("averageCost", Set.of("average price", "average cost", "avg cost")),
            Map.entry("lastPrice", Set.of("last price", "ltp", "last traded price")),
            Map.entry("previousClose", Set.of("previous closing price", "previous close")),
            Map.entry("investedValue", Set.of(
                    "invested value", "investment value", "invested", "inv val")),
            Map.entry("presentValue", Set.of(
                    "present value", "current value", "market value", "cur val")),
            Map.entry("pnl", Set.of(
                    "unrealized p&l", "unrealised p&l", "unrealized pnl",
                    "unrealised pnl", "pnl", "p&l")),
            Map.entry("pnlPercentage", Set.of(
                    "unrealized p&l pct", "unrealised p&l pct", "unrealize p&l pct",
                    "unrealized pnl %", "unrealised pnl %", "pnl %", "p&l %",
                    "unrealized pnl percentage", "unrealised pnl percentage")));

    @Override
    public PortfolioParseResult parse(InputStream input) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("The workbook has no worksheets.");
            }
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<Sheet> sheets = selectSheets(workbook);
            Map<String, Integer> detectedHeaders = detectHeaders(
                    workbook, formatter, evaluator);

            List<ParsedHolding> holdings = new ArrayList<>();
            List<RowImportError> errors = new ArrayList<>();
            int totalRows = 0;
            for (Sheet sheet : sheets) {
                Header header = findHeader(sheet, formatter, evaluator);
                if (header == null) {
                    continue;
                }
                PortfolioParseResult sheetResult = parseRows(
                        sheet, header, formatter, evaluator);
                totalRows += sheetResult.totalRows();
                holdings.addAll(sheetResult.holdings());
                errors.addAll(sheetResult.errors());
            }

            if (detectedHeaders.entrySet().stream()
                    .noneMatch(entry -> containsSheet(sheets, entry.getKey()))) {
                throw new IllegalArgumentException(headerDetectionError(
                        workbook, detectedHeaders));
            }
            return new PortfolioParseResult(totalRows, holdings, errors);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The uploaded file is not a readable XLSX workbook.", exception);
        }
    }

    private PortfolioParseResult parseRows(
            Sheet sheet,
            Header header,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        List<ParsedHolding> holdings = new ArrayList<>();
        List<RowImportError> errors = new ArrayList<>();
        int totalRows = 0;

        for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, formatter, evaluator)) {
                continue;
            }
            String symbol = text(row, header.columns(), "symbol", formatter, evaluator);
            if (symbol.isBlank()) {
                continue;
            }
            totalRows++;
            try {
                holdings.add(parseRow(row, header.columns(), formatter, evaluator));
            } catch (RuntimeException exception) {
                errors.add(new RowImportError(
                        rowIndex + 1,
                        "Sheet '" + sheet.getSheetName() + "': "
                                + (exception.getMessage() == null
                                        ? "Invalid holding row."
                                        : exception.getMessage())));
            }
        }
        return new PortfolioParseResult(totalRows, holdings, errors);
    }

    private ParsedHolding parseRow(
            Row row,
            Map<String, Integer> columns,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        String symbol = requiredText(row, columns, "symbol", formatter, evaluator).toUpperCase(Locale.ROOT);
        BigDecimal quantity = requiredDecimal(row, columns, "quantity", formatter, evaluator);
        BigDecimal averageCost = requiredDecimal(row, columns, "averageCost", formatter, evaluator);
        if (quantity.signum() < 0 || averageCost.signum() < 0) {
            throw new IllegalArgumentException("Quantity and average cost cannot be negative.");
        }

        BigDecimal previousClose = optionalDecimal(row, columns, "previousClose", formatter, evaluator);
        BigDecimal lastPrice = defaultIfNull(
                optionalDecimal(row, columns, "lastPrice", formatter, evaluator),
                previousClose);
        BigDecimal investedValue = quantity.multiply(averageCost);
        BigDecimal presentValue = previousClose == null
                ? investedValue
                : quantity.multiply(previousClose);
        BigDecimal pnl = defaultIfNull(
                optionalDecimal(row, columns, "pnl", formatter, evaluator),
                presentValue.subtract(investedValue));
        BigDecimal pnlPercentage = optionalDecimal(
                row, columns, "pnlPercentage", formatter, evaluator);
        if (pnlPercentage == null) {
            pnlPercentage = investedValue.signum() == 0
                    ? BigDecimal.ZERO
                    : pnl.multiply(ONE_HUNDRED).divide(investedValue, 4, RoundingMode.HALF_UP);
        }

        String explicitType = text(row, columns, "instrumentType", formatter, evaluator);
        String sector = blankToNull(text(row, columns, "sector", formatter, evaluator));
        return new ParsedHolding(
                symbol,
                blankToNull(text(row, columns, "isin", formatter, evaluator)),
                blankToNull(text(row, columns, "companyName", formatter, evaluator)),
                sector,
                instrumentType(symbol, sector, explicitType),
                quantity, averageCost, lastPrice, previousClose,
                investedValue, presentValue, pnl, pnlPercentage);
    }

    private Header findHeader(
            Sheet sheet,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        int end = Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT - 1);
        for (int index = sheet.getFirstRowNum(); index <= end; index++) {
            Row row = sheet.getRow(index);
            Map<String, Integer> columns = mapColumns(row, formatter, evaluator);
            if (columns.containsKey("symbol")
                    && columns.containsKey("quantity")
                    && columns.containsKey("averageCost")) {
                return new Header(index, columns);
            }
        }
        return null;
    }

    private List<Sheet> selectSheets(Workbook workbook) {
        Sheet combined = findSheet(workbook, "Combined");
        if (combined != null) {
            return List.of(combined);
        }
        List<Sheet> selected = new ArrayList<>();
        Sheet equity = findSheet(workbook, "Equity");
        Sheet mutualFunds = findSheet(workbook, "Mutual Funds");
        if (equity != null) {
            selected.add(equity);
        }
        if (mutualFunds != null) {
            selected.add(mutualFunds);
        }
        return selected.isEmpty()
                ? IntStream.range(0, workbook.getNumberOfSheets())
                        .mapToObj(workbook::getSheetAt)
                        .toList()
                : List.copyOf(selected);
    }

    private Sheet findSheet(Workbook workbook, String expectedName) {
        return IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj(workbook::getSheetAt)
                .filter(sheet -> sheet.getSheetName().trim().equalsIgnoreCase(expectedName))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Integer> detectHeaders(
            Workbook workbook,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        Map<String, Integer> detected = new LinkedHashMap<>();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            Header header = findHeader(sheet, formatter, evaluator);
            if (header != null) {
                detected.put(sheet.getSheetName(), header.rowIndex() + 1);
            }
        }
        return detected;
    }

    private boolean containsSheet(List<Sheet> sheets, String name) {
        return sheets.stream().anyMatch(sheet -> sheet.getSheetName().equals(name));
    }

    private String headerDetectionError(
            Workbook workbook,
            Map<String, Integer> detectedHeaders) {
        List<String> sheetNames = IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj(index -> workbook.getSheetAt(index).getSheetName())
                .toList();
        String headerRows = detectedHeaders.isEmpty()
                ? "none"
                : detectedHeaders.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("none");
        return "Could not find a Zerodha holdings header in the selected sheets. "
                + "Required headers are Symbol, Quantity Available, and Average Price. "
                + "Detected sheets: " + sheetNames + ". Detected header rows: "
                + headerRows + ". Scanned the first " + HEADER_SCAN_LIMIT
                + " rows of each sheet.";
    }

    private Map<String, Integer> mapColumns(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        Map<String, Integer> columns = new HashMap<>();
        if (row == null) {
            return columns;
        }
        for (Cell cell : row) {
            String header = normalize(formatter.formatCellValue(cell, evaluator));
            HEADER_ALIASES.forEach((canonical, aliases) -> {
                if (aliases.contains(header)) {
                    columns.putIfAbsent(canonical, cell.getColumnIndex());
                }
            });
        }
        return columns;
    }

    private String requiredText(
            Row row,
            Map<String, Integer> columns,
            String name,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        String value = text(row, columns, name, formatter, evaluator);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Required value is missing: " + name + ".");
        }
        return value;
    }

    private BigDecimal requiredDecimal(
            Row row,
            Map<String, Integer> columns,
            String name,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        BigDecimal value = optionalDecimal(row, columns, name, formatter, evaluator);
        if (value == null) {
            throw new IllegalArgumentException("Required numeric value is missing: " + name + ".");
        }
        return value;
    }

    private BigDecimal optionalDecimal(
            Row row,
            Map<String, Integer> columns,
            String name,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        String value = text(row, columns, name, formatter, evaluator);
        if (value.isBlank() || value.equals("-")) {
            return null;
        }
        String normalized = value.replace(",", "")
                .replace("₹", "")
                .replace("%", "")
                .trim();
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = "-" + normalized.substring(1, normalized.length() - 1);
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid numeric value for " + name + ": " + value + ".");
        }
    }

    private String text(
            Row row,
            Map<String, Integer> columns,
            String name,
            DataFormatter formatter,
            FormulaEvaluator evaluator) {
        Integer column = columns.get(name);
        if (column == null || row == null) {
            return "";
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
    }

    private boolean isBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private InstrumentType instrumentType(
            String symbol,
            String sector,
            String explicitType) {
        String value = normalize(explicitType);
        String normalizedSector = normalize(sector);
        if (value.contains("mutual") || value.equals("mf")) {
            return InstrumentType.MUTUAL_FUND;
        }
        if (value.contains("etf")
                || normalizedSector.equals("etf")
                || symbol.endsWith("ETF")
                || symbol.endsWith("BEES")) {
            return InstrumentType.ETF;
        }
        if (value.contains("equity") || value.contains("stock") || value.equals("eq")) {
            return InstrumentType.EQUITY;
        }
        return explicitType == null || explicitType.isBlank() || explicitType.trim().equals("-")
                ? InstrumentType.EQUITY
                : InstrumentType.UNKNOWN;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace(".", "")
                        .replaceAll("\\s+", " ");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private record Header(int rowIndex, Map<String, Integer> columns) {
    }
}
