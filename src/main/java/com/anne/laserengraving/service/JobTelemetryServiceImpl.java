package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.websocket.JobProgressMessage;
import com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage;
import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.entity.EngravingJobEntity;
import com.anne.laserengraving.entity.JobTelemetryLogEntity;
import com.anne.laserengraving.repository.DeviceRepository;
import com.anne.laserengraving.repository.EngravingJobRepository;
import com.anne.laserengraving.repository.JobTelemetryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobTelemetryServiceImpl implements JobTelemetryService {

    private final EngravingJobRepository jobRepository;
    private final JobTelemetryLogRepository telemetryLogRepository;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void processProgressUpdate(JobProgressMessage progress) {
        if (progress == null || progress.getJobCode() == null) return;

        jobRepository.findByJobCode(progress.getJobCode()).ifPresent(job -> {
            job.setCurrentProgress(progress.getProgressPercent());
            if ("PRINTING".equalsIgnoreCase(progress.getStatus())) {
                job.setStatus("PRINTING");
                if (job.getStartedAt() == null) {
                    job.setStartedAt(LocalDateTime.now());
                }
            } else if ("COMPLETED".equalsIgnoreCase(progress.getStatus()) || progress.getProgressPercent() >= 100.0) {
                job.setStatus("COMPLETED");
                job.setCurrentProgress(100.0);
                if (job.getCompletedAt() == null) {
                    job.setCompletedAt(LocalDateTime.now());
                }
                // Giải phóng máy về trạng thái ONLINE
                releaseDevice(job.getDevice());
            }
            jobRepository.save(job);

            // Ghi log telemetry vào CSDL
            JobTelemetryLogEntity logEntity = JobTelemetryLogEntity.builder()
                    .job(job)
                    .progressPercent(progress.getProgressPercent())
                    .currentLine(progress.getCurrentLine() != null ? progress.getCurrentLine() : 0)
                    .remainingSeconds(progress.getRemainingSeconds())
                    .machineState("RUN")
                    .createdAt(LocalDateTime.now())
                    .build();
            telemetryLogRepository.save(logEntity);

            log.info("[Telemetry] Job {}: Tiến độ {:.1f}% (Dòng {}/{}) -> Broadcast WebSocket",
                    job.getJobCode(), progress.getProgressPercent(), progress.getCurrentLine(), progress.getTotalLines());

            // 1. Gửi vào topic riêng của Job: /topic/jobs/{jobCode}/progress
            messagingTemplate.convertAndSend("/topic/jobs/" + job.getJobCode() + "/progress", progress);

            // 2. Gửi vào topic tiến độ chung cho Web Client: /topic/client/progress
            messagingTemplate.convertAndSend("/topic/client/progress", progress);
        });
    }

    @Override
    @Transactional
    public void processStatusUpdate(JobStatusUpdateMessage statusUpdate) {
        if (statusUpdate == null || statusUpdate.getJobCode() == null) return;

        jobRepository.findByJobCode(statusUpdate.getJobCode()).ifPresent(job -> {
            job.setStatus(statusUpdate.getStatus());

            if ("AWAITING_ONSITE_CONFIRMATION".equalsIgnoreCase(statusUpdate.getStatus())) {
                log.info("[Job Status] Job {} đang CHỜ XÁC NHẬN AN TOÀN TẠI CHỖ!", job.getJobCode());
            } else if ("PRINTING".equalsIgnoreCase(statusUpdate.getStatus())) {
                if (job.getStartedAt() == null) job.setStartedAt(LocalDateTime.now());
                if (job.getConfirmedAt() == null) job.setConfirmedAt(LocalDateTime.now());
                // Chuyển máy sang BUSY
                setDeviceBusy(job.getDevice());
            } else if ("COMPLETED".equalsIgnoreCase(statusUpdate.getStatus())) {
                job.setCurrentProgress(100.0);
                job.setCompletedAt(LocalDateTime.now());
                releaseDevice(job.getDevice());
            } else if ("FAILED".equalsIgnoreCase(statusUpdate.getStatus()) || "CANCELLED".equalsIgnoreCase(statusUpdate.getStatus())) {
                releaseDevice(job.getDevice());
            }

            jobRepository.save(job);

            // Broadcast trạng thái mới tới Web Client
            messagingTemplate.convertAndSend("/topic/jobs/" + job.getJobCode() + "/status", statusUpdate);
            messagingTemplate.convertAndSend("/topic/client/status", statusUpdate);
        });
    }

    private void setDeviceBusy(DeviceEntity device) {
        if (device != null) {
            device.setConnectionStatus("BUSY");
            deviceRepository.save(device);
        }
    }

    private void releaseDevice(DeviceEntity device) {
        if (device != null) {
            device.setConnectionStatus("ONLINE");
            deviceRepository.save(device);
        }
    }
}
