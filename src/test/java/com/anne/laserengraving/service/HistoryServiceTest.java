package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.response.HistoryItemResponse;
import com.anne.laserengraving.entity.GcodeHistoryEntity;
import com.anne.laserengraving.repository.GcodeHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private GcodeHistoryRepository historyRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private GcodeHistoryEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = GcodeHistoryEntity.builder()
                .historyId(101L)
                .originalFilename("test_portrait.png")
                .storedFilePath("uuid_test_portrait.nc")
                .fileSizeBytes(15200L)
                .totalGcodeLines(1250)
                .engravingSpeed(1500)
                .laserPower(750)
                .dpi(254)
                .originXMm(BigDecimal.valueOf(45.0))
                .originYMm(BigDecimal.valueOf(35.0))
                .outputWidthMm(BigDecimal.valueOf(80.0))
                .outputHeightMm(BigDecimal.valueOf(70.0))
                .estimatedTimeSeconds(240)
                .status("Hoàn tất")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách lịch sử trả về đầy đủ tọa độ gốc origin và trạng thái status")
    void testGetHistoryMapping() {
        Pageable pageable = PageRequest.of(0, 10);
        when(historyRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleEntity)));

        Page<HistoryItemResponse> result = historyService.getHistory(null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        HistoryItemResponse item = result.getContent().get(0);
        assertEquals(101L, item.getHistoryId());
        assertEquals("test_portrait.png", item.getOriginalFilename());
        assertEquals(BigDecimal.valueOf(45.0), item.getOriginXMm());
        assertEquals(BigDecimal.valueOf(35.0), item.getOriginYMm());
        assertEquals("Hoàn tất", item.getStatus());
        assertEquals("/api/v1/gcode/download/101", item.getDownloadUrl());
    }

    @Test
    @DisplayName("Xóa bản ghi lịch sử đồng thời xóa tệp vật lý .nc")
    void testDeleteHistory() {
        when(historyRepository.findById(101L)).thenReturn(Optional.of(sampleEntity));

        historyService.deleteHistory(101L, null);

        verify(fileStorageService, times(1)).deleteGcodeFile("uuid_test_portrait.nc");
        verify(historyRepository, times(1)).delete(sampleEntity);
    }
}
