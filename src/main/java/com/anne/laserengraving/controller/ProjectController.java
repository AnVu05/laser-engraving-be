package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.SaveProjectRequest;
import com.anne.laserengraving.dto.response.ApiResponse;
import com.anne.laserengraving.dto.response.ProjectResponse;
import com.anne.laserengraving.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05. Project Management", description = "APIs lưu trữ và quản lý dự án bàn khắc Laser")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Lưu dự án mới hoặc cập nhật", description = "Lưu trạng thái bàn khắc (elements, thông số) cho tài khoản đã đăng nhập")
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> saveProject(
            @Parameter(description = "Gmail của tài khoản người dùng đã đăng nhập", required = true)
            @RequestParam String email,
            @Valid @RequestBody SaveProjectRequest request
    ) {
        ProjectResponse response = projectService.saveProject(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu dự án thành công!", response));
    }

    @Operation(summary = "Lấy danh sách dự án của người dùng", description = "Trả về toàn bộ các dự án đã lưu theo tài khoản Gmail của người dùng")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(
            @Parameter(description = "Gmail của tài khoản người dùng (nếu có)")
            @RequestParam(required = false) String email
    ) {
        List<ProjectResponse> list = projectService.getProjectsByUserEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách dự án thành công!", list));
    }

    @Operation(summary = "Xem chi tiết dự án", description = "Lấy dữ liệu đối tượng và thông số của một dự án cụ thể để nạp lên bàn khắc")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectDetail(
            @PathVariable Long projectId,
            @RequestParam String email
    ) {
        ProjectResponse response = projectService.getProjectDetail(projectId, email);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết dự án thành công!", response));
    }

    @Operation(summary = "Xóa dự án", description = "Xóa dự án đã lưu khỏi CSDL")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long projectId,
            @RequestParam String email
    ) {
        projectService.deleteProject(projectId, email);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa dự án thành công!", null));
    }
}
