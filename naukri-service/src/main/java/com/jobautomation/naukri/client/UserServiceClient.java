package com.jobautomation.naukri.client;

import com.jobautomation.common.dto.UserDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private final WebClient userServiceWebClient;

    public UserServiceClient(@Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    public UserDto getUserByEmail(String email) {
        log.info("Fetching Naukri user credentials for: {}", email);
        try {
            UserDto user = userServiceWebClient.get()
                    .uri("/users/by-email/{email}", email)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();
            if (user == null) {
                throw new InvalidUserException("User not found: " + email);
            }
            return user;
        } catch (InvalidUserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch user: {}", e.getMessage());
            throw new InvalidUserException("Failed to fetch credentials: " + e.getMessage());
        }
    }
}
