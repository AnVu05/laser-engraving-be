package com.anne.laserengraving.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItemResponse {
    private Long historyId;
    private String originalFilename;
    private Long fileSizeBytes;
    private Integer totalGcodeLines;
    private Integer engravingSpeed;
    private Integer laserPower;
    private Integer dpi;
    private BigDecimal originXMm;
    private BigDecimal originYMm;
    private BigDecimal outputWidthMm;
    private BigDecimal outputHeightMm;
    private Integer estimatedTimeSeconds;
    private String mode;
    private Long burnCount;
    private BigDecimal burnPercent;
    private String materialName;
    private String gcodeContent;
    private String status;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
