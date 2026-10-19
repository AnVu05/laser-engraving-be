package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.JobDispatchRequest;
import com.anne.laserengraving.dto.response.JobDispatchResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface JobService {

    /**
     * Điều phối Job khắc laser tới thiết bị IoT kèm tệp G-code tải lên (Multipart)
     */
    JobDispatchResponse dispatchJob(JobDispatchRequest request, MultipartFile file);

    /**
     * Điều phối Job khắc laser tới thiết bị IoT từ chuỗi G-code (JSON Body)
     */
    JobDispatchResponse dispatchJob(JobDispatchRequest request);

    /**
     * Tải tệp G-code an toàn theo jobId kèm xác thực thiết bị và kiểm tra SHA-256
     */
    Resource downloadJobFile(Long jobId, String authHeader, String deviceCodeHeader);

    /**
     * Tải tệp G-code an toàn theo jobCode kèm xác thực thiết bị và kiểm tra SHA-256
     */
    Resource downloadJobFileByCode(String jobCode, String authHeader, String deviceCodeHeader);

    /**
     * Lấy thông tin chi tiết một Job theo ID
     */
    JobDispatchResponse getJobById(Long jobId);

    /**
     * Lấy thông tin chi tiết một Job theo Job Code
     */
    JobDispatchResponse getJobByCode(String jobCode);

    /**
     * Lấy danh sách toàn bộ các Jobs đã điều phối trong hệ thống
     */
    List<JobDispatchResponse> getAllJobs();

    /**
     * Lấy danh sách Jobs theo mã thiết bị (VD: LASER-HN-001)
     */
    List<JobDispatchResponse> getJobsByDevice(String deviceCode);

    /**
     * Hủy bỏ Job
     */
    void cancelJob(String jobCode);

    /**
     * Xác nhận an toàn vật liệu tại chỗ bởi Quản máy / Admin
     */
    void confirmJob(String jobCode);
}
