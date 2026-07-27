package com.example.bonsai_shop.artisan.controller;

import com.example.bonsai_shop.artisan.service.ArtisanCatalogService;
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
@RequestMapping("/artisan/catalog")
@RequiredArgsConstructor
public class ArtisanCatalogController {

    private final ArtisanCatalogService artisanCatalogService;

    @GetMapping
    public String catalog(Model model) {
        addCatalogData(model);
        return "artisan/catalog";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String categoryName,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.createCategory(categoryName, description);
            redirectAttributes.addFlashAttribute("success", "Đã tạo category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/categories/{categoryId}")
    public String updateCategory(@PathVariable Integer categoryId,
                                 @RequestParam String categoryName,
                                 @RequestParam(required = false) String description,
                                 RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.updateCategory(categoryId, categoryName, description);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/categories/{categoryId}/delete")
    public String deleteCategory(@PathVariable Integer categoryId,
                                 RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.deleteCategory(categoryId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa category.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/varieties")
    public String createVariety(@RequestParam Integer categoryId,
                                @RequestParam String varietyName,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.createVariety(categoryId, varietyName, description);
            redirectAttributes.addFlashAttribute("success", "Đã tạo variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/varieties/{varietyId}")
    public String updateVariety(@PathVariable Integer varietyId,
                                @RequestParam Integer categoryId,
                                @RequestParam String varietyName,
                                @RequestParam(required = false) String description,
                                RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.updateVariety(varietyId, categoryId, varietyName, description);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/varieties/{varietyId}/delete")
    public String deleteVariety(@PathVariable Integer varietyId,
                                RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.deleteVariety(varietyId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa variety.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }
    @PostMapping("/tags")
    public String createTag(@RequestParam String tagName,
                            RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.createTag(tagName);
            redirectAttributes.addFlashAttribute("success", "Đã tạo tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/tags/{tagId}")
    public String updateTag(@PathVariable Integer tagId,
                            @RequestParam String tagName,
                            RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.updateTag(tagId, tagName);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    @PostMapping("/tags/{tagId}/delete")
    public String deleteTag(@PathVariable Integer tagId,
                            RedirectAttributes redirectAttributes) {
        try {
            artisanCatalogService.deleteTag(tagId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tag.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/artisan/catalog";
    }

    private void addCatalogData(Model model) {
        model.addAttribute("categories", artisanCatalogService.getCategories());
        model.addAttribute("varieties", artisanCatalogService.getVarieties());
        model.addAttribute("tags", artisanCatalogService.getTags());
        model.addAttribute("categoryIdsInUse", artisanCatalogService.getCategoryIdsInUse());
        model.addAttribute("varietyIdsInUse", artisanCatalogService.getVarietyIdsInUse());
        model.addAttribute("tagIdsInUse", artisanCatalogService.getTagIdsInUse());
    }
}

