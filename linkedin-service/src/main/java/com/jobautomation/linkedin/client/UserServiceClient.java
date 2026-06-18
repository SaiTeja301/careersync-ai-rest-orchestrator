package com.jobautomation.linkedin.client;

import com.jobautomation.common.dto.UserDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * REST client for user-service.
 * Replaces the direct UserServiceImpl injection in the monolith.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient userServiceWebClient;

    public UserServiceClient(@Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    /**
     * Fetches user credentials from user-service by email.
     * Called before LinkedIn login to retrieve encryptedPassword.
     *
     * @param email the LinkedIn registered email
     * @return UserDto with credentials
     * @throws InvalidUserException if user not found
     */
    public UserDto getUserByEmail(String email) {
        log.info("Fetching user credentials for email: {}", email);
        try {
            UserDto user = userServiceWebClient.get()
                    .uri("/users/by-email/{email}", email)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();
            if (user == null) {
                throw new InvalidUserException("User not found with email: " + email);
            }
            return user;
        } catch (InvalidUserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch user from user-service: {}", e.getMessage(), e);
            throw new InvalidUserException("Failed to fetch user credentials: " + e.getMessage());
        }
    }
}
