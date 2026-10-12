package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.entity.EngravingJobEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
class EngravingJobRepositoryTest {

    @Autowired
    private EngravingJobRepository jobRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @DisplayName("Lưu và tìm kiếm tác vụ gia công theo jobCode")
    void testSaveAndFindByJobCode() {
        DeviceEntity device = DeviceEntity.builder()
                .deviceCode("LASER-JOB-DEV")
                .deviceName("Máy Test Job")
                .connectionStatus("ONLINE")
                .apiKeyHash("hash")
                .isActive(true)
                .build();
        deviceRepository.save(device);

        EngravingJobEntity job = EngravingJobEntity.builder()
                .jobCode("JOB-TEST-101")
                .device(device)
                .originalFilename("logo.png")
                .gcodeFilename("logo.gcode")
                .storedFilePath("storage/gcode/logo.gcode")
                .fileSizeBytes(204800L)
                .sha256Checksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .targetWidthMm(100.0)
                .targetHeightMm(100.0)
                .feedRate(1500)
                .laserPower(800)
                .totalLines(12500)
                .status("DISPATCHED")
                .currentProgress(0.0)
                .dispatchedAt(LocalDateTime.now())
                .build();

        jobRepository.save(job);

        Optional<EngravingJobEntity> found = jobRepository.findByJobCode("JOB-TEST-101");
        assertThat(found).isPresent();
        assertThat(found.get().getTargetWidthMm()).isEqualTo(100.0);
        assertThat(found.get().getSha256Checksum()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(found.get().getDevice().getDeviceCode()).isEqualTo("LASER-JOB-DEV");
    }
}
