package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.LaserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaserSettingsRepository extends JpaRepository<LaserSettingsEntity, Integer> {
    List<LaserSettingsEntity> findByUser_UserId(Integer userId);
    Optional<LaserSettingsEntity> findFirstByUser_UserId(Integer userId);
}
