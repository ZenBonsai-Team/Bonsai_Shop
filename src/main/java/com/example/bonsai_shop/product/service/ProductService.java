package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Page<ProductCardDTO> getMarketplaceProducts(Pageable pageable) {
        return productRepository.findMarketplaceProducts(pageable);
    }

    public Page<Product> getAllActiveProducts(Pageable pageable) {
        return productRepository.findAllActiveProducts(pageable);
    }

    public Page<Product> getAvailableProductsOnly(Pageable pageable) {
        return productRepository.findAvailableProductsOnly(pageable);
    }

    public Page<Product> getFilteredProducts(
            String keyword,
            String status,
            Boolean availableOnly,
            String segment,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<String> ages,
            List<String> species,
            List<String> styles,
            Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.filterProducts(
                        keyword, status, availableOnly, segment, category, minPrice, maxPrice, ages, species, styles
                ),
                pageable
        );
    }
    
    public Page<ProductCardDTO> getPreniumProducts(Pageable pageable) {
        return productRepository.findPremiumProducts(pageable);
    }

    public ProductCardDTO getPreniumProductsById(Integer productId) {
        return productRepository.findPremiumProductById(productId);
    }
}
