package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Laser_Settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaserSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Nationalized
    @Column(name = "profile_name", nullable = false, length = 150)
    private String profileName;

    @Column(name = "default_speed", nullable = false, precision = 8, scale = 2)
    private BigDecimal defaultSpeed;

    @Column(name = "max_power", nullable = false)
    private Integer maxPower;

    @Column(name = "default_dpi", nullable = false)
    private Integer defaultDpi;

    @Column(name = "max_width_mm", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal maxWidthMm = BigDecimal.valueOf(300.0);

    @Column(name = "max_height_mm", nullable = false, precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal maxHeightMm = BigDecimal.valueOf(200.0);

    @Column(name = "contrast")
    @Builder.Default
    private Integer contrast = 15;

    @Column(name = "brightness")
    @Builder.Default
    private Integer brightness = 0;

    @Column(name = "invert")
    @Builder.Default
    private Boolean invert = false;

    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "raster";

    @Column(name = "step_size", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal stepSize = BigDecimal.valueOf(10.0);

    @Column(name = "passes")
    @Builder.Default
    private Integer passes = 1;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
