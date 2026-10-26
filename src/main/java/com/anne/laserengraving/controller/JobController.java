package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.JobDispatchRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.JobDispatchResponse;
import com.anne.laserengraving.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "3. IoT Job Dispatching (Điều phối máy khắc & Bảo mật SHA-256)",
     description = "Các API điều phối lệnh gia công tới máy khắc Laser Makerbase MKS DLC32 V2.1 và tải tệp G-code bảo mật qua HTTPS")
public class JobController {

    private final JobService jobService;

    @PostMapping(value = "/dispatch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Điều phối Job khắc laser tới thiết bị IoT (Tải tệp tệp G-code lên)",
            description = "Tiếp nhận tệp G-code (logo.gcode), thiết bị đích (LASER-HN-001) và các tham số kích thước. Tự động tính toán mã băm SHA-256 phần cứng và bắn thông báo tức thời qua WebSocket tới bo mạch MKS DLC32."
    )
    public ResponseEntity<ApiResponse<JobDispatchResponse>> dispatchJobMultipart(
            @ModelAttribute @Valid JobDispatchRequest request,
            @Parameter(description = "Tệp chương trình gia công G-code (.nc, .gcode)")
            @RequestPart(value = "file", required = true) MultipartFile file) {

        long startTime = System.currentTimeMillis();
        JobDispatchResponse response = jobService.dispatchJob(request, file);
        long duration = System.currentTimeMillis() - startTime;
        log.info("[JOB DISPATCH] Hoàn thành điều phối Job {} trong {} ms", response.getJobCode(), duration);

        return ResponseEntity.ok(ApiResponse.ok(response, "Điều phối Job tới thiết bị thành công trong " + duration + " ms"));
    }

    @PostMapping(value = "/dispatch", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Điều phối Job khắc laser tới thiết bị IoT (Chuỗi G-code trực tiếp)",
            description = "Tiếp nhận nội dung chuỗi G-code dạng text thô trong JSON body mà không cần gửi dạng multipart file."
    )
    public ResponseEntity<ApiResponse<JobDispatchResponse>> dispatchJobJson(
            @RequestBody @Valid JobDispatchRequest request) {

        long startTime = System.currentTimeMillis();
        JobDispatchResponse response = jobService.dispatchJob(request);
        long duration = System.currentTimeMillis() - startTime;
        log.info("[JOB DISPATCH JSON] Hoàn thành điều phối Job {} trong {} ms", response.getJobCode(), duration);

        return ResponseEntity.ok(ApiResponse.ok(response, "Điều phối Job tới thiết bị thành công trong " + duration + " ms"));
    }

