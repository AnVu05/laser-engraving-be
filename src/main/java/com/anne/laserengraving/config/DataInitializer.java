package com.anne.laserengraving.config;

import com.anne.laserengraving.service.MaterialPresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo dữ liệu cần thiết cho hệ thống (Production Data Initializer):
 * - Thư viện 6 vật liệu mẫu chuẩn hệ thống (Plywood, Acrylic, Da, MDF, Carton, Nhôm)
 * 
 * Lưu ý:
 * - Không khởi tạo tài khoản admin/demo (người dùng tự đăng ký qua Gmail / Google OAuth).
 * - Không khởi tạo dự án hay lịch sử gia công giả lập.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final MaterialPresetService materialPresetService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== [DATA INITIALIZER] Bắt đầu kiểm tra và khởi tạo dữ liệu chuẩn hệ thống ==========");

        // Khởi tạo 6 vật liệu mẫu chuẩn hệ thống (nếu chưa có trong CSDL)
        try {
            materialPresetService.initDefaultPresets();
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi nạp vật liệu chuẩn hệ thống: {}", ex.getMessage());
        }

        // Khởi tạo máy khắc Laser mẫu LASER-HN-001 (Makerbase MKS DLC32 V2.1)
        initDefaultDevice();

        log.info("========== [DATA INITIALIZER] Hoàn tất nạp dữ liệu chuẩn hệ thống ==========");
    }

    private final com.anne.laserengraving.repository.DeviceRepository deviceRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private void initDefaultDevice() {
        try {
            String defaultToken = "mk_sec_laser_hn_001_secret";
            if (!deviceRepository.existsByDeviceCode("LASER-HN-001")) {
                com.anne.laserengraving.entity.DeviceEntity defaultDevice = com.anne.laserengraving.entity.DeviceEntity.builder()
                        .deviceCode("LASER-HN-001")
                        .deviceName("Máy Khắc Laser Tiêu Chuẩn Makerbase MKS DLC32 #01 (Xưởng Hà Nội)")
                        .connectionStatus("ONLINE")
                        .ipAddress("192.168.1.105")
                        .firmwareVersion("GRBL 1.1h (MKS DLC32 V2.1 ESP32)")
                        .maxWidthMm(300.0)
                        .maxHeightMm(200.0)
                        .maxSpeedMmMin(1500)
                        .maxPowerS(1000)
                        .apiKeyHash(passwordEncoder.encode(defaultToken))
                        .lastHeartbeat(java.time.LocalDateTime.now())
                        .isActive(true)
                        .build();
                deviceRepository.save(defaultDevice);
                log.info("[DATA INITIALIZER] Đã khởi tạo thiết bị mẫu: LASER-HN-001 (Makerbase MKS DLC32 V2.1)");
            } else {
                deviceRepository.findByDeviceCode("LASER-HN-001").ifPresent(device -> {
                    if (device.getApiKeyHash() == null || !passwordEncoder.matches(defaultToken, device.getApiKeyHash())) {
                        device.setApiKeyHash(passwordEncoder.encode(defaultToken));
                        deviceRepository.save(device);
                        log.info("[DATA INITIALIZER] Đã cập nhật apiKeyHash chuẩn cho thiết bị: LASER-HN-001");
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi nạp thiết bị mẫu: {}", ex.getMessage());
        }
    }
}

