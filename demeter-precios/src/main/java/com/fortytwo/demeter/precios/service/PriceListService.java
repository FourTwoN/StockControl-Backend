package com.fortytwo.demeter.precios.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.precios.dto.CreatePriceEntryRequest;
import com.fortytwo.demeter.precios.dto.CreatePriceListRequest;
import com.fortytwo.demeter.precios.dto.PriceListDTO;
import com.fortytwo.demeter.precios.dto.UpdatePriceListRequest;
import com.fortytwo.demeter.precios.model.PriceEntry;
import com.fortytwo.demeter.precios.model.PriceList;
import com.fortytwo.demeter.precios.repository.PriceListRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PriceListService {

    @Inject
    PriceListRepository priceListRepository;

    public PagedResponse<PriceListDTO> findAll(int page, int size, String sort) {
        String orderClause = resolveSort(sort);
        var query = priceListRepository.find("1=1 order by " + orderClause);
        var priceLists = query.page(Page.of(page, size)).list();
        long total = priceListRepository.count();
        var dtos = priceLists.stream().map(PriceListDTO::from).toList();
        return PagedResponse.of(dtos, page, size, total);
    }

    private String resolveSort(String sort) {
        if (sort == null || sort.isBlank()) return "createdAt desc";
        boolean desc = sort.startsWith("-");
        String field = desc ? sort.substring(1) : sort;
        return switch (field) {
            case "name", "createdAt", "effectiveDate" -> field + (desc ? " desc" : " asc");
            default -> "createdAt desc";
        };
    }

    public PriceListDTO findById(UUID id) {
        PriceList priceList = priceListRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", id));
        return PriceListDTO.from(priceList);
    }

    public List<PriceListDTO> findActive() {
        return priceListRepository.findActive().stream()
                .map(PriceListDTO::from)
                .toList();
    }

    @Transactional
    public PriceListDTO create(CreatePriceListRequest request) {
        PriceList priceList = new PriceList();
        priceList.setName(request.name());
        priceList.setDescription(request.description());
        priceList.setEffectiveDate(request.effectiveDate());

        if (request.entries() != null) {
            for (CreatePriceEntryRequest entryReq : request.entries()) {
                PriceEntry entry = buildPriceEntry(entryReq, priceList);
                priceList.getEntries().add(entry);
            }
        }

        priceListRepository.persist(priceList);
        return PriceListDTO.from(priceList);
    }

    @Transactional
    public PriceListDTO update(UUID id, UpdatePriceListRequest request) {
        PriceList priceList = priceListRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", id));

        if (request.name() != null) priceList.setName(request.name());
        if (request.description() != null) priceList.setDescription(request.description());
        if (request.effectiveDate() != null) priceList.setEffectiveDate(request.effectiveDate());

        return PriceListDTO.from(priceList);
    }

    @Transactional
    public PriceListDTO activate(UUID id) {
        PriceList priceList = priceListRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", id));
        priceList.setActive(true);
        return PriceListDTO.from(priceList);
    }

    @Transactional
    public PriceListDTO deactivate(UUID id) {
        PriceList priceList = priceListRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", id));
        priceList.setActive(false);
        return PriceListDTO.from(priceList);
    }

    @Transactional
    public void delete(UUID id) {
        PriceList priceList = priceListRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", id));
        priceListRepository.delete(priceList);
    }

    private PriceEntry buildPriceEntry(CreatePriceEntryRequest request, PriceList priceList) {
        PriceEntry entry = new PriceEntry();
        entry.setPriceList(priceList);
        entry.setProductId(request.productId());
        entry.setPrice(request.price());
        entry.setCurrency(request.currency() != null ? request.currency() : "USD");
        entry.setMinQuantity(request.minQuantity() != null ? request.minQuantity() : BigDecimal.ONE);
        return entry;
    }
}
