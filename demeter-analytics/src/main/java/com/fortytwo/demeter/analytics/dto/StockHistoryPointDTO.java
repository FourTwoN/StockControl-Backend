package com.fortytwo.demeter.analytics.dto;

import java.math.BigDecimal;

public record StockHistoryPointDTO(
    String date,
    BigDecimal totalQuantity,
    BigDecimal activeQuantity,
    BigDecimal depletedQuantity
) {}
