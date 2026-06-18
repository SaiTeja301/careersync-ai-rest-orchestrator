package com.jobautomation.naukri.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user-service.base-url}")
    private String userServiceBaseUrl;

    @Value("${job-service.base-url}")
    private String jobServiceBaseUrl;

    @Bean(name = "userServiceWebClient")
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean(name = "jobServiceWebClient")
    public WebClient jobServiceWebClient() {
        return WebClient.builder()
                .baseUrl(jobServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
