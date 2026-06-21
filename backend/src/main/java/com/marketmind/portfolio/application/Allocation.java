package com.marketmind.portfolio.application;

import java.math.BigDecimal;

public record Allocation(
        String category,
        BigDecimal presentValue,
        BigDecimal percentage) {
}
