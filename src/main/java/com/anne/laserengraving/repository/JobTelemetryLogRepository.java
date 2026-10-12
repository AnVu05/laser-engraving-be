package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.JobTelemetryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobTelemetryLogRepository extends JpaRepository<JobTelemetryLogEntity, Long> {

    List<JobTelemetryLogEntity> findByJob_IdOrderByCreatedAtDesc(Long jobId);
}
