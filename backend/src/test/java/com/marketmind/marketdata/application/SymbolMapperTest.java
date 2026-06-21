package com.marketmind.marketdata.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketmind.marketdata.domain.Exchange;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SymbolMapperTest {

    private final SymbolMapper mapper = new SymbolMapper();

    @ParameterizedTest
    @CsvSource({
        "INFY,INFY.NS",
        "TCS,TCS.NS",
        "HDFCBANK,HDFCBANK.NS",
        "RELIANCE,RELIANCE.NS",
        "ICICIBANK,ICICIBANK.NS",
        "JIOFIN,JIOFIN.NS",
        "RPOWER,RPOWER.NS",
        "NIFTYBEES,NIFTYBEES.NS"
    })
    void shouldMapIndianSymbolsToYahooNseStyle(String input, String expected) {
        assertThat(mapper.toProviderSymbol(input, Exchange.NSE)).isEqualTo(expected);
    }
}
