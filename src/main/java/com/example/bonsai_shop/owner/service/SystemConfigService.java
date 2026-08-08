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

    public String getValue(String key, String defaultValue) {
        Optional<SystemConfig> config = systemConfigRepository.findById(key);
        if (config.isPresent() && config.get().getConfigValue() != null && !config.get().getConfigValue().isEmpty()) {
            return config.get().getConfigValue();
        }
        return defaultValue;
    }

    public List<SystemConfig> getAllConfigs() {
        return systemConfigRepository.findAll();
    }

    @Transactional
    public void updateConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElse(SystemConfig.builder().configKey(key).build());
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }
}
