package com.anne.laserengraving.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Integer id;
    private String deviceCode;
    private String deviceName;
    private String connectionStatus; // ONLINE, BUSY, OFFLINE, ERROR
    private String ipAddress;
    private String firmwareVersion;
    private Double maxWidthMm;
    private Double maxHeightMm;
    private Integer maxSpeedMmMin;
    private Integer maxPowerS;
    private Long configVersion;
    private Long configAppliedVersion;
    private LocalDateTime lastHeartbeat;
    private Boolean isActive;
    private Integer wifiRssi;
    private Long freeHeap;
}
