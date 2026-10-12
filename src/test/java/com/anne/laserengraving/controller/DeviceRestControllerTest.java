package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.device.DeviceResponse;
import com.anne.laserengraving.dto.websocket.DeviceHeartbeatMessage;
import com.anne.laserengraving.service.DeviceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeviceRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceRestController deviceRestController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deviceRestController).build();
    }

    @Test
    @DisplayName("API GET /api/v1/devices trả về danh sách máy laser")
    void testGetAllActiveDevices() throws Exception {
        DeviceResponse dev = DeviceResponse.builder()
                .id(1)
                .deviceCode("LASER-HN-001")
                .deviceName("Máy Khắc Laser Makerbase MKS DLC32 #01")
                .connectionStatus("ONLINE")
                .maxWidthMm(300.0)
                .maxHeightMm(200.0)
                .isActive(true)
                .build();

        when(deviceService.getAllActiveDevices()).thenReturn(List.of(dev));

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceCode").value("LASER-HN-001"))
                .andExpect(jsonPath("$[0].connectionStatus").value("ONLINE"))
                .andExpect(jsonPath("$[0].maxWidthMm").value(300.0));
    }

    @Test
    @DisplayName("API POST /api/v1/devices/heartbeat tiếp nhận nhịp tim")
    void testReceiveHeartbeat() throws Exception {
        DeviceHeartbeatMessage heartbeat = DeviceHeartbeatMessage.builder()
                .deviceCode("LASER-HN-001")
                .status("ONLINE")
                .wifiRssi(-55)
                .freeHeap(180000L)
                .build();

        mockMvc.perform(post("/api/v1/devices/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartbeat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(deviceService).updateHeartbeat(any(DeviceHeartbeatMessage.class));
    }
}
