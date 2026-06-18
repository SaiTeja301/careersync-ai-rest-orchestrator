package com.jobautomation.job.service;

import com.jobautomation.common.dto.JobDto;
import java.util.List;

public interface JobService {
    List<JobDto> getAllJobs();
    JobDto getJobById(Long id);
    void deleteJob(Long id);
    void deleteJobList(List<Long> ids);
    String applyToJob(Long id);
    String applyToJobs(List<Long> ids);
    List<JobDto> bulkSaveJobs(List<JobDto> jobs);
}
