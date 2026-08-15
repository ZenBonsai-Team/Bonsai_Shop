package com.example.bonsai_shop.owner.service;

import com.example.bonsai_shop.entity.SystemConfig;
import com.example.bonsai_shop.owner.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    // Lay gia tri cau hinh theo key, fallback ve defaultValue khi chua co hoac gia tri rong.
    public String getValue(String key, String defaultValue) {
        Optional<SystemConfig> config = systemConfigRepository.findById(key);
        // Chi tra ve gia tri trong database neu config ton tai va configValue khong rong.
        if (config.isPresent() && config.get().getConfigValue() != null && !config.get().getConfigValue().isEmpty()) {
            return config.get().getConfigValue();
        }
        return defaultValue;
    }

    // Lay toan bo cau hinh he thong de hien thi/kiem tra khi can.
    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    // Cap nhat cau hinh theo key; neu key chua ton tai thi tao ban ghi moi.
    @Transactional
    public void updateConfig(String key, String value) {
        // Tim config hien co, fallback build entity moi voi configKey tu form.
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(SystemConfig.builder().configKey(key).build());
        // Gan gia tri moi va luu lai vao database.
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }
}
