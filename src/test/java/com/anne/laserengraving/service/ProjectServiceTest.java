package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.SaveProjectRequest;
import com.anne.laserengraving.dto.response.ProjectResponse;
import com.anne.laserengraving.entity.ProjectEntity;
import com.anne.laserengraving.entity.UserEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.ProjectRepository;
import com.anne.laserengraving.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UserEntity sampleUser;
    private ProjectEntity sampleProject;

    @BeforeEach
    void setUp() {
        sampleUser = UserEntity.builder()
                .userId(1)
                .username("testuser")
                .email("testuser@gmail.com")
                .build();

        sampleProject = ProjectEntity.builder()
                .projectId(10L)
                .user(sampleUser)
                .projectName("Test Project")
                .elementCount(2)
                .elementsJson("[{\"id\":1}]")
                .parametersJson("{\"speed\":1000}")
                .thumbnailUrl("http://example.com/thumb.png")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Lưu dự án mới thành công")
    void testSaveNewProject_Success() {
        SaveProjectRequest request = new SaveProjectRequest();
        request.setProjectName("New Project");
        request.setElementCount(1);
        request.setElementsJson("[]");
        request.setParametersJson("{}");

        when(userRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity entity = invocation.getArgument(0);
            entity.setProjectId(11L);
            return entity;
        });

        ProjectResponse response = projectService.saveProject("testuser@gmail.com", request);

        assertNotNull(response);
        assertEquals(11L, response.getProjectId());
        assertEquals("New Project", response.getProjectName());
        verify(projectRepository, times(1)).save(any(ProjectEntity.class));
    }

    @Test
    @DisplayName("Lấy danh sách dự án của người dùng theo email")
    void testGetProjectsByUserEmail_Success() {
        when(userRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(projectRepository.findByUserOrderByUpdatedAtDesc(sampleUser)).thenReturn(List.of(sampleProject));

        List<ProjectResponse> result = projectService.getProjectsByUserEmail("testuser@gmail.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getProjectId());
    }

    @Test
    @DisplayName("Xem chi tiết dự án thành công khi đúng chủ sở hữu")
    void testGetProjectDetail_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(sampleProject));

        ProjectResponse response = projectService.getProjectDetail(10L, "testuser@gmail.com");

        assertNotNull(response);
        assertEquals(10L, response.getProjectId());
    }

    @Test
    @DisplayName("Xem chi tiết dự án ném ngoại lệ khi sai người dùng")
    void testGetProjectDetail_MismatchUser_ThrowsException() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(sampleProject));

        assertThrows(ResourceNotFoundException.class, () ->
                projectService.getProjectDetail(10L, "otheruser@gmail.com"));
    }

    @Test
    @DisplayName("Xóa dự án thành công")
    void testDeleteProject_Success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(sampleProject));

        projectService.deleteProject(10L, "testuser@gmail.com");

        verify(projectRepository, times(1)).delete(sampleProject);
    }
}
