package com.example.bonsai_shop.owner.controller;

import com.example.bonsai_shop.data.common.CloudinaryFolder;
import com.example.bonsai_shop.data.service.CloudinaryStorageService;
import com.example.bonsai_shop.owner.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping("/owner/system-config")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Slf4j
public class OwnerSystemConfigController {

    private final SystemConfigService systemConfigService;
    private final CloudinaryStorageService cloudinaryStorageService;

    @GetMapping
    public String showConfig(Model model) {
        model.addAttribute("activeMenu", "system-config");
        return "owner/system_config";
    }

    @PostMapping
    public String saveConfig(
            @RequestParam Map<String, String> params,
            @RequestParam(value = "home_banner_image_file", required = false) MultipartFile homeFile,
            @RequestParam(value = "marketplace_banner_image_file", required = false) MultipartFile marketFile,
            @RequestParam(value = "community_banner_image_file", required = false) MultipartFile communityFile,
            @RequestParam(value = "luxury_banner_image_file", required = false) MultipartFile luxuryFile,
            Model model) {

        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_file") || key.equals("_csrf")) {
                    continue;
                }
                systemConfigService.updateConfig(key, entry.getValue());
            }

            uploadAndSaveImage(homeFile, "home_banner_image");
            uploadAndSaveImage(marketFile, "marketplace_banner_image");
            uploadAndSaveImage(communityFile, "community_banner_image");
            uploadAndSaveImage(luxuryFile, "luxury_banner_image");

            model.addAttribute("success", "Cập nhật cấu hình hệ thống thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi lưu cấu hình hệ thống: ", e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        model.addAttribute("activeMenu", "system-config");
        return "owner/system_config";
    }

    private void uploadAndSaveImage(MultipartFile file, String configKey) {
        if (file != null && !file.isEmpty()) {
            var uploadResponse = cloudinaryStorageService.uploadImage(file, CloudinaryFolder.BANNER);
            systemConfigService.updateConfig(configKey, uploadResponse.getUrl());
        }
    }
}
