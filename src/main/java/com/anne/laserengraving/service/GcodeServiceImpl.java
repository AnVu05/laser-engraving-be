package com.anne.laserengraving.service;

import com.anne.laserengraving.core.gcode.GcodeGeneratorEngine;
import com.anne.laserengraving.core.gcode.GcodeModel;
import com.anne.laserengraving.core.image.ImageProcessingEngine;
import com.anne.laserengraving.dto.request.GcodeGenerateRequest;
import com.anne.laserengraving.dto.response.GcodeGenerateResponse;
import com.anne.laserengraving.entity.GcodeHistoryEntity;
import com.anne.laserengraving.entity.LaserSettingsEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.InvalidParameterException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.GcodeHistoryRepository;
import com.anne.laserengraving.repository.LaserSettingsRepository;
import com.anne.laserengraving.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcodeServiceImpl implements GcodeService {

    private final ImageProcessingEngine imageProcessingEngine;
    private final GcodeGeneratorEngine gcodeGeneratorEngine;
    private final FileStorageService fileStorageService;
    private final GcodeHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final LaserSettingsRepository settingsRepository;

    @Override
    @Transactional
    public GcodeGenerateResponse generateGcode(GcodeGenerateRequest request, Integer userId) {
        MultipartFile file = request.getImage();
        if (file == null || file.isEmpty()) {
            throw new InvalidParameterException("Tệp ảnh tải lên không được để trống.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "uploaded_image.png";
        }

        // Đọc ảnh vào BufferedImage
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new InvalidParameterException("Định dạng tệp không được hỗ trợ hoặc file ảnh bị hỏng.");
            }
        } catch (IOException e) {
            throw new InvalidParameterException("Không thể đọc tệp ảnh: " + e.getMessage());
        }

        // 1. Tính toán số pixel mục tiêu dựa trên DPI và kích thước mm
        int dpi = (request.getDpi() != null) ? request.getDpi() : 254;
        double originXMm = (request.getOriginXMm() != null) ? request.getOriginXMm() : 0.0;
        double originYMm = (request.getOriginYMm() != null) ? request.getOriginYMm() : 0.0;
        double targetWidthMm = request.getTargetWidthMm();
        double targetHeightMm = request.getTargetHeightMm();

        int targetWidthPx = (int) Math.round((targetWidthMm / 25.4) * dpi);
        int targetHeightPx = (int) Math.round((targetHeightMm / 25.4) * dpi);

        log.info("Bắt đầu xử lý ảnh '{}': Gốc (X={}, Y={}), Kích thước gốc {}x{} px -> Mục tiêu {}x{} px (DPI {})",
                originalFilename, originXMm, originYMm, originalImage.getWidth(), originalImage.getHeight(), targetWidthPx, targetHeightPx, dpi);

        // 2. Resize ảnh
        BufferedImage resizedImage = imageProcessingEngine.resizeImage(originalImage, targetWidthPx, targetHeightPx);

        // 3. Tiền xử lý & Dithering
        int[][] binaryMatrix = imageProcessingEngine.binarize(resizedImage, request.getDitherAlgorithm());

        // 4. Sinh G-code chuẩn GRBL
        GcodeModel gcodeModel = gcodeGeneratorEngine.generateRasterGcode(
                binaryMatrix,
                targetWidthPx,
                targetHeightPx,
                originXMm,
                originYMm,
                targetWidthMm,
                targetHeightMm,
                request.getEngravingSpeed(),
                request.getLaserPower(),
                dpi
        );

        // 5. Lưu tệp G-code cục bộ
        String storedFilePath = fileStorageService.storeGcodeFile(originalFilename, gcodeModel.getFullGcodeContent());

        // 6. Lưu lịch sử vào CSDL
        UserEntity user = resolveUser(userId);
        LaserSettingsEntity setting = settingsRepository.findFirstByUser_UserId(user.getUserId()).orElse(null);

        GcodeHistoryEntity historyEntity = GcodeHistoryEntity.builder()
                .user(user)
                .setting(setting)
                .originalFilename(originalFilename)
                .storedFilePath(storedFilePath)
                .fileSizeBytes(gcodeModel.getFileSizeBytes())
                .totalGcodeLines(gcodeModel.getTotalLines())
                .engravingSpeed(request.getEngravingSpeed())
                .laserPower(request.getLaserPower())
                .dpi(dpi)
                .originXMm(BigDecimal.valueOf(originXMm))
                .originYMm(BigDecimal.valueOf(originYMm))
                .outputWidthMm(BigDecimal.valueOf(gcodeModel.getActualWidthMm()))
                .outputHeightMm(BigDecimal.valueOf(gcodeModel.getActualHeightMm()))
                .estimatedTimeSeconds(gcodeModel.getEstimatedTimeSeconds())
                .mode(request.getMode() != null ? request.getMode() : "raster")
                .burnCount(request.getBurnCount())
                .burnPercent(request.getBurnPercent() != null ? BigDecimal.valueOf(request.getBurnPercent()) : null)
                .materialName(request.getMaterialName())
                .gcodeContent(gcodeModel.getFullGcodeContent())
                .status("Hoàn tất")
                .build();

        GcodeHistoryEntity saved = historyRepository.save(historyEntity);

        log.info("Hoàn tất sinh G-code thành công: History ID {}, Tổng {} dòng lệnh, Kích thước {} bytes",
                saved.getHistoryId(), gcodeModel.getTotalLines(), gcodeModel.getFileSizeBytes());

        return GcodeGenerateResponse.builder()
                .historyId(saved.getHistoryId())
                .filename(originalFilename + ".nc")
                .fileSizeBytes(gcodeModel.getFileSizeBytes())
                .totalLines(gcodeModel.getTotalLines())
                .outputWidthMm(gcodeModel.getActualWidthMm())
                .outputHeightMm(gcodeModel.getActualHeightMm())
                .estimatedTimeSeconds(gcodeModel.getEstimatedTimeSeconds())
                .downloadUrl("/api/v1/gcode/download/" + saved.getHistoryId())
                .gcodePreviewSnippet(gcodeModel.getPreviewSnippet())
                .fullGcodeContent(gcodeModel.getFullGcodeContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadGcodeFile(Long historyId) {
        GcodeHistoryEntity history = historyRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu G-code có ID: " + historyId));

        return fileStorageService.loadGcodeFileAsResource(history.getStoredFilePath());
    }

    private UserEntity resolveUser(Integer userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElseGet(this::getOrCreateDefaultOperator);
        }
        return getOrCreateDefaultOperator();
    }

    private UserEntity getOrCreateDefaultOperator() {
        return userRepository.findByUsername("operator")
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username("operator")
                        .passwordHash("noop")
                        .email("operator@laser.local")
                        .role("OPERATOR")
                        .build()));
    }
}
