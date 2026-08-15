package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.dto.ArtisanProductFormDTO;
import com.example.bonsai_shop.entity.Product;
import com.example.bonsai_shop.entity.ProductMedia;
import com.example.bonsai_shop.entity.Variety;
import com.example.bonsai_shop.artisan.service.ArtisanProductService;
import com.example.bonsai_shop.artisan.service.ProductJournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
// Controller quản lý vòng đời sản phẩm bonsai của artisan.
public class ArtisanProductController {

    private final ArtisanProductService artisanProductService;
    private final ProductJournalService productJournalService;

    @GetMapping
    // Hiển thị danh sách sản phẩm của artisan theo bộ lọc trạng thái.
    public String myProducts(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "ALL") String status,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        List<Product> products = artisanProductService.getMyProducts(userDetails.getUsername());
        int pageSize = Math.min(Math.max(size, 1), 50);
        int totalPages = Math.max((int) Math.ceil((double) products.size() / pageSize), 1);
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int fromIndex = Math.min(safePage * pageSize, products.size());
        int toIndex = Math.min(fromIndex + pageSize, products.size());
        Page<Product> productPage = new PageImpl<>(
                products.subList(fromIndex, toIndex),
                PageRequest.of(safePage, pageSize),
                products.size()
        );
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSize", pageSize);
        return "artisan/products";
    }

    @GetMapping("/new")
    // Mở form tạo sản phẩm mới.
    public String createForm(Model model) {
        addProductFormData(model, null, new ArtisanProductFormDTO());
        return "artisan/product-form";
    }

    @PostMapping
    // Tạo sản phẩm nháp từ dữ liệu form.
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
            // Sau khi tao product thanh cong, dieu huong sang buoc upload image/video cho product vua tao.
            return "redirect:/artisan/products/" + product.getProductId() + "/media";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            addProductFormData(model, null, form);
            return "artisan/product-form";
        }
    }

    @GetMapping("/{productId}/edit")
    // Mở form chỉnh sửa sản phẩm thuộc artisan hiện tại.
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
    // Cập nhật thông tin sản phẩm nếu còn được phép sửa.
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
    // Xóa sản phẩm nháp hoặc sản phẩm chưa phát sinh ràng buộc.
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
    // Hiển thị trang quản lý ảnh/video của sản phẩm.
    public String mediaForm(@AuthenticationPrincipal UserDetails userDetails,
                            @PathVariable Integer productId,
                            Model model) {
        Product product = artisanProductService.getMyProduct(userDetails.getUsername(), productId);
        model.addAttribute("product", product);
        model.addAttribute("mediaList", artisanProductService.getMedia(product));
        model.addAttribute("isSold", artisanProductService.isSold(product));
        // View dung isEditable de an form upload khi product khong con duoc phep sua media.
        model.addAttribute("isEditable", artisanProductService.isEditable(product));
        return "artisan/product-media";
    }

    @PostMapping("/{productId}/media")
    // Upload nhiều media và chọn thumbnail mặc định nếu có.
    public String addMedia(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Integer productId,
                           @RequestParam(required = false) List<MultipartFile> files,
                           @RequestParam(required = false) List<String> mediaTypes,
                           @RequestParam(required = false) List<String> slotTypes,
                           @RequestParam(required = false) List<String> captions,
                           @RequestParam(required = false) Integer thumbnailIndex,
                           RedirectAttributes redirectAttributes) {
        try {
            int uploadedCount = artisanProductService.addMediaBatch(
                    userDetails.getUsername(),
                    productId,
                    files,
                    mediaTypes,
                    slotTypes,
                    captions,
                    thumbnailIndex
            );
            redirectAttributes.addFlashAttribute("success", "Đã thêm " + uploadedCount + " media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        // PRG pattern: redirect ve trang media de tranh submit lai file khi refresh.
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/thumbnail")
    // Đặt một media ảnh làm thumbnail của sản phẩm.
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
    // Cập nhật thứ tự, góc chụp và caption của media.
    public String updateMediaOrder(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable Integer productId,
                                   @RequestParam(required = false) List<Integer> mediaIds,
                                   @RequestParam(required = false) List<Integer> displayOrders,
                                   @RequestParam(required = false) List<String> slotTypes,
                                   @RequestParam(required = false) List<String> captions,
                                   RedirectAttributes redirectAttributes) {
        try {
            artisanProductService.updateMediaOrder(userDetails.getUsername(), productId, mediaIds, displayOrders, slotTypes, captions);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật media.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/media";
    }

    @PostMapping("/{productId}/media/{mediaId}/delete")
    // Xóa media khỏi sản phẩm và storage.
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
    // Hiển thị nhật ký chăm sóc/cập nhật trạng thái cây.
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
    // Thêm một sự kiện nhật ký mới cho sản phẩm.
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
    // Xóa sự kiện nhật ký thuộc sản phẩm.
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
    // Cập nhật tiêu đề và mô tả của sự kiện nhật ký.
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
    // Bật/tắt hiển thị sự kiện nhật ký cho khách hàng.
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
    // Bổ sung ảnh cho một sự kiện nhật ký.
    public String addJournalEventMedia(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable Integer productId,
                                       @PathVariable Integer eventId,
                                       @RequestParam(required = false) List<MultipartFile> files,
                                       RedirectAttributes redirectAttributes) {
        try {
            productJournalService.addMediaToEvent(userDetails.getUsername(), productId, eventId, files);
            redirectAttributes.addFlashAttribute("success", "Đã bổ sung ảnh cho cập nhật cây.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/media/{mediaId}/cover")
    // Chọn ảnh đại diện cho sự kiện nhật ký.
    public String setJournalEventCoverMedia(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Integer productId,
                                            @PathVariable Integer eventId,
                                            @PathVariable Integer mediaId,
                                            RedirectAttributes redirectAttributes) {
        try {
            productJournalService.setCoverMedia(userDetails.getUsername(), productId, eventId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt ảnh đại diện cho nhật ký.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/media/{mediaId}/replace")
    // Thay file ảnh của một media nhật ký.
    public String replaceJournalEventMedia(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Integer productId,
                                           @PathVariable Integer eventId,
                                           @PathVariable Integer mediaId,
                                           @RequestParam MultipartFile file,
                                           RedirectAttributes redirectAttributes) {
        try {
            productJournalService.replaceMedia(userDetails.getUsername(), productId, eventId, mediaId, file);
            redirectAttributes.addFlashAttribute("success", "Đã thay ảnh nhật ký.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @PostMapping("/{productId}/journal/{eventId}/media/{mediaId}/delete")
    // Xóa ảnh khỏi sự kiện nhật ký.
    public String deleteJournalEventMedia(@AuthenticationPrincipal UserDetails userDetails,
                                          @PathVariable Integer productId,
                                          @PathVariable Integer eventId,
                                          @PathVariable Integer mediaId,
                                          RedirectAttributes redirectAttributes) {
        try {
            productJournalService.deleteMedia(userDetails.getUsername(), productId, eventId, mediaId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa ảnh nhật ký.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/products/" + productId + "/journal";
    }

    @GetMapping("/{productId}/preview")
    // Hiển thị bản preview sản phẩm trước/sau khi publish.
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
    // Publish sản phẩm lên marketplace sau khi đủ điều kiện.
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
    // Ẩn sản phẩm khỏi marketplace khi trạng thái cho phép.
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
    // Hiện lại sản phẩm trên marketplace.
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

    // Nạp dữ liệu dropdown và lựa chọn hiện tại cho form sản phẩm.
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

    // Xác định category đang chọn dựa trên variety của form hoặc sản phẩm.
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
