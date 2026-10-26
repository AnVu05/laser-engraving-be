package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.request.DeviceConfigUpdateRequest;
import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RenderIotGatewayClient renderGatewayClient;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    @DisplayName("Cập nhật Heartbeat thành công chuyển máy sang ONLINE")
    void testUpdateHeartbeat_Success() {
        DeviceEntity device = DeviceEntity.builder()
                .id(1)
                .deviceCode("LASER-HN-001")
                .deviceName("Máy Laser HN")
                .connectionStatus("OFFLINE")
                .isActive(true)
                .build();

        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(device));

        DeviceHeartbeatMessage heartbeat = DeviceHeartbeatMessage.builder()
                .deviceCode("LASER-HN-001")
                .status("ONLINE")
                .wifiRssi(-60)
                .freeHeap(150000L)
                .timestamp(System.currentTimeMillis())
                .build();

        deviceService.updateHeartbeat(heartbeat);

        assertThat(device.getConnectionStatus()).isEqualTo("ONLINE");
        assertThat(device.getLastHeartbeat()).isNotNull();
        verify(deviceRepository, times(1)).save(device);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/devices/status"), any(DeviceResponse.class));
    }

    @Test
    @DisplayName("Lấy danh sách các máy đang hoạt động")
    void testGetAllActiveDevices() {
        DeviceEntity dev1 = DeviceEntity.builder()
                .id(1)
                .deviceCode("LASER-HN-001")
                .deviceName("Máy 01")
                .connectionStatus("ONLINE")
                .isActive(true)
                .build();

        when(deviceRepository.findByIsActiveTrue()).thenReturn(List.of(dev1));

        List<DeviceResponse> result = deviceService.getAllActiveDevices();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceCode()).isEqualTo("LASER-HN-001");
    }

    @Test
    @DisplayName("Cập nhật cấu hình thiết bị tăng phiên bản và gửi cấu hình tới board")
    void givenValidConfig_whenUpdateDeviceConfig_thenPublishVersionedConfig() {
        DeviceEntity device = DeviceEntity.builder()
                .id(1).deviceCode("LASER-HN-001").deviceName("Máy Laser")
                .maxWidthMm(300.0).maxHeightMm(200.0).maxSpeedMmMin(1500).maxPowerS(1000)
                .configVersion(4L).configAppliedVersion(4L).apiKeyHash("hash").isActive(true).build();
        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(device));
        DeviceConfigUpdateRequest request = new DeviceConfigUpdateRequest();
        request.setMaxWidthMm(250.0);
        request.setMaxHeightMm(180.0);
        request.setMaxSpeedMmMin(1200);
        request.setMaxPowerS(900);

        DeviceResponse response = deviceService.updateDeviceConfig("LASER-HN-001", request);

        assertThat(response.getConfigVersion()).isEqualTo(5L);
        assertThat(response.getConfigAppliedVersion()).isEqualTo(4L);
        assertThat(response.getMaxWidthMm()).isEqualTo(250.0);
        verify(messagingTemplate).convertAndSend(eq("/topic/device/LASER-HN-001/config"), any(DeviceResponse.class));
        verify(deviceRepository).save(device);
    }
}
