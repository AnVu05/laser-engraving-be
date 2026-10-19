package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.SaveProjectRequest;
import com.anne.laserengraving.dto.response.ProjectResponse;
import com.anne.laserengraving.exception.GlobalExceptionHandler;
import com.anne.laserengraving.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectController projectController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProjectResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = ProjectResponse.builder()
                .projectId(1L)
                .userId(1)
                .userEmail("testuser@gmail.com")
                .projectName("Test Project")
                .elementCount(2)
                .elementsJson("[]")
                .parametersJson("{}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/projects - Lưu dự án thành công")
    void testSaveProject_Success() throws Exception {
        SaveProjectRequest request = new SaveProjectRequest();
        request.setProjectName("Test Project");
        request.setElementCount(2);
        request.setElementsJson("[]");
        request.setParametersJson("{}");

        when(projectService.saveProject(eq("testuser@gmail.com"), any(SaveProjectRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/projects")
                        .param("email", "testuser@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(1L))
                .andExpect(jsonPath("$.data.projectName").value("Test Project"));
    }

    @Test
    @DisplayName("GET /api/v1/projects - Lấy danh sách dự án của người dùng")
    void testGetProjects_Success() throws Exception {
        when(projectService.getProjectsByUserEmail("testuser@gmail.com"))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/projects")
                        .param("email", "testuser@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].projectId").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId} - Xem chi tiết dự án")
    void testGetProjectDetail_Success() throws Exception {
        when(projectService.getProjectDetail(1L, "testuser@gmail.com"))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/projects/1")
                        .param("email", "testuser@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId} - Xóa dự án")
    void testDeleteProject_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/1")
                        .param("email", "testuser@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(projectService).deleteProject(1L, "testuser@gmail.com");
    }
}
