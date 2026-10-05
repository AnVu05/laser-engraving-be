package com.anne.laserengraving.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveHistoryRequest {

    @NotBlank(message = "Tên tệp hoặc đối tượng gia công không được để trống")
    @JsonProperty("originalFilename")
    @JsonAlias({"name", "projectName", "filename", "fileName"})
    private String originalFilename;

    @JsonProperty("totalGcodeLines")
    @JsonAlias({"lines", "totalLines"})
    private Integer totalGcodeLines;

    @JsonProperty("engravingSpeed")
    @JsonAlias({"feedRate", "speed"})
    private Integer engravingSpeed;

    @JsonProperty("laserPower")
    @JsonAlias({"power"})
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

    @JsonProperty("gcodeContent")
    @JsonAlias({"gcode", "content"})
    private String gcodeContent;

    @Builder.Default
    private String status = "Hoàn tất";
}
