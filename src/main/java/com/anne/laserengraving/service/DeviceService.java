package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;

import java.util.List;

public interface DeviceService {

    List<DeviceResponse> getAllActiveDevices();

    DeviceResponse getDeviceByCode(String deviceCode);

    void updateHeartbeat(DeviceHeartbeatMessage heartbeat);

    void updateDeviceStatus(String deviceCode, String status);

    void checkDeviceTimeouts();
}
