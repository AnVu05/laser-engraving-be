package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.websocket.JobProgressMessage;
import com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage;
import com.anne.laserengraving.service.DeviceService;
import com.anne.laserengraving.service.JobTelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class DeviceWebSocketControllerTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private JobTelemetryService jobTelemetryService;

    @InjectMocks
    private DeviceWebSocketController webSocketController;

    @Test
    @DisplayName("Xử lý bản tin STOMP Heartbeat từ bo mạch")
    void testHandleDeviceHeartbeat() {
        DeviceHeartbeatMessage msg = DeviceHeartbeatMessage.builder()
                .deviceCode("LASER-HN-001")
                .status("ONLINE")
                .build();

        webSocketController.handleDeviceHeartbeat(msg, Map.of("authenticatedDeviceCode", "LASER-HN-001"));
        verify(deviceService).updateHeartbeat(msg);
    }

    @Test
    @DisplayName("Xử lý bản tin STOMP Telemetry tiến độ (35%, 60%, 100%) từ bo mạch")
    void testHandleDeviceTelemetry() {
        JobProgressMessage progress = JobProgressMessage.builder()
                .deviceCode("LASER-HN-001")
                .jobCode("JOB-TEST-101")
                .progressPercent(35.0)
                .currentLine(5250)
                .totalLines(15000)
                .build();

        webSocketController.handleDeviceTelemetry(progress, Map.of("authenticatedDeviceCode", "LASER-HN-001"));
        verify(jobTelemetryService).processProgressUpdate(progress);
    }

    @Test
    @DisplayName("Xử lý bản tin STOMP Trạng thái Job từ bo mạch")
    void testHandleDeviceJobStatus() {
        JobStatusUpdateMessage status = JobStatusUpdateMessage.builder()
                .deviceCode("LASER-HN-001")
                .jobCode("JOB-TEST-101")
                .status("AWAITING_ONSITE_CONFIRMATION")
                .build();

        webSocketController.handleDeviceJobStatus(status, Map.of("authenticatedDeviceCode", "LASER-HN-001"));
        verify(jobTelemetryService).processStatusUpdate(status);
    }
}
