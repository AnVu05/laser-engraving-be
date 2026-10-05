package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.GcodeGenerateRequest;
import com.anne.laserengraving.dto.response.GcodeGenerateResponse;
import org.springframework.core.io.Resource;

public interface GcodeService {
    GcodeGenerateResponse generateGcode(GcodeGenerateRequest request, Integer userId);
    Resource downloadGcodeFile(Long historyId);
}
