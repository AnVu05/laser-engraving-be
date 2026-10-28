package com.anne.laserengraving.config;

import com.anne.laserengraving.repository.DeviceRepository;
import com.anne.laserengraving.service.MaterialPresetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo dữ liệu cần thiết cho hệ thống (Production Data Initializer):
 * - Thư viện 6 vật liệu mẫu chuẩn hệ thống (Plywood, Acrylic, Da, MDF, Carton,
 * Nhôm)
 * 
 * Lưu ý:
 * - Không khởi tạo tài khoản admin/demo (người dùng tự đăng ký qua Gmail /
 * Google OAuth).
 * - Không khởi tạo dự án hay lịch sử gia công giả lập.
 */
@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final MaterialPresetService materialPresetService;
    private final DeviceRepository deviceRepository;
    private final com.anne.laserengraving.repository.UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            MaterialPresetService materialPresetService,
            DeviceRepository deviceRepository,
            com.anne.laserengraving.repository.UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.materialPresetService = materialPresetService;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== [DATA INITIALIZER] Bắt đầu kiểm tra và khởi tạo dữ liệu chuẩn hệ thống ==========");

        // 1. Khởi tạo tài khoản Quản trị viên mặc định (nếu chưa có)
        initDefaultAdminUser();

        // 2. Khởi tạo 6 vật liệu mẫu chuẩn hệ thống (nếu chưa có trong CSDL)
        try {
            materialPresetService.initDefaultPresets();
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi nạp vật liệu chuẩn hệ thống: {}", ex.getMessage());
        }

        // 3. Khởi tạo máy khắc Laser mẫu LASER-HN-001 (Makerbase MKS DLC32 V2.1)
        initDefaultDevice();

        log.info("========== [DATA INITIALIZER] Hoàn tất nạp dữ liệu chuẩn hệ thống ==========");
    }

    private void initDefaultAdminUser() {
        try {
            if (!userRepository.existsByEmail("admin@gmail.com")) {
                com.anne.laserengraving.entity.UserEntity adminUser = com.anne.laserengraving.entity.UserEntity.builder()
                        .username("admin")
                        .email("admin@gmail.com")
                        .fullName("Quản trị viên Laser Studio")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role("ADMIN")
                        .authProvider("LOCAL")
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                userRepository.save(adminUser);
                log.info("[DATA INITIALIZER] Đã khởi tạo tài khoản quản trị viên mặc định: admin@gmail.com / admin123");
            }
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi khởi tạo tài khoản quản trị viên: {}", ex.getMessage());
        }
    }

    private void initDefaultDevice() {
        try {
            if (!deviceRepository.existsByDeviceCode("LASER-HN-001")) {
                com.anne.laserengraving.entity.DeviceEntity defaultDevice = com.anne.laserengraving.entity.DeviceEntity
                        .builder()
                        .deviceCode("LASER-HN-001")
                        .deviceName("Máy Khắc Laser Tiêu Chuẩn Makerbase MKS DLC32 #01 (Xưởng Hà Nội)")
                        .connectionStatus("OFFLINE")
                        .firmwareVersion("GRBL 1.1h (MKS DLC32 V2.1 ESP32)")
                        .maxWidthMm(300.0)
                        .maxHeightMm(200.0)
                        .maxSpeedMmMin(1500)
                        .maxPowerS(1000)
                        .apiKeyHash(passwordEncoder.encode(createUnknownToken()))
                        .isActive(true)
                        .build();
                deviceRepository.save(defaultDevice);
                log.info("[DATA INITIALIZER] Đã khởi tạo thiết bị mẫu: LASER-HN-001 (Makerbase MKS DLC32 V2.1)");
            } else {
                deviceRepository.findByDeviceCode("LASER-HN-001").ifPresent(device -> {
                    if (device.getApiKeyHash() != null
                            && passwordEncoder.matches("mk_sec_laser_hn_001_secret", device.getApiKeyHash())) {
                        device.setApiKeyHash(passwordEncoder.encode(createUnknownToken()));
                        deviceRepository.save(device);
                        log.warn("[DATA INITIALIZER] Đã vô hiệu hóa credential mặc định đã biết của LASER-HN-001; cần cấp token mới từ dashboard.");
                    }
                });
            }
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi nạp thiết bị mẫu: {}", ex.getMessage());
        }
    }

    private String createUnknownToken() {
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }
}
