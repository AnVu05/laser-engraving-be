package com.anne.laserengraving.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Job_Telemetry_Logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTelemetryLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private EngravingJobEntity job;

    @Column(name = "progress_percent", nullable = false)
    private Double progressPercent;

    @Column(name = "current_line", nullable = false)
    private Integer currentLine;

    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    @Column(name = "machine_state", nullable = false, length = 20)
    @Builder.Default
    private String machineState = "RUN"; // IDLE, RUN, HOLD, ALARM

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (machineState == null) machineState = "RUN";
    }
}
