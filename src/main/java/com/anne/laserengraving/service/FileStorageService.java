package com.anne.laserengraving.service;

import org.springframework.core.io.Resource;

public interface FileStorageService {
    String storeGcodeFile(String filename, String content);
    Resource loadGcodeFileAsResource(String relativePath);
    void deleteGcodeFile(String relativePath);
}
