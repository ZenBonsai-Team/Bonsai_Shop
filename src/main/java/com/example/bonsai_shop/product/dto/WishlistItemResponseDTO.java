package com.example.bonsai_shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponseDTO {
    private Integer productId;
    private String productCode;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private String productStatus;
    private Boolean isVisible;
    private Integer segmentId;
    private String segmentName;
    private LocalDateTime createdAt;
    private Boolean canAddToCart;
    private String detailUrl;
}
