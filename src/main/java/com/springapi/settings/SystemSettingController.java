package com.springapi.settings;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapi.settings.dto.SystemSettingRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemSettingController {

    private final SystemSettingService service;

    public SystemSettingController(SystemSettingService service) {
        this.service = service;
    }

    @GetMapping
    public List<SystemSetting> getAllSettings() {
        return service.getAllSettings();
    }

    @GetMapping("/category/{category}")
    public List<SystemSetting> getSettingsByCategory(
            @PathVariable String category) {
        return service.getSettingsByCategory(category);
    }

    @GetMapping("/{settingKey}")
    public SystemSetting getSettingByKey(
            @PathVariable String settingKey) {
        return service.getSettingByKey(settingKey);
    }

    @PutMapping("/{settingKey}")
    public SystemSetting updateSetting(
            @PathVariable String settingKey,
            @Valid @RequestBody SystemSettingRequest request) {

        return service.saveSetting(
                settingKey,
                request.settingValue(),
                request.category(),
                request.description()
        );
    }
}
