package com.anne.laserengraving.repository;

import com.anne.laserengraving.entity.ProjectEntity;
import com.anne.laserengraving.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    List<ProjectEntity> findByUserOrderByUpdatedAtDesc(UserEntity user);
    List<ProjectEntity> findByUser_UserIdOrderByUpdatedAtDesc(Integer userId);
}
