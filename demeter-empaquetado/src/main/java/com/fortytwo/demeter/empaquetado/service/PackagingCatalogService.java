package com.fortytwo.demeter.empaquetado.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.empaquetado.dto.*;
import com.fortytwo.demeter.empaquetado.model.PackagingCatalog;
import com.fortytwo.demeter.empaquetado.repository.*;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PackagingCatalogService {
    @Inject PackagingCatalogRepository catalogRepository;
    @Inject PackagingTypeRepository typeRepository;
    @Inject PackagingMaterialRepository materialRepository;
    @Inject PackagingColorRepository colorRepository;

    public PagedResponse<PackagingCatalogDTO> findAll(int page, int size, String search, UUID typeId, UUID materialId, UUID colorId) {
        StringBuilder query = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        if (search != null && !search.isBlank()) {
            query.append(" and lower(name) like ?").append(paramIndex++);
            params.add("%" + search.toLowerCase() + "%");
        }
        if (typeId != null) {
            query.append(" and type.id = ?").append(paramIndex++);
            params.add(typeId);
        }
        if (materialId != null) {
            query.append(" and material.id = ?").append(paramIndex++);
            params.add(materialId);
        }
        if (colorId != null) {
            query.append(" and color.id = ?").append(paramIndex++);
            params.add(colorId);
        }
        String jpql = query.toString();
        long total = catalogRepository.count(jpql, params.toArray());
        var list = catalogRepository.find(jpql, params.toArray()).page(Page.of(page, size)).list();
        return PagedResponse.of(list.stream().map(PackagingCatalogDTO::from).toList(), page, size, total);
    }

    public PackagingCatalogDTO findById(UUID id) {
        return PackagingCatalogDTO.from(catalogRepository.findByIdOptional(id).orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", id)));
    }

    @Transactional
    public PackagingCatalogDTO create(CreatePackagingCatalogRequest req) {
        PackagingCatalog c = new PackagingCatalog();
        c.setName(req.name());
        c.setCapacity(req.capacity());
        c.setUnit(req.unit());
        if (req.typeId() != null) {
            c.setType(typeRepository.findByIdOptional(req.typeId()).orElseThrow(() -> new EntityNotFoundException("PackagingType", req.typeId())));
        }
        if (req.materialId() != null) {
            c.setMaterial(materialRepository.findByIdOptional(req.materialId()).orElseThrow(() -> new EntityNotFoundException("PackagingMaterial", req.materialId())));
        }
        if (req.colorId() != null) {
            c.setColor(colorRepository.findByIdOptional(req.colorId()).orElseThrow(() -> new EntityNotFoundException("PackagingColor", req.colorId())));
        }
        catalogRepository.persist(c);
        return PackagingCatalogDTO.from(c);
    }

    @Transactional
    public PackagingCatalogDTO update(UUID id, UpdatePackagingCatalogRequest req) {
        PackagingCatalog c = catalogRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", id));
        if (req.name() != null) c.setName(req.name());
        if (req.capacity() != null) c.setCapacity(req.capacity());
        if (req.unit() != null) c.setUnit(req.unit());
        if (req.typeId() != null) {
            c.setType(typeRepository.findByIdOptional(req.typeId())
                    .orElseThrow(() -> new EntityNotFoundException("PackagingType", req.typeId())));
        }
        if (req.materialId() != null) {
            c.setMaterial(materialRepository.findByIdOptional(req.materialId())
                    .orElseThrow(() -> new EntityNotFoundException("PackagingMaterial", req.materialId())));
        }
        if (req.colorId() != null) {
            c.setColor(colorRepository.findByIdOptional(req.colorId())
                    .orElseThrow(() -> new EntityNotFoundException("PackagingColor", req.colorId())));
        }
        return PackagingCatalogDTO.from(c);
    }

    @Transactional
    public void delete(UUID id) {
        catalogRepository.delete(catalogRepository.findByIdOptional(id).orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", id)));
    }
}
