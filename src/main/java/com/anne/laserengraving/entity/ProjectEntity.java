package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "Laser_Projects", indexes = {
        @Index(name = "IX_Laser_Projects_UserId", columnList = "user_id, updated_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Nationalized
    @Column(name = "project_name", nullable = false, length = 150)
    private String projectName;

    @Column(name = "element_count")
    @Builder.Default
    private Integer elementCount = 0;

    @Lob
    @Column(name = "elements_json", columnDefinition = "NVARCHAR(MAX)")
    private String elementsJson;

    @Lob
    @Column(name = "parameters_json", columnDefinition = "NVARCHAR(MAX)")
    private String parametersJson;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
