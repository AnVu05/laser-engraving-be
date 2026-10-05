package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Material_Presets", indexes = {
        @Index(name = "IX_Material_Presets_Key", columnList = "material_key"),
        @Index(name = "IX_Material_Presets_User", columnList = "user_id, is_system")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialPresetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preset_id")
    private Long presetId;

    @Column(name = "material_key", nullable = false, length = 60)
    private String materialKey;

    @Nationalized
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "icon", length = 50)
    @Builder.Default
    private String icon = "layers";

    @Nationalized
    @Column(name = "badge", length = 60)
    private String badge;

    @Nationalized
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cut_feed", nullable = false)
    @Builder.Default
    private Integer cutFeed = 300;

    @Column(name = "cut_power", nullable = false)
    @Builder.Default
    private Integer cutPower = 80;

    @Column(name = "cut_passes", nullable = false)
    @Builder.Default
    private Integer cutPasses = 1;

    @Column(name = "engrave_feed", nullable = false)
    @Builder.Default
    private Integer engraveFeed = 2000;

    @Column(name = "engrave_power", nullable = false)
    @Builder.Default
    private Integer engravePower = 50;

    @Column(name = "dpi", nullable = false)
    @Builder.Default
    private Integer dpi = 254;

    @Column(name = "mode", length = 20)
    @Builder.Default
    private String mode = "vector";

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
