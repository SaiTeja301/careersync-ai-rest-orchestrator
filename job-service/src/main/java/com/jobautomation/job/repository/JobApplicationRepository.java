package com.jobautomation.job.repository;

import com.jobautomation.job.entity.JobApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplicationEntity, Long> {
}
