package com.jobautomation.ai.service.impl;

import com.jobautomation.ai.client.JobServiceClient;
import com.jobautomation.ai.client.UserServiceClient;
import com.jobautomation.ai.entity.AiResponseEntity;
import com.jobautomation.ai.mapper.AiEntityMapper;
import com.jobautomation.ai.repository.AiResponseRepository;
import com.jobautomation.ai.service.AiAgentService;
import com.jobautomation.ai.service.PromptBuilderService;
import com.jobautomation.common.dto.AiResponseDto;
import com.jobautomation.common.dto.JobApplicationDto;
import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.common.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

/**
 * AI agent service — refactored from monolith's AiAgentServiceImpl.
 *
 * Key changes:
 *  - ALL 4 direct repository dependencies REMOVED:
 *    JobRepository, JobApplicationRepository, UserRepository, ResumeRepository
 *  - Replaced with reactive WebClient calls via UserServiceClient and JobServiceClient
 *  - PromptBuilderService handles the context assembly (extracted from this class)
 */
@Service
public class AiAgentServiceImpl implements AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentServiceImpl.class);

    private final WebClient geminiWebClient;
    private final AiResponseRepository aiResponseRepository;
    private final AiEntityMapper aiEntityMapper;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final PromptBuilderService promptBuilderService;

    public AiAgentServiceImpl(
            @Qualifier("geminiWebClient") WebClient geminiWebClient,
            AiResponseRepository aiResponseRepository,
            AiEntityMapper aiEntityMapper,
            UserServiceClient userServiceClient,
            JobServiceClient jobServiceClient,
            PromptBuilderService promptBuilderService) {
        this.geminiWebClient = geminiWebClient;
        this.aiResponseRepository = aiResponseRepository;
        this.aiEntityMapper = aiEntityMapper;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.promptBuilderService = promptBuilderService;
    }

    @Override
    public Mono<AiResponseDto> askAgent(String userPrompt, Long userId) {
        // Fetch jobs and applications from job-service in parallel
        Mono<List<JobDto>> jobsMono = jobServiceClient.getJobs();
        Mono<List<JobApplicationDto>> applicationsMono = jobServiceClient.getApplications();

        // Fetch user and resume from user-service if userId provided
        Mono<UserDto> userMono = userId != null
                ? userServiceClient.getUser(userId).onErrorReturn(new UserDto())
                : Mono.just(new UserDto());
        Mono<ResumeDto> resumeMono = userId != null
                ? userServiceClient.getResume(userId).onErrorReturn(new ResumeDto())
                : Mono.just(new ResumeDto());

        return Mono.zip(jobsMono, applicationsMono, userMono, resumeMono)
                .flatMap(tuple -> {
                    List<JobDto> jobs = tuple.getT1();
                    List<JobApplicationDto> applications = tuple.getT2();
                    UserDto user = tuple.getT3();
                    ResumeDto resume = tuple.getT4();

                    String fullPrompt = promptBuilderService.buildAgentPrompt(
                            userPrompt, jobs, applications,
                            user.getEmail() != null ? user : null,
                            resume.getContent() != null ? resume : null);

                    return callGemini(fullPrompt, userPrompt);
                });
    }

    @Override
    public Mono<AiResponseDto> analyzeResume(Long userId) {
        Mono<List<JobDto>> jobsMono = jobServiceClient.getJobs();
        Mono<ResumeDto> resumeMono = userId != null
                ? userServiceClient.getResume(userId).onErrorReturn(new ResumeDto())
                : Mono.just(new ResumeDto());

        return Mono.zip(jobsMono, resumeMono)
                .flatMap(tuple -> {
                    List<JobDto> jobs = tuple.getT1();
                    ResumeDto resume = tuple.getT2();
                    String analysisPrompt = promptBuilderService.buildResumeAnalysisPrompt(
                            resume.getContent(), jobs);
                    return callGemini(analysisPrompt, analysisPrompt);
                });
    }

    /**
     * Posts a prompt to the Gemini API and saves the response to ai_db.
     */
    private Mono<AiResponseDto> callGemini(String fullPrompt, String originalPrompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", fullPrompt)
                        })
                });

        return geminiWebClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String responseText = response.toString();
                    AiResponseEntity entity = new AiResponseEntity();
                    entity.setPrompt(originalPrompt);
                    entity.setResponse(responseText);
                    AiResponseEntity saved = aiResponseRepository.save(entity);
                    log.info("AI response saved with id: {}", saved.getId());
                    return aiEntityMapper.toDto(saved);
                })
                .doOnError(e -> log.error("Gemini API call failed: {}", e.getMessage(), e));
    }
}
