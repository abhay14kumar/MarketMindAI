package com.marketmind.marketdata.api;

import java.util.List;

import com.marketmind.marketdata.application.PriceFeedService;
import com.marketmind.marketdata.application.RealPriceRefreshService;
import com.marketmind.marketdata.domain.PriceSource;
import com.marketmind.marketdata.dto.ManualPriceRequest;
import com.marketmind.marketdata.dto.MarketInstrumentRequest;
import com.marketmind.marketdata.dto.MarketInstrumentResponse;
import com.marketmind.marketdata.dto.PriceFeedJobResponse;
import com.marketmind.marketdata.dto.PriceSnapshotResponse;
import com.marketmind.marketdata.dto.PriceProviderStatusResponse;
import com.marketmind.marketdata.mapper.PriceFeedMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Price Feed", description = "Manual and mock market price snapshots")
public class PriceFeedController {

    private final PriceFeedService service;
    private final RealPriceRefreshService realRefreshService;
    private final PriceFeedMapper mapper;

    public PriceFeedController(
            PriceFeedService service,
            RealPriceRefreshService realRefreshService,
            PriceFeedMapper mapper) {
        this.service = service;
        this.realRefreshService = realRefreshService;
        this.mapper = mapper;
    }

    @PostMapping("/instruments")
    @Operation(summary = "Register a market instrument")
    @ApiResponse(responseCode = "201", description = "Instrument registered")
    public ResponseEntity<MarketInstrumentResponse> createInstrument(
            @Valid @RequestBody MarketInstrumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(
                service.createInstrument(
                        request.symbol(), request.isin(), request.name(),
                        request.exchange(), request.currency())));
    }

    @GetMapping("/instruments")
    @Operation(summary = "List market instruments")
    public List<MarketInstrumentResponse> getInstruments() {
        return service.getInstruments().stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/prices/manual")
    @Operation(summary = "Record a manual price snapshot")
    @ApiResponse(responseCode = "201", description = "Price snapshot recorded")
    public ResponseEntity<PriceSnapshotResponse> updateManualPrice(
            @Valid @RequestBody ManualPriceRequest request) {
        if (request.source() != PriceSource.MANUAL && request.source() != PriceSource.MOCK) {
            throw new IllegalArgumentException(
                    "Manual price updates only support MANUAL or MOCK sources.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(
                service.updateManualPrice(
                        request.symbol(), request.exchange(), request.lastPrice(),
                        request.previousClose(), request.source())));
    }

    @GetMapping("/prices/latest/{symbol}")
    @Operation(summary = "Get the latest price for a symbol")
    public PriceSnapshotResponse getLatestPrice(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9.-]{0,63}$")
            String symbol) {
        return mapper.toResponse(service.getLatestPrice(symbol));
    }

    @GetMapping("/prices/latest")
    @Operation(summary = "List latest prices")
    public List<PriceSnapshotResponse> getLatestPrices() {
        return service.getLatestPrices().stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/prices/mock-refresh")
    @Operation(
            summary = "Refresh mock prices",
            description = "Creates deterministic mock prices for imported portfolio holdings. "
                    + "No external provider is called.")
    public PriceFeedJobResponse refreshMockPrices() {
        return mapper.toResponse(service.refreshMockPrices());
    }

    @PostMapping("/prices/refresh-real")
    @Operation(
            summary = "Refresh public prices",
            description = "Best-effort refresh for all current portfolio holdings using the "
                    + "configured credential-free provider. Individual symbol failures are "
                    + "recorded and do not abort other symbols.")
    public PriceFeedJobResponse refreshRealPrices() {
        return mapper.toResponse(realRefreshService.refreshPortfolioPrices());
    }

    @GetMapping("/prices/provider-status")
    @Operation(summary = "Get configured price provider and latest refresh status")
    public PriceProviderStatusResponse getProviderStatus() {
        var status = realRefreshService.getStatus();
        return new PriceProviderStatusResponse(
                status.configuredProvider(), status.scheduledRefreshEnabled(),
                status.refreshIntervalSeconds(), status.lastRefreshStatus(),
                status.requestedSymbols(), status.successfulSymbols(),
                status.failedSymbols(), status.errorSummary(), status.lastRefreshAt());
    }
}
