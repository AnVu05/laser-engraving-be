package com.anne.laserengraving.dto.request;

import com.anne.laserengraving.core.image.DitherAlgorithm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GcodeGenerateRequest {

    @NotNull(message = "Tệp ảnh không được để trống")
    private MultipartFile image;

    @Min(value = 0, message = "Tọa độ gốc X tối thiểu là 0 mm")
    @Max(value = 300, message = "Tọa độ gốc X tối đa là 300 mm")
    @Builder.Default
    private Double originXMm = 0.0;

    @Min(value = 0, message = "Tọa độ gốc Y tối thiểu là 0 mm")
    @Max(value = 200, message = "Tọa độ gốc Y tối đa là 200 mm")
    @Builder.Default
    private Double originYMm = 0.0;

    @NotNull(message = "Chiều rộng mục tiêu không được để trống")
    @Min(value = 10, message = "Chiều rộng tối thiểu là 10 mm")
    @Max(value = 300, message = "Chiều rộng tối đa là 300 mm")
    private Double targetWidthMm;

    @NotNull(message = "Chiều cao mục tiêu không được để trống")
    @Min(value = 10, message = "Chiều cao tối thiểu là 10 mm")
    @Max(value = 200, message = "Chiều cao tối đa là 200 mm")
    private Double targetHeightMm;

    @NotNull(message = "Tốc độ khắc không được để trống")
    @Min(value = 100, message = "Tốc độ tối thiểu là 100 mm/min")
    @Max(value = 5000, message = "Tốc độ tối đa là 5000 mm/min")
    private Integer engravingSpeed;

    @NotNull(message = "Công suất Laser không được để trống")
    @Min(value = 0, message = "Công suất tối thiểu là 0")
    @Max(value = 1000, message = "Công suất tối đa là 1000")
    private Integer laserPower;

    @Min(value = 100, message = "DPI tối thiểu là 100")
    @Max(value = 600, message = "DPI tối đa là 600")
    @Builder.Default
    private Integer dpi = 254;

    @Builder.Default
    private DitherAlgorithm ditherAlgorithm = DitherAlgorithm.FLOYD_STEINBERG;

    @Builder.Default
    private String mode = "raster";

    private Long burnCount;

    private Double burnPercent;

    private String materialName;
}
