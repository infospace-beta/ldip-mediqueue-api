package com.springapi.settings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository
        extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);

    List<SystemSetting> findAllByCategoryOrderBySettingKeyAsc(String category);

    List<SystemSetting> findAllByOrderByCategoryAscSettingKeyAsc();

    boolean existsBySettingKey(String settingKey);
}
