package com.retailpos.service;

import com.retailpos.dto.ProductCreateRequest;
import com.retailpos.dto.ProductResponse;
import com.retailpos.dto.ProductUpdateRequest;
import com.retailpos.entity.Category;
import com.retailpos.entity.Product;
import com.retailpos.exception.CategoryNotFoundException;
import com.retailpos.exception.DuplicateSkuException;
import com.retailpos.exception.ProductNotFoundException;
import com.retailpos.repository.CategoryRepository;
import com.retailpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAllByActiveTrue(pageable)
                .map(this::toProductResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Category category = getActiveCategory(request.categoryId());

        Product product = new Product();
        applyProductData(
                product,
                request.name(),
                request.sku(),
                request.price(),
                category
        );

        product.setStockQuantity(request.stockQuantity());

        Product savedProduct = productRepository.save(product);

        return toProductResponse(savedProduct);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equals(request.sku())
                && productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Category category = getActiveCategory(request.categoryId());

        applyProductData(
                product,
                request.name(),
                request.sku(),
                request.price(),
                category
        );

        return toProductResponse(product);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);
        productRepository.save(product);
    }

    private Category getActiveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private void applyProductData(
            Product product,
            String name,
            String sku,
            java.math.BigDecimal price,
            Category category
    ) {
        product.setName(name);
        product.setSku(sku);
        product.setPrice(price);
        product.setCategory(category);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}