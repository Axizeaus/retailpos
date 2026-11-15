package com.retailpos.service;

import com.retailpos.dto.ProductCreateRequest;
import com.retailpos.dto.ProductResponse;
import com.retailpos.dto.ProductUpdateRequest;
import com.retailpos.entity.Category;
import com.retailpos.entity.Product;
import com.retailpos.exception.*;
import com.retailpos.repository.CategoryRepository;
import com.retailpos.repository.ProductRepository;
import com.retailpos.repository.ShopRepository;
import com.retailpos.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final TenantContext tenantContext;

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Long shopId = tenantContext.getShopId();
        return productRepository.findAllByShopIdAndActiveTrue(shopId,pageable)
                .map(this::toProductResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Long shopId = tenantContext.getShopId();

        if (productRepository.existsByShopIdAndSku(shopId, request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Category category = getActiveCategory(request.categoryId());

        String barcode = normalizeBarcode(request.barcode());

        if (barcode != null && productRepository.existsByShopIdAndBarcode(shopId, barcode)) {
            throw new DuplicateBarcodeException(barcode);
        }

        Product product = new Product();
        applyProductData(
                product,
                request.name(),
                request.sku(),
                barcode,
                request.price(),
                category
        );

        product.setShop(shopRepository.getReferenceById(shopId));
        product.setStockQuantity(request.stockQuantity());

        Product savedProduct = productRepository.save(product);

        return toProductResponse(savedProduct);
    }

    public ProductResponse getProductById(Long id) {
        Long shopId = tenantContext.getShopId();

        Product product = productRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Long shopId = tenantContext.getShopId();

        Product product = productRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equals(request.sku())
                && productRepository.existsByShopIdAndSkuAndIdNot(shopId, request.sku(), id)) {
            throw new DuplicateSkuException(request.sku());
        }

        String barcode = normalizeBarcode(request.barcode());

        if (barcode != null
                && productRepository.existsByShopIdAndBarcodeAndIdNot(shopId, barcode, id)) {
            throw new DuplicateBarcodeException(barcode);
        }

        Category category = getActiveCategory(request.categoryId());

        applyProductData(
                product,
                request.name(),
                request.sku(),
                barcode,
                request.price(),
                category
        );

        return toProductResponse(product);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Long shopId = tenantContext.getShopId();

        Product product = productRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse getProductByBarcode(String barcode) {
        Long shopId = tenantContext.getShopId();

        String normalizedBarcode = normalizeBarcode(barcode);

        if (normalizedBarcode == null) {
            throw new IllegalArgumentException("Barcode must not be blank");
        }

        Product product = productRepository.findByShopIdAndBarcodeAndActiveTrue(shopId, normalizedBarcode)
                .orElseThrow(() -> new ProductBarcodeNotFoundException(normalizedBarcode));

        return toProductResponse(product);
    }

    private Category getActiveCategory(Long categoryId) {
        Long shopId = tenantContext.getShopId();

        return categoryRepository.findByIdAndShopId(categoryId, shopId)
                .filter(Category::isActive)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private String normalizeBarcode(String barcode) {
        return barcode == null || barcode.isBlank() ? null : barcode.trim();
    }

    private void applyProductData(
            Product product,
            String name,
            String sku,
            String barcode,
            java.math.BigDecimal price,
            Category category
    ) {
        product.setName(name);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setPrice(price);
        product.setCategory(category);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}