package com.anne.laserengraving.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaserSettingsUpdateRequest {

    @NotBlank(message = "Tên cấu hình không được để trống")
    private String profileName;

    @NotNull(message = "Tốc độ mặc định không được để trống")
    @Min(value = 100, message = "Tốc độ tối thiểu là 100 mm/min")
    @Max(value = 5000, message = "Tốc độ tối đa là 5000 mm/min")
    private BigDecimal defaultSpeed;

    @NotNull(message = "Công suất tối đa không được để trống")
    @Min(value = 0, message = "Công suất tối thiểu là 0")
    @Max(value = 1000, message = "Công suất tối đa là 1000")
    private Integer maxPower;

    @NotNull(message = "DPI mặc định không được để trống")
    @Min(value = 100, message = "DPI tối thiểu là 100")
    @Max(value = 600, message = "DPI tối đa là 600")
    private Integer defaultDpi;

    private BigDecimal maxWidthMm;
    private BigDecimal maxHeightMm;

    private Integer contrast;
    private Integer brightness;
    private Boolean invert;
    private String mode;
    private BigDecimal stepSize;
    private Integer passes;
}
