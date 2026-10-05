package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.GcodeGenerateRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.GcodeGenerateResponse;
import com.anne.laserengraving.service.GcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gcode")
@RequiredArgsConstructor
@Tag(name = "2. G-Code Generation (Xử lý ảnh & Sinh mã)", description = "Các API biên dịch hình ảnh tải lên thành mã G-code chuẩn GRBL ($32=1) và tải tệp .nc")
public class GcodeController {

    private final GcodeService gcodeService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Biên dịch hình ảnh và sinh mã G-code GRBL",
            description = "Nhận tệp ảnh cùng các thông số kích thước (W, H), toạ độ (X, Y), tốc độ Feed rate, công suất laser và DPI. Ứng dụng thuật toán Floyd-Steinberg Dithering để sinh mã G-code tối ưu."
    )
    public ResponseEntity<ApiResponse<GcodeGenerateResponse>> generateGcode(
            @ModelAttribute @Valid GcodeGenerateRequest request,
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId) {

        GcodeGenerateResponse response = gcodeService.generateGcode(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Sinh mã G-code thành công"));
    }

    @GetMapping("/download/{historyId}")
    @Operation(
            summary = "Tải về tệp G-code (.nc)",
            description = "Tải về tệp chương trình gia công G-code định dạng .nc theo mã định danh lịch sử."
    )
    public ResponseEntity<Resource> downloadGcode(
            @Parameter(description = "ID bản ghi lịch sử gia công")
            @PathVariable("historyId") Long historyId) {
        Resource resource = gcodeService.downloadGcodeFile(historyId);

        String filename = resource.getFilename();
        if (filename == null) {
            filename = "laser_engraving_" + historyId + ".nc";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
