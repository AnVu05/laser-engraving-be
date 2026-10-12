package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.DeviceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @DisplayName("Lưu và tìm kiếm thiết bị theo mã device_code")
    void testSaveAndFindByDeviceCode() {
        DeviceEntity device = DeviceEntity.builder()
                .deviceCode("LASER-TEST-001")
                .deviceName("Máy Khắc Test MKS DLC32")
                .connectionStatus("ONLINE")
                .ipAddress("192.168.1.200")
                .firmwareVersion("GRBL 1.1h")
                .maxWidthMm(300.0)
                .maxHeightMm(200.0)
                .maxSpeedMmMin(1500)
                .maxPowerS(1000)
                .apiKeyHash("test_hash_123")
                .lastHeartbeat(LocalDateTime.now())
                .isActive(true)
                .build();

        deviceRepository.save(device);

        Optional<DeviceEntity> found = deviceRepository.findByDeviceCode("LASER-TEST-001");
        assertThat(found).isPresent();
        assertThat(found.get().getDeviceName()).isEqualTo("Máy Khắc Test MKS DLC32");
        assertThat(found.get().getConnectionStatus()).isEqualTo("ONLINE");
    }

    @Test
    @DisplayName("Tìm kiếm tất cả thiết bị đang kích hoạt (isActive = true)")
    void testFindByIsActiveTrue() {
        DeviceEntity activeDevice = DeviceEntity.builder()
                .deviceCode("LASER-ACTIVE")
                .deviceName("Máy Đang Bật")
                .connectionStatus("ONLINE")
                .apiKeyHash("hash_active")
                .isActive(true)
                .build();

        DeviceEntity inactiveDevice = DeviceEntity.builder()
                .deviceCode("LASER-INACTIVE")
                .deviceName("Máy Đã Khóa")
                .connectionStatus("OFFLINE")
                .apiKeyHash("hash_inactive")
                .isActive(false)
                .build();

        deviceRepository.save(activeDevice);
        deviceRepository.save(inactiveDevice);

        List<DeviceEntity> activeList = deviceRepository.findByIsActiveTrue();
        assertThat(activeList).extracting(DeviceEntity::getDeviceCode).contains("LASER-ACTIVE");
        assertThat(activeList).extracting(DeviceEntity::getDeviceCode).doesNotContain("LASER-INACTIVE");
    }
}
