package com.example.bonsai_shop.product.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.ProductSpecifications;
import org.springframework.transaction.annotation.Transactional;
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
            List<String> priceRanges,
            Pageable pageable) {
        return productRepository.findAll(
                ProductSpecifications.filterProducts(
                        keyword, status, availableOnly, segment, category, minPrice, maxPrice, ages, species, styles, priceRanges
                ),
                pageable
        );
    }

    public Page<ProductCardDTO> getPremiumProducts(Pageable pageable) {
        return productRepository.findPremiumProducts(pageable);
    }

    public ProductCardDTO getPremiumProductsById(Integer productId) {
        return productRepository.findPremiumProductById(productId);
    }

    @Transactional(readOnly = true)
    public Product getProductById(Integer id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            if (product.getProductMedias() != null) {
                product.getProductMedias().size();
            }
            if (product.getReviews() != null) {
                product.getReviews().size();
            }
        }
        return product;
    }

    @Transactional
    public boolean incrementViewCountForCustomer(Integer productId, Authentication authentication) {
        if (productId == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isCustomer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);

        if (isCustomer) {
            return productRepository.incrementViewCount(productId) > 0;
        }
        return false;
    }

    public List<Product> getTop5MostViewed() {
        return productRepository
                .findTop5ByProductStatusOrderByViewCountDesc("AVAILABLE");
    }
}
