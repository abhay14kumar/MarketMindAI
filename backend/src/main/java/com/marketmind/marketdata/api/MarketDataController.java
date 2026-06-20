package com.marketmind.marketdata.api;

import java.util.List;

import com.marketmind.marketdata.application.MarketDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "Market Data", description = "Read-only market-data endpoints")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final MarketDataApiMapper mapper;

    public MarketDataController(MarketDataService marketDataService, MarketDataApiMapper mapper) {
        this.marketDataService = marketDataService;
        this.mapper = mapper;
    }

    @GetMapping("/indexes")
    @Operation(
            summary = "List market indexes",
            description = "Returns mock latest observations for supported market indexes.")
    @ApiResponse(
            responseCode = "200",
            description = "Market indexes returned",
            content = @Content(array = @ArraySchema(schema = @Schema(
                    implementation = MarketIndexResponse.class))))
    public List<MarketIndexResponse> getIndexes() {
        return marketDataService.getMarketIndexes().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/prices/{symbol}")
    @Operation(
            summary = "Get daily prices",
            description = "Returns mock daily OHLCV observations for a validated symbol.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Daily prices returned",
                content = @Content(array = @ArraySchema(schema = @Schema(
                        implementation = StockPriceDailyResponse.class)))),
        @ApiResponse(
                responseCode = "422",
                description = "Symbol validation failed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<StockPriceDailyResponse> getPrices(
            @Parameter(description = "Exchange symbol, for example RELIANCE")
            @PathVariable
            @Pattern(
                    regexp = "^[A-Za-z0-9][A-Za-z0-9.-]{0,31}$",
                    message = "Symbol must contain only letters, numbers, periods, or hyphens.")
            String symbol) {
        return marketDataService.getDailyPrices(symbol).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/health")
    @Operation(
            summary = "Get market-data health",
            description = "Reports the configured market-data provider and operating mode.")
    @ApiResponse(responseCode = "200", description = "Market-data module is available")
    public MarketHealthResponse getHealth() {
        return mapper.toResponse(marketDataService.getHealth());
    }
}
