package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.MaterialPresetRequest;
import com.anne.laserengraving.dto.response.MaterialPresetResponse;
import com.anne.laserengraving.entity.MaterialPresetEntity;
import com.anne.laserengraving.exception.InvalidParameterException;
import com.anne.laserengraving.repository.MaterialPresetRepository;
import com.anne.laserengraving.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialPresetServiceTest {

    @Mock
    private MaterialPresetRepository materialPresetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MaterialPresetServiceImpl materialPresetService;

    private MaterialPresetEntity systemMaterial;
    private MaterialPresetEntity customMaterial;

    @BeforeEach
    void setUp() {
        systemMaterial = MaterialPresetEntity.builder()
                .presetId(1L)
                .materialKey("mat-plywood")
                .name("Gỗ ép Plywood (3mm)")
                .icon("forest")
                .badge("Phổ biến nhất")
                .description("Ván ép bạch dương")
                .cutFeed(350)
                .cutPower(100)
                .cutPasses(2)
                .engraveFeed(2000)
                .engravePower(65)
                .dpi(254)
                .mode("vector")
                .isSystem(true)
                .build();

        customMaterial = MaterialPresetEntity.builder()
                .presetId(10L)
                .materialKey("mat-custom-walnut")
                .name("Gỗ Óc Chó Tự Nhiên (5mm)")
                .icon("nature")
                .badge("Gỗ cao cấp")
                .description("Gỗ cứng cao cấp")
                .cutFeed(180)
                .cutPower(100)
                .cutPasses(3)
                .engraveFeed(1500)
                .engravePower(70)
                .dpi(300)
                .mode("vector")
                .isSystem(false)
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách vật liệu hệ thống thành công")
    void testGetAllMaterialsSystem() {
        when(materialPresetRepository.findByIsSystemTrueOrderByPresetIdAsc())
                .thenReturn(List.of(systemMaterial));

        List<MaterialPresetResponse> result = materialPresetService.getAllMaterials(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("mat-plywood", result.get(0).getId());
        assertEquals("Gỗ ép Plywood (3mm)", result.get(0).getName());
        assertTrue(result.get(0).getIsSystem());
    }

    @Test
    @DisplayName("Tạo vật liệu mới tùy chỉnh thành công")
    void testCreateMaterial() {
        MaterialPresetRequest req = MaterialPresetRequest.builder()
                .name("Gỗ Óc Chó Tự Nhiên (5mm)")
                .materialKey("mat-custom-walnut")
                .icon("nature")
                .badge("Gỗ cao cấp")
                .description("Gỗ cứng cao cấp")
                .cutFeed(180)
                .cutPower(100)
                .cutPasses(3)
                .engraveFeed(1500)
                .engravePower(70)
                .dpi(300)
                .mode("vector")
                .build();

        when(materialPresetRepository.existsByMaterialKey("mat-custom-walnut")).thenReturn(false);
        when(materialPresetRepository.save(any(MaterialPresetEntity.class))).thenReturn(customMaterial);

        MaterialPresetResponse response = materialPresetService.createMaterial(req, null);

        assertNotNull(response);
        assertEquals("Gỗ Óc Chó Tự Nhiên (5mm)", response.getName());
        assertEquals(180, response.getCutFeed());
        assertFalse(response.getIsSystem());
    }

    @Test
    @DisplayName("Chặn xóa vật liệu hệ thống, ném InvalidParameterException")
    void testDeleteSystemMaterialFails() {
        when(materialPresetRepository.findById(1L)).thenReturn(Optional.of(systemMaterial));

        InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> {
            materialPresetService.deleteMaterial(1L, null);
        });

        assertTrue(ex.getMessage().contains("mặc định của hệ thống"));
        verify(materialPresetRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa vật liệu tùy chỉnh thành công")
    void testDeleteCustomMaterialSuccess() {
        when(materialPresetRepository.findById(10L)).thenReturn(Optional.of(customMaterial));

        materialPresetService.deleteMaterial(10L, null);

        verify(materialPresetRepository, times(1)).delete(customMaterial);
    }
}
