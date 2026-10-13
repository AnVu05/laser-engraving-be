package com.anne.laserengraving.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu điều phối Job khắc laser tới thiết bị IoT")
public class JobDispatchRequest {

    @NotBlank(message = "Mã thiết bị đích (deviceCode) không được để trống")
    @Schema(description = "Mã định danh thiết bị máy khắc đích (VD: LASER-HN-001)", example = "LASER-HN-001")
    private String deviceCode;

    @Schema(description = "Tên tệp G-code gốc", example = "logo.gcode")
    @Builder.Default
    private String originalFilename = "logo.gcode";

    @Schema(description = "Nội dung chuỗi mã lệnh G-code (sử dụng khi không tải file multipart)")
    private String gcodeContent;

    @Schema(description = "Chiều rộng vùng khắc (mm)", example = "100.0")
    @Builder.Default
    private Double targetWidthMm = 100.0;

    @Schema(description = "Chiều cao vùng khắc (mm)", example = "100.0")
    @Builder.Default
    private Double targetHeightMm = 100.0;

    @Schema(description = "Tốc độ chạy dao Feed rate (mm/min)", example = "1500")
    @Builder.Default
    private Integer feedRate = 1500;

    @Schema(description = "Công suất phát tia Laser PWM (0 - 1000)", example = "800")
    @Builder.Default
    private Integer laserPower = 800;

    @Schema(description = "Loại phôi vật liệu khắc (VD: Gỗ Plywood 3mm, Mica đen)", example = "Gỗ Plywood")
    @Builder.Default
    private String materialType = "Gỗ Plywood";

    @Schema(description = "Mã ID người dùng gửi lệnh (nếu có)", example = "1")
    private Integer userId;
}
