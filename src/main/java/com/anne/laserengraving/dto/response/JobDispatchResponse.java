package com.anne.laserengraving.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Kết quả điều phối Job khắc laser tới thiết bị IoT")
public class JobDispatchResponse {

    @Schema(description = "ID bản ghi Job trong CSDL", example = "1")
    private Long jobId;

    @Schema(description = "Mã Job duy nhất (Job Code)", example = "JOB-20261020-A1B2C3D4")
    private String jobCode;

    @Schema(description = "Mã thiết bị đích tiếp nhận Job", example = "LASER-HN-001")
    private String deviceCode;

    @Schema(description = "Tên thiết bị đích", example = "Máy Khắc Laser Tiêu Chuẩn Makerbase MKS DLC32 #01")
    private String deviceName;

    @Schema(description = "Tên tệp gốc", example = "logo.gcode")
    private String originalFilename;

    @Schema(description = "Dung lượng tệp tính theo bytes", example = "14580")
    private Long fileSizeBytes;

    @Schema(description = "Mã băm SHA-256 (64 ký tự hex) bảo mật toàn vẹn", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String sha256Checksum;

    @Schema(description = "Chiều rộng vùng khắc (mm)", example = "100.0")
    private Double targetWidthMm;

    @Schema(description = "Chiều cao vùng khắc (mm)", example = "100.0")
    private Double targetHeightMm;

    @Schema(description = "Tốc độ chạy dao Feed rate (mm/min)", example = "1500")
    private Integer feedRate;

    @Schema(description = "Công suất phát tia Laser PWM", example = "800")
    private Integer laserPower;

    @Schema(description = "Tổng số dòng lệnh G-code hợp lệ cần thực thi", example = "3500")
    private Integer totalLines;

    @Schema(description = "Trạng thái Job hiện tại", example = "DISPATCHED")
    private String status;

    @Schema(description = "Tiến độ gia công hiện tại (%)", example = "0.0")
    private Double currentProgress;

    @Schema(description = "Loại vật liệu phôi đã chọn", example = "Gỗ Plywood")
    private String materialType;

    @Schema(description = "Đường dẫn URL tải tệp G-code bảo mật qua HTTPS", example = "/api/v1/jobs/1/download")
    private String downloadUrl;

    @Schema(description = "Thời điểm điều phối Job")
    private LocalDateTime dispatchedAt;

    @Schema(description = "Vị trí trong hàng đợi gia công (nếu đang chờ)", example = "1")
    private Integer queuePosition;

    @Schema(description = "Thông báo trạng thái điều phối", example = "Job đã vào hàng đợi gia công")
    private String message;
}
