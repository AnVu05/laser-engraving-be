package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Integer> {

    Optional<DeviceEntity> findByDeviceCode(String deviceCode);

    List<DeviceEntity> findByIsActiveTrue();

    List<DeviceEntity> findByConnectionStatus(String connectionStatus);

    boolean existsByDeviceCode(String deviceCode);
}
