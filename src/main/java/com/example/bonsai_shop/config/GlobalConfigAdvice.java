package com.example.bonsai_shop.config;

import com.example.bonsai_shop.owner.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalConfigAdvice {

    private final SystemConfigService systemConfigService;

    @ModelAttribute("sysConfig")
    public SystemConfigService getSystemConfig() {
        return systemConfigService;
    }
}
