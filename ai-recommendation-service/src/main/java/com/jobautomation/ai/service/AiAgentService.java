package com.jobautomation.ai.service;

import com.jobautomation.common.dto.AiResponseDto;
import reactor.core.publisher.Mono;

public interface AiAgentService {
    Mono<AiResponseDto> askAgent(String prompt, Long userId);
    Mono<AiResponseDto> analyzeResume(Long userId);
}
