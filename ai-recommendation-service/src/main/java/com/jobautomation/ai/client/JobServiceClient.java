package com.jobautomation.ai.client;

import com.jobautomation.common.dto.JobApplicationDto;
import com.jobautomation.common.dto.JobDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;

/**
 * Reactive REST client for job-service.
 * Replaces direct JobRepository and JobApplicationRepository in monolith's AiAgentServiceImpl.
 */
@Component
public class JobServiceClient {

    private static final Logger log = LoggerFactory.getLogger(JobServiceClient.class);
    private final WebClient jobServiceWebClient;

    public JobServiceClient(@Qualifier("jobServiceWebClient") WebClient jobServiceWebClient) {
        this.jobServiceWebClient = jobServiceWebClient;
    }

    public Mono<List<JobDto>> getJobs() {
        return jobServiceWebClient.get()
                .uri("/add-jobs/jobs")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<JobDto>>() {})
                .onErrorReturn(Collections.emptyList())
                .doOnNext(jobs -> log.debug("Fetched {} jobs from job-service", jobs.size()));
    }

    public Mono<List<JobApplicationDto>> getApplications() {
        return jobServiceWebClient.get()
                .uri("/apply-job/jobs/applications")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<JobApplicationDto>>() {})
                .onErrorReturn(Collections.emptyList());
    }
}
