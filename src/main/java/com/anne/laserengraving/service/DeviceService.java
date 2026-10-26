package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.dto.request.DeviceConfigUpdateRequest;

import java.util.List;

public interface DeviceService {

    List<DeviceResponse> getAllActiveDevices();

    DeviceResponse getDeviceByCode(String deviceCode);

    DeviceResponse updateDeviceConfig(String deviceCode, DeviceConfigUpdateRequest request);

    void acknowledgeDeviceConfig(String deviceCode, Long configVersion);

    void updateHeartbeat(DeviceHeartbeatMessage heartbeat);

    void updateDeviceStatus(String deviceCode, String status);

    void checkDeviceTimeouts();
}
