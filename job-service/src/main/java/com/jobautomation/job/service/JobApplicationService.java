package com.jobautomation.job.service;

public interface JobApplicationService {
    String updateApplicationStatus(Long applicationId, boolean applied);
}
