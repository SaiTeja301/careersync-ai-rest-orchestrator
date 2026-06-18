package com.jobautomation.job.repository;

import com.jobautomation.job.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {
    List<JobEntity> findByJobUrl(String jobUrl);
    Optional<JobEntity> findFirstByJobUrlOrderByIdAsc(String jobUrl);
}
