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

        log.info("========== [DATA INITIALIZER] Hoàn tất nạp dữ liệu chuẩn hệ thống ==========");
    }
}
