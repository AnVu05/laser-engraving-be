package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.GcodeHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GcodeHistoryRepository extends JpaRepository<GcodeHistoryEntity, Long> {
    Page<GcodeHistoryEntity> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<GcodeHistoryEntity> findByUser_EmailIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);
    Page<GcodeHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<GcodeHistoryEntity> findByHistoryIdAndUser_UserId(Long historyId, Integer userId);
    Optional<GcodeHistoryEntity> findByHistoryIdAndUser_EmailIgnoreCase(Long historyId, String email);
}
