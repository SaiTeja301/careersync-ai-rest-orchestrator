package com.jobautomation.job.service.impl;

import com.jobautomation.job.entity.JobApplicationEntity;
import com.jobautomation.job.repository.JobApplicationRepository;
import com.jobautomation.job.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository applicationRepository;

    @Autowired
    public JobApplicationServiceImpl(JobApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public String updateApplicationStatus(Long applicationId, boolean applied) {
        return applicationRepository.findById(applicationId).map(app -> {
            app.setJobApplied(applied);
            app.setStatus(applied
                    ? JobApplicationEntity.ApplicationStatus.APPLIED
                    : JobApplicationEntity.ApplicationStatus.FAILED);
            applicationRepository.save(app);
            return "Status updated successfully";
        }).orElse("Application not found with id: " + applicationId);
    }
}
