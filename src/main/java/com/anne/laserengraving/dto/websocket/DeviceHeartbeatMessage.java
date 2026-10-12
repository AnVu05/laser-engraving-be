package com.anne.laserengraving.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceHeartbeatMessage {
    private String type;           // "HEARTBEAT"
    private String deviceCode;     // "LASER-HN-001"
    private String status;         // "ONLINE", "BUSY", "IDLE"
    private Long freeHeap;         // Bộ nhớ RAM còn trống trên ESP32
    private Integer wifiRssi;      // Cường độ sóng WiFi (dBm)
    private Long timestamp;
}
