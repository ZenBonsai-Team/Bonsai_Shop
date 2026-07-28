package com.example.bonsai_shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductMediaDTO {
    private Integer mediaId;
    private String mediaUrl;
    private String mediaType;
    private String slotType;
    private String caption;
    private Boolean thumbnail;
    private Integer displayOrder;
}
