package com.anne.laserengraving.service;

import com.anne.laserengraving.entity.DeviceEntity;
import com.anne.laserengraving.exception.ResourceNotFoundException;
import com.anne.laserengraving.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceCredentialServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void givenValidBearer_whenAuthenticate_thenReturnActiveDevice() {
        DeviceEntity device = DeviceEntity.builder().deviceCode("LASER-HN-001").deviceName("Laser")
                .apiKeyHash("bcrypt-hash").isActive(true).build();
        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(device));
        when(passwordEncoder.matches("secret", "bcrypt-hash")).thenReturn(true);

        DeviceCredentialService service = new DeviceCredentialService(deviceRepository, passwordEncoder);

        assertSame(device, service.authenticate("LASER-HN-001", "Bearer secret"));
    }

    @Test
    void givenMissingBearer_whenAuthenticate_thenReject() {
        DeviceCredentialService service = new DeviceCredentialService(deviceRepository, passwordEncoder);

        assertThrows(SecurityException.class, () -> service.authenticate("LASER-HN-001", null));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void givenValidDevice_whenRotateCredential_thenPersistOnlyHash() {
        DeviceEntity device = DeviceEntity.builder().deviceCode("LASER-HN-001").deviceName("Laser")
                .apiKeyHash("old-hash").isActive(true).build();
        when(deviceRepository.findByDeviceCode("LASER-HN-001")).thenReturn(Optional.of(device));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        DeviceCredentialService service = new DeviceCredentialService(deviceRepository, passwordEncoder);
        String token = service.rotateCredential("LASER-HN-001");

        assertNotNull(token);
        assertNotEquals("new-hash", token);
        assertEquals("new-hash", device.getApiKeyHash());
        verify(deviceRepository).save(device);
    }

    @Test
    void givenUnknownDevice_whenRotateCredential_thenNotFound() {
        when(deviceRepository.findByDeviceCode("missing")).thenReturn(Optional.empty());
        DeviceCredentialService service = new DeviceCredentialService(deviceRepository, passwordEncoder);

        assertThrows(ResourceNotFoundException.class, () -> service.rotateCredential("missing"));
    }
}
