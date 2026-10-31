package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "Engraving_Jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngravingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, unique = true, length = 50)
    private String jobCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Nationalized
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Nationalized
    @Column(name = "gcode_filename", nullable = false, length = 255)
    private String gcodeFilename;

    @Nationalized
    @Column(name = "stored_file_path", nullable = false, length = 500)
    private String storedFilePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    private String sha256Checksum;

    @Column(name = "target_width_mm", nullable = false)
    private Double targetWidthMm;

    @Column(name = "target_height_mm", nullable = false)
    private Double targetHeightMm;

    @Column(name = "feed_rate", nullable = false)
    private Integer feedRate;

    @Column(name = "laser_power", nullable = false)
    private Integer laserPower;

    @Column(name = "total_lines", nullable = false)
    @Builder.Default
    private Integer totalLines = 0;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DISPATCHED"; // QUEUED, DISPATCHED, DOWNLOADING, VALIDATING, AWAITING_ONSITE_CONFIRMATION, PRINTING, COMPLETED, FAILED, CANCELLED

    @Column(name = "current_progress", nullable = false)
    @Builder.Default
    private Double currentProgress = 0.0;

    @Nationalized
    @Column(name = "onsite_operator_name", length = 100)
    private String onsiteOperatorName;

    @Nationalized
    @Column(name = "material_type", length = 100)
    private String materialType;

    @Column(name = "dispatched_at", nullable = false)
    @Builder.Default
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (dispatchedAt == null) dispatchedAt = LocalDateTime.now();
        if (status == null) status = "DISPATCHED";
        if (currentProgress == null) currentProgress = 0.0;
        if (totalLines == null) totalLines = 0;
    }
}
