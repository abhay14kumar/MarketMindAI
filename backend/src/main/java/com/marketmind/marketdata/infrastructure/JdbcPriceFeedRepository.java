package com.marketmind.marketdata.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.marketmind.marketdata.application.PortfolioPriceInput;
import com.marketmind.marketdata.application.PriceFeedRepository;
import com.marketmind.marketdata.domain.Exchange;
import com.marketmind.marketdata.domain.MarketInstrument;
import com.marketmind.marketdata.domain.PriceFeedJob;
import com.marketmind.marketdata.domain.PriceFeedStatus;
import com.marketmind.marketdata.domain.PriceSnapshot;
import com.marketmind.marketdata.domain.PriceSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPriceFeedRepository implements PriceFeedRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPriceFeedRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MarketInstrument saveInstrument(MarketInstrument instrument) {
        jdbcTemplate.update("""
                INSERT INTO market_instrument (
                    id, symbol, isin, name, exchange, currency, active, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (exchange, symbol) DO UPDATE SET
                    isin = COALESCE(EXCLUDED.isin, market_instrument.isin),
                    name = COALESCE(EXCLUDED.name, market_instrument.name),
                    currency = EXCLUDED.currency,
                    active = EXCLUDED.active,
                    updated_at = EXCLUDED.updated_at
                """,
                instrument.id(), instrument.symbol(), instrument.isin(), instrument.name(),
                instrument.exchange().name(), instrument.currency(), instrument.active(),
                timestamp(instrument.createdAt()), timestamp(instrument.updatedAt()));
        return findInstrument(instrument.symbol(), instrument.exchange()).orElseThrow();
    }

    @Override
    public Optional<MarketInstrument> findInstrument(String symbol, Exchange exchange) {
        return jdbcTemplate.query("""
                SELECT id, symbol, isin, name, exchange, currency, active, created_at, updated_at
                FROM market_instrument
                WHERE symbol = ? AND exchange = ?
                """, instrumentMapper(), symbol, exchange.name()).stream().findFirst();
    }

    @Override
    public List<MarketInstrument> findInstruments() {
        return jdbcTemplate.query("""
                SELECT id, symbol, isin, name, exchange, currency, active, created_at, updated_at
                FROM market_instrument
                ORDER BY symbol, exchange
                """, instrumentMapper());
    }

    @Override
    public PriceSnapshot saveSnapshot(PriceSnapshot snapshot, UUID feedJobId) {
        jdbcTemplate.update("""
                INSERT INTO price_snapshot (
                    id, instrument_id, feed_job_id, last_price, previous_close,
                    source, captured_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(), snapshot.instrumentId(), feedJobId, snapshot.lastPrice(),
                snapshot.previousClose(), snapshot.source().name(),
                timestamp(snapshot.capturedAt()), timestamp(snapshot.createdAt()));
        return snapshot;
    }

    @Override
    public Optional<PriceSnapshot> findLatest(String symbol) {
        return jdbcTemplate.query("""
                SELECT ps.id, ps.instrument_id, mi.symbol, mi.exchange,
                       ps.last_price, ps.previous_close, ps.source,
                       ps.captured_at, ps.created_at
                FROM price_snapshot ps
                JOIN market_instrument mi ON mi.id = ps.instrument_id
                WHERE mi.symbol = ?
                ORDER BY
                    CASE ps.source
                        WHEN 'REAL' THEN 100
                        WHEN 'PUBLIC' THEN 95
                        WHEN 'YAHOO' THEN 90
                        WHEN 'FINNHUB' THEN 90
                        WHEN 'NSE' THEN 90
                        WHEN 'ZERODHA' THEN 85
                        WHEN 'MANUAL' THEN 70
                        WHEN 'MOCK' THEN 10
                        ELSE 0
                    END DESC,
                    ps.captured_at DESC,
                    ps.created_at DESC
                LIMIT 1
                """, snapshotMapper(), symbol).stream().findFirst();
    }

    @Override
    public List<PriceSnapshot> findAllLatest() {
        return jdbcTemplate.query("""
                SELECT DISTINCT ON (mi.symbol)
                       ps.id, ps.instrument_id, mi.symbol, mi.exchange,
                       ps.last_price, ps.previous_close, ps.source,
                       ps.captured_at, ps.created_at
                FROM price_snapshot ps
                JOIN market_instrument mi ON mi.id = ps.instrument_id
                ORDER BY mi.symbol,
                    CASE ps.source
                        WHEN 'REAL' THEN 100
                        WHEN 'PUBLIC' THEN 95
                        WHEN 'YAHOO' THEN 90
                        WHEN 'FINNHUB' THEN 90
                        WHEN 'NSE' THEN 90
                        WHEN 'ZERODHA' THEN 85
                        WHEN 'MANUAL' THEN 70
                        WHEN 'MOCK' THEN 10
                        ELSE 0
                    END DESC,
                    ps.captured_at DESC,
                    ps.created_at DESC
                """, snapshotMapper());
    }

    @Override
    public PriceFeedJob saveJob(PriceFeedJob job) {
        jdbcTemplate.update("""
                INSERT INTO price_feed_job (
                    id, source, provider, status,
                    requested_instruments, updated_instruments,
                    requested_symbols_count, successful_symbols_count,
                    failed_symbols_count, error_message, error_summary,
                    started_at, completed_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, LEFT(?, 1000), ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    provider = EXCLUDED.provider,
                    status = EXCLUDED.status,
                    updated_instruments = EXCLUDED.updated_instruments,
                    requested_symbols_count = EXCLUDED.requested_symbols_count,
                    successful_symbols_count = EXCLUDED.successful_symbols_count,
                    failed_symbols_count = EXCLUDED.failed_symbols_count,
                    error_message = EXCLUDED.error_message,
                    error_summary = EXCLUDED.error_summary,
                    completed_at = EXCLUDED.completed_at
                """,
                job.id(), job.source().name(), job.provider(), job.status().name(),
                job.requestedInstruments(), job.updatedInstruments(),
                job.requestedInstruments(), job.updatedInstruments(),
                job.failedInstruments(), job.errorMessage(), job.errorMessage(),
                timestamp(job.startedAt()), timestamp(job.completedAt()),
                timestamp(job.createdAt()));
        return job;
    }

    @Override
    public Optional<PriceFeedJob> findLatestProviderJob() {
        return jdbcTemplate.query("""
                SELECT id, source, provider, status,
                       requested_symbols_count, successful_symbols_count,
                       failed_symbols_count, COALESCE(error_summary, error_message) AS error_summary,
                       started_at, completed_at, created_at
                FROM price_feed_job
                WHERE source IN ('REAL', 'PUBLIC', 'YAHOO', 'FINNHUB', 'NSE')
                ORDER BY started_at DESC
                LIMIT 1
                """, (resultSet, rowNumber) -> new PriceFeedJob(
                resultSet.getObject("id", UUID.class),
                PriceSource.valueOf(resultSet.getString("source")),
                resultSet.getString("provider"),
                PriceFeedStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("requested_symbols_count"),
                resultSet.getInt("successful_symbols_count"),
                resultSet.getInt("failed_symbols_count"),
                resultSet.getString("error_summary"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                instant(resultSet, "created_at"))).stream().findFirst();
    }

    @Override
    public List<PortfolioPriceInput> findPortfolioPriceInputs() {
        return jdbcTemplate.query("""
                SELECT symbol, isin, company_name, average_cost, last_price, previous_close
                FROM portfolio_holding
                ORDER BY symbol
                """, (resultSet, rowNumber) -> new PortfolioPriceInput(
                resultSet.getString("symbol"),
                resultSet.getString("isin"),
                resultSet.getString("company_name"),
                resultSet.getBigDecimal("average_cost"),
                resultSet.getBigDecimal("last_price"),
                resultSet.getBigDecimal("previous_close")));
    }

    private RowMapper<MarketInstrument> instrumentMapper() {
        return (resultSet, rowNumber) -> new MarketInstrument(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("symbol"),
                resultSet.getString("isin"),
                resultSet.getString("name"),
                Exchange.valueOf(resultSet.getString("exchange")),
                resultSet.getString("currency"),
                resultSet.getBoolean("active"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at"));
    }

    private RowMapper<PriceSnapshot> snapshotMapper() {
        return (resultSet, rowNumber) -> new PriceSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("instrument_id", UUID.class),
                resultSet.getString("symbol"),
                Exchange.valueOf(resultSet.getString("exchange")),
                resultSet.getBigDecimal("last_price"),
                resultSet.getBigDecimal("previous_close"),
                PriceSource.valueOf(resultSet.getString("source")),
                instant(resultSet, "captured_at"),
                instant(resultSet, "created_at"));
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