    @GetMapping("/{jobId}/download")
    @Operation(
            summary = "Tải tệp G-code bảo mật theo Job ID kèm xác thực Bearer Token thiết bị",
            description = "Cho phép bo mạch Makerbase MKS DLC32 hoặc Client tải file G-code. Trả về luồng file kèm Header X-File-SHA256 để bo mạch đối soát tính toàn vẹn phần cứng."
    )
    public ResponseEntity<Resource> downloadJobById(
            @Parameter(description = "ID bản ghi Job trong CSDL")
            @PathVariable("jobId") Long jobId,
            @Parameter(description = "Mã xác thực Bearer Token của thiết bị")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Parameter(description = "Mã thiết bị gửi yêu cầu tải (VD: LASER-HN-001)")
            @RequestHeader(value = "X-Device-Code", required = false) String deviceCodeHeader) {

        JobDispatchResponse job = jobService.getJobById(jobId);
        Resource resource = jobService.downloadJobFile(jobId, authHeader, deviceCodeHeader);

        String filename = job.getOriginalFilename() != null ? job.getOriginalFilename() : "laser_job_" + jobId + ".gcode";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header("X-File-SHA256", job.getSha256Checksum())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/code/{jobCode}/download")
    @Operation(
            summary = "Tải tệp G-code bảo mật theo Job Code kèm xác thực Bearer Token",
            description = "Tải file theo mã Job Code (VD: JOB-20261020-001). Trả về luồng file kèm Header X-File-SHA256."
    )
    public ResponseEntity<Resource> downloadJobByCode(
            @Parameter(description = "Mã định danh Job (VD: JOB-20261020-A1B2C3D4)")
            @PathVariable("jobCode") String jobCode,
            @Parameter(description = "Mã xác thực Bearer Token của thiết bị")
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Parameter(description = "Mã thiết bị gửi yêu cầu tải")
            @RequestHeader(value = "X-Device-Code", required = false) String deviceCodeHeader) {

        JobDispatchResponse job = jobService.getJobByCode(jobCode);
        Resource resource = jobService.downloadJobFileByCode(jobCode, authHeader, deviceCodeHeader);

        String filename = job.getOriginalFilename() != null ? job.getOriginalFilename() : jobCode + ".gcode";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header("X-File-SHA256", job.getSha256Checksum())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Tra cứu thông tin chi tiết của một Job theo ID")
    public ResponseEntity<ApiResponse<JobDispatchResponse>> getJobById(@PathVariable("jobId") Long jobId) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getJobById(jobId), "Lấy thông tin Job thành công"));
    }

    @GetMapping("/code/{jobCode}")
    @Operation(summary = "Tra cứu thông tin chi tiết của một Job theo Job Code")
    public ResponseEntity<ApiResponse<JobDispatchResponse>> getJobByCode(@PathVariable("jobCode") String jobCode) {
        return ResponseEntity.ok(ApiResponse.ok(jobService.getJobByCode(jobCode), "Lấy thông tin Job thành công"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các Jobs trong hệ thống (Hỗ trợ lọc theo thiết bị)")
    public ResponseEntity<ApiResponse<List<JobDispatchResponse>>> getJobs(
            @Parameter(description = "Lọc theo mã thiết bị (nếu có)")
            @RequestParam(value = "deviceCode", required = false) String deviceCode) {

        List<JobDispatchResponse> jobs = (deviceCode != null && !deviceCode.isBlank())
                ? jobService.getJobsByDevice(deviceCode)
                : jobService.getAllJobs();

        return ResponseEntity.ok(ApiResponse.ok(jobs, "Lấy danh sách Jobs thành công"));
    }

    @PostMapping("/{jobCode}/cancel")
    @Operation(summary = "Hủy bỏ một Job gia công")
    public ResponseEntity<ApiResponse<Void>> cancelJob(@PathVariable("jobCode") String jobCode) {
        jobService.cancelJob(jobCode);
        return ResponseEntity.ok(ApiResponse.ok(null, "Hủy Job thành công"));
    }

    @PostMapping("/{jobCode}/confirm")
    @Operation(summary = "Xác nhận an toàn vật liệu tại chỗ bởi Quản máy / Admin")
    public ResponseEntity<ApiResponse<Void>> confirmJob(@PathVariable("jobCode") String jobCode) {
        jobService.confirmJob(jobCode);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xác nhận an toàn Job thành công"));
    }

    @GetMapping("/{jobCode}/telemetry")
    @Operation(summary = "Lấy telemetry thời gian thực của Job từ Render IoT Gateway (tiến độ, toạ độ X/Y, cảm biến)")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getJobTelemetry(@PathVariable("jobCode") String jobCode) {
        java.util.Map<String, Object> telemetry = jobService.getJobTelemetryFromGateway(jobCode);
        return ResponseEntity.ok(ApiResponse.ok(telemetry, "Lấy telemetry thành công"));
    }

    @PostMapping("/{jobCode}/command")
    @Operation(summary = "Gửi lệnh điều khiển tức thời (PAUSE, RESUME, CANCEL, HOME) xuống máy qua Render Gateway")
    public ResponseEntity<ApiResponse<Void>> sendJobCommand(
            @PathVariable("jobCode") String jobCode,
            @RequestParam("action") String action,
            @RequestParam(value = "reason", required = false) String reason) {
        JobDispatchResponse job = jobService.getJobByCode(jobCode);
        String deviceCode = job.getDeviceCode() != null ? job.getDeviceCode() : "LASER-HN-001";
        boolean sent = jobService.sendDeviceCommand(deviceCode, action, jobCode, reason);
        if (sent) {
            return ResponseEntity.ok(ApiResponse.ok(null, "Đã gửi lệnh " + action + " xuống thiết bị thành công"));
        } else {
            return ResponseEntity.ok(ApiResponse.ok(null, "Lệnh " + action + " đã được ghi nhận cục bộ"));
        }
    }
}
