package com.marketmind.portfolio.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketmind.portfolio.application.PageResult;
import com.marketmind.portfolio.application.PortfolioRepository;
import com.marketmind.portfolio.domain.BrokerType;
import com.marketmind.portfolio.domain.ImportStatus;
import com.marketmind.portfolio.domain.InstrumentType;
import com.marketmind.portfolio.domain.Portfolio;
import com.marketmind.portfolio.domain.PortfolioHolding;
import com.marketmind.portfolio.domain.PortfolioImportJob;
import com.marketmind.portfolio.domain.PortfolioSnapshot;
import com.marketmind.portfolio.domain.RowImportError;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortfolioRepository implements PortfolioRepository {

    private static final TypeReference<List<RowImportError>> ROW_ERRORS_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcPortfolioRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Portfolio getOrCreatePortfolio(BrokerType brokerType, String name) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO portfolio (id, name, broker_type, currency, created_at, updated_at)
                VALUES (?, ?, ?, 'INR', ?, ?)
                ON CONFLICT (broker_type, name) DO NOTHING
                """,
                UUID.randomUUID(), name, brokerType.name(), timestamp(now), timestamp(now));
        return jdbcTemplate.queryForObject("""
                SELECT id, name, broker_type, currency, created_at, updated_at
                FROM portfolio
                WHERE broker_type = ? AND name = ?
                """, portfolioMapper(), brokerType.name(), name);
    }

    @Override
    public PortfolioImportJob saveImportJob(PortfolioImportJob job) {
        jdbcTemplate.update("""
                INSERT INTO portfolio_import_job (
                    id, portfolio_id, broker_type, original_file_name, status,
                    total_rows, imported_rows, rejected_rows, row_errors, error_message,
                    started_at, completed_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    total_rows = EXCLUDED.total_rows,
                    imported_rows = EXCLUDED.imported_rows,
                    rejected_rows = EXCLUDED.rejected_rows,
                    row_errors = EXCLUDED.row_errors,
                    error_message = EXCLUDED.error_message,
                    completed_at = EXCLUDED.completed_at
                """,
                job.id(), job.portfolioId(), job.brokerType().name(), job.originalFileName(),
                job.status().name(), job.totalRows(), job.importedRows(), job.rejectedRows(),
                serializeErrors(job.rowErrors()), job.errorMessage(), timestamp(job.startedAt()),
                timestamp(job.completedAt()), timestamp(job.createdAt()));
        return job;
    }

    @Override
    public void replaceHoldings(UUID portfolioId, List<PortfolioHolding> holdings) {
        jdbcTemplate.update("DELETE FROM portfolio_holding WHERE portfolio_id = ?", portfolioId);
        jdbcTemplate.batchUpdate("""
                INSERT INTO portfolio_holding (
                    id, portfolio_id, import_job_id, symbol, isin, company_name, sector,
                    instrument_type, quantity, average_cost, last_price, previous_close,
                    invested_value, present_value, unrealized_pnl,
                    unrealized_pnl_percentage, as_of, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                holdings,
                Math.min(holdings.size(), 250),
                (statement, holding) -> {
                    statement.setObject(1, holding.id());
                    statement.setObject(2, holding.portfolioId());
                    statement.setObject(3, holding.importJobId());
                    statement.setString(4, holding.symbol());
                    statement.setString(5, holding.isin());
                    statement.setString(6, holding.companyName());
                    statement.setString(7, holding.sector());
                    statement.setString(8, holding.instrumentType().name());
                    statement.setBigDecimal(9, holding.quantity());
                    statement.setBigDecimal(10, holding.averageCost());
                    statement.setBigDecimal(11, holding.lastPrice());
                    statement.setBigDecimal(12, holding.previousClose());
                    statement.setBigDecimal(13, holding.investedValue());
                    statement.setBigDecimal(14, holding.presentValue());
                    statement.setBigDecimal(15, holding.unrealizedPnl());
                    statement.setBigDecimal(16, holding.unrealizedPnlPercentage());
                    statement.setTimestamp(17, timestamp(holding.asOf()));
                    statement.setTimestamp(18, timestamp(holding.createdAt()));
                    statement.setTimestamp(19, timestamp(holding.updatedAt()));
                });
    }

    @Override
    public PortfolioSnapshot saveSnapshot(PortfolioSnapshot snapshot) {
        jdbcTemplate.update("""
                INSERT INTO portfolio_snapshot (
                    id, portfolio_id, import_job_id, total_invested_value,
                    total_present_value, total_unrealized_pnl,
                    total_unrealized_pnl_percentage, total_holdings, captured_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(), snapshot.portfolioId(), snapshot.importJobId(),
                snapshot.totalInvestedValue(), snapshot.totalPresentValue(),
                snapshot.totalUnrealizedPnl(), snapshot.totalUnrealizedPnlPercentage(),
                snapshot.totalHoldings(), timestamp(snapshot.capturedAt()),
                timestamp(snapshot.createdAt()));
        return snapshot;
    }

    @Override
    public Optional<PortfolioSnapshot> findLatestSnapshot(UUID portfolioId) {
        return jdbcTemplate.query("""
                SELECT id, portfolio_id, import_job_id, total_invested_value,
                       total_present_value, total_unrealized_pnl,
                       total_unrealized_pnl_percentage, total_holdings,
                       captured_at, created_at
                FROM portfolio_snapshot
                WHERE portfolio_id = ?
                ORDER BY captured_at DESC
                LIMIT 1
                """, snapshotMapper(), portfolioId).stream().findFirst();
    }

    @Override
    public PageResult<PortfolioHolding> findHoldings(UUID portfolioId, int page, int size) {
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portfolio_holding WHERE portfolio_id = ?",
                Long.class,
                portfolioId);
        List<PortfolioHolding> content = jdbcTemplate.query("""
                SELECT id, portfolio_id, import_job_id, symbol, isin, company_name, sector,
                       instrument_type, quantity, average_cost, last_price, previous_close,
                       invested_value, present_value, unrealized_pnl,
                       unrealized_pnl_percentage, as_of, created_at, updated_at
                FROM portfolio_holding
                WHERE portfolio_id = ?
                ORDER BY present_value DESC, symbol ASC
                LIMIT ? OFFSET ?
                """, holdingMapper(), portfolioId, size, (long) page * size);
        return page(content, page, size, total);
    }

    @Override
    public PageResult<PortfolioImportJob> findImportJobs(UUID portfolioId, int page, int size) {
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portfolio_import_job WHERE portfolio_id = ?",
                Long.class,
                portfolioId);
        List<PortfolioImportJob> content = jdbcTemplate.query("""
                SELECT id, portfolio_id, broker_type, original_file_name, status,
                       total_rows, imported_rows, rejected_rows, row_errors::TEXT,
                       error_message, started_at, completed_at, created_at
                FROM portfolio_import_job
                WHERE portfolio_id = ?
                ORDER BY started_at DESC
                LIMIT ? OFFSET ?
                """, importJobMapper(), portfolioId, size, (long) page * size);
        return page(content, page, size, total);
    }

    private RowMapper<Portfolio> portfolioMapper() {
        return (resultSet, rowNumber) -> new Portfolio(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                BrokerType.valueOf(resultSet.getString("broker_type")),
                resultSet.getString("currency"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private RowMapper<PortfolioHolding> holdingMapper() {
        return (resultSet, rowNumber) -> new PortfolioHolding(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("portfolio_id", UUID.class),
                resultSet.getObject("import_job_id", UUID.class),
                resultSet.getString("symbol"),
                resultSet.getString("isin"),
                resultSet.getString("company_name"),
                resultSet.getString("sector"),
                InstrumentType.valueOf(resultSet.getString("instrument_type")),
                resultSet.getBigDecimal("quantity"),
                resultSet.getBigDecimal("average_cost"),
                resultSet.getBigDecimal("last_price"),
                resultSet.getBigDecimal("previous_close"),
                resultSet.getBigDecimal("invested_value"),
                resultSet.getBigDecimal("present_value"),
                resultSet.getBigDecimal("unrealized_pnl"),
                resultSet.getBigDecimal("unrealized_pnl_percentage"),
                instant(resultSet, "as_of"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private RowMapper<PortfolioImportJob> importJobMapper() {
        return (resultSet, rowNumber) -> new PortfolioImportJob(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("portfolio_id", UUID.class),
                BrokerType.valueOf(resultSet.getString("broker_type")),
                resultSet.getString("original_file_name"),
                ImportStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("total_rows"),
                resultSet.getInt("imported_rows"),
                resultSet.getInt("rejected_rows"),
                deserializeErrors(resultSet.getString("row_errors")),
                resultSet.getString("error_message"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"));
    }

    private RowMapper<PortfolioSnapshot> snapshotMapper() {
        return (resultSet, rowNumber) -> new PortfolioSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("portfolio_id", UUID.class),
                resultSet.getObject("import_job_id", UUID.class),
                resultSet.getBigDecimal("total_invested_value"),
                resultSet.getBigDecimal("total_present_value"),
                resultSet.getBigDecimal("total_unrealized_pnl"),
                resultSet.getBigDecimal("total_unrealized_pnl_percentage"),
                resultSet.getInt("total_holdings"),
                instant(resultSet, "captured_at"),
                instant(resultSet, "created_at"));
    }

    private <T> PageResult<T> page(List<T> content, int page, int size, long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private String serializeErrors(List<RowImportError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize portfolio row errors.", exception);
        }
    }

    private List<RowImportError> deserializeErrors(String value) {
        try {
            return objectMapper.readValue(value, ROW_ERRORS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize portfolio row errors.", exception);
        }
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
