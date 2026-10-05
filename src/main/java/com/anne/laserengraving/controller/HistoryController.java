package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.SaveHistoryRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.HistoryItemResponse;
import com.anne.laserengraving.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "3. History (Lịch sử gia công)", description = "Các API lưu lịch sử gia công từ Frontend/Web Serial, xem danh sách, tải G-code và xoá lịch sử")
public class HistoryController {

    private final HistoryService historyService;

    @PostMapping
    @Operation(
            summary = "Lưu lịch sử gia công từ Frontend",
            description = "Ghi nhận kết quả phiên khắc laser (G-code, tốc độ, công suất, trạng thái) sau khi gia công trực tiếp từ trình duyệt vào CSDL SQL Server."
    )
    public ResponseEntity<ApiResponse<HistoryItemResponse>> saveHistory(
            @Parameter(description = "Gmail của tài khoản người dùng (nếu có)")
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId,
            @Valid @RequestBody SaveHistoryRequest request) {

        HistoryItemResponse response = historyService.saveHistory(email, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Lưu lịch sử gia công thành công!"));
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách lịch sử gia công có phân trang",
            description = "Trả về danh sách các tác vụ khắc laser đã thực hiện theo người dùng với số trang và kích thước trang."
    )
    public ResponseEntity<ApiResponse<Page<HistoryItemResponse>>> getHistory(
            @Parameter(description = "Gmail người dùng (nếu có)")
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId,
            @Parameter(description = "Chỉ số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HistoryItemResponse> result = historyService.getHistory(userId, email, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result, "Lấy danh sách lịch sử thành công"));
    }

    @GetMapping("/{historyId}")
    @Operation(
            summary = "Xem chi tiết một bản ghi lịch sử",
            description = "Lấy thông tin chi tiết của một tệp G-code đã sinh bao gồm toạ độ, tốc độ, công suất và số dòng lệnh."
    )
    public ResponseEntity<ApiResponse<HistoryItemResponse>> getHistoryDetail(
            @Parameter(description = "ID bản ghi lịch sử")
            @PathVariable("historyId") Long historyId,
            @Parameter(description = "Gmail người dùng (nếu có)")
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId) {

        HistoryItemResponse detail = historyService.getHistoryDetail(historyId, userId, email);
        return ResponseEntity.ok(ApiResponse.ok(detail, "Lấy chi tiết lịch sử thành công"));
    }

    @DeleteMapping("/{historyId}")
    @Operation(
            summary = "Xoá một bản ghi lịch sử gia công",
            description = "Xoá tệp G-code và dữ liệu lịch sử tương ứng trong CSDL."
    )
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @Parameter(description = "ID bản ghi lịch sử cần xoá")
            @PathVariable("historyId") Long historyId,
            @Parameter(description = "Gmail người dùng (nếu có)")
            @RequestParam(value = "email", required = false) String email,
            @Parameter(description = "ID người dùng (nếu có)")
            @RequestParam(value = "userId", required = false) Integer userId) {

        historyService.deleteHistory(historyId, userId, email);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa lịch sử thành công"));
    }
}
