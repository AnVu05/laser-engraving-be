package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.MaterialPresetRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.MaterialPresetResponse;
import com.anne.laserengraving.service.MaterialPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
@Tag(name = "4. Material Presets (Thư viện Vật liệu mẫu)", description = "Quản lý thông số gia công khắc/cắt laser tương ứng với các loại phôi vật liệu (Gỗ, Mica, Da, MDF, Carton, Kim loại...)")
public class MaterialPresetController {

    private final MaterialPresetService materialPresetService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách vật liệu mẫu",
            description = "Trả về danh sách tất cả các loại phôi vật liệu mặc định của hệ thống kèm theo các phôi tùy chỉnh của người dùng (nếu có truyền email)."
    )
    public ResponseEntity<ApiResponse<List<MaterialPresetResponse>>> getAllMaterials(
            @Parameter(description = "Email người dùng hiện tại (nếu có)")
            @RequestParam(value = "email", required = false) String email) {

        List<MaterialPresetResponse> materials = materialPresetService.getAllMaterials(email);
        return ResponseEntity.ok(ApiResponse.ok(materials, "Lấy danh sách vật liệu mẫu thành công"));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Xem chi tiết một vật liệu mẫu theo ID",
            description = "Lấy đầy đủ thông số cắt/khắc laser, số lần đi dao, công suất, tốc độ của phôi theo ID."
    )
    public ResponseEntity<ApiResponse<MaterialPresetResponse>> getMaterialById(
            @Parameter(description = "ID bản ghi vật liệu mẫu")
            @PathVariable("id") Long id) {

        MaterialPresetResponse material = materialPresetService.getMaterialById(id);
        return ResponseEntity.ok(ApiResponse.ok(material, "Lấy chi tiết vật liệu thành công"));
    }

    @GetMapping("/key/{key}")
    @Operation(
            summary = "Xem chi tiết một vật liệu mẫu theo mã key",
            description = "Lấy thông số phôi theo mã định danh duy nhất (VD: mat-plywood, mat-acrylic)."
    )
    public ResponseEntity<ApiResponse<MaterialPresetResponse>> getMaterialByKey(
            @Parameter(description = "Mã khóa vật liệu (VD: mat-plywood)")
            @PathVariable("key") String key) {

        MaterialPresetResponse material = materialPresetService.getMaterialByKey(key);
        return ResponseEntity.ok(ApiResponse.ok(material, "Lấy chi tiết vật liệu thành công"));
    }

    @PostMapping
    @Operation(
            summary = "Tạo mới vật liệu mẫu tùy chỉnh",
            description = "Cho phép người dùng lưu cấu hình thông số gia công của một phôi vật liệu mới vào thư viện cá nhân."
    )
    public ResponseEntity<ApiResponse<MaterialPresetResponse>> createMaterial(
            @Parameter(description = "Email người dùng tạo vật liệu (nếu có)")
            @RequestParam(value = "email", required = false) String email,
            @Valid @RequestBody MaterialPresetRequest request) {

        MaterialPresetResponse response = materialPresetService.createMaterial(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Tạo mới vật liệu mẫu thành công!"));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật thông số vật liệu mẫu tùy chỉnh",
            description = "Chỉnh sửa thông số phôi do người dùng tạo. Không cho phép sửa đổi 6 mẫu chuẩn hệ thống."
    )
    public ResponseEntity<ApiResponse<MaterialPresetResponse>> updateMaterial(
            @Parameter(description = "ID bản ghi vật liệu cần cập nhật")
            @PathVariable("id") Long id,
            @Parameter(description = "Email người dùng cập nhật")
            @RequestParam(value = "email", required = false) String email,
            @Valid @RequestBody MaterialPresetRequest request) {

        MaterialPresetResponse response = materialPresetService.updateMaterial(id, request, email);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật vật liệu mẫu thành công!"));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa vật liệu mẫu tùy chỉnh",
            description = "Xóa phôi người dùng tạo khỏi thư viện. Không cho phép xóa vật liệu chuẩn hệ thống."
    )
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @Parameter(description = "ID bản ghi vật liệu cần xóa")
            @PathVariable("id") Long id,
            @Parameter(description = "Email người dùng yêu cầu xóa")
            @RequestParam(value = "email", required = false) String email) {

        materialPresetService.deleteMaterial(id, email);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa vật liệu thành công!"));
    }

    @PostMapping("/init-defaults")
    @Operation(
            summary = "Khởi tạo lại 6 vật liệu chuẩn hệ thống",
            description = "Nạp lại các mẫu vật liệu gốc (Plywood, Acrylic, Da, MDF, Carton, Nhôm) nếu chưa tồn tại trong CSDL."
    )
    public ResponseEntity<ApiResponse<Void>> initDefaults() {
        materialPresetService.initDefaultPresets();
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã khởi tạo các vật liệu chuẩn hệ thống"));
    }
}
