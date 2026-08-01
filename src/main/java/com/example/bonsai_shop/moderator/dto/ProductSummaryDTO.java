package com.example.bonsai_shop.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryDTO {
    private Integer productId;
    private String productCode;
    private String productName;
    private String categoryName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal deposit;
    private BigDecimal subtotal;
}
