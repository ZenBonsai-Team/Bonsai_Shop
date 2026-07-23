package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/artisan/products")
@RequiredArgsConstructor
public class ArtisanProductController {

    private final ArtisanProductService artisanProductService;

    @GetMapping
    public String myProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Product> products = artisanProductService.getMyProducts(userDetails.getUsername());
        model.addAttribute("products", products);
        return "artisan/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        addProductFormData(model, null);
        return "artisan/product-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam Integer varietyId,
                         @RequestParam Integer segmentId,
                         @RequestParam String productName,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer age,
                         @RequestParam Float height,
                         @RequestParam Float trunkDiameter,
                         @RequestParam String style,
                         @RequestParam BigDecimal price,
                         @RequestParam(required = false) List<Integer> tagIds,
                         RedirectAttributes redirectAttributes) {
        try {
            Product product = artisanProductService.createProduct(
                    userDetails.getUsername(),
                    varietyId,
                    segmentId,
                    productName,
                    description,
                    age,
                    height,
                    trunkDiameter,
                    style,
                    price,
                    "DRAFT",
                    tagIds
            );
            redirectAttributes.addFlashAttribute("success", "Đã lưu thông tin cây. Tiếp tục upload media.");
            return "redirect:/artisan/products/" + product.getProductId() + "/media";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/artisan/products/new";
        }
    }

    @GetMapping("/{productId}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Integer productId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);
        if (!artisanProductService.isEditable(product)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể sửa sản phẩm nháp hoặc đã ẩn.");
            return "redirect:/artisan/products/" + productId + "/preview";
        }
        addProductFormData(model, product);
        return "artisan/product-form";
    }

    @PostMapping("/{productId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer productId,
                         @RequestParam Integer varietyId,
                         @RequestParam Integer segmentId,
                         @RequestParam String productName,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer age,
                         @RequestParam Float height,
                         @RequestParam Float trunkDiameter,
                         @RequestParam String style,
                         @RequestParam BigDecimal price,
                         @RequestParam String productStatus,
                         @RequestParam(required = false) List<Integer> tagIds,
                         RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.updateProduct(
                    userDetails.getUsername(),
                    productId,
                    varietyId,
                    segmentId,
                    productName,
                    description,
                    age,
                    height,
                    trunkDiameter,
                    style,
                    price,
                    productStatus,
                    tagIds
            );
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm.");
            return "redirect:/artisan/products/" + productId + "/preview";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/artisan/products/" + productId + "/edit";
        }
    }

    @PostMapping("/{productId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer productId,
                         RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.deleteProduct(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products";
    }

    @GetMapping("/{productId}/media")
    public String mediaForm(@AuthenticationPrincipal UserDetails userDetails,
                            @PathVariable Integer productId,
                            Model model) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);
        model.addAttribute("product", product);
        model.addAttribute("mediaList", artisanProductService.getMedia(product));
        model.addAttribute("isSold", artisanProductService.isSold(product));
        model.addAttribute("isEditable", artisanProductService.isEditable(product));
        return "artisan/product-media";
    }

    @PostMapping("/{productId}/media")
    public String addMedia(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Integer productId,
                           @RequestParam MultipartFile file,
                           @RequestParam(required = false) String slotType,
                           @RequestParam(required = false) String caption,
                           @RequestParam(defaultValue = "false") Boolean isThumbnail,
                           RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.addMedia(
                    userDetails.getUsername(),
                    productId,
                    file,
                    slotType,
                    caption,
                    isThumbnail
            );
            redirectAttributes.addFlashAttribute("success", "Đã thêm media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/thumbnail")
    public String setThumbnail(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Integer productId,
                               @PathVariable Integer mediaId,
                               RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.setThumbnail(userDetails.getUsername(), productId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt thumbnail.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/order")
    public String updateMediaOrder(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable Integer productId,
                                   @RequestParam(required = false) List<Integer> mediaIds,
                                   @RequestParam(required = false) List<Integer> displayOrders,
                                   RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.updateMediaOrder(userDetails.getUsername(), productId, mediaIds, displayOrders);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật thứ tự media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/delete")
    public String deleteMedia(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Integer productId,
                              @PathVariable Integer mediaId,
                              RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.deleteMedia(userDetails.getUsername(), productId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @GetMapping("/{productId}/preview")
    public String preview(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable Integer productId,
                          Model model) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);
        List<ProductMedia> mediaList = artisanProductService.getMedia(product);
        ProductMedia thumbnail = mediaList.stream()
                .filter(media -> Boolean.TRUE.equals(media.getIsThumbnail()))
                .findFirst()
                .orElse(mediaList.isEmpty() ? null : mediaList.get(0));

        model.addAttribute("product", product);
        model.addAttribute("mediaList", mediaList);
        model.addAttribute("thumbnail", thumbnail);
        model.addAttribute("tags", artisanProductService.getProductTags(product));
        model.addAttribute("imageCount", mediaList.stream().filter(media -> "IMAGE".equals(media.getMediaType())).count());
        model.addAttribute("videoCount", mediaList.stream().filter(media -> "VIDEO".equals(media.getMediaType())).count());
        model.addAttribute("isSold", artisanProductService.isSold(product));
        model.addAttribute("isEditable", artisanProductService.isEditable(product));
        model.addAttribute("isHideable", artisanProductService.isHideable(product));
        return "artisan/product-preview";
    }

    @PostMapping("/{productId}/publish")
    public String publish(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable Integer productId,
                          RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.publish(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã publish sản phẩm.");
            return "redirect:/artisan/products";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/artisan/products";
        }
    }

    @PostMapping("/{productId}/hide")
    public String hide(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable Integer productId,
                       RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.hideProduct(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn sản phẩm khỏi marketplace.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products";
    }

    private void addProductFormData(Model model, Product product) {
        model.addAttribute("product", product);
        model.addAttribute("categories", artisanProductService.getCategories());
        model.addAttribute("varieties", artisanProductService.getVarieties());
        model.addAttribute("segments", artisanProductService.getSegments());
        model.addAttribute("tags", artisanProductService.getTags());
        model.addAttribute("selectedCategoryId", product == null ? null : product.getVariety().getCategory().getCategoryId());
        model.addAttribute("selectedTagIds", product == null ? List.of() : artisanProductService.getSelectedTagIds(product));
    }
}
