package com.example.bonsai_shop.seller.controller;

import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.seller.service.SellerProductService;
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
@RequestMapping("/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @GetMapping
    public String myProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Product> products = sellerProductService.getMyProducts(userDetails.getUsername());
        model.addAttribute("products", products);
        return "seller/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        addProductFormData(model, null);
        return "seller/product-form";
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
                         @RequestParam(defaultValue = "false") Boolean isPublicPrice,
                         @RequestParam(required = false) List<Integer> tagIds,
                         RedirectAttributes redirectAttributes) {
        try {
            Product product = sellerProductService.createProduct(
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
                    isPublicPrice,
                    "DRAFT",
                    tagIds
            );
            redirectAttributes.addFlashAttribute("success", "Đã lưu thông tin cây. Tiếp tục upload media.");
            return "redirect:/seller/products/" + product.getProductId() + "/media";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/products/new";
        }
    }

    @GetMapping("/{productId}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Integer productId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Product product = sellerProductService.getMyProduct(userDetails.getUsername(), productId);
        if (!sellerProductService.isEditable(product)) {
            redirectAttributes.addFlashAttribute("error", "Chỉ có thể sửa sản phẩm nháp hoặc đã ẩn.");
            return "redirect:/seller/products/" + productId + "/preview";
        }
        addProductFormData(model, product);
        return "seller/product-form";
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
                         @RequestParam(defaultValue = "false") Boolean isPublicPrice,
                         @RequestParam String productStatus,
                         @RequestParam(required = false) List<Integer> tagIds,
                         RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.updateProduct(
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
                    isPublicPrice,
                    productStatus,
                    tagIds
            );
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm.");
            return "redirect:/seller/products/" + productId + "/preview";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/products/" + productId + "/edit";
        }
    }

    @PostMapping("/{productId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer productId,
                         RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.deleteProduct(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    @GetMapping("/{productId}/media")
    public String mediaForm(@AuthenticationPrincipal UserDetails userDetails,
                            @PathVariable Integer productId,
                            Model model) {
        Product product = sellerProductService.getMyProduct(userDetails.getUsername(), productId);
        model.addAttribute("product", product);
        model.addAttribute("mediaList", sellerProductService.getMedia(product));
        model.addAttribute("isSold", sellerProductService.isSold(product));
        model.addAttribute("isEditable", sellerProductService.isEditable(product));
        return "seller/product-media";
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
            sellerProductService.addMedia(
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
        return "redirect:/seller/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/thumbnail")
    public String setThumbnail(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Integer productId,
                               @PathVariable Integer mediaId,
                               RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.setThumbnail(userDetails.getUsername(), productId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt thumbnail.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/order")
    public String updateMediaOrder(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable Integer productId,
                                   @RequestParam(required = false) List<Integer> mediaIds,
                                   @RequestParam(required = false) List<Integer> displayOrders,
                                   RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.updateMediaOrder(userDetails.getUsername(), productId, mediaIds, displayOrders);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật thứ tự media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/delete")
    public String deleteMedia(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Integer productId,
                              @PathVariable Integer mediaId,
                              RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.deleteMedia(userDetails.getUsername(), productId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products/" + productId + "/media";
    }

    @GetMapping("/{productId}/preview")
    public String preview(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable Integer productId,
                          Model model) {
        Product product = sellerProductService.getMyProduct(userDetails.getUsername(), productId);
        List<ProductMedia> mediaList = sellerProductService.getMedia(product);
        ProductMedia thumbnail = mediaList.stream()
                .filter(media -> Boolean.TRUE.equals(media.getIsThumbnail()))
                .findFirst()
                .orElse(mediaList.isEmpty() ? null : mediaList.get(0));

        model.addAttribute("product", product);
        model.addAttribute("mediaList", mediaList);
        model.addAttribute("thumbnail", thumbnail);
        model.addAttribute("tags", sellerProductService.getProductTags(product));
        model.addAttribute("imageCount", mediaList.stream().filter(media -> "IMAGE".equals(media.getMediaType())).count());
        model.addAttribute("videoCount", mediaList.stream().filter(media -> "VIDEO".equals(media.getMediaType())).count());
        model.addAttribute("isSold", sellerProductService.isSold(product));
        model.addAttribute("isEditable", sellerProductService.isEditable(product));
        model.addAttribute("isHideable", sellerProductService.isHideable(product));
        return "seller/product-preview";
    }

    @PostMapping("/{productId}/publish")
    public String publish(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable Integer productId,
                          RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.publish(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã publish sản phẩm.");
            return "redirect:/seller/products";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/products";
        }
    }

    @PostMapping("/{productId}/hide")
    public String hide(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable Integer productId,
                       RedirectAttributes redirectAttributes) {
        try {
            sellerProductService.hideProduct(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn sản phẩm khỏi marketplace.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/products";
    }

    private void addProductFormData(Model model, Product product) {
        model.addAttribute("product", product);
        model.addAttribute("categories", sellerProductService.getCategories());
        model.addAttribute("varieties", sellerProductService.getVarieties());
        model.addAttribute("segments", sellerProductService.getSegments());
        model.addAttribute("tags", sellerProductService.getTags());
        model.addAttribute("selectedCategoryId", product == null ? null : product.getVariety().getCategory().getCategoryId());
        model.addAttribute("selectedTagIds", product == null ? List.of() : sellerProductService.getSelectedTagIds(product));
    }
}
