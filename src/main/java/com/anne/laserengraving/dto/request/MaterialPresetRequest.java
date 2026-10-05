package com.anne.laserengraving.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo mới hoặc cập nhật vật liệu mẫu (Material Preset)")
public class MaterialPresetRequest {

    @NotBlank(message = "Tên vật liệu không được để trống")
    @Size(max = 150, message = "Tên vật liệu không được vượt quá 150 ký tự")
    @Schema(description = "Tên vật liệu (phôi)", example = "Gỗ ép Plywood (3mm)")
    private String name;

    @Schema(description = "Mã định danh vật liệu (tự sinh nếu để trống)", example = "mat-plywood")
    private String materialKey;

    @Schema(description = "Icon Google Material Symbols", example = "forest")
    private String icon;

    @Schema(description = "Nhãn huy hiệu / ghi chú ngắn", example = "Phổ biến nhất")
    private String badge;

    @Schema(description = "Mô tả chi tiết đặc tính phôi", example = "Ván ép bạch dương chuyên dụng cho cắt mô hình và khắc tranh.")
    private String description;

    @Min(value = 0, message = "Tốc độ cắt không được nhỏ hơn 0")
    @Schema(description = "Tốc độ cắt Vector (mm/phút)", example = "350")
    private Integer cutFeed;

    @Min(value = 0, message = "Công suất cắt tối thiểu 0%")
    @Max(value = 100, message = "Công suất cắt tối đa 100%")
    @Schema(description = "Công suất laser cắt Vector (%)", example = "100")
    private Integer cutPower;

    @Min(value = 0, message = "Số lượt cắt tối thiểu 0")
    @Schema(description = "Số lượt cắt qua (Passes)", example = "2")
    private Integer cutPasses;

    @Min(value = 0, message = "Tốc độ khắc không được nhỏ hơn 0")
    @Schema(description = "Tốc độ khắc Raster (mm/phút)", example = "2000")
    private Integer engraveFeed;

    @Min(value = 0, message = "Công suất khắc tối thiểu 0%")
    @Max(value = 100, message = "Công suất khắc tối đa 100%")
    @Schema(description = "Công suất laser khắc Raster (%)", example = "65")
    private Integer engravePower;

    @Min(value = 50, message = "Độ phân giải DPI tối thiểu 50")
    @Max(value = 1200, message = "Độ phân giải DPI tối đa 1200")
    @Schema(description = "Độ phân giải khắc (DPI)", example = "254")
    private Integer dpi;

    @Schema(description = "Chế độ gia công mặc định ('vector' hoặc 'raster')", example = "vector")
    private String mode;
}
