package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Gcode_History", indexes = {
        @Index(name = "IX_Gcode_History_UserId", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GcodeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id")
    private LaserSettingsEntity setting;

    @Nationalized
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_file_path", nullable = false, length = 500)
    private String storedFilePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "total_gcode_lines", nullable = false)
    private Integer totalGcodeLines;

    @Column(name = "engraving_speed", nullable = false)
    private Integer engravingSpeed;

    @Column(name = "laser_power", nullable = false)
    private Integer laserPower;

    @Column(name = "dpi", nullable = false)
    private Integer dpi;

    @Column(name = "origin_x_mm", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal originXMm = BigDecimal.ZERO;

    @Column(name = "origin_y_mm", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal originYMm = BigDecimal.ZERO;

    @Column(name = "output_width_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal outputWidthMm;

    @Column(name = "output_height_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal outputHeightMm;

    @Column(name = "estimated_time_seconds")
    private Integer estimatedTimeSeconds;

    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "raster";

    @Column(name = "burn_count")
    private Long burnCount;

    @Column(name = "burn_percent", precision = 5, scale = 2)
    private BigDecimal burnPercent;

    @Column(name = "material_name", length = 100)
    private String materialName;

    @Lob
    @Column(name = "gcode_content", columnDefinition = "NVARCHAR(MAX)")
    private String gcodeContent;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "Hoàn tất";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
