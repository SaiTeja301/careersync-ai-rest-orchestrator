package com.jobautomation.user.repository;

import com.jobautomation.user.entity.ResumeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<ResumeEntity, Long> {
    List<ResumeEntity> findByUserId(Long userId);
}
