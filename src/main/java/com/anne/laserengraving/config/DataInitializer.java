package com.anne.laserengraving.config;

import com.anne.laserengraving.entity.GcodeHistoryEntity;
import com.anne.laserengraving.entity.LaserSettingsEntity;
import com.anne.laserengraving.entity.ProjectEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.repository.GcodeHistoryRepository;
import com.anne.laserengraving.repository.LaserSettingsRepository;
import com.anne.laserengraving.repository.ProjectRepository;
import com.anne.laserengraving.repository.UserRepository;
import com.anne.laserengraving.service.MaterialPresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Khởi tạo dữ liệu mẫu ban đầu (Data Initializer) cho Microsoft SQL Server:
 * - Tài khoản Demo & Admin
 * - Cấu hình máy Laser chuẩn
 * - Thư viện vật liệu mẫu (Plywood, Acrylic, Da, MDF, Carton, Nhôm)
 * - Các dự án bàn vẽ mẫu (Biển tên mica, Khung tranh gỗ, Móc khóa da)
 * - Lịch sử gia công mẫu
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final LaserSettingsRepository laserSettingsRepository;
    private final ProjectRepository projectRepository;
    private final GcodeHistoryRepository gcodeHistoryRepository;
    private final MaterialPresetService materialPresetService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========== [DATA INITIALIZER] Bắt đầu kiểm tra và khởi tạo dữ liệu ban đầu ==========");

        // 1. Khởi tạo tài khoản mẫu
        UserEntity demoGmail = initUser("demo@gmail.com", "demo_gmail", "Laser@123", "Laser Operator Demo", "OPERATOR");
        UserEntity demoUser = initUser("demo@laserstudio.vn", "demo_operator", "Laser@123", "Kỹ thuật viên Laser Demo", "OPERATOR");
        UserEntity adminUser = initUser("admin@laserstudio.vn", "admin_system", "Admin@123", "Quản trị viên Hệ thống", "ADMIN");

        // 2. Khởi tạo cấu hình máy mặc định
        if (demoGmail != null) {
            initSettings(demoGmail);
        }
        if (demoUser != null) {
            initSettings(demoUser);
        }
        if (adminUser != null) {
            initSettings(adminUser);
        }

        // 3. Khởi tạo 6 vật liệu mẫu hệ thống
        try {
            materialPresetService.initDefaultPresets();
        } catch (Exception ex) {
            log.warn("[DATA INITIALIZER] Lỗi khi seed vật liệu mẫu: {}", ex.getMessage());
        }

        // 4. Khởi tạo các dự án bàn khắc mẫu
        if (demoGmail != null) {
            initSampleProjects(demoGmail);
            initSampleHistory(demoGmail);
        }
        if (demoUser != null) {
            initSampleProjects(demoUser);
            initSampleHistory(demoUser);
        }

        log.info("========== [DATA INITIALIZER] Hoàn tất khởi tạo dữ liệu mẫu vào SQL Server ==========");
    }

    private UserEntity initUser(String email, String username, String rawPassword, String fullName, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity user = UserEntity.builder()
                    .email(email)
                    .username(username)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .authProvider("LOCAL")
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();
            UserEntity saved = userRepository.save(user);
            log.info("[DATA INITIALIZER] Đã tạo tài khoản mẫu: {} (Mật khẩu: {})", email, rawPassword);
            return saved;
        });
    }

    private void initSettings(UserEntity user) {
        if (laserSettingsRepository.findByUser_UserId(user.getUserId()).isEmpty()) {
            LaserSettingsEntity settings = LaserSettingsEntity.builder()
                    .user(user)
                    .profileName("Tiêu chuẩn Laser GRBL Diode 40W (300×200mm)")
                    .defaultSpeed(BigDecimal.valueOf(1500.0))
                    .maxPower(1000)
                    .defaultDpi(254)
                    .maxWidthMm(BigDecimal.valueOf(300.0))
                    .maxHeightMm(BigDecimal.valueOf(200.0))
                    .contrast(15)
                    .brightness(0)
                    .invert(false)
                    .mode("raster")
                    .stepSize(BigDecimal.valueOf(10.0))
                    .passes(1)
                    .updatedAt(LocalDateTime.now())
                    .build();
            laserSettingsRepository.save(settings);
            log.info("[DATA INITIALIZER] Đã tạo cấu hình tham số máy mặc định cho user: {}", user.getEmail());
        }
    }

    private void initSampleProjects(UserEntity user) {
        List<ProjectEntity> existing = projectRepository.findByUserOrderByUpdatedAtDesc(user);
        if (existing.isEmpty()) {
            log.info("[DATA INITIALIZER] Bắt đầu khởi tạo các dự án mẫu cho user: {}", user.getEmail());

            // Dự án 1: Biển tên Mica LED để bàn (Vector Cut & Line)
            String p1Elements = "["
                    + "{\"id\":\"rect-base\",\"type\":\"rect\",\"name\":\"\\u0110\\u1EBF mica bo g\\u00F3c 120\\u00D760 mm\",\"x\":20.0,\"y\":20.0,\"width\":120.0,\"height\":60.0,\"cornerRadius\":6,\"aspectRatio\":2.0,\"isAspectLocked\":false,\"mode\":\"vector\"},"
                    + "{\"id\":\"circle-hole\",\"type\":\"circle\",\"name\":\"L\\u1ED7 lu\\u1ED3n d\\u00E2y LED \\u00D8 12 mm\",\"x\":74.0,\"y\":44.0,\"width\":12.0,\"height\":12.0,\"aspectRatio\":1.0,\"isAspectLocked\":true,\"mode\":\"vector\"},"
                    + "{\"id\":\"text-logo\",\"type\":\"text\",\"name\":\"T\\u00EAn th\\u01B0\\u01A1ng hi\\u1EC7u PhotonCraft\",\"text\":\"PHOTONCRAFT GRBL\",\"fontFamily\":\"sans\",\"x\":35.0,\"y\":30.0,\"width\":90.0,\"height\":14.0,\"aspectRatio\":6.42,\"isAspectLocked\":false,\"mode\":\"vector\"}"
                    + "]";
            String p1Params = "{\"feedRate\":2400,\"laserPower\":45,\"dpi\":300,\"mode\":\"vector\"}";

            ProjectEntity project1 = ProjectEntity.builder()
                    .user(user)
                    .projectName("Bi\u1EC3n t\u00EAn mica LED \u0111\u1EC3 b\u00E0n (120\u00D760 mm)")
                    .elementCount(3)
                    .elementsJson(p1Elements)
                    .parametersJson(p1Params)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusDays(2))
                    .build();

            // Dự án 2: Khung tranh chân dung Plywood (Khổ A5 Raster Engrave)
            String p2Elements = "["
                    + "{\"id\":\"elem-img-1\",\"type\":\"image\",\"name\":\"portrait_bach_duong.png\",\"ext\":\"PNG\",\"pixelDims\":\"1200 \\u00D7 900 px\",\"desc\":\"\\u1EA2nh ch\\u00E2n dung ngh\\u1EC7 thu\\u1EADt\",\"previewUrl\":null,\"x\":40.0,\"y\":30.0,\"width\":100.0,\"height\":75.0,\"aspectRatio\":1.33,\"isAspectLocked\":true,\"mode\":\"raster\"},"
                    + "{\"id\":\"rect-frame\",\"type\":\"rect\",\"name\":\"Khung vi\\u1EC1n g\\u1ED7 trang tr\\u00ED\",\"x\":35.0,\"y\":25.0,\"width\":110.0,\"height\":85.0,\"cornerRadius\":4,\"aspectRatio\":1.29,\"isAspectLocked\":false,\"mode\":\"vector\"}"
                    + "]";
            String p2Params = "{\"feedRate\":2000,\"laserPower\":65,\"dpi\":254,\"mode\":\"raster\"}";

            ProjectEntity project2 = ProjectEntity.builder()
                    .user(user)
                    .projectName("Khung tranh ch\u00E2n dung Plywood (Kh\u1ED5 A5)")
                    .elementCount(2)
                    .elementsJson(p2Elements)
                    .parametersJson(p2Params)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .build();

            // Dự án 3: Móc khóa Da khắc thương hiệu (50×50 mm)
            String p3Elements = "["
                    + "{\"id\":\"circle-tag\",\"type\":\"circle\",\"name\":\"Ph\\u00F4i da tr\\u00F2n \\u00D8 50 mm\",\"x\":50.0,\"y\":40.0,\"width\":50.0,\"height\":50.0,\"aspectRatio\":1.0,\"isAspectLocked\":true,\"mode\":\"vector\"},"
                    + "{\"id\":\"circle-keyring\",\"type\":\"circle\",\"name\":\"L\\u1ED7 m\\u00F3c khuy\\u00EAn \\u00D8 5 mm\",\"x\":72.5,\"y\":43.0,\"width\":5.0,\"height\":5.0,\"aspectRatio\":1.0,\"isAspectLocked\":true,\"mode\":\"vector\"},"
                    + "{\"id\":\"text-vintage\",\"type\":\"text\",\"name\":\"Ch\\u1EEF Vintage kh\\u1EAFc s\\u00E2u\",\"text\":\"LASER CNC\",\"fontFamily\":\"serif\",\"x\":60.0,\"y\":65.0,\"width\":30.0,\"height\":10.0,\"aspectRatio\":3.0,\"isAspectLocked\":false,\"mode\":\"raster\"}"
                    + "]";
            String p3Params = "{\"feedRate\":3200,\"laserPower\":30,\"dpi\":254,\"mode\":\"raster\"}";

            ProjectEntity project3 = ProjectEntity.builder()
                    .user(user)
                    .projectName("M\u00F3c kh\u00F3a Da kh\u1EAFc th\u01B0\u01A1ng hi\u1EC7u (50\u00D750 mm)")
                    .elementCount(3)
                    .elementsJson(p3Elements)
                    .parametersJson(p3Params)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            projectRepository.saveAll(List.of(project1, project2, project3));
            log.info("[DATA INITIALIZER] Đã tạo thành công 3 dự án thiết kế mẫu vào bảng Projects.");
        } else {
            // Sửa lỗi font encoding dấu hỏi chấm nếu các bản ghi trước đó đã lưu sai charset
            for (ProjectEntity p : existing) {
                boolean changed = false;
                if (p.getProjectName().contains("?")) {
                    if (p.getProjectName().toLowerCase().contains("móc") || p.getProjectName().toLowerCase().contains("m?c")) {
                        p.setProjectName("M\u00F3c kh\u00F3a Da kh\u1EAFc th\u01B0\u01A1ng hi\u1EC7u (50\u00D750 mm)");
                        changed = true;
                    } else if (p.getProjectName().toLowerCase().contains("khung") || p.getProjectName().toLowerCase().contains("plywood")) {
                        p.setProjectName("Khung tranh ch\u00E2n dung Plywood (Kh\u1ED5 A5)");
                        changed = true;
                    } else if (p.getProjectName().toLowerCase().contains("bi") || p.getProjectName().toLowerCase().contains("mica")) {
                        p.setProjectName("Bi\u1EC3n t\u00EAn mica LED \u0111\u1EC3 b\u00E0n (120\u00D760 mm)");
                        changed = true;
                    }
                }
                if (changed) {
                    projectRepository.save(p);
                    log.info("[DATA INITIALIZER] Đã khắc phục lỗi font tiếng Việt cho dự án ID {}: {}", p.getProjectId(), p.getProjectName());
                }
            }
        }
    }

    private void initSampleHistory(UserEntity user) {
        if (gcodeHistoryRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId(), null).isEmpty()) {
            log.info("[DATA INITIALIZER] Bắt đầu khởi tạo dữ liệu lịch sử gia công mẫu...");

            GcodeHistoryEntity h1 = GcodeHistoryEntity.builder()
                    .user(user)
                    .originalFilename("portrait_bach_duong.png")
                    .storedFilePath("storage/gcode/init_portrait_sample.nc")
                    .fileSizeBytes(184200L)
                    .totalGcodeLines(14820)
                    .engravingSpeed(2000)
                    .laserPower(650)
                    .dpi(254)
                    .originXMm(BigDecimal.valueOf(40.0))
                    .originYMm(BigDecimal.valueOf(30.0))
                    .outputWidthMm(BigDecimal.valueOf(100.0))
                    .outputHeightMm(BigDecimal.valueOf(75.0))
                    .estimatedTimeSeconds(420)
                    .mode("raster")
                    .materialName("Gỗ ép Plywood (3mm)")
                    .status("Hoàn tất")
                    .gcodeContent("; Sample G-code Portrait 100x75mm\nG21\nG90\n$32=1\nG0 X0 Y0 F3000\nM5\n; Processing...")
                    .createdAt(LocalDateTime.now().minusHours(3))
                    .build();

            GcodeHistoryEntity h2 = GcodeHistoryEntity.builder()
                    .user(user)
                    .originalFilename("bien_ten_led.dxf")
                    .storedFilePath("storage/gcode/init_led_sign.nc")
                    .fileSizeBytes(42500L)
                    .totalGcodeLines(1850)
                    .engravingSpeed(2400)
                    .laserPower(450)
                    .dpi(300)
                    .originXMm(BigDecimal.valueOf(20.0))
                    .originYMm(BigDecimal.valueOf(20.0))
                    .outputWidthMm(BigDecimal.valueOf(120.0))
                    .outputHeightMm(BigDecimal.valueOf(60.0))
                    .estimatedTimeSeconds(160)
                    .mode("vector")
                    .materialName("Mica Acrylic trong suốt (2.5mm)")
                    .status("Hoàn tất")
                    .gcodeContent("; Sample G-code Acrylic Sign\nG21\nG90\n$32=1\nG0 X0 Y0 F3000\nM5\n; Cutting Vector...")
                    .createdAt(LocalDateTime.now().minusHours(1))
                    .build();

            GcodeHistoryEntity h3 = GcodeHistoryEntity.builder()
                    .user(user)
                    .originalFilename("moc_khoa_da.png")
                    .storedFilePath("storage/gcode/init_leather_tag.nc")
                    .fileSizeBytes(61200L)
                    .totalGcodeLines(3200)
                    .engravingSpeed(3200)
                    .laserPower(300)
                    .dpi(254)
                    .originXMm(BigDecimal.valueOf(50.0))
                    .originYMm(BigDecimal.valueOf(40.0))
                    .outputWidthMm(BigDecimal.valueOf(50.0))
                    .outputHeightMm(BigDecimal.valueOf(50.0))
                    .estimatedTimeSeconds(95)
                    .mode("raster")
                    .materialName("Da thuộc tự nhiên & Simili")
                    .status("Hoàn tất")
                    .gcodeContent("; Sample G-code Leather Tag\nG21\nG90\n$32=1\nG0 X0 Y0 F3000\nM5\n; Engraving...")
                    .createdAt(LocalDateTime.now().minusMinutes(20))
                    .build();

            gcodeHistoryRepository.saveAll(List.of(h1, h2, h3));
            log.info("[DATA INITIALIZER] Đã tạo thành công 3 bản ghi lịch sử gia công mẫu vào bảng Gcode_History.");
        }
    }
}
