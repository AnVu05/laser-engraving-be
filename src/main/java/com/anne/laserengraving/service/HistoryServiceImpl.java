package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.SaveHistoryRequest;
import com.anne.laserengraving.dto.response.HistoryItemResponse;
import com.anne.laserengraving.entity.GcodeHistoryEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.GcodeHistoryRepository;
import com.anne.laserengraving.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final GcodeHistoryRepository historyRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HistoryItemResponse> getHistory(Integer userId, String email, Pageable pageable) {
        Page<GcodeHistoryEntity> page;
        if (email != null && !email.trim().isEmpty()) {
            page = historyRepository.findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(email.trim().toLowerCase(), pageable);
        } else if (userId != null) {
            page = historyRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            page = historyRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryItemResponse getHistoryDetail(Long historyId, Integer userId, String email) {
        GcodeHistoryEntity entity;
        if (email != null && !email.trim().isEmpty()) {
            entity = historyRepository.findByHistoryIdAndUser_EmailIgnoreCase(historyId, email.trim().toLowerCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        } else if (userId != null) {
            entity = historyRepository.findByHistoryIdAndUser_UserId(historyId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        } else {
            entity = historyRepository.findById(historyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        }
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public HistoryItemResponse saveHistory(String email, Integer userId, SaveHistoryRequest request) {
        UserEntity user = null;
        if (email != null && !email.trim().isEmpty()) {
            user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        }
        if (user == null && userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            String targetEmail = (email != null && !email.trim().isEmpty()) ? email.trim().toLowerCase() : "operator@laser.local";
            user = userRepository.findByEmail(targetEmail)
                    .orElseGet(() -> userRepository.findAll().stream().findFirst()
                            .orElseGet(() -> userRepository.save(UserEntity.builder()
                                    .username("operator_" + System.currentTimeMillis())
                                    .passwordHash("noop")
                                    .email(targetEmail)
                                    .role("OPERATOR")
                                    .build())));
        }

        String rawFilename = request.getOriginalFilename();
        if (rawFilename == null || rawFilename.trim().isEmpty()) {
            rawFilename = "laser_job_" + System.currentTimeMillis() + ".nc";
        } else if (!rawFilename.toLowerCase().endsWith(".nc") && !rawFilename.toLowerCase().endsWith(".gcode")) {
            rawFilename = rawFilename.replaceFirst("\\.[^.]+$", "") + ".nc";
        }

        String gcode = request.getGcodeContent() != null ? request.getGcodeContent() : "";
        byte[] gcodeBytes = gcode.getBytes(StandardCharsets.UTF_8);
        long fileSize = gcodeBytes.length;

        String storedPath = fileStorageService.storeGcodeFile(rawFilename, gcode);

        int lines = (request.getTotalGcodeLines() != null && request.getTotalGcodeLines() > 0)
                ? request.getTotalGcodeLines()
                : (int) gcode.lines().count();
        if (lines <= 0) lines = 1;

        int speed = request.getEngravingSpeed() != null ? request.getEngravingSpeed() : 1500;
        int power = request.getLaserPower() != null ? request.getLaserPower() : 750;
        int dpi = request.getDpi() != null ? request.getDpi() : 254;

        BigDecimal widthMm = request.getOutputWidthMm() != null ? request.getOutputWidthMm() : BigDecimal.valueOf(80.0);
        BigDecimal heightMm = request.getOutputHeightMm() != null ? request.getOutputHeightMm() : BigDecimal.valueOf(70.8);
        BigDecimal originX = request.getOriginXMm() != null ? request.getOriginXMm() : BigDecimal.ZERO;
        BigDecimal originY = request.getOriginYMm() != null ? request.getOriginYMm() : BigDecimal.ZERO;

        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus()
                : "Hoàn tất";

        GcodeHistoryEntity entity = GcodeHistoryEntity.builder()
                .user(user)
                .originalFilename(rawFilename)
                .storedFilePath(storedPath)
                .fileSizeBytes(fileSize)
                .totalGcodeLines(lines)
                .engravingSpeed(speed)
                .laserPower(power)
                .dpi(dpi)
                .originXMm(originX)
                .originYMm(originY)
                .outputWidthMm(widthMm)
                .outputHeightMm(heightMm)
                .estimatedTimeSeconds(request.getEstimatedTimeSeconds())
                .mode(request.getMode() != null ? request.getMode() : "raster")
                .burnCount(request.getBurnCount())
                .burnPercent(request.getBurnPercent())
                .materialName(request.getMaterialName())
                .gcodeContent(gcode)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        GcodeHistoryEntity saved = historyRepository.save(entity);
        log.info("[HISTORY] Đã lưu lịch sử gia công '{}' (ID: {}) cho người dùng {}", saved.getOriginalFilename(), saved.getHistoryId(), user.getEmail());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteHistory(Long historyId, Integer userId, String email) {
        GcodeHistoryEntity entity;
        if (email != null && !email.trim().isEmpty()) {
            entity = historyRepository.findByHistoryIdAndUser_EmailIgnoreCase(historyId, email.trim().toLowerCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        } else if (userId != null) {
            entity = historyRepository.findByHistoryIdAndUser_UserId(historyId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        } else {
            entity = historyRepository.findById(historyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử có ID: " + historyId));
        }

        // Xóa tệp G-code vật lý
        try {
            fileStorageService.deleteGcodeFile(entity.getStoredFilePath());
        } catch (Exception e) {
            log.warn("Không thể xóa file G-code vật lý {}: {}", entity.getStoredFilePath(), e.getMessage());
        }

        // Xóa bản ghi trong CSDL
        historyRepository.delete(entity);
        log.info("[HISTORY] Đã xóa lịch sử ID {}", historyId);
    }

    private HistoryItemResponse mapToResponse(GcodeHistoryEntity entity) {
        return HistoryItemResponse.builder()
                .historyId(entity.getHistoryId())
                .originalFilename(entity.getOriginalFilename())
                .fileSizeBytes(entity.getFileSizeBytes())
                .totalGcodeLines(entity.getTotalGcodeLines())
                .engravingSpeed(entity.getEngravingSpeed())
                .laserPower(entity.getLaserPower())
                .dpi(entity.getDpi())
                .originXMm(entity.getOriginXMm())
                .originYMm(entity.getOriginYMm())
                .outputWidthMm(entity.getOutputWidthMm())
                .outputHeightMm(entity.getOutputHeightMm())
                .estimatedTimeSeconds(entity.getEstimatedTimeSeconds())
                .mode(entity.getMode())
                .burnCount(entity.getBurnCount())
                .burnPercent(entity.getBurnPercent())
                .materialName(entity.getMaterialName())
                .gcodeContent(entity.getGcodeContent())
                .status(entity.getStatus() != null ? entity.getStatus() : "Hoàn tất")
                .downloadUrl("/api/v1/gcode/download/" + entity.getHistoryId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
