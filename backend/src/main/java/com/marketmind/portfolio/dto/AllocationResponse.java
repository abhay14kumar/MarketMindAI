package com.marketmind.portfolio.dto;

import java.math.BigDecimal;

public record AllocationResponse(
        String category,
        BigDecimal presentValue,
        BigDecimal percentage) {
}
