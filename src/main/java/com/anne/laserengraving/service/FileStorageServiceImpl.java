package com.anne.laserengraving.service;

import com.anne.laserengraving.exception.FileStorageException;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.laser.storage-path:storage/gcode}")
    private String storagePathStr;

    private Path storageLocation;

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(storagePathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Không thể khởi tạo thư mục lưu trữ file G-code: " + storagePathStr, e);
        }
    }

    @Override
    public String storeGcodeFile(String filename, String content) {
        try {
            String safeName = UUID.randomUUID() + "_" + filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!safeName.endsWith(".nc") && !safeName.endsWith(".gcode")) {
                safeName += ".nc";
            }
            Path targetLocation = this.storageLocation.resolve(safeName);
            Files.writeString(targetLocation, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return safeName;
        } catch (IOException e) {
            throw new FileStorageException("Lỗi ghi tệp G-code vào ổ đĩa", e);
        }
    }

    @Override
    public Resource loadGcodeFileAsResource(String relativePath) {
        try {
            Path filePath = this.storageLocation.resolve(relativePath).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new FileStorageException("Đường dẫn file không hợp lệ (Path Traversal detected)");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Tệp G-code không tồn tại hoặc không thể đọc: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Định dạng đường dẫn file không hợp lệ", e);
        }
    }

    @Override
    public void deleteGcodeFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = this.storageLocation.resolve(relativePath).normalize();
            if (filePath.startsWith(this.storageLocation)) {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.info("Đã xóa tệp G-code vật lý: {}", filePath);
                }
            }
        } catch (IOException e) {
            log.warn("Không thể xóa tệp G-code vật lý: {}", relativePath, e);
        }
    }
}
