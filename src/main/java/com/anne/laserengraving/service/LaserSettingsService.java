package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.LaserSettingsUpdateRequest;
import com.anne.laserengraving.dto.response.LaserSettingsResponse;

public interface LaserSettingsService {
    LaserSettingsResponse getCurrentSettings(Integer userId);
    LaserSettingsResponse updateSettings(Integer userId, LaserSettingsUpdateRequest request);
}
