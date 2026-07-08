package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {
    private final ProductService productService;

    @GetMapping("/marketplace")
    public String marketplace(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(name = "availableOnly", required = false) String availableOnly,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<String> ages,
            @RequestParam(required = false) List<String> species,
            @RequestParam(required = false) List<String> styles,
            @RequestParam(required = false) List<String> priceRanges,
            @RequestParam(required = false) String sort,
            Model model) {

        boolean showAvailableOnly = "on".equals(availableOnly) || "true".equals(availableOnly);

        Sort springSort;
        if ("price_asc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("age_desc".equals(sort)) {
            springSort = Sort.by(Sort.Direction.DESC, "age");
        } else {
            springSort = Sort.by(Sort.Direction.DESC, "productId");
        }

        Page<Product> products = productService.getFilteredProducts(
                keyword,
                status,
                showAvailableOnly,
                segment,
                category,
                minPrice,
                maxPrice,
                ages,
                species,
                styles,
                priceRanges,
                PageRequest.of(page, 12, springSort)
        );

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("availableOnly", showAvailableOnly);
        model.addAttribute("segment", segment);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("ages", ages);
        model.addAttribute("species", species);
        model.addAttribute("styles", styles);
        model.addAttribute("priceRanges", priceRanges);
        model.addAttribute("sort", sort);

        return "/product/marketplace";
    }

    @GetMapping({"/products/detail", "/product/{id}"})
    public String productDetail(@PathVariable(value = "id", required = false) Integer id, Model model) {
        if (id != null) {
            Product product = productService.getProductById(id);
            model.addAttribute("product", product);
        } else {
            Page<Product> products = productService.getAllActiveProducts(PageRequest.of(0, 1));
            if (!products.isEmpty()) {
                Product product = productService.getProductById(products.getContent().get(0).getProductId());
                model.addAttribute("product", product);
            }
        }
        return "product/product-detail";
    }
}