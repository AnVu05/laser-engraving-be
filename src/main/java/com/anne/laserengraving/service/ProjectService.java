package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.SaveProjectRequest;
import com.anne.laserengraving.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {
    ProjectResponse saveProject(String userEmail, SaveProjectRequest request);
    List<ProjectResponse> getProjectsByUserEmail(String userEmail);
    ProjectResponse getProjectDetail(Long projectId, String userEmail);
    void deleteProject(Long projectId, String userEmail);
}
