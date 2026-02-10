package com.fortytwo.demeter.empaquetado.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePackagingCatalogRequest(
    String name,
    UUID typeId,
    UUID materialId,
    UUID colorId,
    BigDecimal capacity,
    String unit
) {}
