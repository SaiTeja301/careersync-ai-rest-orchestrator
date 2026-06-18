package com.jobautomation.linkedin.service.impl;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.dto.UserDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import com.jobautomation.linkedin.automation.LinkedInLoginBot;
import com.jobautomation.linkedin.client.JobServiceClient;
import com.jobautomation.linkedin.client.UserServiceClient;
import com.jobautomation.linkedin.service.LinkedInScrapingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * LinkedIn scraping service — refactored from monolith's JobScrapingServiceImpl.
 *
 * Key changes from monolith:
 *  - UserServiceImpl direct injection REMOVED → replaced with UserServiceClient (REST)
 *  - JobRepository direct write REMOVED → replaced with JobServiceClient (REST POST /jobs/bulk)
 *  - SaveJobs() deduplication logic MOVED to job-service.JobDeduplicationService
 */
@Service
public class LinkedInScrapingServiceImpl implements LinkedInScrapingService {

    private static final Logger log = LoggerFactory.getLogger(LinkedInScrapingServiceImpl.class);

    private final LinkedInLoginBot linkedInLoginBot;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;

    private String linkedinUserNameOrEmail;
    private String linkedinPassword;

    @Autowired
    public LinkedInScrapingServiceImpl(LinkedInLoginBot linkedInLoginBot,
                                       UserServiceClient userServiceClient,
                                       JobServiceClient jobServiceClient) {
        this.linkedInLoginBot = linkedInLoginBot;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
    }

    @Override
    public List<JobDto> scrapeAndSaveJobs(String linkedinUserEmail) throws InvalidUserException {
        try {
            linkedinLogin(linkedinUserEmail);
            List<JobDto> scrapedJobDtos = linkedInLoginBot.scrapeJobs();
            scrapedJobDtos.forEach(job -> job.setPlatform("LinkedIn"));
            List<JobDto> savedJobs = jobServiceClient.postBulkJobs(scrapedJobDtos);
            log.info("Saved {} new LinkedIn jobs via job-service.", savedJobs.size());
            return savedJobs;
        } catch (InvalidUserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during LinkedIn scraping: {}", e.getMessage(), e);
            throw new InvalidUserException("Error during LinkedIn scraping: " + e.getMessage());
        }
    }

    @Override
    public List<JobDto> scrapeAndSaveJobs(String linkedinUserEmail, String title, Integer timeHours)
            throws InvalidUserException {
        try {
            linkedinLogin(linkedinUserEmail);
            List<JobDto> scrapedJobDtos = linkedInLoginBot.scrapeLatestJobsByTitleandTime(title, timeHours);
            scrapedJobDtos.forEach(job -> job.setPlatform("LinkedIn"));
            List<JobDto> savedJobs = jobServiceClient.postBulkJobs(scrapedJobDtos);
            log.info("Saved {} new filtered LinkedIn jobs via job-service.", savedJobs.size());
            return savedJobs;
        } catch (InvalidUserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during filtered LinkedIn scraping: {}", e.getMessage(), e);
            throw new InvalidUserException("Error during filtered LinkedIn scraping: " + e.getMessage());
        }
    }

    @Override
    public void linkedinLogin(String linkedinUserEmail) throws InvalidUserException {
        log.info("Resolving LinkedIn credentials for: {}", linkedinUserEmail);
        try {
            // Fetch credentials from user-service via REST (replaces direct UserServiceImpl injection)
            UserDto userObj = userServiceClient.getUserByEmail(linkedinUserEmail);
            if (!userObj.getEmail().equalsIgnoreCase(linkedinUserEmail)) {
                throw new InvalidUserException("User email mismatch: " + linkedinUserEmail);
            }
            linkedinUserNameOrEmail = userObj.getEmail().trim();
            linkedinPassword = userObj.getEncryptedPassword().trim();
            log.info("Credentials resolved — initiating LinkedIn login...");
            linkedInLoginBot.linkedinLogin(linkedinUserNameOrEmail, linkedinPassword);
        } catch (InvalidUserException e) {
            throw e;
        } catch (NoSuchElementException nse) {
            log.error("Login failed — element not found: {}", nse.getMessage(), nse);
            throw new InvalidUserException("Login failed, element not found: " + nse.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during LinkedIn login: {}", e.getMessage(), e);
            throw new InvalidUserException("LinkedIn login failed: " + e.getMessage());
        }
    }
}
