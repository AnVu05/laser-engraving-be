package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.LaserSettingsUpdateRequest;
import com.anne.laserengraving.dto.response.LaserSettingsResponse;
import com.anne.laserengraving.entity.LaserSettingsEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.LaserSettingsRepository;
import com.anne.laserengraving.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LaserSettingsServiceImpl implements LaserSettingsService {

    private final LaserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LaserSettingsResponse getCurrentSettings(Integer userId) {
        LaserSettingsEntity entity = settingsRepository.findFirstByUser_UserId(userId)
                .orElseGet(this::createDefaultSettingsFallback);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public LaserSettingsResponse updateSettings(Integer userId, LaserSettingsUpdateRequest request) {
        LaserSettingsEntity entity = (userId != null)
                ? settingsRepository.findFirstByUser_UserId(userId).orElse(null)
                : settingsRepository.findAll().stream().findFirst().orElse(null);

        if (entity == null) {
            UserEntity user = null;
            if (userId != null) {
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
            } else {
                user = userRepository.findAll().stream().findFirst().orElse(null);
            }
            entity = LaserSettingsEntity.builder()
                    .user(user)
                    .build();
        }

        entity.setProfileName(request.getProfileName());
        entity.setDefaultSpeed(request.getDefaultSpeed());
        entity.setMaxPower(request.getMaxPower());
        entity.setDefaultDpi(request.getDefaultDpi());
        if (request.getMaxWidthMm() != null) {
            entity.setMaxWidthMm(request.getMaxWidthMm());
        }
        if (request.getMaxHeightMm() != null) {
            entity.setMaxHeightMm(request.getMaxHeightMm());
        }
        if (request.getContrast() != null) {
            entity.setContrast(request.getContrast());
        }
        if (request.getBrightness() != null) {
            entity.setBrightness(request.getBrightness());
        }
        if (request.getInvert() != null) {
            entity.setInvert(request.getInvert());
        }
        if (request.getMode() != null) {
            entity.setMode(request.getMode());
        }
        if (request.getStepSize() != null) {
            entity.setStepSize(request.getStepSize());
        }
        if (request.getPasses() != null) {
            entity.setPasses(request.getPasses());
        }

        LaserSettingsEntity saved = settingsRepository.save(entity);
        return mapToResponse(saved);
    }

    private LaserSettingsEntity createDefaultSettingsFallback() {
        return LaserSettingsEntity.builder()
                .settingId(1)
                .profileName("Mặc định (Gỗ ép mỏng 3mm)")
                .defaultSpeed(BigDecimal.valueOf(1500.0))
                .maxPower(800)
                .defaultDpi(254)
                .maxWidthMm(BigDecimal.valueOf(300.0))
                .maxHeightMm(BigDecimal.valueOf(200.0))
                .contrast(15)
                .brightness(0)
                .invert(false)
                .mode("raster")
                .stepSize(BigDecimal.valueOf(10.0))
                .passes(1)
                .build();
    }

    private LaserSettingsResponse mapToResponse(LaserSettingsEntity entity) {
        return LaserSettingsResponse.builder()
                .settingId(entity.getSettingId())
                .profileName(entity.getProfileName())
                .defaultSpeed(entity.getDefaultSpeed())
                .maxPower(entity.getMaxPower())
                .defaultDpi(entity.getDefaultDpi())
                .maxWidthMm(entity.getMaxWidthMm())
                .maxHeightMm(entity.getMaxHeightMm())
                .contrast(entity.getContrast())
                .brightness(entity.getBrightness())
                .invert(entity.getInvert())
                .mode(entity.getMode())
                .stepSize(entity.getStepSize())
                .passes(entity.getPasses())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
