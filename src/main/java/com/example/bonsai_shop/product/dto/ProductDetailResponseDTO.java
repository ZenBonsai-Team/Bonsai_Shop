package com.example.bonsai_shop.product.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponseDTO {
    private Integer productId;
    private String productCode;
    private String productName;
    private String description;
    private Integer age;
    private Float height;
    private Float trunkDiameter;
    private String style;
    private BigDecimal price;
    private Boolean isPublicPrice;
    private String productStatus;
    private Boolean isVisible;
    private Integer segmentId;
    private String segmentName;
    private String imageUrl;
}
