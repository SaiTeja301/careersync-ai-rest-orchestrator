package com.jobautomation.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-goog-api-key", apiKey != null ? apiKey : "")
                .build();
    }
}
