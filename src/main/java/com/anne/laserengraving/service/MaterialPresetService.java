package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.MaterialPresetRequest;
import com.anne.laserengraving.dto.response.MaterialPresetResponse;

import java.util.List;

public interface MaterialPresetService {

    /**
     * Lấy toàn bộ danh sách vật liệu mẫu khả dụng (hệ thống + người dùng tạo)
     */
    List<MaterialPresetResponse> getAllMaterials(String email);

    /**
     * Lấy chi tiết vật liệu mẫu theo ID bản ghi
     */
    MaterialPresetResponse getMaterialById(Long presetId);

    /**
     * Lấy chi tiết vật liệu mẫu theo mã định danh (VD: mat-plywood)
     */
    MaterialPresetResponse getMaterialByKey(String materialKey);

    /**
     * Tạo mới một mẫu vật liệu tùy chỉnh
     */
    MaterialPresetResponse createMaterial(MaterialPresetRequest request, String email);

    /**
     * Cập nhật thông số mẫu vật liệu (chỉ áp dụng cho vật liệu người dùng tạo)
     */
    MaterialPresetResponse updateMaterial(Long presetId, MaterialPresetRequest request, String email);

    /**
     * Xóa mẫu vật liệu tùy chỉnh (chặn xóa vật liệu hệ thống)
     */
    void deleteMaterial(Long presetId, String email);

    /**
     * Khởi tạo 6 loại phôi vật liệu chuẩn ban đầu nếu CSDL chưa có
     */
    void initDefaultPresets();
}
