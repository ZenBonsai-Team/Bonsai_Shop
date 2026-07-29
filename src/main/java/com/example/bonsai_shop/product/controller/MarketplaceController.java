package com.example.bonsai_shop.product.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import com.example.bonsai_shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {
    private static final List<ImageSlotDefinition> IMAGE_SLOT_DEFINITIONS = List.of(
            new ImageSlotDefinition("OVERVIEW", "Tổng quan"),
            new ImageSlotDefinition("FRONT", "Mặt trước"),
            new ImageSlotDefinition("BACK", "Mặt sau"),
            new ImageSlotDefinition("LEFT", "Bên trái"),
            new ImageSlotDefinition("RIGHT", "Bên phải"),
            new ImageSlotDefinition("TRUNK", "Thân cây"),
            new ImageSlotDefinition("BRANCH", "Cành"),
            new ImageSlotDefinition("POT", "Chậu"),
            new ImageSlotDefinition("DETAIL", "Chi tiết")
    );

    private final ProductService productService;
    private final ProductJournalService productJournalService;

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
                PageRequest.of(page, 12, springSort));

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
        model.addAttribute("activePage", "marketplace");

        return "product/marketplace";
    }

    @GetMapping("/product/{id}")
    public String productDetailById(
            @PathVariable("id") Integer id,
            Authentication authentication,
            Model model) {
        return processProductDetail(id, authentication, model);
    }

    @GetMapping("/products/detail")
    public String productDetailByParam(
            @RequestParam(value = "id", required = false) Integer id,
            Authentication authentication,
            Model model) {
        return processProductDetail(id, authentication, model);
    }

    private String processProductDetail(Integer id, Authentication authentication, Model model) {
        Product product = null;
        if (id != null) {
            product = productService.getProductById(id);
        }

        if (product == null) {
            Page<Product> products = productService.getAllActiveProducts(PageRequest.of(0, 1));
            if (!products.isEmpty()) {
                product = productService.getProductById(products.getContent().get(0).getProductId());
            }
        }

        if (product == null) {
            return "redirect:/marketplace";
        }

        boolean viewCountIncremented = productService.incrementViewCountForCustomer(product.getProductId(), authentication);
        if (viewCountIncremented) {
            product.setViewCount((product.getViewCount() == null ? 0 : product.getViewCount()) + 1);
        }

        model.addAttribute("product", product);
        model.addAttribute("productImages", getMediaByType(product, "IMAGE"));
        model.addAttribute("productImageSlots", getImageSlots(product));
        model.addAttribute("productVideos", getMediaByType(product, "VIDEO"));
        model.addAttribute("productTags", productService.getProductTags(product));
        model.addAttribute("artisanManagedProductCount", productService.countManagedProductsByArtisan(
                product.getCreatedBy() == null ? null : product.getCreatedBy().getUserId()
        ));
        model.addAttribute("journalEvents", productJournalService.getPublicEvents(product));
        model.addAttribute("activePage", "marketplace");
        return "product/product-detail";
    }

    private List<ProductMedia> getMediaByType(Product product, String mediaType) {
        if (product == null || product.getProductMedias() == null) {
            return Collections.emptyList();
        }
        return product.getProductMedias().stream()
                .filter(media -> mediaType.equals(media.getMediaType()))
                .toList();
    }

    private List<ProductImageSlot> getImageSlots(Product product) {
        List<ProductMedia> images = getMediaByType(product, "IMAGE").stream()
                .sorted(mediaDisplayComparator())
                .toList();
        if (images.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> definedSlotTypes = IMAGE_SLOT_DEFINITIONS.stream()
                .map(ImageSlotDefinition::slotType)
                .collect(java.util.stream.Collectors.toSet());
        List<ProductImageSlot> imageSlots = new ArrayList<>();

        for (ImageSlotDefinition slotDefinition : IMAGE_SLOT_DEFINITIONS) {
            images.stream()
                    .filter(media -> slotDefinition.slotType().equals(media.getSlotType()))
                    .forEach(media -> imageSlots.add(new ProductImageSlot(
                            slotDefinition.slotType(),
                            slotDefinition.label(),
                            media
                    )));
        }

        images.stream()
                .filter(media -> media.getSlotType() == null || !definedSlotTypes.contains(media.getSlotType()))
                .forEach(media -> imageSlots.add(new ProductImageSlot("OTHER", "Ảnh khác", media)));

        return imageSlots;
    }

    private Comparator<ProductMedia> mediaDisplayComparator() {
        return Comparator
                .comparing(ProductMedia::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProductMedia::getMediaId, Comparator.nullsLast(Integer::compareTo));
    }

    public record ImageSlotDefinition(String slotType, String label) {
    }

    public record ProductImageSlot(String slotType, String label, ProductMedia media) {
    }
}
