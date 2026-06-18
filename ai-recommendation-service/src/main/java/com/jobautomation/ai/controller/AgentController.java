package com.jobautomation.ai.controller;

import com.jobautomation.ai.service.AiAgentService;
import com.jobautomation.common.dto.AiResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/aijobagent")
public class AgentController {

    private final AiAgentService aiAgentService;

    @Autowired
    public AgentController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping("/ask-agent")
    public Mono<AiResponseDto> askAgent(@RequestParam String prompt, @RequestParam Long userId) {
        return aiAgentService.askAgent(prompt, userId);
    }

    @GetMapping("/analyze-resume")
    public Mono<AiResponseDto> analyzeResume(@RequestParam Long userId) {
        return aiAgentService.analyzeResume(userId);
    }
}
