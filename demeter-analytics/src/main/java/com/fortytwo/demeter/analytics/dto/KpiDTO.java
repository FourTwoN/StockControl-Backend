package com.fortytwo.demeter.analytics.dto;

import java.math.BigDecimal;

public record KpiDTO(
    String id,
    String label,
    BigDecimal value,
    BigDecimal previousValue,
    String unit,
    String trend
) {}
