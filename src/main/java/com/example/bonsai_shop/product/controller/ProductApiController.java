package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.dto.ProductDetailResponseDTO;
import com.example.bonsai_shop.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDTO> getProductDetailForModerator(@PathVariable Integer productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        ProductDetailResponseDTO dto = ProductDetailResponseDTO.builder()
                .productId(product.getProductId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .description(product.getDescription())
                .age(product.getAge())
                .height(product.getHeight())
                .trunkDiameter(product.getTrunkDiameter())
                .style(product.getStyle())
                .price(product.getPrice())
                .productStatus(product.getProductStatus())
                .imageUrl(product.getFirstImageUrl())
                .build();

        return ResponseEntity.ok(dto);
    }
}
