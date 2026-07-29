package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/artisan/products")
@RequiredArgsConstructor
public class ArtisanProductController {

    private final ArtisanProductService artisanProductService;
    private final ProductJournalService productJournalService;

    @GetMapping
    public String myProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Product> products = artisanProductService.getMyProducts(userDetails.getUsername());
        model.addAttribute("products", products);
        return "artisan/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        addProductFormData(model, null, new ArtisanProductFormDTO());
        return "artisan/product-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("productForm") ArtisanProductFormDTO form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addProductFormData(model, null, form);
            return "artisan/product-form";
        }

        try {
            Product product = artisanProductService.createProduct(userDetails.getUsername(), form);
            redirectAttributes.addFlashAttribute("success", "Đã lưu thông tin cây. Tiếp tục upload media.");
            return "redirect:/artisan/products/" + product.getProductId() + "/media";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            addProductFormData(model, null, form);
            return "artisan/product-form";
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
        addProductFormData(model, product, artisanProductService.toFormDTO(product));
        return "artisan/product-form";
    }

    @PostMapping("/{productId}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Integer productId,
                         @Valid @ModelAttribute("productForm") ArtisanProductFormDTO form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);

        if (bindingResult.hasErrors()) {
            addProductFormData(model, product, form);
            return "artisan/product-form";
        }

        try {
            artisanProductService.updateProduct(userDetails.getUsername(), productId, form);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm.");
            return "redirect:/artisan/products/" + productId + "/preview";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            addProductFormData(model, product, form);
            return "artisan/product-form";
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
                                   @RequestParam(required = false) List<String> captions,
                                   RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.updateMediaOrder(userDetails.getUsername(), productId, mediaIds, displayOrders, captions);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật media.");
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

    @GetMapping("/{productId}/journal")
    public String journal(@AuthenticationPrincipal UserDetails userDetails,
                          @PathVariable Integer productId,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);
        if (artisanProductService.isSold(product)) {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm đã bán nên không hiển thị nhật ký cây.");
            return "redirect:/artisan/products/" + productId + "/preview";
        }
        model.addAttribute("product", product);
        model.addAttribute("journalEvents", productJournalService.getMyProductEvents(userDetails.getUsername(), productId));
        model.addAttribute("today", LocalDate.now());
        return "artisan/product-journal";
    }

    @PostMapping("/{productId}/journal")
    public String addJournalEvent(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Integer productId,
                                  @RequestParam(required = false) String eventType,
                                  @RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(defaultValue = "false") Boolean isPublic,
                                  @RequestParam(required = false) List<MultipartFile> files,
                                  RedirectAttributes redirectAttributes) {
        try {
            productJournalService.addEvent(userDetails.getUsername(), productId, LocalDate.now(), eventType, title, description, isPublic, files);
            redirectAttributes.addFlashAttribute("success", "Đã thêm cập nhật trạng thái cây.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/delete")
    public String deleteJournalEvent(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable Integer productId,
                                     @PathVariable Integer eventId,
                                     RedirectAttributes redirectAttributes) {
        try {
            productJournalService.deleteEvent(userDetails.getUsername(), productId, eventId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa cập nhật cây.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}")
    public String updateJournalEventText(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable Integer productId,
                                         @PathVariable Integer eventId,
                                         @RequestParam String title,
                                         @RequestParam(required = false) String description,
                                         RedirectAttributes redirectAttributes) {
        try {
            productJournalService.updateEventText(userDetails.getUsername(), productId, eventId, title, description);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tiêu đề và mô tả.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/visibility")
    public String updateJournalEventVisibility(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable Integer productId,
                                               @PathVariable Integer eventId,
                                               @RequestParam(defaultValue = "false") Boolean isPublic,
                                               RedirectAttributes redirectAttributes) {
        try {
            productJournalService.updateEventVisibility(userDetails.getUsername(), productId, eventId, isPublic);
            redirectAttributes.addFlashAttribute("success", Boolean.TRUE.equals(isPublic)
                    ? "Đã hiển thị nhật ký cho khách."
                    : "Đã ẩn nhật ký khỏi trang khách.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/media")
    public String addJournalEventMedia(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Integer productId,
                                       @PathVariable Integer eventId,
                                       @RequestParam(required = false) List<MultipartFile> files,
                                       RedirectAttributes redirectAttributes) {
        try {
            productJournalService.addMediaToEvent(userDetails.getUsername(), productId, eventId, files);
            redirectAttributes.addFlashAttribute("success", "Đã bổ sung ảnh/video cho cập nhật cây.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
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
        model.addAttribute("isVisible", artisanProductService.isVisible(product));
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

    @PostMapping("/{productId}/show")
    public String show(@AuthenticationPrincipal UserDetails userDetails,
                       @PathVariable Integer productId,
                       RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.showProduct(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("success", "Đã hiện sản phẩm trên marketplace.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products";
    }

    private void addProductFormData(Model model, Product product, ArtisanProductFormDTO form) {
        List<Variety> varieties = artisanProductService.getVarieties();

        model.addAttribute("product", product);
        model.addAttribute("productForm", form);
        model.addAttribute("categories", artisanProductService.getCategories());
        model.addAttribute("varieties", varieties);
        model.addAttribute("segments", artisanProductService.getSegments());
        model.addAttribute("tags", artisanProductService.getTags());
        model.addAttribute("selectedCategoryId", resolveSelectedCategoryId(form, product, varieties));
        model.addAttribute("selectedTagIds", form.getTagIds() == null ? List.of() : form.getTagIds());
    }

    private Integer resolveSelectedCategoryId(ArtisanProductFormDTO form, Product product, List<Variety> varieties) {
        if (form.getVarietyId() != null) {
            return varieties.stream()
                    .filter(variety -> form.getVarietyId().equals(variety.getVarietyId()))
                    .findFirst()
                    .map(variety -> variety.getCategory().getCategoryId())
                    .orElse(null);
        }

        if (product == null || product.getVariety() == null || product.getVariety().getCategory() == null) {
            return null;
        }

        return product.getVariety().getCategory().getCategoryId();
    }
}
