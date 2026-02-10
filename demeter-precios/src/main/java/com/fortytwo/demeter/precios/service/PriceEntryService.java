package com.fortytwo.demeter.precios.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.precios.dto.CreatePriceEntryRequest;
import com.fortytwo.demeter.precios.dto.PriceEntryDTO;
import com.fortytwo.demeter.precios.dto.UpdatePriceEntryRequest;
import com.fortytwo.demeter.precios.model.PriceEntry;
import com.fortytwo.demeter.precios.model.PriceList;
import com.fortytwo.demeter.precios.repository.PriceEntryRepository;
import com.fortytwo.demeter.precios.repository.PriceListRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PriceEntryService {

    @Inject
    PriceEntryRepository priceEntryRepository;

    @Inject
    PriceListRepository priceListRepository;

    public List<PriceEntryDTO> findByPriceList(UUID priceListId) {
        return priceEntryRepository.findByPriceListId(priceListId).stream()
                .map(PriceEntryDTO::from)
                .toList();
    }

    public PriceEntryDTO findById(UUID id) {
        PriceEntry entry = priceEntryRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PriceEntry", id));
        return PriceEntryDTO.from(entry);
    }

    @Transactional
    public PriceEntryDTO addEntry(UUID priceListId, CreatePriceEntryRequest request) {
        PriceList priceList = priceListRepository.findByIdOptional(priceListId)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", priceListId));

        PriceEntry entry = buildPriceEntry(request, priceList);
        priceEntryRepository.persist(entry);
        return PriceEntryDTO.from(entry);
    }

    @Transactional
    public List<PriceEntryDTO> bulkAddEntries(UUID priceListId, List<CreatePriceEntryRequest> requests) {
        PriceList priceList = priceListRepository.findByIdOptional(priceListId)
                .orElseThrow(() -> new EntityNotFoundException("PriceList", priceListId));

        List<PriceEntry> entries = requests.stream()
                .map(request -> buildPriceEntry(request, priceList))
                .toList();

        priceEntryRepository.persist(entries);
        return entries.stream().map(PriceEntryDTO::from).toList();
    }

    @Transactional
    public PriceEntryDTO updateEntry(UUID entryId, UpdatePriceEntryRequest request) {
        PriceEntry entry = priceEntryRepository.findByIdOptional(entryId)
                .orElseThrow(() -> new EntityNotFoundException("PriceEntry", entryId));
        if (request.price() != null) entry.setPrice(request.price());
        if (request.currency() != null) entry.setCurrency(request.currency());
        if (request.minQuantity() != null) entry.setMinQuantity(request.minQuantity());
        return PriceEntryDTO.from(entry);
    }

    @Transactional
    public void deleteEntry(UUID entryId) {
        PriceEntry entry = priceEntryRepository.findByIdOptional(entryId)
                .orElseThrow(() -> new EntityNotFoundException("PriceEntry", entryId));
        priceEntryRepository.delete(entry);
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
