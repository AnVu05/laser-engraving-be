package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.MaterialPresetRequest;
import com.anne.laserengraving.dto.response.MaterialPresetResponse;
import com.anne.laserengraving.entity.MaterialPresetEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.InvalidParameterException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.MaterialPresetRepository;
import com.anne.laserengraving.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialPresetServiceImpl implements MaterialPresetService {

    private final MaterialPresetRepository materialPresetRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void initDefaultPresets() {
        if (materialPresetRepository.countByIsSystemTrue() > 0) {
            log.debug("Vật liệu mẫu hệ thống đã tồn tại trong CSDL.");
            return;
        }

        log.info("Bắt đầu khởi tạo 6 vật liệu mẫu hệ thống chuẩn...");

        List<MaterialPresetEntity> defaultPresets = List.of(
                MaterialPresetEntity.builder()
                        .materialKey("mat-plywood")
                        .name("Gỗ ép Plywood (3mm)")
                        .icon("forest")
                        .badge("Phổ biến nhất")
                        .description("Ván ép bạch dương / bồ đề chuyên dụng cho cắt mô hình và khắc tranh.")
                        .cutFeed(350)
                        .cutPower(100)
                        .cutPasses(2)
                        .engraveFeed(2000)
                        .engravePower(65)
                        .dpi(254)
                        .mode("vector")
                        .isSystem(true)
                        .build(),

                MaterialPresetEntity.builder()
                        .materialKey("mat-acrylic")
                        .name("Mica Acrylic trong suốt (2.5mm)")
                        .icon("view_in_ar")
                        .badge("Cắt sắc cạnh")
                        .description("Mica đúc cho mép cắt bóng gương, khắc bề mặt mờ trắng đục.")
                        .cutFeed(250)
                        .cutPower(95)
                        .cutPasses(1)
                        .engraveFeed(2400)
                        .engravePower(45)
                        .dpi(300)
                        .mode("vector")
                        .isSystem(true)
                        .build(),

                MaterialPresetEntity.builder()
                        .materialKey("mat-leather")
                        .name("Da thuộc tự nhiên & Simili")
                        .icon("inventory_2")
                        .badge("Thời trang")
                        .description("Ví da, thắt lưng, móc khóa da. Khắc sắc nét không bị cháy lem mép.")
                        .cutFeed(600)
                        .cutPower(55)
                        .cutPasses(1)
                        .engraveFeed(3200)
                        .engravePower(30)
                        .dpi(254)
                        .mode("raster")
                        .isSystem(true)
                        .build(),

                MaterialPresetEntity.builder()
                        .materialKey("mat-mdf")
                        .name("Gỗ công nghiệp MDF (3mm)")
                        .icon("dashboard")
                        .badge("Độ mịn cao")
                        .description("Gỗ ép sợi đồng nhất, bề mặt phẳng tuyệt đối, khắc logo độ tương phản cao.")
                        .cutFeed(220)
                        .cutPower(100)
                        .cutPasses(2)
                        .engraveFeed(1800)
                        .engravePower(70)
                        .dpi(254)
                        .mode("vector")
                        .isSystem(true)
                        .build(),

                MaterialPresetEntity.builder()
                        .materialKey("mat-cardboard")
                        .name("Bìa Carton cứng & Giấy Kraft")
                        .icon("description")
                        .badge("Cắt cực nhanh")
                        .description("Bao bì, hộp quà, thiệp cưới laser. Tốc độ cắt cao chống cháy mép.")
                        .cutFeed(1200)
                        .cutPower(60)
                        .cutPasses(1)
                        .engraveFeed(4000)
                        .engravePower(25)
                        .dpi(150)
                        .mode("vector")
                        .isSystem(true)
                        .build(),

                MaterialPresetEntity.builder()
                        .materialKey("mat-anodized")
                        .name("Nhôm phủ sơn / Anodized")
                        .icon("shield")
                        .badge("Khắc kim loại")
                        .description("Khắc bóc lớp oxit màu tạo nét khắc trắng sáng vĩnh viễn trên kim loại.")
                        .cutFeed(0)
                        .cutPower(0)
                        .cutPasses(0)
                        .engraveFeed(1600)
                        .engravePower(85)
                        .dpi(300)
                        .mode("raster")
                        .isSystem(true)
                        .build()
        );

        materialPresetRepository.saveAll(defaultPresets);
        log.info("Khởi tạo thành công {} vật liệu mẫu hệ thống vào CSDL.", defaultPresets.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialPresetResponse> getAllMaterials(String email) {
        List<MaterialPresetEntity> entities;
        if (email != null && !email.trim().isEmpty()) {
            entities = materialPresetRepository.findAllAvailableForUser(email.trim());
        } else {
            entities = materialPresetRepository.findByIsSystemTrueOrderByPresetIdAsc();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialPresetResponse getMaterialById(Long presetId) {
        MaterialPresetEntity entity = materialPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật liệu mẫu với ID: " + presetId));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialPresetResponse getMaterialByKey(String materialKey) {
        MaterialPresetEntity entity = materialPresetRepository.findByMaterialKey(materialKey)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật liệu mẫu với mã: " + materialKey));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public MaterialPresetResponse createMaterial(MaterialPresetRequest request, String email) {
        UserEntity user = null;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findByEmail(email.trim()).orElse(null);
        }

        String key = request.getMaterialKey();
        if (key == null || key.trim().isEmpty()) {
            key = "mat-custom-" + UUID.randomUUID().toString().substring(0, 8);
        } else {
            key = key.trim().toLowerCase();
            if (materialPresetRepository.existsByMaterialKey(key)) {
                key = key + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
        }

        MaterialPresetEntity entity = MaterialPresetEntity.builder()
                .materialKey(key)
                .name(request.getName().trim())
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon().trim() : "layers")
                .badge(request.getBadge() != null ? request.getBadge().trim() : "Tùy chỉnh")
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .cutFeed(request.getCutFeed() != null ? request.getCutFeed() : 300)
                .cutPower(request.getCutPower() != null ? request.getCutPower() : 80)
                .cutPasses(request.getCutPasses() != null ? request.getCutPasses() : 1)
                .engraveFeed(request.getEngraveFeed() != null ? request.getEngraveFeed() : 2000)
                .engravePower(request.getEngravePower() != null ? request.getEngravePower() : 50)
                .dpi(request.getDpi() != null ? request.getDpi() : 254)
                .mode(request.getMode() != null && !request.getMode().isBlank() ? request.getMode().trim().toLowerCase() : "vector")
                .isSystem(false)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        MaterialPresetEntity saved = materialPresetRepository.save(entity);
        log.info("Đã tạo mới vật liệu tùy chỉnh: {} (key: {})", saved.getName(), saved.getMaterialKey());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public MaterialPresetResponse updateMaterial(Long presetId, MaterialPresetRequest request, String email) {
        MaterialPresetEntity entity = materialPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật liệu mẫu với ID: " + presetId));

        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new InvalidParameterException("Không thể chỉnh sửa vật liệu mặc định của hệ thống!");
        }

        if (email != null && !email.trim().isEmpty() && entity.getUser() != null) {
            if (!email.trim().equalsIgnoreCase(entity.getUser().getEmail())) {
                throw new InvalidParameterException("Bạn không có quyền chỉnh sửa vật liệu của người dùng khác!");
            }
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName().trim());
        }
        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            entity.setIcon(request.getIcon().trim());
        }
        if (request.getBadge() != null) {
            entity.setBadge(request.getBadge().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription().trim());
        }
        if (request.getCutFeed() != null) {
            entity.setCutFeed(request.getCutFeed());
        }
        if (request.getCutPower() != null) {
            entity.setCutPower(request.getCutPower());
        }
        if (request.getCutPasses() != null) {
            entity.setCutPasses(request.getCutPasses());
        }
        if (request.getEngraveFeed() != null) {
            entity.setEngraveFeed(request.getEngraveFeed());
        }
        if (request.getEngravePower() != null) {
            entity.setEngravePower(request.getEngravePower());
        }
        if (request.getDpi() != null) {
            entity.setDpi(request.getDpi());
        }
        if (request.getMode() != null && !request.getMode().isBlank()) {
            entity.setMode(request.getMode().trim().toLowerCase());
        }

        MaterialPresetEntity updated = materialPresetRepository.save(entity);
        log.info("Đã cập nhật vật liệu: {} (ID: {})", updated.getName(), updated.getPresetId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long presetId, String email) {
        MaterialPresetEntity entity = materialPresetRepository.findById(presetId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật liệu mẫu với ID: " + presetId));

        if (Boolean.TRUE.equals(entity.getIsSystem())) {
            throw new InvalidParameterException("Không thể xóa vật liệu mặc định của hệ thống!");
        }

        if (email != null && !email.trim().isEmpty() && entity.getUser() != null) {
            if (!email.trim().equalsIgnoreCase(entity.getUser().getEmail())) {
                throw new InvalidParameterException("Bạn không có quyền xóa vật liệu của người dùng khác!");
            }
        }

        materialPresetRepository.delete(entity);
        log.info("Đã xóa vật liệu tùy chỉnh ID: {}", presetId);
    }

    private MaterialPresetResponse toResponse(MaterialPresetEntity entity) {
        String frontendId = entity.getMaterialKey() != null && !entity.getMaterialKey().isBlank()
                ? entity.getMaterialKey()
                : String.valueOf(entity.getPresetId());

        return MaterialPresetResponse.builder()
                .id(frontendId)
                .presetId(entity.getPresetId())
                .materialKey(entity.getMaterialKey())
                .name(entity.getName())
                .icon(entity.getIcon())
                .badge(entity.getBadge())
                .desc(entity.getDescription())
                .description(entity.getDescription())
                .cutFeed(entity.getCutFeed())
                .cutPower(entity.getCutPower())
                .cutPasses(entity.getCutPasses())
                .engraveFeed(entity.getEngraveFeed())
                .engravePower(entity.getEngravePower())
                .dpi(entity.getDpi())
                .mode(entity.getMode())
                .isSystem(entity.getIsSystem())
                .createdByEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
