package com.jobautomation.ai.client;

import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.common.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive REST client for user-service.
 * Replaces direct UserRepository and ResumeRepository in monolith's AiAgentServiceImpl.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private final WebClient userServiceWebClient;

    public UserServiceClient(@Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    public Mono<UserDto> getUser(Long userId) {
        return userServiceWebClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .bodyToMono(UserDto.class)
                .doOnError(e -> log.error("Failed to fetch user {}: {}", userId, e.getMessage()));
    }

    public Mono<ResumeDto> getResume(Long userId) {
        return userServiceWebClient.get()
                .uri("/resume/users/{userId}/resume", userId)
                .retrieve()
                .bodyToMono(ResumeDto.class)
                .onErrorReturn(new ResumeDto())
                .doOnNext(r -> log.debug("Resume fetched for user {}", userId));
    }
}
