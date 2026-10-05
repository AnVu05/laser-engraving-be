package com.anne.laserengraving.controller;

import com.anne.laserengraving.dto.request.MaterialPresetRequest;
import com.anne.laserengraving.dto.response.MaterialPresetResponse;
import com.anne.laserengraving.service.MaterialPresetService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaterialPresetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MaterialPresetService materialPresetService;

    @InjectMocks
    private MaterialPresetController materialPresetController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MaterialPresetResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(materialPresetController).build();

        sampleResponse = MaterialPresetResponse.builder()
                .id("mat-plywood")
                .presetId(1L)
                .materialKey("mat-plywood")
                .name("Gỗ ép Plywood (3mm)")
                .icon("forest")
                .badge("Phổ biến nhất")
                .desc("Ván ép bạch dương")
                .cutFeed(350)
                .cutPower(100)
                .cutPasses(2)
                .engraveFeed(2000)
                .engravePower(65)
                .dpi(254)
                .mode("vector")
                .isSystem(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/materials trả về danh sách vật liệu mẫu")
    void testGetAllMaterials() throws Exception {
        when(materialPresetService.getAllMaterials(null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("mat-plywood"))
                .andExpect(jsonPath("$.data[0].name").value("Gỗ ép Plywood (3mm)"))
                .andExpect(jsonPath("$.data[0].cutFeed").value(350));
    }

    @Test
    @DisplayName("POST /api/v1/materials tạo mới vật liệu tùy chỉnh")
    void testCreateMaterial() throws Exception {
        MaterialPresetRequest req = MaterialPresetRequest.builder()
                .name("Gỗ Mun Đen")
                .cutFeed(200)
                .cutPower(90)
                .build();

        when(materialPresetService.createMaterial(any(), eq(null))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/materials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("mat-plywood"));
    }

    @Test
    @DisplayName("DELETE /api/v1/materials/{id} xóa vật liệu")
    void testDeleteMaterial() throws Exception {
        doNothing().when(materialPresetService).deleteMaterial(5L, null);

        mockMvc.perform(delete("/api/v1/materials/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(materialPresetService, times(1)).deleteMaterial(5L, null);
    }
}
