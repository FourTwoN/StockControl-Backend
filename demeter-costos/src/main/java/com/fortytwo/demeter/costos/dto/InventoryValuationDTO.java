package com.fortytwo.demeter.costos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryValuationDTO(
    BigDecimal totalValue,
    long totalUnits,
    String currency,
    List<CategoryValuationDTO> byCategory
) {
    public record CategoryValuationDTO(
        UUID categoryId,
        String categoryName,
        BigDecimal totalValue,
        long totalUnits
    ) {}
}
