package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.SaveProjectRequest;
import com.anne.laserengraving.dto.response.ProjectResponse;
import com.anne.laserengraving.entity.ProjectEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.ProjectRepository;
import com.anne.laserengraving.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ProjectResponse saveProject(String userEmail, SaveProjectRequest request) {
        UserEntity user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + userEmail));

        ProjectEntity project;
        if (request.getProjectId() != null && request.getProjectId() > 0) {
            // CẬP NHẬT ĐÈ LÊN DỰ ÁN CŨ ĐANG MỞ
            project = projectRepository.findById(request.getProjectId())
                    .orElseGet(() -> ProjectEntity.builder()
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .build());

            // Đảm bảo dự án thuộc quyền sở hữu của user đang đăng nhập
            if (project.getUser() == null || !project.getUser().getEmail().equalsIgnoreCase(userEmail)) {
                project.setUser(user);
            }

            project.setProjectName(request.getProjectName());
            project.setElementCount(request.getElementCount() != null ? request.getElementCount() : 0);
            project.setElementsJson(request.getElementsJson());
            project.setParametersJson(request.getParametersJson());
            if (request.getThumbnailUrl() != null) {
                project.setThumbnailUrl(request.getThumbnailUrl());
            }
            project.setUpdatedAt(LocalDateTime.now());
            log.info("[PROJECT] Đã cập nhật đè thành công dự án cũ '{}' (ID: {}) cho người dùng {}",
                    project.getProjectName(), project.getProjectId(), userEmail);
        } else {
            // TẠO MỚI DỰ ÁN
            project = ProjectEntity.builder()
                    .user(user)
                    .projectName(request.getProjectName())
                    .elementCount(request.getElementCount() != null ? request.getElementCount() : 0)
                    .elementsJson(request.getElementsJson())
                    .parametersJson(request.getParametersJson())
                    .thumbnailUrl(request.getThumbnailUrl())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            log.info("[PROJECT] Đã tạo mới dự án '{}' cho người dùng {}", request.getProjectName(), userEmail);
        }

        ProjectEntity saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByUserEmail(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return List.of();
        }

        String targetEmail = userEmail.trim().toLowerCase();

        return userRepository.findByEmail(targetEmail)
                .map(user -> projectRepository.findByUserOrderByUpdatedAtDesc(user)
                        .stream()
                        .map(this::mapToResponse)
                        .toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectDetail(Long projectId, String userEmail) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án có ID: " + projectId));

        if (!project.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Bạn không có quyền truy cập vào dự án này!");
        }

        return mapToResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, String userEmail) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dự án có ID: " + projectId));

        if (!project.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Bạn không có quyền xóa dự án này!");
        }

        projectRepository.delete(project);
        log.info("[PROJECT] Đã xóa dự án ID {} của người dùng {}", projectId, userEmail);
    }

    private ProjectResponse mapToResponse(ProjectEntity entity) {
        return ProjectResponse.builder()
                .projectId(entity.getProjectId())
                .userId(entity.getUser().getUserId())
                .userEmail(entity.getUser().getEmail())
                .projectName(entity.getProjectName())
                .elementCount(entity.getElementCount())
                .elementsJson(entity.getElementsJson())
                .parametersJson(entity.getParametersJson())
                .thumbnailUrl(entity.getThumbnailUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
