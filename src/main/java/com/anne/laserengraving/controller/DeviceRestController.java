package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.request.DeviceConfigUpdateRequest;
import com.anne.laserengraving.service.DeviceService;
import com.anne.laserengraving.service.DeviceCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Laser Devices", description = "API Quản lý thiết bị máy khắc Laser IoT (MKS DLC32 V2.1)")
public class DeviceRestController {

    private final DeviceService deviceService;
    private final DeviceCredentialService credentialService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các máy khắc Laser đang hoạt động trong hệ thống")
    public ResponseEntity<List<DeviceResponse>> getAllActiveDevices() {
        return ResponseEntity.ok(deviceService.getAllActiveDevices());
    }

    @GetMapping("/{deviceCode}")
    @Operation(summary = "Tra cứu thông số kỹ thuật của một máy khắc cụ thể (VD: LASER-HN-001)")
    public ResponseEntity<DeviceResponse> getDeviceByCode(@PathVariable String deviceCode) {
        return ResponseEntity.ok(deviceService.getDeviceByCode(deviceCode));
    }

    @PutMapping("/{deviceCode}/config")
    @Operation(summary = "Cập nhật giới hạn vận hành của máy và đồng bộ tới firmware")
    public ResponseEntity<DeviceResponse> updateDeviceConfig(
            @PathVariable String deviceCode,
            @Valid @RequestBody DeviceConfigUpdateRequest request) {
        return ResponseEntity.ok(deviceService.updateDeviceConfig(deviceCode, request));
    }

    @PostMapping("/{deviceCode}/token")
    @Operation(summary = "Tạo lại token thiết bị; token chỉ trả về một lần để nhập vào captive portal")
    public ResponseEntity<Map<String, String>> rotateDeviceToken(@PathVariable String deviceCode) {
        return ResponseEntity.ok(Map.of("deviceCode", deviceCode, "deviceToken", credentialService.rotateCredential(deviceCode)));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Endpoint HTTP Fallback nhận nhịp tim Heartbeat từ bo mạch")
    public ResponseEntity<Map<String, Object>> receiveHttpHeartbeat(
            @RequestBody DeviceHeartbeatMessage heartbeat,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Device-Code") String deviceCode) {
        credentialService.authenticate(deviceCode, authorization);
        if (!deviceCode.equalsIgnoreCase(heartbeat.getDeviceCode())) {
            throw new SecurityException("Mã thiết bị heartbeat không khớp credential.");
        }
        deviceService.updateHeartbeat(heartbeat);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Heartbeat received",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
