package com.fortytwo.demeter.analytics.dto;

import java.math.BigDecimal;

public record SalesSummaryDTO(
    String period,
    long totalSales,
    BigDecimal totalRevenue,
    BigDecimal averageOrderValue,
    long totalItemsSold
) {}
