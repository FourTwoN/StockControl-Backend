package com.fortytwo.demeter.precios.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdatePriceEntryRequest(
    @DecimalMin("0") BigDecimal price,
    @Size(max = 3) String currency,
    @DecimalMin("0") BigDecimal minQuantity
) {}
