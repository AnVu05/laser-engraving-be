package com.anne.laserengraving.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RenderIotGatewayClientTest {

    @Test
    @DisplayName("Khi Render Gateway Client bị tắt (enabled=false) thì không gửi request")
    void testDisabledGatewayClientDoesNotSend() {
        RenderIotGatewayClient client = new RenderIotGatewayClient("http://localhost:8081", "secret", false);

        Optional<Map<String, Object>> result = client.dispatchJobToGateway(
                "JOB-001", "LASER-HN-001", "logo.gcode", "G0 X0 Y0", 100.0, 100.0, 1000, 500, "Wood"
        );
        assertTrue(result.isEmpty());

        Optional<Map<String, Object>> telemetry = client.getDeviceTelemetry("LASER-HN-001");
        assertTrue(telemetry.isEmpty());

        boolean cmdSuccess = client.sendDeviceCommand("LASER-HN-001", "PAUSE", "JOB-001", "reason");
        assertFalse(cmdSuccess);

        boolean configSuccess = client.syncDeviceConfig("LASER-HN-001", Map.of("configVersion", 1L));
        assertFalse(configSuccess);
    }

    @Test
    @DisplayName("Khi Render Gateway không phản hồi (unreachable) thì trả về Optional.empty mà không ném Exception")
    void testUnreachableGatewayReturnsEmptyGracefully() {
        // Point to an invalid port that cannot connect
        RenderIotGatewayClient client = new RenderIotGatewayClient("http://localhost:59999", "secret", true);

        Optional<Map<String, Object>> result = client.dispatchJobToGateway(
                "JOB-001", "LASER-HN-001", "logo.gcode", "G0 X0 Y0", 100.0, 100.0, 1000, 500, "Wood"
        );
        assertTrue(result.isEmpty());

        Optional<Map<String, Object>> telemetry = client.getDeviceTelemetry("LASER-HN-001");
        assertTrue(telemetry.isEmpty());

        boolean cmdSuccess = client.sendDeviceCommand("LASER-HN-001", "PAUSE", "JOB-001", "reason");
        assertFalse(cmdSuccess);
    }
}
