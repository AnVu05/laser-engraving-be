package com.anne.laserengraving.service;

import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.repository.DeviceRepository;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DeviceCredentialService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    public DeviceEntity authenticate(String deviceCode, String authorization) {
        if (deviceCode == null || deviceCode.isBlank() || authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new SecurityException("Thiếu mã thiết bị hoặc Bearer token.");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode.trim())
                .filter(DeviceEntity::getIsActive)
                .orElseThrow(() -> new SecurityException("Thiết bị không tồn tại hoặc đã bị vô hiệu hóa."));

        if (token.isBlank() || device.getApiKeyHash() == null
                || !passwordEncoder.matches(token, device.getApiKeyHash())) {
            throw new SecurityException("Bearer token của thiết bị không hợp lệ.");
        }
        return device;
    }

    public String rotateCredential(String deviceCode) {
        DeviceEntity device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thiết bị: " + deviceCode));
        byte[] secret = new byte[32];
        SECURE_RANDOM.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        device.setApiKeyHash(passwordEncoder.encode(token));
        deviceRepository.save(device);
        return token;
    }
}
