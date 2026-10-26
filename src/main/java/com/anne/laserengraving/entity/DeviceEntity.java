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
    private String connectionStatus = "OFFLINE"; // ONLINE, BUSY, OFFLINE, ERROR

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "firmware_version", nullable = false, length = 50)
    private String firmwareVersion = "GRBL 1.1h (MKS DLC32 V2.1)";

    @Column(name = "max_width_mm", nullable = false)
    private Double maxWidthMm = 300.0;

    @Column(name = "max_height_mm", nullable = false)
    private Double maxHeightMm = 200.0;

    @Column(name = "max_speed_mm_min", nullable = false)
    private Integer maxSpeedMmMin = 1500;

    @Column(name = "max_power_s", nullable = false)
    private Integer maxPowerS = 1000;

    @Column(name = "config_version", nullable = false)
    private Long configVersion = 1L;

    @Column(name = "config_applied_version", nullable = false)
    private Long configAppliedVersion = 0L;

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder
    public DeviceEntity(Integer id, String deviceCode, String deviceName,
                        String connectionStatus, String ipAddress,
                        String firmwareVersion, Double maxWidthMm, Double maxHeightMm,
                        Integer maxSpeedMmMin, Integer maxPowerS, Long configVersion, Long configAppliedVersion, String apiKeyHash,
                        LocalDateTime lastHeartbeat, Boolean isActive,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.deviceCode = deviceCode;
        this.deviceName = deviceName;
        this.connectionStatus = connectionStatus != null ? connectionStatus : "OFFLINE";
        this.ipAddress = ipAddress;
        this.firmwareVersion = firmwareVersion != null ? firmwareVersion : "GRBL 1.1h (MKS DLC32 V2.1)";
        this.maxWidthMm = maxWidthMm != null ? maxWidthMm : 300.0;
        this.maxHeightMm = maxHeightMm != null ? maxHeightMm : 200.0;
        this.maxSpeedMmMin = maxSpeedMmMin != null ? maxSpeedMmMin : 1500;
        this.maxPowerS = maxPowerS != null ? maxPowerS : 1000;
        this.configVersion = configVersion != null ? configVersion : 1L;
        this.configAppliedVersion = configAppliedVersion != null ? configAppliedVersion : 0L;
        this.apiKeyHash = apiKeyHash;
        this.lastHeartbeat = lastHeartbeat;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
        if (updatedAt == null)
            updatedAt = LocalDateTime.now();
        if (connectionStatus == null)
            connectionStatus = "OFFLINE";
        if (isActive == null)
            isActive = true;
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
