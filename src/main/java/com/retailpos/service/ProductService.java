package com.retailpos.service;

import com.retailpos.dto.ProductCreateRequest;
import com.retailpos.dto.ProductResponse;
import com.retailpos.dto.ProductUpdateRequest;
import com.retailpos.entity.Product;
import com.retailpos.exception.DuplicateSkuException;
import com.retailpos.exception.ProductNotFoundException;
import com.retailpos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = new Product();

        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        Product savedProduct = productRepository.save(product);

        return toProductResponse(savedProduct);
    }

    public ProductResponse getProductById(Long id){
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request){
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())){
            throw new DuplicateSkuException(request.sku());
        }

        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());

        Product updatedProduct = productRepository.save(product);

        return toProductResponse(updatedProduct);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);

        productRepository.save(product);
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive()
        );
    }

}