package com.fortytwo.demeter.inventario.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.inventario.dto.CreateStockBatchRequest;
import com.fortytwo.demeter.inventario.dto.StockBatchDTO;
import com.fortytwo.demeter.inventario.dto.UpdateStockBatchRequest;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.inventario.repository.StockBatchRepository;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class StockBatchService {

    @Inject
    StockBatchRepository stockBatchRepository;

    @Inject
    ProductRepository productRepository;

    public PagedResponse<StockBatchDTO> findAll(int page, int size, UUID productId, UUID locationId, String status) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        if (productId != null) {
            query.append(" and product.id = ?").append(paramIndex++);
            params.add(productId);
        }
        if (locationId != null) {
            query.append(" and warehouseId = ?").append(paramIndex++);
            params.add(locationId);
        }
        if (status != null && !status.isBlank()) {
            query.append(" and status = ?").append(paramIndex++);
            params.add(BatchStatus.valueOf(status));
        }
        String jpql = query.toString();
        long total = stockBatchRepository.count(jpql, params.toArray());
        var batches = stockBatchRepository.find(jpql + " order by createdAt desc", params.toArray())
                .page(Page.of(page, size)).list();
        return PagedResponse.of(batches.stream().map(StockBatchDTO::from).toList(), page, size, total);
    }

    public StockBatchDTO findById(UUID id) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        return StockBatchDTO.from(batch);
    }

    public List<StockBatchDTO> findByProductId(UUID productId) {
        return stockBatchRepository.findByProductId(productId)
                .stream().map(StockBatchDTO::from).toList();
    }

    public List<StockBatchDTO> findByWarehouseId(UUID warehouseId) {
        return stockBatchRepository.findByWarehouseId(warehouseId)
                .stream().map(StockBatchDTO::from).toList();
    }

    public List<StockBatchDTO> findByStatus(BatchStatus status) {
        return stockBatchRepository.findByStatus(status)
                .stream().map(StockBatchDTO::from).toList();
    }

    @Transactional
    public StockBatchDTO create(CreateStockBatchRequest request) {
        StockBatch batch = new StockBatch();
        batch.setProduct(productRepository.findByIdOptional(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product", request.productId())));
        batch.setBatchCode(request.batchCode());
        batch.setQuantity(request.quantity());
        batch.setUnit(request.unit());
        batch.setWarehouseId(request.warehouseId());
        batch.setBinId(request.binId());
        batch.setCustomAttributes(request.customAttributes());
        batch.setEntryDate(request.entryDate() != null ? request.entryDate() : Instant.now());
        batch.setExpiryDate(request.expiryDate());

        stockBatchRepository.persist(batch);
        return StockBatchDTO.from(batch);
    }

    @Transactional
    public StockBatchDTO update(UUID id, UpdateStockBatchRequest request) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));

        if (request.unit() != null) batch.setUnit(request.unit());
        if (request.warehouseId() != null) batch.setWarehouseId(request.warehouseId());
        if (request.binId() != null) batch.setBinId(request.binId());
        if (request.status() != null) batch.setStatus(BatchStatus.valueOf(request.status()));
        if (request.customAttributes() != null) batch.setCustomAttributes(request.customAttributes());
        if (request.expiryDate() != null) batch.setExpiryDate(request.expiryDate());

        return StockBatchDTO.from(batch);
    }

    @Transactional
    public void updateQuantity(UUID id, BigDecimal newQuantity) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        batch.setQuantity(newQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus(BatchStatus.DEPLETED);
        }
    }

    @Transactional
    public void delete(UUID id) {
        StockBatch batch = stockBatchRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("StockBatch", id));
        stockBatchRepository.delete(batch);
    }
}
