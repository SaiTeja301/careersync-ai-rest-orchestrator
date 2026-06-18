package com.jobautomation.linkedin.client;

import com.jobautomation.common.dto.JobDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;

/**
 * REST client for job-service.
 * Sends scraped jobs to job-service for deduplication and persistence.
 * Replaces the direct JobRepository write in the monolith.
 */
@Component
public class JobServiceClient {

    private static final Logger log = LoggerFactory.getLogger(JobServiceClient.class);

    private final WebClient jobServiceWebClient;

    public JobServiceClient(@Qualifier("jobServiceWebClient") WebClient jobServiceWebClient) {
        this.jobServiceWebClient = jobServiceWebClient;
    }

    /**
     * Posts scraped jobs to job-service for bulk deduplication and save.
     *
     * @param jobs list of scraped JobDto objects
     * @return list of saved/updated JobDto objects returned by job-service
     */
    public List<JobDto> postBulkJobs(List<JobDto> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("Sending {} jobs to job-service for deduplication and save.", jobs.size());
        try {
            List<JobDto> saved = jobServiceWebClient.post()
                    .uri("/add-jobs/bulk")
                    .bodyValue(jobs)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<JobDto>>() {})
                    .block();
            return saved != null ? saved : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to post jobs to job-service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
