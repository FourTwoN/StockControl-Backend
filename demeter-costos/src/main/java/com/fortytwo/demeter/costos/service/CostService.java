package com.fortytwo.demeter.costos.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.costos.dto.CostDTO;
import com.fortytwo.demeter.costos.dto.CostTrendDTO;
import com.fortytwo.demeter.costos.dto.CreateCostRequest;
import com.fortytwo.demeter.costos.dto.InventoryValuationDTO;
import com.fortytwo.demeter.costos.dto.ProductCostDTO;
import com.fortytwo.demeter.costos.dto.UpdateCostRequest;
import com.fortytwo.demeter.costos.model.Cost;
import com.fortytwo.demeter.costos.repository.CostRepository;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CostService {

    @Inject
    CostRepository costRepository;

    @Inject
    ProductRepository productRepository;

    public PagedResponse<CostDTO> findAll(int page, int size) {
        var query = costRepository.findAll();
        var costs = query.page(Page.of(page, size)).list();
        long total = query.count();
        var dtos = costs.stream().map(CostDTO::from).toList();
        return PagedResponse.of(dtos, page, size, total);
    }

    public CostDTO findById(UUID id) {
        Cost cost = costRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Cost", id));
        return CostDTO.from(cost);
    }

    public List<CostDTO> findByProduct(UUID productId) {
        return costRepository.findByProductId(productId).stream()
                .map(CostDTO::from)
                .toList();
    }

    public List<CostDTO> findByBatch(UUID batchId) {
        return costRepository.findByBatchId(batchId).stream()
                .map(CostDTO::from)
                .toList();
    }

    @Transactional
    public CostDTO create(CreateCostRequest request) {
        Cost cost = new Cost();
        cost.setProductId(request.productId());
        cost.setBatchId(request.batchId());
        cost.setCostType(request.costType());
        cost.setAmount(request.amount());
        cost.setCurrency(request.currency() != null ? request.currency() : "USD");
        cost.setDescription(request.description());
        cost.setEffectiveDate(request.effectiveDate());

        costRepository.persist(cost);
        return CostDTO.from(cost);
    }

    @Transactional
    public CostDTO update(UUID id, UpdateCostRequest request) {
        Cost cost = costRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Cost", id));

        if (request.productId() != null) cost.setProductId(request.productId());
        if (request.batchId() != null) cost.setBatchId(request.batchId());
        if (request.costType() != null) cost.setCostType(request.costType());
        if (request.amount() != null) cost.setAmount(request.amount());
        if (request.currency() != null) cost.setCurrency(request.currency());
        if (request.description() != null) cost.setDescription(request.description());
        if (request.effectiveDate() != null) cost.setEffectiveDate(request.effectiveDate());

        return CostDTO.from(cost);
    }

    @Transactional
    public void delete(UUID id) {
        Cost cost = costRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Cost", id));
        costRepository.delete(cost);
    }

    public PagedResponse<ProductCostDTO> getProductCosts(int page, int size) {
        List<Cost> allCosts = costRepository.listAll();
        Map<UUID, List<Cost>> costsByProduct = allCosts.stream()
                .filter(c -> c.getProductId() != null)
                .collect(Collectors.groupingBy(Cost::getProductId));

        List<ProductCostDTO> all = costsByProduct.entrySet().stream()
                .map(entry -> {
                    UUID productId = entry.getKey();
                    List<Cost> costs = entry.getValue();
                    Product product = productRepository.findById(productId);
                    String name = product != null ? product.getName() : "Unknown";
                    String sku = product != null ? product.getSku() : "N/A";
                    BigDecimal avg = costs.stream().map(Cost::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(costs.size()), 2, RoundingMode.HALF_UP);
                    BigDecimal lastCost = costs.stream()
                            .max(Comparator.comparing(Cost::getEffectiveDate))
                            .map(Cost::getAmount).orElse(BigDecimal.ZERO);
                    String currency = costs.stream().findFirst()
                            .map(Cost::getCurrency).orElse("USD");
                    return new ProductCostDTO(productId, name, sku, avg, lastCost, currency);
                })
                .sorted(Comparator.comparing(ProductCostDTO::productName))
                .toList();

        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        return PagedResponse.of(all.subList(fromIndex, toIndex), page, size, all.size());
    }

    public InventoryValuationDTO getValuation() {
        List<Cost> allCosts = costRepository.listAll();
        Map<UUID, List<Cost>> costsByProduct = allCosts.stream()
                .filter(c -> c.getProductId() != null)
                .collect(Collectors.groupingBy(Cost::getProductId));

        BigDecimal totalValue = BigDecimal.ZERO;
        long totalUnits = 0;
        Map<UUID, BigDecimal> valueByCat = new HashMap<>();
        Map<UUID, Long> unitsByCat = new HashMap<>();
        Map<UUID, String> catNames = new HashMap<>();

        for (var entry : costsByProduct.entrySet()) {
            UUID productId = entry.getKey();
            List<Cost> costs = entry.getValue();
            Product product = productRepository.findById(productId);

            BigDecimal productTotalCost = costs.stream()
                    .map(Cost::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalValue = totalValue.add(productTotalCost);
            totalUnits += costs.size();

            if (product != null && product.getCategory() != null) {
                UUID catId = product.getCategory().getId();
                catNames.putIfAbsent(catId, product.getCategory().getName());
                valueByCat.merge(catId, productTotalCost, BigDecimal::add);
                unitsByCat.merge(catId, (long) costs.size(), Long::sum);
            }
        }

        List<InventoryValuationDTO.CategoryValuationDTO> byCategory = valueByCat.entrySet().stream()
                .map(e -> new InventoryValuationDTO.CategoryValuationDTO(
                        e.getKey(), catNames.get(e.getKey()),
                        e.getValue(), unitsByCat.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparing(InventoryValuationDTO.CategoryValuationDTO::totalValue).reversed())
                .toList();

        return new InventoryValuationDTO(totalValue, totalUnits, "USD", byCategory);
    }

    public List<CostTrendDTO> getTrends(UUID productId, LocalDate from, LocalDate to) {
        List<Cost> costs = costRepository.findByProductId(productId);
        return costs.stream()
                .filter(c -> from == null || !c.getEffectiveDate().isBefore(from))
                .filter(c -> to == null || !c.getEffectiveDate().isAfter(to))
                .sorted(Comparator.comparing(Cost::getEffectiveDate))
                .map(c -> new CostTrendDTO(c.getEffectiveDate(), c.getAmount(), c.getCostType()))
                .toList();
    }
}
