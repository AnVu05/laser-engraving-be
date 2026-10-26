package com.anne.laserengraving.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceConfigUpdateRequest {
    @NotNull @DecimalMin("10.0") @DecimalMax("300.0")
    private Double maxWidthMm;

    @NotNull @DecimalMin("10.0") @DecimalMax("200.0")
    private Double maxHeightMm;

    @NotNull @Min(100) @Max(5000)
    private Integer maxSpeedMmMin;

    @NotNull @Min(0) @Max(1000)
    private Integer maxPowerS;
}
