package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.MaterialPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialPresetRepository extends JpaRepository<MaterialPresetEntity, Long> {

    List<MaterialPresetEntity> findByIsSystemTrueOrderByPresetIdAsc();

    @Query("SELECT m FROM MaterialPresetEntity m WHERE m.isSystem = true OR (m.user IS NOT NULL AND m.user.email = :email) ORDER BY m.isSystem DESC, m.presetId ASC")
    List<MaterialPresetEntity> findAllAvailableForUser(@Param("email") String email);

    Optional<MaterialPresetEntity> findByMaterialKey(String materialKey);

    boolean existsByMaterialKey(String materialKey);

    long countByIsSystemTrue();
}
