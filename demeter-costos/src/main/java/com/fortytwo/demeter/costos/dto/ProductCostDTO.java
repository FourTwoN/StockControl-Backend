package com.fortytwo.demeter.costos.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCostDTO(
    UUID productId,
    String productName,
    String productSku,
    BigDecimal averageCost,
    BigDecimal lastCost,
    String currency
) {}
