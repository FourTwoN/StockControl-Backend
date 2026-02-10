package com.fortytwo.demeter.analytics.service;

import com.fortytwo.demeter.analytics.dto.BatchMovementDetail;
import com.fortytwo.demeter.analytics.dto.DashboardSummary;
import com.fortytwo.demeter.analytics.dto.InventoryValuation;
import com.fortytwo.demeter.analytics.dto.KpiDTO;
import com.fortytwo.demeter.analytics.dto.LocationOccupancy;
import com.fortytwo.demeter.analytics.dto.MovementHistory;
import com.fortytwo.demeter.analytics.dto.MovementSummary;
import com.fortytwo.demeter.analytics.dto.SalesSummaryDTO;
import com.fortytwo.demeter.analytics.dto.StockHistoryPointDTO;
import com.fortytwo.demeter.analytics.dto.StockSummary;
import com.fortytwo.demeter.analytics.dto.TopProductSales;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.costos.model.Cost;
import com.fortytwo.demeter.costos.repository.CostRepository;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.model.StockBatchMovement;
import com.fortytwo.demeter.inventario.model.StockMovement;
import com.fortytwo.demeter.inventario.repository.StockBatchMovementRepository;
import com.fortytwo.demeter.inventario.repository.StockBatchRepository;
import com.fortytwo.demeter.inventario.repository.StockMovementRepository;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import com.fortytwo.demeter.ubicaciones.model.StorageBin;
import com.fortytwo.demeter.ubicaciones.model.Warehouse;
import com.fortytwo.demeter.ubicaciones.repository.StorageBinRepository;
import com.fortytwo.demeter.ubicaciones.repository.WarehouseRepository;
import com.fortytwo.demeter.ventas.model.Sale;
import com.fortytwo.demeter.ventas.model.SaleItem;
import com.fortytwo.demeter.ventas.model.SaleStatus;
import com.fortytwo.demeter.ventas.repository.SaleItemRepository;
import com.fortytwo.demeter.ventas.repository.SaleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsService {

    private static final Logger LOG = Logger.getLogger(AnalyticsService.class);

    @Inject
    ProductRepository productRepository;

    @Inject
    StockBatchRepository stockBatchRepository;

    @Inject
    StockMovementRepository stockMovementRepository;

    @Inject
    StockBatchMovementRepository stockBatchMovementRepository;

    @Inject
    SaleRepository saleRepository;

    @Inject
    SaleItemRepository saleItemRepository;

    @Inject
    CostRepository costRepository;

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    StorageBinRepository storageBinRepository;

    public List<StockSummary> getStockSummary() {
        LOG.debug("Generating stock summary");

        List<StockBatch> activeBatches = stockBatchRepository.findByStatus(BatchStatus.ACTIVE);

        Map<UUID, List<StockBatch>> batchesByProduct = activeBatches.stream()
                .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));

        return batchesByProduct.entrySet().stream()
                .map(entry -> {
                    UUID productId = entry.getKey();
                    List<StockBatch> batches = entry.getValue();
                    Product product = productRepository.findById(productId);

                    String productName = product != null ? product.getName() : "Unknown";
                    String productSku = product != null ? product.getSku() : "N/A";

                    BigDecimal totalQuantity = batches.stream()
                            .map(StockBatch::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    String unit = batches.stream()
                            .findFirst()
                            .map(StockBatch::getUnit)
                            .orElse("N/A");

                    return new StockSummary(
                            productId,
                            productName,
                            productSku,
                            batches.size(),
                            totalQuantity,
                            unit
                    );
                })
                .sorted(Comparator.comparing(StockSummary::productName))
                .toList();
    }

    public List<MovementSummary> getMovementsByDateRange(Instant from, Instant to) {
        LOG.debugf("Generating movement summary from %s to %s", from, to);

        List<StockMovement> movements = stockMovementRepository
                .find("performedAt >= ?1 and performedAt <= ?2", from, to)
                .list();

        Map<MovementType, List<StockMovement>> byType = movements.stream()
                .collect(Collectors.groupingBy(StockMovement::getMovementType));

        return byType.entrySet().stream()
                .map(entry -> {
                    String type = entry.getKey().name();
                    List<StockMovement> typeMovements = entry.getValue();

                    BigDecimal totalQuantity = typeMovements.stream()
                            .map(StockMovement::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Instant firstMovement = typeMovements.stream()
                            .map(StockMovement::getPerformedAt)
                            .min(Instant::compareTo)
                            .orElse(from);

                    Instant lastMovement = typeMovements.stream()
                            .map(StockMovement::getPerformedAt)
                            .max(Instant::compareTo)
                            .orElse(to);

                    return new MovementSummary(
                            type,
                            typeMovements.size(),
                            totalQuantity,
                            firstMovement,
                            lastMovement
                    );
                })
                .sorted(Comparator.comparing(MovementSummary::movementType))
                .toList();
    }

    public List<InventoryValuation> getInventoryValuation() {
        LOG.debug("Generating inventory valuation");

        List<StockBatch> activeBatches = stockBatchRepository.findByStatus(BatchStatus.ACTIVE);

        Map<UUID, List<StockBatch>> batchesByProduct = activeBatches.stream()
                .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));

        return batchesByProduct.entrySet().stream()
                .map(entry -> {
                    UUID productId = entry.getKey();
                    List<StockBatch> batches = entry.getValue();
                    Product product = productRepository.findById(productId);

                    String productName = product != null ? product.getName() : "Unknown";

                    BigDecimal totalQuantity = batches.stream()
                            .map(StockBatch::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<Cost> costs = costRepository.findByProductId(productId);
                    BigDecimal averageCost = calculateAverageCost(costs);

                    BigDecimal totalValue = totalQuantity.multiply(averageCost)
                            .setScale(2, RoundingMode.HALF_UP);

                    String currency = costs.stream()
                            .findFirst()
                            .map(Cost::getCurrency)
                            .orElse("USD");

                    return new InventoryValuation(
                            productId,
                            productName,
                            totalQuantity,
                            averageCost,
                            totalValue,
                            currency
                    );
                })
                .sorted(Comparator.comparing(InventoryValuation::totalValue).reversed())
                .toList();
    }

    public List<TopProductSales> getTopProductsBySales(int limit) {
        LOG.debugf("Generating top %d products by sales", limit);

        List<Sale> completedSales = saleRepository.findByStatus(SaleStatus.COMPLETED);

        List<SaleItem> allItems = completedSales.stream()
                .flatMap(sale -> sale.getItems().stream())
                .toList();

        Map<UUID, List<SaleItem>> itemsByProduct = allItems.stream()
                .collect(Collectors.groupingBy(SaleItem::getProductId));

        return itemsByProduct.entrySet().stream()
                .map(entry -> {
                    UUID productId = entry.getKey();
                    List<SaleItem> items = entry.getValue();
                    Product product = productRepository.findById(productId);

                    String productName = product != null ? product.getName() : "Unknown";
                    String productSku = product != null ? product.getSku() : "N/A";

                    long totalSales = items.stream()
                            .map(item -> item.getSale().getId())
                            .distinct()
                            .count();

                    BigDecimal totalQuantitySold = items.stream()
                            .map(SaleItem::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalRevenue = items.stream()
                            .map(SaleItem::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new TopProductSales(
                            productId,
                            productName,
                            productSku,
                            totalSales,
                            totalQuantitySold,
                            totalRevenue
                    );
                })
                .sorted(Comparator.comparing(TopProductSales::totalRevenue).reversed())
                .limit(limit)
                .toList();
    }

    public List<LocationOccupancy> getLocationOccupancy() {
        LOG.debug("Generating location occupancy");

        List<Warehouse> warehouses = warehouseRepository.findActive();
        List<StorageBin> allBins = storageBinRepository.find("deletedAt IS NULL").list();

        Map<UUID, List<StorageBin>> binsByWarehouse = allBins.stream()
                .collect(Collectors.groupingBy(bin ->
                        bin.getLocation().getArea().getWarehouse().getId()));

        return warehouses.stream()
                .map(warehouse -> {
                    List<StorageBin> warehouseBins = binsByWarehouse
                            .getOrDefault(warehouse.getId(), List.of());

                    long totalBins = warehouseBins.size();
                    long occupiedBins = warehouseBins.stream()
                            .filter(StorageBin::isOccupied)
                            .count();

                    double occupancyRate = totalBins > 0
                            ? (double) occupiedBins / totalBins * 100.0
                            : 0.0;

                    return new LocationOccupancy(
                            warehouse.getId(),
                            warehouse.getName(),
                            totalBins,
                            occupiedBins,
                            Math.round(occupancyRate * 100.0) / 100.0
                    );
                })
                .sorted(Comparator.comparing(LocationOccupancy::occupancyRate).reversed())
                .toList();
    }

    public DashboardSummary getDashboard() {
        LOG.debug("Generating dashboard summary");

        long totalProducts = productRepository.count();
        long activeBatches = stockBatchRepository.count("status", BatchStatus.ACTIVE);
        long totalWarehouses = warehouseRepository.count("deletedAt IS NULL");
        long pendingSales = saleRepository.count("status", SaleStatus.PENDING);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant endOfToday = startOfToday.plusSeconds(86400);
        long completedSalesToday = saleRepository
                .count("status = ?1 and soldAt >= ?2 and soldAt < ?3",
                        SaleStatus.COMPLETED, startOfToday, endOfToday);

        List<InventoryValuation> valuations = getInventoryValuation();
        BigDecimal totalInventoryValue = valuations.stream()
                .map(InventoryValuation::totalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        List<MovementSummary> recentMovementsByType = getMovementsByDateRange(
                thirtyDaysAgo, Instant.now());

        return new DashboardSummary(
                totalProducts,
                activeBatches,
                totalWarehouses,
                pendingSales,
                completedSalesToday,
                totalInventoryValue,
                List.copyOf(recentMovementsByType)
        );
    }

    public PagedResponse<MovementHistory> getMovementHistory(
            int page, int size, String movementType, Instant from, Instant to) {
        LOG.debugf("Fetching movement history page=%d, size=%d, type=%s", page, size, movementType);

        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;

        if (movementType != null && !movementType.isBlank()) {
            query.append(" and movementType = ?").append(paramIndex++);
            params.add(MovementType.valueOf(movementType));
        }
        if (from != null) {
            query.append(" and performedAt >= ?").append(paramIndex++);
            params.add(from);
        }
        if (to != null) {
            query.append(" and performedAt <= ?").append(paramIndex++);
            params.add(to);
        }

        String jpql = query.toString();
        long totalElements = stockMovementRepository
                .count(jpql, params.toArray());

        List<StockMovement> movements = stockMovementRepository
                .find(jpql + " order by performedAt desc", params.toArray())
                .page(page, size)
                .list();

        List<MovementHistory> content = movements.stream()
                .map(this::toMovementHistory)
                .toList();

        return PagedResponse.of(content, page, size, totalElements);
    }

    private MovementHistory toMovementHistory(StockMovement movement) {
        List<StockBatchMovement> batchMovements = stockBatchMovementRepository
                .find("movement.id", movement.getId())
                .list();

        List<BatchMovementDetail> batchDetails = batchMovements.stream()
                .map(bm -> {
                    StockBatch batch = bm.getBatch();
                    String batchCode = batch != null ? batch.getBatchCode() : "N/A";
                    return new BatchMovementDetail(
                            batch != null ? batch.getId() : null,
                            batchCode,
                            bm.getQuantity()
                    );
                })
                .toList();

        return new MovementHistory(
                movement.getId(),
                movement.getMovementType().name(),
                movement.getQuantity(),
                movement.getUnit(),
                movement.getNotes(),
                movement.getPerformedBy(),
                movement.getPerformedAt(),
                List.copyOf(batchDetails)
        );
    }

    public List<KpiDTO> getKpis() {
        LOG.debug("Generating KPIs");

        long totalProducts = productRepository.count();
        long activeBatches = stockBatchRepository.count("status", BatchStatus.ACTIVE);
        long pendingSales = saleRepository.count("status", SaleStatus.PENDING);

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant endOfToday = startOfToday.plusSeconds(86400);
        long completedSalesToday = saleRepository.count(
                "status = ?1 and soldAt >= ?2 and soldAt < ?3",
                SaleStatus.COMPLETED, startOfToday, endOfToday);

        List<InventoryValuation> valuations = getInventoryValuation();
        BigDecimal totalInventoryValue = valuations.stream()
                .map(InventoryValuation::totalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compute trends - compare with 30 days ago data
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant thirtyDaysAgoStart = thirtyDaysAgo.minusSeconds(86400);
        long previousCompletedSales = saleRepository.count(
                "status = ?1 and soldAt >= ?2 and soldAt < ?3",
                SaleStatus.COMPLETED, thirtyDaysAgoStart, thirtyDaysAgo);

        return List.of(
                new KpiDTO("total_products", "Total Products",
                        BigDecimal.valueOf(totalProducts), null, "count", null),
                new KpiDTO("active_batches", "Active Batches",
                        BigDecimal.valueOf(activeBatches), null, "count", null),
                new KpiDTO("pending_sales", "Pending Sales",
                        BigDecimal.valueOf(pendingSales), null, "count", null),
                new KpiDTO("completed_sales_today", "Completed Sales Today",
                        BigDecimal.valueOf(completedSalesToday),
                        BigDecimal.valueOf(previousCompletedSales), "count",
                        completedSalesToday >= previousCompletedSales ? "up" : "down"),
                new KpiDTO("total_inventory_value", "Total Inventory Value",
                        totalInventoryValue, null, "currency", null)
        );
    }

    public List<StockHistoryPointDTO> getStockHistory(Instant from, Instant to) {
        LOG.debugf("Generating stock history from %s to %s", from, to);

        List<StockMovement> movements = stockMovementRepository
                .find("performedAt >= ?1 and performedAt <= ?2 order by performedAt asc", from, to)
                .list();

        Map<String, BigDecimal> quantityByDate = new LinkedHashMap<>();
        for (StockMovement m : movements) {
            String date = m.getPerformedAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
            BigDecimal change = switch (m.getMovementType()) {
                case ENTRADA -> m.getQuantity();
                case MUERTE, VENTA -> m.getQuantity().negate();
                case TRASPLANTE -> BigDecimal.ZERO;
                case AJUSTE -> m.getQuantity();
            };
            quantityByDate.merge(date, change, BigDecimal::add);
        }

        return quantityByDate.entrySet().stream()
                .map(e -> new StockHistoryPointDTO(
                        e.getKey(),
                        e.getValue(),
                        e.getValue().compareTo(BigDecimal.ZERO) > 0 ? e.getValue() : BigDecimal.ZERO,
                        e.getValue().compareTo(BigDecimal.ZERO) < 0 ? e.getValue().abs() : BigDecimal.ZERO))
                .toList();
    }

    public List<SalesSummaryDTO> getSalesSummary(String period) {
        LOG.debugf("Generating sales summary with period=%s", period);

        List<Sale> completedSales = saleRepository.findByStatus(SaleStatus.COMPLETED);

        Map<String, List<Sale>> grouped;
        if ("weekly".equalsIgnoreCase(period)) {
            grouped = completedSales.stream().collect(Collectors.groupingBy(sale -> {
                LocalDate date = sale.getSoldAt().atZone(ZoneOffset.UTC).toLocalDate();
                return date.getYear() + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            }));
        } else {
            grouped = completedSales.stream().collect(Collectors.groupingBy(sale -> {
                LocalDate date = sale.getSoldAt().atZone(ZoneOffset.UTC).toLocalDate();
                return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            }));
        }

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Sale> sales = entry.getValue();
                    long totalSalesCount = sales.size();
                    BigDecimal totalRevenue = sales.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avgOrderValue = totalSalesCount > 0
                            ? totalRevenue.divide(BigDecimal.valueOf(totalSalesCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    long totalItems = sales.stream()
                            .flatMap(s -> s.getItems().stream())
                            .count();
                    return new SalesSummaryDTO(entry.getKey(), totalSalesCount, totalRevenue, avgOrderValue, totalItems);
                })
                .toList();
    }

    private BigDecimal calculateAverageCost(List<Cost> costs) {
        if (costs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalCost = costs.stream()
                .map(Cost::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalCost.divide(BigDecimal.valueOf(costs.size()), 2, RoundingMode.HALF_UP);
    }
}
