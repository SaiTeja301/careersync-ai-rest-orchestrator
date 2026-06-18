package com.jobautomation.ai.repository;

import com.jobautomation.ai.entity.AiResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiResponseRepository extends JpaRepository<AiResponseEntity, Long> {
}
