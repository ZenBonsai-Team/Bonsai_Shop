package com.example.bonsai_shop.seller.controller;

import com.example.bonsai_shop.seller.service.SellerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/seller/catalog")
@RequiredArgsConstructor
public class SellerCatalogController {

    private final SellerCatalogService sellerCatalogService;

    @GetMapping
    public String catalog(Model model) {
        addCatalogData(model);
        return "seller/catalog";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String categoryName,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.createCategory(categoryName, description);
            redirectAttributes.addFlashAttribute("success", "Đã tạo category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/categories/{categoryId}")
    public String updateCategory(@PathVariable Integer categoryId,
                                 @RequestParam String categoryName,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.updateCategory(categoryId, categoryName, description);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/categories/{categoryId}/delete")
    public String deleteCategory(@PathVariable Integer categoryId,
                                 RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.deleteCategory(categoryId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/varieties")
    public String createVariety(@RequestParam Integer categoryId,
                                @RequestParam String varietyName,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.createVariety(categoryId, varietyName, description);
            redirectAttributes.addFlashAttribute("success", "Đã tạo variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/varieties/{varietyId}")
    public String updateVariety(@PathVariable Integer varietyId,
                                @RequestParam Integer categoryId,
                                @RequestParam String varietyName,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.updateVariety(varietyId, categoryId, varietyName, description);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/varieties/{varietyId}/delete")
    public String deleteVariety(@PathVariable Integer varietyId,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.deleteVariety(varietyId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/segments")
    public String createSegment(@RequestParam String segmentName,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.createSegment(segmentName);
            redirectAttributes.addFlashAttribute("success", "Đã tạo segment.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/segments/{segmentId}")
    public String updateSegment(@PathVariable Integer segmentId,
                                @RequestParam String segmentName,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.updateSegment(segmentId, segmentName);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật segment.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/segments/{segmentId}/delete")
    public String deleteSegment(@PathVariable Integer segmentId,
                                RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.deleteSegment(segmentId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa segment.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/tags")
    public String createTag(@RequestParam String tagName,
                            RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.createTag(tagName);
            redirectAttributes.addFlashAttribute("success", "Đã tạo tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/tags/{tagId}")
    public String updateTag(@PathVariable Integer tagId,
                            @RequestParam String tagName,
                            RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.updateTag(tagId, tagName);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    @PostMapping("/tags/{tagId}/delete")
    public String deleteTag(@PathVariable Integer tagId,
                            RedirectAttributes redirectAttributes) {
        try {
            sellerCatalogService.deleteTag(tagId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/catalog";
    }

    private void addCatalogData(Model model) {
        model.addAttribute("categories", sellerCatalogService.getCategories());
        model.addAttribute("varieties", sellerCatalogService.getVarieties());
        model.addAttribute("segments", sellerCatalogService.getSegments());
        model.addAttribute("tags", sellerCatalogService.getTags());
    }
}
