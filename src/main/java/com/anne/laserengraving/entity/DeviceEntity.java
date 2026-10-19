package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "Devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "device_code", nullable = false, unique = true, length = 50)
    private String deviceCode;

    @Nationalized
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "connection_status", nullable = false, length = 20)
    @Builder.Default
    private String connectionStatus = "OFFLINE"; // ONLINE, BUSY, OFFLINE, ERROR

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "firmware_version", nullable = false, length = 50)
    @Builder.Default
    private String firmwareVersion = "GRBL 1.1h (MKS DLC32 V2.1)";

    @Column(name = "max_width_mm", nullable = false)
    @Builder.Default
    private Double maxWidthMm = 300.0;

    @Column(name = "max_height_mm", nullable = false)
    @Builder.Default
    private Double maxHeightMm = 200.0;

    @Column(name = "max_speed_mm_min", nullable = false)
    @Builder.Default
    private Integer maxSpeedMmMin = 1500;

    @Column(name = "max_power_s", nullable = false)
    @Builder.Default
    private Integer maxPowerS = 1000;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (connectionStatus == null) connectionStatus = "OFFLINE";
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getStatus() {
        return this.connectionStatus;
    }

    public void setStatus(String status) {
        this.connectionStatus = status;
    }
}
