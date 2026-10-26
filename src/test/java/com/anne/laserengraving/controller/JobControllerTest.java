package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.JobDispatchRequest;
import com.anne.laserengraving.dto.response.JobDispatchResponse;
import com.anne.laserengraving.exception.GlobalExceptionHandler;
import com.anne.laserengraving.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobController jobController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JobDispatchResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = JobDispatchResponse.builder()
                .jobId(100L)
                .jobCode("JOB-20261020-A1B2C3D4")
                .deviceCode("LASER-HN-001")
                .deviceName("Máy Khắc Laser Makerbase MKS DLC32 #01")
                .originalFilename("logo.gcode")
                .fileSizeBytes(1024L)
                .sha256Checksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .targetWidthMm(100.0)
                .targetHeightMm(100.0)
                .feedRate(1500)
                .laserPower(800)
                .totalLines(250)
                .status("DISPATCHED")
                .currentProgress(0.0)
                .materialType("Gỗ Plywood")
                .downloadUrl("/api/v1/jobs/100/download")
                .dispatchedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("API POST /api/v1/jobs/dispatch (Multipart) tiếp nhận file và điều phối Job thành công")
    void testDispatchJobMultipart_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.gcode", "text/plain", "G21\nG90\n$32=1\n".getBytes(StandardCharsets.UTF_8));

        when(jobService.dispatchJob(any(JobDispatchRequest.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(multipart("/api/v1/jobs/dispatch")
                        .file(file)
                        .param("deviceCode", "LASER-HN-001")
                        .param("targetWidthMm", "100.0")
                        .param("targetHeightMm", "100.0")
                        .param("feedRate", "1500")
                        .param("laserPower", "800"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobCode").value("JOB-20261020-A1B2C3D4"))
                .andExpect(jsonPath("$.data.deviceCode").value("LASER-HN-001"))
                .andExpect(jsonPath("$.data.sha256Checksum").value("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    @Test
    @DisplayName("API POST /api/v1/jobs/dispatch (JSON) tiếp nhận chuỗi G-code và điều phối Job")
    void testDispatchJobJson_Success() throws Exception {
        JobDispatchRequest request = JobDispatchRequest.builder()
                .deviceCode("LASER-HN-001")
                .originalFilename("logo.gcode")
                .gcodeContent("G21\nG90\n$32=1\nM5\n")
                .targetWidthMm(100.0)
                .targetHeightMm(100.0)
                .feedRate(1500)
                .laserPower(800)
                .build();

        when(jobService.dispatchJob(any(JobDispatchRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/jobs/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(100L))
                .andExpect(jsonPath("$.data.jobCode").value("JOB-20261020-A1B2C3D4"));
    }

    @Test
    @DisplayName("API GET /api/v1/jobs/{jobId}/download trả về luồng file kèm header X-File-SHA256")
    void testDownloadJobById_Success() throws Exception {
        byte[] gcodeBytes = "G21\nG90\n$32=1\n".getBytes(StandardCharsets.UTF_8);
        when(jobService.getJobById(100L)).thenReturn(sampleResponse);
        when(jobService.downloadJobFile(eq(100L), anyString(), anyString()))
                .thenReturn(new ByteArrayResource(gcodeBytes));

        mockMvc.perform(get("/api/v1/jobs/100/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mk_sec_laser_hn_001_secret")
                        .header("X-Device-Code", "LASER-HN-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"logo.gcode\""))
                .andExpect(content().string("G21\nG90\n$32=1\n"));
    }

    @Test
    @DisplayName("API GET /api/v1/jobs/code/{jobCode}/download tải file theo mã Job")
    void testDownloadJobByCode_Success() throws Exception {
        byte[] gcodeBytes = "M5\nG0 X0 Y0\n".getBytes(StandardCharsets.UTF_8);
        when(jobService.getJobByCode("JOB-20261020-A1B2C3D4")).thenReturn(sampleResponse);
        when(jobService.downloadJobFileByCode(eq("JOB-20261020-A1B2C3D4"), anyString(), anyString()))
                .thenReturn(new ByteArrayResource(gcodeBytes));

        mockMvc.perform(get("/api/v1/jobs/code/JOB-20261020-A1B2C3D4/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mk_sec_laser_hn_001_secret")
                        .header("X-Device-Code", "LASER-HN-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-SHA256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))
                .andExpect(content().string("M5\nG0 X0 Y0\n"));
    }

    @Test
    @DisplayName("API GET /api/v1/jobs/{jobId} trả về chi tiết Job")
    void testGetJobById_Success() throws Exception {
        when(jobService.getJobById(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/jobs/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(100L));
    }

    @Test
    @DisplayName("API GET /api/v1/jobs tra cứu danh sách Jobs theo máy")
    void testGetJobs_ByDevice() throws Exception {
        when(jobService.getJobsByDevice("LASER-HN-001")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/jobs").param("deviceCode", "LASER-HN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].deviceCode").value("LASER-HN-001"));
    }

    @Test
    @DisplayName("API POST /api/v1/jobs/{jobCode}/cancel hủy bỏ Job")
    void testCancelJob_Success() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/JOB-20261020-A1B2C3D4/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(jobService).cancelJob("JOB-20261020-A1B2C3D4");
    }

    @Test
    @DisplayName("API GET /api/v1/jobs/{jobCode}/telemetry lấy dữ liệu telemetry từ Render Gateway")
    void testGetJobTelemetry_Success() throws Exception {
        when(jobService.getJobTelemetryFromGateway("JOB-20261020-A1B2C3D4")).thenReturn(java.util.Map.of(
                "jobCode", "JOB-20261020-A1B2C3D4",
                "progressPercent", 75.0,
                "x", 50.5,
                "y", 25.0
        ));

        mockMvc.perform(get("/api/v1/jobs/JOB-20261020-A1B2C3D4/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.progressPercent").value(75.0))
                .andExpect(jsonPath("$.data.x").value(50.5));
    }

    @Test
    @DisplayName("API POST /api/v1/jobs/{jobCode}/command gửi lệnh điều khiển máy")
    void testSendJobCommand_Success() throws Exception {
        when(jobService.getJobByCode("JOB-20261020-A1B2C3D4")).thenReturn(sampleResponse);
        when(jobService.sendDeviceCommand(eq("LASER-HN-001"), eq("PAUSE"), eq("JOB-20261020-A1B2C3D4"), any()))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/jobs/JOB-20261020-A1B2C3D4/command")
                        .param("action", "PAUSE")
                        .param("reason", "Khói bụi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PAUSE")));
    }
}
