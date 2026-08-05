package com.example.bonsai_shop.product.service;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.User;
import com.example.bonsai_shop.entity.Wishlist;
import com.example.bonsai_shop.product.dto.WishlistItemResponseDTO;
import com.example.bonsai_shop.product.repository.ProductRepository;
import com.example.bonsai_shop.product.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final Integer LUXURY_SEGMENT_ID = 3;

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponseDTO> getWishlistItems(User customer) {
        return wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getUserId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countWishlistItems(User customer) {
        return wishlistRepository.countByCustomerId(customer.getUserId());
    }

    @Transactional
    public boolean addToWishlist(User customer, Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));

        if (wishlistRepository.countByCustomerIdAndProductId(customer.getUserId(), productId) > 0) {
            return false;
        }

        Wishlist wishlist = Wishlist.builder()
                .customer(customer)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        wishlistRepository.save(wishlist);
        return true;
    }

    @Transactional
    public boolean removeFromWishlist(User customer, Integer productId) {
        if (wishlistRepository.countByCustomerIdAndProductId(customer.getUserId(), productId) == 0) {
            return false;
        }
        wishlistRepository.deleteByCustomerIdAndProductId(customer.getUserId(), productId);
        return true;
    }

    @Transactional
    public boolean toggleWishlist(User customer, Integer productId) {
        if (wishlistRepository.countByCustomerIdAndProductId(customer.getUserId(), productId) > 0) {
            wishlistRepository.deleteByCustomerIdAndProductId(customer.getUserId(), productId);
            return false;
        }
        addToWishlist(customer, productId);
        return true;
    }

    private WishlistItemResponseDTO toDto(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        boolean canAddToCart = "AVAILABLE".equalsIgnoreCase(product.getProductStatus())
                && !Boolean.FALSE.equals(product.getIsVisible())
                && (product.getSegment() == null
                    || product.getSegment().getSegmentId() == null
                    || !LUXURY_SEGMENT_ID.equals(product.getSegment().getSegmentId()));

        return WishlistItemResponseDTO.builder()
                .productId(product.getProductId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .productImage(product.getFirstImageUrl())
                .price(product.getPrice())
                .isPublicPrice(product.getIsPublicPrice())
                .productStatus(product.getProductStatus())
                .isVisible(product.getIsVisible())
                .segmentId(product.getSegment() == null ? null : product.getSegment().getSegmentId())
                .segmentName(product.getSegment() == null ? null : product.getSegment().getSegmentName())
                .createdAt(wishlist.getCreatedAt())
                .canAddToCart(canAddToCart)
                .detailUrl(detailUrl(product))
                .build();
    }

    private String detailUrl(Product product) {
        if (!Boolean.FALSE.equals(product.getIsVisible())
                && product.getSegment() != null
                && LUXURY_SEGMENT_ID.equals(product.getSegment().getSegmentId())) {
            return "/bonsai-luxury-detail/" + product.getProductId();
        }
        return "/product/" + product.getProductId();
    }
}
