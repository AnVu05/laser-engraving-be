package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.websocket.JobProgressMessage;
import com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage;
import com.anne.laserengraving.service.DeviceService;
import com.anne.laserengraving.service.JobTelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DeviceWebSocketController {

    private final DeviceService deviceService;
    private final JobTelemetryService jobTelemetryService;

    /**
     * Nhận Heartbeat PING từ bo mạch Makerbase MKS DLC32 V2.1 gửi lên qua STOMP:
     * Destination: /app/device/heartbeat
     */
    @MessageMapping("/device/heartbeat")
    public void handleDeviceHeartbeat(@Payload DeviceHeartbeatMessage message) {
        log.debug("[WS IN] Heartbeat từ thiết bị: {}", message.getDeviceCode());
        deviceService.updateHeartbeat(message);
    }

    /**
     * Nhận Telemetry tiến độ (35%, 60%, 100%) từ bo mạch Makerbase MKS DLC32 V2.1 gửi lên qua STOMP:
     * Destination: /app/device/telemetry
     */
    @MessageMapping("/device/telemetry")
    public void handleDeviceTelemetry(@Payload JobProgressMessage message) {
        log.info("[WS IN] Telemetry từ thiết bị {}: Job {}, Tiến độ {}%",
                message.getDeviceCode(), message.getJobCode(), message.getProgressPercent());
        jobTelemetryService.processProgressUpdate(message);
    }

    /**
     * Nhận Cập nhật trạng thái Job từ bo mạch MKS DLC32 V2.1:
     * Destination: /app/device/status
     */
    @MessageMapping("/device/status")
    public void handleDeviceJobStatus(@Payload JobStatusUpdateMessage message) {
        log.info("[WS IN] Trạng thái Job {} từ thiết bị {}: {}",
                message.getJobCode(), message.getDeviceCode(), message.getStatus());
        jobTelemetryService.processStatusUpdate(message);
    }
}
