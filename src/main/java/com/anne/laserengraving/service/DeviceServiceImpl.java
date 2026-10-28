package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.request.DeviceConfigUpdateRequest;
import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RenderIotGatewayClient renderGatewayClient;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllActiveDevices() {
        return deviceRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse getDeviceByCode(String deviceCode) {
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị có mã: " + deviceCode));
        return mapToResponse(device);
    }

    @Override
    @Transactional
    public DeviceResponse updateDeviceConfig(String deviceCode, DeviceConfigUpdateRequest request) {
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị có mã: " + deviceCode));

        device.setMaxWidthMm(request.getMaxWidthMm());
        device.setMaxHeightMm(request.getMaxHeightMm());
        device.setMaxSpeedMmMin(request.getMaxSpeedMmMin());
        device.setMaxPowerS(request.getMaxPowerS());
        long newVersion = device.getConfigVersion() == null ? 1L : device.getConfigVersion() + 1L;
        device.setConfigVersion(newVersion);
        deviceRepository.save(device);

        messagingTemplate.convertAndSend("/topic/device/" + deviceCode + "/config", mapToResponse(device));

        // Đồng bộ cấu hình sang Render IoT Gateway
        try {
            renderGatewayClient.syncDeviceConfig(deviceCode, java.util.Map.of(
                    "maxWidthMm", device.getMaxWidthMm(),
                    "maxHeightMm", device.getMaxHeightMm(),
                    "maxSpeedMmMin", device.getMaxSpeedMmMin(),
                    "maxPowerS", device.getMaxPowerS(),
                    "configVersion", newVersion
            ));
        } catch (Exception e) {
            log.warn("[DEVICE CONFIG] Bỏ qua lỗi đồng bộ cấu hình sang Render Gateway: {}", e.getMessage());
        }

        return mapToResponse(device);
    }

    @Override
    @Transactional
    public void acknowledgeDeviceConfig(String deviceCode, Long configVersion) {
        deviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
            if (device.getConfigVersion() != null && device.getConfigVersion().equals(configVersion)) {
                device.setConfigAppliedVersion(configVersion);
                deviceRepository.save(device);
                messagingTemplate.convertAndSend("/topic/devices/config", mapToResponse(device));
            }
        });
    }

    @Override
    @Transactional
    public void updateHeartbeat(DeviceHeartbeatMessage heartbeat) {
        if (heartbeat == null || heartbeat.getDeviceCode() == null) {
            return;
        }

        deviceRepository.findByDeviceCode(heartbeat.getDeviceCode()).ifPresent(device -> {
            boolean wasOffline = "OFFLINE".equalsIgnoreCase(device.getConnectionStatus());
            device.setLastHeartbeat(LocalDateTime.now());
            
            // Nếu thiết bị đang không bận thì duy trì trạng thái ONLINE
            if (!"BUSY".equalsIgnoreCase(device.getConnectionStatus())) {
                device.setConnectionStatus("ONLINE");
            }
            
            deviceRepository.save(device);

            Long reportedVersion = heartbeat.getConfigVersion();
            if (reportedVersion == null || !reportedVersion.equals(device.getConfigVersion())) {
                messagingTemplate.convertAndSend("/topic/device/" + device.getDeviceCode() + "/config", mapToResponse(device));
            }

            // Nếu vừa mới từ OFFLINE -> ONLINE, thông báo cho Web Client
            if (wasOffline) {
                log.info("Thiết bị {} đã KẾT NỐI TRỰC TUYẾN (ONLINE)!", device.getDeviceCode());
                messagingTemplate.convertAndSend("/topic/devices/status", mapToResponse(device));
            }
        });
    }

    @Override
    @Transactional
    public void updateDeviceStatus(String deviceCode, String status) {
        deviceRepository.findByDeviceCode(deviceCode).ifPresent(device -> {
            device.setConnectionStatus(status);
            device.setUpdatedAt(LocalDateTime.now());
            deviceRepository.save(device);
            log.info("Cập nhật trạng thái máy {}: {}", deviceCode, status);
            messagingTemplate.convertAndSend("/topic/devices/status", mapToResponse(device));
        });
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 4000) // Quét đồng bộ định kỳ mỗi 4 giây
    public void checkDeviceTimeouts() {
        List<DeviceEntity> devices = deviceRepository.findByIsActiveTrue();

        for (DeviceEntity device : devices) {
            String oldStatus = device.getConnectionStatus();
            String targetStatus = oldStatus;

            // 1. Kiểm tra trạng thái thực tế từ Render IoT Gateway nếu được bật
            boolean checkedGateway = false;
            if (renderGatewayClient.isEnabled()) {
                try {
                    var gatewayStatusOpt = renderGatewayClient.getDeviceStatus(device.getDeviceCode());
                    if (gatewayStatusOpt.isPresent()) {
                        checkedGateway = true;
                        var data = gatewayStatusOpt.get();
                        String gwStatus = (String) data.get("status");
                        boolean gwActive = Boolean.TRUE.equals(data.get("isActive"));

                        if ("ONLINE".equalsIgnoreCase(gwStatus) || gwActive) {
                            targetStatus = "BUSY".equalsIgnoreCase(oldStatus) ? "BUSY" : "ONLINE";
                        } else {
                            targetStatus = "OFFLINE";
                        }
                    }
                } catch (Exception e) {
                    log.debug("[DEVICE TIMEOUT] Lỗi truy vấn Render Gateway cho máy {}: {}", device.getDeviceCode(), e.getMessage());
                }
            }

            // 2. Fallback kiểm tra nhịp tim cục bộ nếu không dùng Render Gateway
            if (!checkedGateway) {
                LocalDateTime threshold = LocalDateTime.now().minusSeconds(45);
                if ("ONLINE".equalsIgnoreCase(oldStatus) || "BUSY".equalsIgnoreCase(oldStatus)) {
                    if (device.getLastHeartbeat() == null || device.getLastHeartbeat().isBefore(threshold)) {
                        targetStatus = "OFFLINE";
                    }
                }
            }

            if (!targetStatus.equalsIgnoreCase(oldStatus)) {
                device.setConnectionStatus(targetStatus);
                device.setUpdatedAt(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("[DEVICE MONITOR] Thiết bị {} cập nhật trạng thái: {} -> {}", device.getDeviceCode(), oldStatus, targetStatus);
                messagingTemplate.convertAndSend("/topic/devices/status", mapToResponse(device));
            }
        }
    }

    private DeviceResponse mapToResponse(DeviceEntity entity) {
        String status = entity.getConnectionStatus() != null ? entity.getConnectionStatus() : "OFFLINE";
        LocalDateTime lastHeartbeat = entity.getLastHeartbeat();
        Integer wifiRssi = null;
        Long freeHeap = null;

        // Đồng bộ trạng thái thực tế thời gian thực từ Render Cloud Gateway
        if (renderGatewayClient.isEnabled()) {
            try {
                var gatewayStatusOpt = renderGatewayClient.getDeviceStatus(entity.getDeviceCode());
                if (gatewayStatusOpt.isPresent()) {
                    var data = gatewayStatusOpt.get();
                    String gwStatus = (String) data.get("status");
                    boolean gwActive = Boolean.TRUE.equals(data.get("isActive"));

                    if ("ONLINE".equalsIgnoreCase(gwStatus) || gwActive) {
                        status = "BUSY".equalsIgnoreCase(entity.getConnectionStatus()) ? "BUSY" : "ONLINE";
                        lastHeartbeat = LocalDateTime.now();
                    } else {
                        status = "OFFLINE";
                    }

                    if (data.get("lastHeartbeat") instanceof java.util.Map hb) {
                        if (hb.get("wifiRssi") instanceof Number num) wifiRssi = num.intValue();
                        if (hb.get("freeHeap") instanceof Number num) freeHeap = num.longValue();
                    }
                }
            } catch (Exception e) {
                log.debug("[DEVICE STATUS] Không thể lấy trạng thái từ Render Gateway: {}", e.getMessage());
            }
        }

        return DeviceResponse.builder()
                .id(entity.getId())
                .deviceCode(entity.getDeviceCode())
                .deviceName(entity.getDeviceName())
                .connectionStatus(status)
                .ipAddress(entity.getIpAddress())
                .firmwareVersion(entity.getFirmwareVersion())
                .maxWidthMm(entity.getMaxWidthMm())
                .maxHeightMm(entity.getMaxHeightMm())
                .maxSpeedMmMin(entity.getMaxSpeedMmMin())
                .maxPowerS(entity.getMaxPowerS())
                .configVersion(entity.getConfigVersion())
                .configAppliedVersion(entity.getConfigAppliedVersion())
                .lastHeartbeat(lastHeartbeat)
                .isActive(entity.getIsActive())
                .wifiRssi(wifiRssi)
                .freeHeap(freeHeap)
                .build();
    }
}
