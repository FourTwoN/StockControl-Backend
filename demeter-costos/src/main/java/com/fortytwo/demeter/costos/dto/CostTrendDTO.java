package com.fortytwo.demeter.costos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CostTrendDTO(
    LocalDate date,
    BigDecimal amount,
    String costType
) {}
