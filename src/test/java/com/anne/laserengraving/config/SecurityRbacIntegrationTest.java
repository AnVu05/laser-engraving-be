package com.anne.laserengraving.config;

import com.anne.laserengraving.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityRbacIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API Admin /api/v1/jobs bị từ chối 401 Unauthorized")
    void testGetJobs_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yêu cầu đăng nhập tài khoản Quản trị viên (ADMIN) để thực hiện thao tác này!"));
    }

    @Test
    @DisplayName("Khách chưa đăng nhập gọi API Phê duyệt An toàn /api/v1/jobs/JOB-TEST/confirm bị từ chối 401 Unauthorized")
    void testConfirmJob_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/JOB-TEST/confirm"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Người dùng vai trò USER gọi API Admin /api/v1/jobs bị từ chối 403 Forbidden")
    void testGetJobs_UserRole_Returns403() throws Exception {
        String userToken = jwtUtil.generateToken("user@gmail.com", 2, "USER");

        mockMvc.perform(get("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền quản trị (ADMIN/OPERATOR) để truy cập tài nguyên này!"));
    }

    @Test
    @DisplayName("Quản trị viên ADMIN gọi API Admin /api/v1/jobs được chấp thuận 200 OK")
    void testGetJobs_AdminRole_Returns200() throws Exception {
        String adminToken = jwtUtil.generateToken("admin@gmail.com", 1, "ADMIN");

        mockMvc.perform(get("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Endpoint công khai /api/v1/materials cho phép khách truy cập 200 OK")
    void testGetMaterials_Public_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
