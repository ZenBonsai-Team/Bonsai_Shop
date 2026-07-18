package com.example.bonsai_shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCardDTO {

    private Integer productId;

    private String productCode;

    private String productName;

    private String varietyName;

    private Integer age;

    private Float height;

    private Float trunkDiameter;

    private BigDecimal price;

    private String artisanName;

    private String status;

    private String thumbnailUrl;
}