package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.product.dto.ProductCardDTO;
import com.example.bonsai_shop.product.dto.ProductMediaDTO;
import com.example.bonsai_shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PremiumBonsaiController {
    private static final Map<String, String> SLOT_TYPE_LABELS = Map.of(
            "FRONT", "Mặt trước",
            "BACK", "Mặt sau",
            "LEFT", "Bên trái",
            "RIGHT", "Bên phải",
            "DETAIL", "Cận cảnh chi tiết",
            "TRUNK", "Cận cảnh thân cây",
            "BRANCH", "Cận cảnh cành",
            "POT", "Chậu cây",
            "OVERVIEW", "Tổng quan"
    );

    private final ProductService productService;

    @GetMapping("/bonsai-luxury")
    public String premium(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 8);

        Page<ProductCardDTO> product = productService.getPremiumProducts(pageable);
        model.addAttribute("product", product);
        return "product/bonsai-luxury";
    }

    @GetMapping("/bonsai-luxury-detail/{productId}")
    public String premiumDetail(
            @PathVariable Integer productId,
            Authentication authentication,
            Model model) {
        ProductCardDTO product =
                productService.getPremiumProductsById(productId);
        if (product != null) {
            productService.incrementViewCountForCustomer(productId, authentication);
        }
        List<ProductMediaDTO> mediaList = productService.getPremiumProductMedia(productId);
        model.addAttribute("product", product);
        model.addAttribute("slotTypeLabels", SLOT_TYPE_LABELS);
        model.addAttribute("imageMediaList", mediaList.stream()
                .filter(media -> !"VIDEO".equalsIgnoreCase(media.getMediaType()))
                .toList());
        model.addAttribute("videoMediaList", mediaList.stream()
                .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                .toList());
        return "product/bonsai-luxury-detail";
    }
}
