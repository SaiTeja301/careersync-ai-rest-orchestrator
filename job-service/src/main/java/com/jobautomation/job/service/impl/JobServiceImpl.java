package com.jobautomation.job.service.impl;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.job.entity.JobEntity;
import com.jobautomation.job.mapper.JobEntityMapper;
import com.jobautomation.job.repository.JobRepository;
import com.jobautomation.job.service.JobDeduplicationService;
import com.jobautomation.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobEntityMapper jobEntityMapper;
    private final JobDeduplicationService jobDeduplicationService;

    @Autowired
    public JobServiceImpl(JobRepository jobRepository,
                          JobEntityMapper jobEntityMapper,
                          JobDeduplicationService jobDeduplicationService) {
        this.jobRepository = jobRepository;
        this.jobEntityMapper = jobEntityMapper;
        this.jobDeduplicationService = jobDeduplicationService;
    }

    @Override
    public List<JobDto> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(jobEntityMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public JobDto getJobById(Long id) {
        return jobRepository.findById(id)
                .map(jobEntityMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }

    @Override
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    @Override
    public void deleteJobList(List<Long> ids) {
        jobRepository.deleteAllById(ids);
    }

    @Override
    public String applyToJob(Long id) {
        return jobRepository.findById(id).map(job -> {
            // Automation trigger placeholder — will be implemented in linkedin-service
            return "Triggered application for job: " + job.getTitle();
        }).orElse("Job not found with id: " + id);
    }

    @Override
    public String applyToJobs(List<Long> ids) {
        List<JobEntity> jobs = jobRepository.findAllById(ids);
        if (jobs.isEmpty()) {
            return "No jobs found for provided IDs.";
        }
        return "Triggered application for " + jobs.size() + " jobs.";
    }

    @Override
    public List<JobDto> bulkSaveJobs(List<JobDto> jobs) {
        return jobDeduplicationService.deduplicateAndSave(jobs);
    }
}
