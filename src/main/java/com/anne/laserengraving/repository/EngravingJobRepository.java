package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.EngravingJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EngravingJobRepository extends JpaRepository<EngravingJobEntity, Long> {

    Optional<EngravingJobEntity> findByJobCode(String jobCode);

    List<EngravingJobEntity> findAllByOrderByIdDesc();

    List<EngravingJobEntity> findByDeviceIdOrderByDispatchedAtDesc(Integer deviceId);

    List<EngravingJobEntity> findByUser_UserIdOrderByDispatchedAtDesc(Integer userId);

    List<EngravingJobEntity> findByStatus(String status);

    List<EngravingJobEntity> findByStatusIn(List<String> statuses);

    boolean existsByDeviceIdAndStatusIn(Integer deviceId, List<String> statuses);

    long countByDeviceIdAndStatus(Integer deviceId, String status);

    List<EngravingJobEntity> findByDeviceIdAndStatusOrderByDispatchedAtAsc(Integer deviceId, String status);

    List<EngravingJobEntity> findByStatusOrderByDispatchedAtAsc(String status);
}
