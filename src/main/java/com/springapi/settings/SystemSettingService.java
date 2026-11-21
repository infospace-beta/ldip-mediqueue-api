package com.springapi.settings;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return repository.findAllByOrderByCategoryAscSettingKeyAsc();
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getSettingsByCategory(String category) {
        return repository.findAllByCategoryOrderBySettingKeyAsc(category);
    }

    @Transactional(readOnly = true)
    public SystemSetting getSettingByKey(String settingKey) {
        return repository.findBySettingKey(settingKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Setting not found: " + settingKey));
    }

    public SystemSetting saveSetting(
            String settingKey,
            String settingValue,
            String category,
            String description) {

        SystemSetting setting = repository.findBySettingKey(settingKey)
                .orElseGet(SystemSetting::new);

        setting.setSettingKey(settingKey);
        setting.setSettingValue(settingValue);
        setting.setCategory(category);
        setting.setDescription(description);

        return repository.save(setting);
    }
}
