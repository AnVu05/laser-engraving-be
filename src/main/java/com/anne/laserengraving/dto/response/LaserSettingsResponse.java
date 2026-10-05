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
public class LaserSettingsResponse {
    private Integer settingId;
    private String profileName;
    private BigDecimal defaultSpeed;
    private Integer maxPower;
    private Integer defaultDpi;
    private BigDecimal maxWidthMm;
    private BigDecimal maxHeightMm;
    private Integer contrast;
    private Integer brightness;
    private Boolean invert;
    private String mode;
    private BigDecimal stepSize;
    private Integer passes;
    private LocalDateTime updatedAt;
}
