package com.anne.laserengraving.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin vật liệu mẫu phôi gia công laser")
public class MaterialPresetResponse {

    @Schema(description = "Mã định danh vật liệu (dùng cho Frontend)", example = "mat-plywood")
    private String id;

    @Schema(description = "ID bản ghi CSDL", example = "1")
    private Long presetId;

    @Schema(description = "Mã khóa vật liệu duy nhất", example = "mat-plywood")
    private String materialKey;

    @Schema(description = "Tên vật liệu", example = "Gỗ ép Plywood (3mm)")
    private String name;

    @Schema(description = "Icon Google Material Symbols", example = "forest")
    private String icon;

    @Schema(description = "Huy hiệu / Điểm nổi bật", example = "Phổ biến nhất")
    private String badge;

    @Schema(description = "Mô tả ngắn (alias cho Frontend)", example = "Ván ép bạch dương chuyên dụng cho cắt mô hình và khắc tranh.")
    private String desc;

    @Schema(description = "Mô tả chi tiết", example = "Ván ép bạch dương chuyên dụng cho cắt mô hình và khắc tranh.")
    private String description;

    @Schema(description = "Tốc độ cắt Vector (mm/phút)", example = "350")
    private Integer cutFeed;

    @Schema(description = "Công suất cắt Vector (%)", example = "100")
    private Integer cutPower;

    @Schema(description = "Số lượt cắt qua (Passes)", example = "2")
    private Integer cutPasses;

    @Schema(description = "Tốc độ khắc Raster (mm/phút)", example = "2000")
    private Integer engraveFeed;

    @Schema(description = "Công suất khắc Raster (%)", example = "65")
    private Integer engravePower;

    @Schema(description = "Độ phân giải khắc (DPI)", example = "254")
    private Integer dpi;

    @Schema(description = "Chế độ gia công mặc định ('vector' hoặc 'raster')", example = "vector")
    private String mode;

    @Schema(description = "Vật liệu chuẩn của hệ thống (không được xóa)", example = "true")
    private Boolean isSystem;

    @Schema(description = "Email người tạo (nếu là vật liệu tùy chỉnh)", example = "user@gmail.com")
    private String createdByEmail;

    @Schema(description = "Thời điểm khởi tạo", example = "2026-09-30T10:00:00")
    private LocalDateTime createdAt;
}
