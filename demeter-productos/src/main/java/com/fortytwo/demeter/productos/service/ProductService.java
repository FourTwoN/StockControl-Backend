package com.fortytwo.demeter.productos.service;

import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.productos.dto.*;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.model.ProductState;
import com.fortytwo.demeter.productos.repository.CategoryRepository;
import com.fortytwo.demeter.productos.repository.FamilyRepository;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class ProductService {

    @Inject
    ProductRepository productRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    FamilyRepository familyRepository;

    public PagedResponse<ProductDTO> findAll(int page, int size, String search) {
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            String jpql = "lower(name) like ?1 or lower(sku) like ?1";
            long total = productRepository.count(jpql, pattern);
            var products = productRepository.find(jpql, pattern).page(Page.of(page, size)).list();
            return PagedResponse.of(products.stream().map(ProductDTO::from).toList(), page, size, total);
        }
        var query = productRepository.findAll();
        var products = query.page(Page.of(page, size)).list();
        long total = query.count();
        return PagedResponse.of(products.stream().map(ProductDTO::from).toList(), page, size, total);
    }

    public ProductDTO findById(UUID id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        return ProductDTO.from(product);
    }

    @Transactional
    public ProductDTO create(CreateProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());

        if (request.categoryId() != null) {
            product.setCategory(categoryRepository.findByIdOptional(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category", request.categoryId())));
        }
        if (request.familyId() != null) {
            product.setFamily(familyRepository.findByIdOptional(request.familyId())
                    .orElseThrow(() -> new EntityNotFoundException("Family", request.familyId())));
        }

        productRepository.persist(product);
        return ProductDTO.from(product);
    }

    @Transactional
    public ProductDTO update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.state() != null) product.setState(ProductState.valueOf(request.state()));
        if (request.categoryId() != null) {
            product.setCategory(categoryRepository.findByIdOptional(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category", request.categoryId())));
        }
        if (request.familyId() != null) {
            product.setFamily(familyRepository.findByIdOptional(request.familyId())
                    .orElseThrow(() -> new EntityNotFoundException("Family", request.familyId())));
        }

        return ProductDTO.from(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        productRepository.delete(product);
    }
}
