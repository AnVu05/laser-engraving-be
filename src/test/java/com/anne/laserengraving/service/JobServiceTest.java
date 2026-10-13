package com.anne.laserengraving.service;

import com.anne.laserengraving.core.gcode.GcodeSafetyValidator;
import com.anne.laserengraving.dto.request.JobDispatchRequest;
import com.anne.laserengraving.dto.response.JobDispatchResponse;
import com.anne.laserengraving.dto.websocket.JobNotificationMessage;
import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.entity.EngravingJobEntity;
import com.anne.laserengraving.exception.LaserSafetyException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.DeviceRepository;
import com.anne.laserengraving.repository.EngravingJobRepository;
import com.anne.laserengraving.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private EngravingJobRepository jobRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GcodeSafetyValidator safetyValidator;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private JobServiceImpl jobService;

    private DeviceEntity testDevice;
    private String validGcodeContent;

    @BeforeEach
    void setUp() {
        testDevice = DeviceEntity.builder()
                .id(1)
                .deviceCode("LASER-HN-001")
                .deviceName("Máy Khắc Laser Makerbase MKS DLC32 #01")
                .connectionStatus("ONLINE")
                .maxWidthMm(300.0)
                .maxHeightMm(200.0)
                .maxSpeedMmMin(1500)
                .maxPowerS(1000)
                .apiKeyHash("$2a$10$encodedDefaultTokenHash")
                .isActive(true)
                .build();

        validGcodeContent = """
                G21
                G90
                $32=1
                M5
                G0 X10.0 Y10.0
                G1 X20.0 Y10.0 F1500 M3 S800
                M5
                G0 X0 Y0
                M2
                """;
    }

    @Test
    @DisplayName("Điều phối Job thành công: Sinh SHA-256, lưu DB và bắn WebSocket")
    void testDispatchJob_Success() {
        JobDispatchRequest request = JobDispatchRequest.builder()
                .deviceCode("LASER-HN-001")
                .originalFilename("logo.gcode")
                .targetWidthMm(100.0)
                .targetHeightMm(100.0)
                .feedRate(1500)
                .laserPower(800)
                .materialType("Gỗ Plywood")
                .build();

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.gcode", "text/plain", validGcodeContent.getBytes(StandardCharsets.UTF_8));

        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(testDevice));
        when(fileStorageService.storeGcodeFile(anyString(), anyString())).thenReturn("stored_logo.gcode");
        when(jobRepository.save(any(EngravingJobEntity.class))).thenAnswer(invocation -> {
            EngravingJobEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        JobDispatchResponse response = jobService.dispatchJob(request, file);

        assertNotNull(response);
        assertEquals(100L, response.getJobId());
        assertTrue(response.getJobCode().startsWith("JOB-"));
        assertEquals("LASER-HN-001", response.getDeviceCode());
        assertEquals("logo.gcode", response.getOriginalFilename());
        assertEquals(64, response.getSha256Checksum().length());
        assertEquals("DISPATCHED", response.getStatus());

        // Kiểm tra đã broadcast qua WebSocket STOMP
        verify(messagingTemplate).convertAndSend(eq("/topic/device/LASER-HN-001/jobs"), any(JobNotificationMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/client/jobs"), any(JobNotificationMessage.class));
    }

    @Test
    @DisplayName("Điều phối Job qua JSON body chuỗi G-code thành công")
    void testDispatchJobJson_Success() {
        JobDispatchRequest request = JobDispatchRequest.builder()
                .deviceCode("LASER-HN-001")
                .originalFilename("logo.gcode")
                .gcodeContent(validGcodeContent)
                .targetWidthMm(100.0)
                .targetHeightMm(100.0)
                .feedRate(1500)
                .laserPower(800)
                .materialType("Gỗ Plywood")
                .build();

        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(testDevice));
        when(fileStorageService.storeGcodeFile(anyString(), anyString())).thenReturn("stored_logo.gcode");
        when(jobRepository.save(any(EngravingJobEntity.class))).thenAnswer(invocation -> {
            EngravingJobEntity entity = invocation.getArgument(0);
            entity.setId(101L);
            return entity;
        });

        JobDispatchResponse response = jobService.dispatchJob(request);

        assertNotNull(response);
        assertEquals(101L, response.getJobId());
        assertEquals("LASER-HN-001", response.getDeviceCode());
        verify(messagingTemplate).convertAndSend(eq("/topic/device/LASER-HN-001/jobs"), any(JobNotificationMessage.class));
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi điều phối tới thiết bị không tồn tại")
    void testDispatchJob_DeviceNotFound() {
        JobDispatchRequest request = JobDispatchRequest.builder()
                .deviceCode("NON-EXISTENT-DEVICE")
                .gcodeContent(validGcodeContent)
                .build();

        when(deviceRepository.findByDeviceCode("NON-EXISTENT-DEVICE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.dispatchJob(request));
    }

    @Test
    @DisplayName("Ném LaserSafetyException khi thông số hoặc G-code vi phạm an toàn")
    void testDispatchJob_SafetyViolation() {
        JobDispatchRequest request = JobDispatchRequest.builder()
                .deviceCode("LASER-HN-001")
                .gcodeContent(validGcodeContent)
                .targetWidthMm(350.0) // Vượt quá khổ 300
                .targetHeightMm(100.0)
                .build();

        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(testDevice));
        doThrow(new LaserSafetyException("Vùng khắc vượt quá khổ làm việc an toàn của máy"))
                .when(safetyValidator).validateParameters(eq(350.0), eq(100.0), anyInt(), anyInt(), anyInt());

        assertThrows(LaserSafetyException.class, () -> jobService.dispatchJob(request));
        verify(jobRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tải file G-code bảo mật thành công khi xác thực đúng Bearer Token")
    void testDownloadJobFile_Success() {
        EngravingJobEntity job = EngravingJobEntity.builder()
                .id(100L)
                .jobCode("JOB-20261020-TEST")
                .device(testDevice)
                .originalFilename("logo.gcode")
                .storedFilePath("stored_logo.gcode")
                .sha256Checksum("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(fileStorageService.loadGcodeFileAsResource("stored_logo.gcode"))
                .thenReturn(new ByteArrayResource(validGcodeContent.getBytes(StandardCharsets.UTF_8)));

        // Gửi Bearer token mặc định
        Resource resource = jobService.downloadJobFile(100L, "Bearer mk_sec_laser_hn_001_secret", "LASER-HN-001");

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    @DisplayName("Ném SecurityException khi thiết bị gửi sai Bearer Token")
    void testDownloadJobFile_InvalidToken_Throws() {
        EngravingJobEntity job = EngravingJobEntity.builder()
                .id(100L)
                .jobCode("JOB-20261020-TEST")
                .device(testDevice)
                .storedFilePath("stored_logo.gcode")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));
        when(passwordEncoder.matches(eq("wrong_token"), anyString())).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                jobService.downloadJobFile(100L, "Bearer wrong_token", "LASER-HN-001"));
    }

    @Test
    @DisplayName("Ném SecurityException khi header X-Device-Code không khớp thiết bị đích")
    void testDownloadJobFile_MismatchDeviceCode_Throws() {
        EngravingJobEntity job = EngravingJobEntity.builder()
                .id(100L)
                .jobCode("JOB-20261020-TEST")
                .device(testDevice)
                .storedFilePath("stored_logo.gcode")
                .build();

        when(jobRepository.findById(100L)).thenReturn(Optional.of(job));

        assertThrows(SecurityException.class, () ->
                jobService.downloadJobFile(100L, "Bearer mk_sec_laser_hn_001_secret", "WRONG-DEVICE-002"));
    }

    @Test
    @DisplayName("Hủy bỏ Job thành công và bắn thông báo cập nhật")
    void testCancelJob_Success() {
        EngravingJobEntity job = EngravingJobEntity.builder()
                .id(100L)
                .jobCode("JOB-20261020-TEST")
                .device(testDevice)
                .status("DISPATCHED")
                .build();

        when(jobRepository.findByJobCode("JOB-20261020-TEST")).thenReturn(Optional.of(job));

        jobService.cancelJob("JOB-20261020-TEST");

        assertEquals("CANCELLED", job.getStatus());
        verify(jobRepository).save(job);
        verify(messagingTemplate).convertAndSend("/topic/jobs/JOB-20261020-TEST/status", "CANCELLED");
    }
}
