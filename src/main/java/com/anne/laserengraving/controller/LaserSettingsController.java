package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.LaserSettingsUpdateRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.LaserSettingsResponse;
import com.anne.laserengraving.service.LaserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "4. Laser Settings (Cấu hình máy khắc)", description = "Các API truy xuất và cập nhật thông số máy khắc (tốc độ Feed rate, công suất PWM, DPI, kích thước bàn làm việc)")
public class LaserSettingsController {

    private final LaserSettingsService settingsService;

    @GetMapping
    @Operation(
            summary = "Lấy cấu hình tham số máy khắc hiện tại",
            description = "Trả về thông số cấu hình mặc định hoặc cấu hình riêng của người vận hành."
    )
    public ResponseEntity<ApiResponse<LaserSettingsResponse>> getSettings(
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId) {
        LaserSettingsResponse settings = settingsService.getCurrentSettings(userId);
        return ResponseEntity.ok(ApiResponse.ok(settings, "Lấy cấu hình máy khắc thành công"));
    }

    @PutMapping
    @Operation(
            summary = "Cập nhật cấu hình tham số máy khắc",
            description = "Lưu thiết lập mới về tốc độ, công suất và giới hạn cơ khí cho máy khắc laser."
    )
    public ResponseEntity<ApiResponse<LaserSettingsResponse>> updateSettings(
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestBody @Valid LaserSettingsUpdateRequest request) {
        LaserSettingsResponse updated = settingsService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Cập nhật cấu hình máy khắc thành công"));
    }
}
