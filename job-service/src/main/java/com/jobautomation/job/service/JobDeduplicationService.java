package com.jobautomation.job.service;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.job.entity.JobEntity;
import com.jobautomation.job.mapper.JobEntityMapper;
import com.jobautomation.job.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles deduplication and bulk persistence of scraped jobs.
 * Extracted from the monolith's JobScrapingServiceImpl.SaveJobs() method.
 * Called by both linkedin-service and naukri-service via POST /jobs/bulk.
 */
@Service
public class JobDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(JobDeduplicationService.class);

    private final JobRepository jobRepository;
    private final JobEntityMapper jobEntityMapper;

    @Autowired
    public JobDeduplicationService(JobRepository jobRepository, JobEntityMapper jobEntityMapper) {
        this.jobRepository = jobRepository;
        this.jobEntityMapper = jobEntityMapper;
    }

    /**
     * Deduplicate scraped jobs against the database and persist new ones.
     * Duplicate detection key: company + title + jobUrl.
     * If a duplicate exists in DB, its status fields are updated.
     *
     * @param scrapedJobDtos list of jobs freshly scraped from a portal
     * @return combined list of newly saved + updated job DTOs
     */
    @Transactional
    public List<JobDto> deduplicateAndSave(List<JobDto> scrapedJobDtos) {
        if (scrapedJobDtos == null || scrapedJobDtos.isEmpty()) {
            log.info("No jobs to process.");
            return Collections.emptyList();
        }

        log.info("Processing {} scraped jobs.", scrapedJobDtos.size());

        // Step 1: Detect in-batch duplicates
        Map<String, JobDto> jobMap = new HashMap<>();
        Set<String> inBatchDuplicateUrls = new HashSet<>();

        for (JobDto jobDto : scrapedJobDtos) {
            String key = jobDto.getCompany() + "|" + jobDto.getTitle() + "|" + jobDto.getJobUrl();
            if (jobMap.containsKey(key)) {
                log.info("In-batch duplicate: {} at {}", jobDto.getTitle(), jobDto.getCompany());
                inBatchDuplicateUrls.add(jobDto.getJobUrl());
            } else {
                jobMap.put(key, jobDto);
            }
        }

        // Step 2: Check DB for duplicates and collect update candidates
        List<JobDto> duplicatesToRemove = new ArrayList<>();
        List<JobEntity> updatedEntities = new ArrayList<>();

        for (JobDto scrapedJobDto : scrapedJobDtos) {
            if (inBatchDuplicateUrls.contains(scrapedJobDto.getJobUrl())) {
                duplicatesToRemove.add(scrapedJobDto);
                continue;
            }
            try {
                Optional<JobEntity> existingOpt = jobRepository.findFirstByJobUrlOrderByIdAsc(scrapedJobDto.getJobUrl());
                if (existingOpt.isPresent()) {
                    JobEntity existing = existingOpt.get();
                    log.info("DB duplicate, updating status: {} at {}", scrapedJobDto.getTitle(), scrapedJobDto.getCompany());
                    existing.setJob_applyed_count_status(scrapedJobDto.getJob_applyed_count_status());
                    existing.setJob_posted(scrapedJobDto.getJob_posted());
                    updatedEntities.add(jobRepository.save(existing));
                    duplicatesToRemove.add(scrapedJobDto);
                }
            } catch (Exception e) {
                log.error("Error checking duplicate for {} at {}: {}",
                        scrapedJobDto.getTitle(), scrapedJobDto.getCompany(), e.getMessage());
            }
        }

        // Step 3: Remove duplicates and save new jobs
        scrapedJobDtos.removeAll(duplicatesToRemove);
        log.info("Removed {} duplicates. Saving {} new jobs.", duplicatesToRemove.size(), scrapedJobDtos.size());

        List<JobEntity> newEntities = scrapedJobDtos.stream()
                .map(jobEntityMapper::toEntity)
                .collect(Collectors.toList());

        List<JobEntity> savedEntities = jobRepository.saveAllAndFlush(newEntities);

        if (savedEntities.isEmpty()) {
            log.warn("No new jobs saved.");
        } else {
            log.info("Saved {} new jobs.", savedEntities.size());
        }

        // Return only newly saved jobs
        return savedEntities.stream()
                .map(jobEntityMapper::toDto)
                .collect(Collectors.toList());
    }
}
