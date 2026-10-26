package com.anne.laserengraving.service;

import com.anne.laserengraving.core.gcode.GcodeSafetyValidator;
import com.anne.laserengraving.dto.request.JobDispatchRequest;
import com.anne.laserengraving.dto.response.JobDispatchResponse;
import com.anne.laserengraving.dto.websocket.JobNotificationMessage;
import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.entity.EngravingJobEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.InvalidParameterException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.DeviceRepository;
import com.anne.laserengraving.repository.EngravingJobRepository;
import com.anne.laserengraving.repository.UserRepository;
import com.anne.laserengraving.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private static final Pattern GCODE_PARAMETER = Pattern.compile("(?i)([XYFS])\\s*(-?\\d+(?:\\.\\d+)?)");

    private final EngravingJobRepository jobRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final GcodeSafetyValidator safetyValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final PasswordEncoder passwordEncoder;
    private final RenderIotGatewayClient renderGatewayClient;

    @Override
    @Transactional
    public JobDispatchResponse dispatchJob(JobDispatchRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidParameterException("Tệp G-code tải lên không được để trống");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidParameterException("Không thể đọc tệp G-code tải lên: " + e.getMessage());
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = (request.getOriginalFilename() != null && !request.getOriginalFilename().isBlank())
                    ? request.getOriginalFilename() : "logo.gcode";
        }

        return processAndDispatch(request, filename, fileBytes);
    }

    @Override
    @Transactional
    public JobDispatchResponse dispatchJob(JobDispatchRequest request) {
        if (request.getGcodeContent() == null || request.getGcodeContent().isBlank()) {
            throw new InvalidParameterException("Nội dung chuỗi G-code (gcodeContent) không được để trống");
        }

        byte[] fileBytes = request.getGcodeContent().getBytes(StandardCharsets.UTF_8);
        String filename = (request.getOriginalFilename() != null && !request.getOriginalFilename().isBlank())
                ? request.getOriginalFilename() : "logo.gcode";

        return processAndDispatch(request, filename, fileBytes);
    }

    private JobDispatchResponse processAndDispatch(JobDispatchRequest request, String filename, byte[] fileBytes) {
        // 1. Kiểm tra thiết bị đích
        DeviceEntity device = deviceRepository.findByDeviceCode(request.getDeviceCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị có mã: " + request.getDeviceCode()));

        if (Boolean.FALSE.equals(device.getIsActive())) {
            throw new InvalidParameterException("Thiết bị " + request.getDeviceCode() + " hiện đang bị vô hiệu hóa.");
        }

        // 2. Tính toán mã băm SHA-256 bảo mật
        String sha256Checksum = CryptoUtil.calculateSha256(fileBytes);
        String gcodeContent = new String(fileBytes, StandardCharsets.UTF_8);

        // 3. Phân tích số dòng lệnh & Tiền kiểm tra an toàn G-code
        List<String> lines = gcodeContent.lines().toList();
        int totalLines = (int) lines.stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith(";") && !l.startsWith("("))
                .count();

        double width = request.getTargetWidthMm() != null ? request.getTargetWidthMm() : 100.0;
        double height = request.getTargetHeightMm() != null ? request.getTargetHeightMm() : 100.0;
        int speed = request.getFeedRate() != null ? request.getFeedRate() : 1500;
        int power = request.getLaserPower() != null ? request.getLaserPower() : 800;

        if (width > device.getMaxWidthMm() || height > device.getMaxHeightMm()
                || speed > device.getMaxSpeedMmMin() || power > device.getMaxPowerS()) {
            throw new InvalidParameterException("Thông số job vượt quá giới hạn cấu hình của thiết bị " + device.getDeviceCode());
        }

        // Tiền kiểm định an toàn theo chuẩn máy
        safetyValidator.validateParameters(width, height, speed, power, 254);
        safetyValidator.validateGcodeLines(lines);
        validateDeviceGcodeLimits(lines, device);

        // 4. Lưu trữ tệp vật lý vào FileStorage
        String storedFilePath = fileStorageService.storeGcodeFile(filename, gcodeContent);

        // 5. Sinh mã Job Code độc nhất: JOB-yyyyMMdd-XXXXX
        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String jobCode = String.format("JOB-%s-%s", dateStr, randomSuffix);

        // Người dùng (nếu có)
        UserEntity user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        // 6. Lưu bản ghi vào bảng Engraving_Jobs
        EngravingJobEntity job = EngravingJobEntity.builder()
                .jobCode(jobCode)
                .device(device)
                .user(user)
                .originalFilename(filename)
                .gcodeFilename(storedFilePath)
                .storedFilePath(storedFilePath)
                .fileSizeBytes((long) fileBytes.length)
                .sha256Checksum(sha256Checksum)
                .targetWidthMm(width)
                .targetHeightMm(height)
                .feedRate(speed)
                .laserPower(power)
                .totalLines(totalLines)
                .status("DISPATCHED")
                .currentProgress(0.0)
                .materialType(request.getMaterialType() != null ? request.getMaterialType() : "Gỗ Plywood")
                .dispatchedAt(LocalDateTime.now())
                .build();

        job = jobRepository.save(job);
        log.info("[DISPATCH] Đã tạo Job {}: Thiết bị {}, File: {}, Size: {} bytes, SHA-256: {}",
                job.getJobCode(), device.getDeviceCode(), job.getOriginalFilename(), job.getFileSizeBytes(), sha256Checksum);

        // 7. Bắn thông báo đẩy WebSocket tức thời tới bo mạch và Web Client
        String downloadUrl = "/api/v1/jobs/" + job.getId() + "/download";
        JobNotificationMessage notification = JobNotificationMessage.builder()
                .event("NEW_JOB_AVAILABLE")
                .jobId(job.getId())
                .jobCode(job.getJobCode())
                .fileName(job.getOriginalFilename())
                .fileSizeBytes(job.getFileSizeBytes())
                .sha256Checksum(sha256Checksum)
                .downloadUrl(downloadUrl)
                .targetWidthMm(job.getTargetWidthMm())
                .targetHeightMm(job.getTargetHeightMm())
                .feedRate(job.getFeedRate())
                .laserPower(job.getLaserPower())
                .build();

        // Kênh riêng của bo mạch MKS DLC32: /topic/device/{deviceCode}/jobs
        messagingTemplate.convertAndSend("/topic/device/" + device.getDeviceCode() + "/jobs", notification);

        // Kênh chung Web Client Studio: /topic/client/jobs
        messagingTemplate.convertAndSend("/topic/client/jobs", notification);

        // 8. Chuyển tiếp Job sang Render IoT Gateway (nếu có cấu hình)
        try {
            renderGatewayClient.dispatchJobToGateway(
                    job.getJobCode(),
                    device.getDeviceCode(),
                    job.getOriginalFilename(),
                    gcodeContent,
                    job.getTargetWidthMm(),
                    job.getTargetHeightMm(),
                    job.getFeedRate(),
                    job.getLaserPower(),
                    job.getMaterialType()
            );
        } catch (Exception e) {
            log.warn("[DISPATCH] Bỏ qua lỗi chuyển tiếp sang Render Gateway: {}", e.getMessage());
        }

        return mapToResponse(job, downloadUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadJobFile(Long jobId, String authHeader, String deviceCodeHeader) {
        EngravingJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có ID: " + jobId));

        authenticateDeviceAccess(job.getDevice(), authHeader, deviceCodeHeader);

        return fileStorageService.loadGcodeFileAsResource(job.getStoredFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadJobFileByCode(String jobCode, String authHeader, String deviceCodeHeader) {
        EngravingJobEntity job = jobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có mã: " + jobCode));

        authenticateDeviceAccess(job.getDevice(), authHeader, deviceCodeHeader);

        return fileStorageService.loadGcodeFileAsResource(job.getStoredFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public JobDispatchResponse getJobById(Long jobId) {
        EngravingJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có ID: " + jobId));
        return mapToResponse(job, "/api/v1/jobs/" + job.getId() + "/download");
    }

    @Override
    @Transactional(readOnly = true)
    public JobDispatchResponse getJobByCode(String jobCode) {
        EngravingJobEntity job = jobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có mã: " + jobCode));
        return mapToResponse(job, "/api/v1/jobs/" + job.getId() + "/download");
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDispatchResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(j -> mapToResponse(j, "/api/v1/jobs/" + j.getId() + "/download"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDispatchResponse> getJobsByDevice(String deviceCode) {
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị có mã: " + deviceCode));
        return jobRepository.findByDeviceIdOrderByDispatchedAtDesc(device.getId()).stream()
                .map(j -> mapToResponse(j, "/api/v1/jobs/" + j.getId() + "/download"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelJob(String jobCode) {
        EngravingJobEntity job = jobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có mã: " + jobCode));

        job.setStatus("CANCELLED");
        jobRepository.save(job);

        // Bắn thông báo cập nhật trạng thái
        messagingTemplate.convertAndSend("/topic/jobs/" + jobCode + "/status", "CANCELLED");
        log.info("[CANCEL] Đã hủy bỏ Job: {}", jobCode);

        // Chuyển tiếp lệnh dừng xuống bo mạch qua Render Gateway
        try {
            String devCode = job.getDevice() != null ? job.getDevice().getDeviceCode() : "LASER-HN-001";
            renderGatewayClient.sendDeviceCommand(devCode, "CANCEL", jobCode, "User cancelled from Web Studio");
        } catch (Exception e) {
            log.warn("[CANCEL] Bỏ qua lỗi gửi lệnh CANCEL sang Render Gateway: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void confirmJob(String jobCode) {
        EngravingJobEntity job = jobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có mã: " + jobCode));

        job.setStatus("PRINTING");
        if (job.getStartedAt() == null) job.setStartedAt(java.time.LocalDateTime.now());
        if (job.getConfirmedAt() == null) job.setConfirmedAt(java.time.LocalDateTime.now());
        jobRepository.save(job);

        String deviceCode = job.getDevice() != null ? job.getDevice().getDeviceCode() : "LASER-HN-001";
        if (job.getDevice() != null) {
            job.getDevice().setConnectionStatus("BUSY");
            deviceRepository.save(job.getDevice());
        }

        com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage statusMsg =
                com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage.builder()
                        .type("JOB_STATUS_CHANGE")
                        .deviceCode(deviceCode)
                        .jobCode(jobCode)
                        .status("PRINTING")
                        .message("Quản máy đã xác nhận an toàn vật liệu. Bắt đầu phát tia khắc!")
                        .timestamp(System.currentTimeMillis())
                        .build();

        // Bắn thông báo tới thiết bị và các kênh Client
        messagingTemplate.convertAndSend("/topic/device/" + deviceCode + "/jobs", statusMsg);
        messagingTemplate.convertAndSend("/topic/jobs/" + jobCode + "/status", statusMsg);
        messagingTemplate.convertAndSend("/topic/client/status", statusMsg);

        log.info("[CONFIRM] Quản máy đã phê duyệt an toàn Job: {} trên thiết bị: {}", jobCode, deviceCode);
    }

    private void authenticateDeviceAccess(DeviceEntity device, String authHeader, String deviceCodeHeader) {
        if (!Boolean.TRUE.equals(device.getIsActive())) {
            throw new SecurityException("Thiết bị đã bị vô hiệu hóa.");
        }
        if (deviceCodeHeader == null || deviceCodeHeader.isBlank()) {
            throw new SecurityException("Thiếu mã thiết bị X-Device-Code.");
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.substring(7).isBlank()) {
            throw new SecurityException("Thiếu Bearer token của thiết bị.");
        }
        // Kiểm tra Device Code header nếu có gửi kèm
        if (deviceCodeHeader != null && !deviceCodeHeader.isBlank()) {
            if (!device.getDeviceCode().equalsIgnoreCase(deviceCodeHeader.trim())) {
                log.warn("[AUTH DEVICE] Header X-Device-Code '{}' không khớp với máy của Job '{}'",
                        deviceCodeHeader, device.getDeviceCode());
                throw new SecurityException("Mã thiết bị X-Device-Code không khớp với thiết bị đích của Job.");
            }
        }

        // Kiểm tra Bearer Token nếu có Authorization header
        if (authHeader != null && !authHeader.isBlank()) {
            String token = authHeader.startsWith("Bearer ")
                    ? authHeader.substring(7).trim()
                    : authHeader.trim();

            boolean isValid = passwordEncoder.matches(token, device.getApiKeyHash());
            if (!isValid) {
                log.warn("[AUTH DEVICE] Token '{}' không hợp lệ cho thiết bị '{}'", token, device.getDeviceCode());
                throw new SecurityException("Khóa bảo mật Bearer Token của thiết bị không chính xác.");
            }
        }
    }

    private void validateDeviceGcodeLimits(List<String> lines, DeviceEntity device) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).split(";", 2)[0].trim();
            if (line.isEmpty()) continue;
            boolean rapidMove = line.matches("(?i)^G0\\b.*");
            Matcher matcher = GCODE_PARAMETER.matcher(line);
            while (matcher.find()) {
                char parameter = Character.toUpperCase(matcher.group(1).charAt(0));
                double value = Double.parseDouble(matcher.group(2));
                boolean unsafe = switch (parameter) {
                    case 'X' -> value < 0 || value > device.getMaxWidthMm();
                    case 'Y' -> value < 0 || value > device.getMaxHeightMm();
                    case 'F' -> !rapidMove && value > device.getMaxSpeedMmMin();
                    case 'S' -> value < 0 || value > device.getMaxPowerS();
                    default -> false;
                };
                if (unsafe) {
                    throw new InvalidParameterException("G-code dòng " + (index + 1)
                            + " vượt giới hạn cấu hình của máy " + device.getDeviceCode() + " (" + parameter + value + ").");
                }
            }
        }
    }

    private JobDispatchResponse mapToResponse(EngravingJobEntity entity, String downloadUrl) {
        return JobDispatchResponse.builder()
                .jobId(entity.getId())
                .jobCode(entity.getJobCode())
                .deviceCode(entity.getDevice() != null ? entity.getDevice().getDeviceCode() : null)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getDeviceName() : null)
                .originalFilename(entity.getOriginalFilename())
                .fileSizeBytes(entity.getFileSizeBytes())
                .sha256Checksum(entity.getSha256Checksum())
                .targetWidthMm(entity.getTargetWidthMm())
                .targetHeightMm(entity.getTargetHeightMm())
                .feedRate(entity.getFeedRate())
                .laserPower(entity.getLaserPower())
                .totalLines(entity.getTotalLines())
                .status(entity.getStatus())
                .currentProgress(entity.getCurrentProgress())
                .materialType(entity.getMaterialType())
                .downloadUrl(downloadUrl)
                .dispatchedAt(entity.getDispatchedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getJobTelemetryFromGateway(String jobCode) {
        EngravingJobEntity job = jobRepository.findByJobCode(jobCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Job có mã: " + jobCode));

        String devCode = job.getDevice() != null ? job.getDevice().getDeviceCode() : "LASER-HN-001";
        return renderGatewayClient.getDeviceTelemetry(devCode)
                .orElse(java.util.Map.of(
                        "jobCode", jobCode,
                        "deviceCode", devCode,
                        "progressPercent", job.getCurrentProgress() != null ? job.getCurrentProgress() : 0.0,
                        "status", job.getStatus()
                ));
    }

    @Override
    public boolean sendDeviceCommand(String deviceCode, String action, String jobCode, String reason) {
        return renderGatewayClient.sendDeviceCommand(deviceCode, action, jobCode, reason);
    }
}
