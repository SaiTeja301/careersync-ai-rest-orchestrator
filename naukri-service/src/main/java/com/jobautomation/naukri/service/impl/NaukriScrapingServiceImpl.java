package com.jobautomation.naukri.service.impl;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.dto.UserDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import com.jobautomation.naukri.automation.NaukriJobBot;
import com.jobautomation.naukri.client.JobServiceClient;
import com.jobautomation.naukri.client.UserServiceClient;
import com.jobautomation.naukri.service.NaukriScrapingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Naukri scraping service — refactored from monolith's NaukriScrapingServiceImpl.
 *
 * Key changes:
 *  - UserServiceImpl direct injection REMOVED → replaced with UserServiceClient (REST)
 *  - JobScrapingService.SaveJobs() delegation REMOVED → replaced with JobServiceClient POST
 */
@Service
public class NaukriScrapingServiceImpl implements NaukriScrapingService {

    private static final Logger log = LoggerFactory.getLogger(NaukriScrapingServiceImpl.class);

    @Value("${naukri.max.pages:5}")
    private int defaultMaxPages;

    private final NaukriJobBot naukriJobBot;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;

    public NaukriScrapingServiceImpl(NaukriJobBot naukriJobBot,
                                     UserServiceClient userServiceClient,
                                     JobServiceClient jobServiceClient) {
        this.naukriJobBot = naukriJobBot;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
    }

    @Override
    public void naukriLogin(String naukriUserEmail) throws InvalidUserException {
        log.info("Resolving Naukri credentials for: {}", naukriUserEmail);
        try {
            UserDto user = userServiceClient.getUserByEmail(naukriUserEmail);
            if (!user.getEmail().equalsIgnoreCase(naukriUserEmail)) {
                throw new InvalidUserException("User email mismatch: " + naukriUserEmail);
            }
            String email = user.getEmail().trim();
            String password = user.getEncryptedPassword().trim();
            log.info("Credentials resolved — initiating Naukri login...");
            naukriJobBot.naukriLogin(email, password);
            log.info("Naukri login complete.");
        } catch (InvalidUserException e) {
            throw e;
        } catch (Exception e) {
            log.error("Naukri login error: {}", e.getMessage(), e);
            throw new InvalidUserException("Naukri login failed: " + e.getMessage());
        }
    }

    @Override
    public List<JobDto> scrapeAndSaveNaukriJobs(String naukriUserEmail, String keyword,
                                                  String location, Integer experience,
                                                  Integer wfhType, Integer pageCount)
            throws InvalidUserException {
        naukriLogin(naukriUserEmail);
        int pages = (pageCount != null && pageCount > 0) ? pageCount : defaultMaxPages;
        String searchUrl = buildNaukriSearchUrl(keyword, location, experience, wfhType);
        log.info("Naukri search URL: {}", searchUrl);
        try {
            List<JobDto> scrapedJobs = naukriJobBot.scrapeJobsBySearchUrl(searchUrl, pages);
            scrapedJobs.forEach(job -> job.setPlatform("Naukri"));
            log.info("Scraped {} Naukri jobs.", scrapedJobs.size());
            List<JobDto> savedJobs = jobServiceClient.postBulkJobs(scrapedJobs);
            log.info("Saved {} new Naukri jobs via job-service.", savedJobs.size());
            return savedJobs;
        } catch (Exception e) {
            log.error("Naukri scrape error: {}", e.getMessage(), e);
            throw new InvalidUserException("Naukri scraping failed: " + e.getMessage());
        }
    }

    @Override
    public List<JobDto> scrapeAndSaveRecommendedJobs(String naukriUserEmail) throws InvalidUserException {
        naukriLogin(naukriUserEmail);
        try {
            List<JobDto> scrapedJobs = naukriJobBot.scrapeRecommendedJobs();
            scrapedJobs.forEach(job -> job.setPlatform("Naukri"));
            log.info("Scraped {} recommended Naukri jobs.", scrapedJobs.size());
            List<JobDto> savedJobs = jobServiceClient.postBulkJobs(scrapedJobs);
            log.info("Saved {} new recommended Naukri jobs via job-service.", savedJobs.size());
            return savedJobs;
        } catch (Exception e) {
            log.error("Recommended jobs error: {}", e.getMessage(), e);
            throw new InvalidUserException("Recommended jobs scraping failed: " + e.getMessage());
        }
    }

    private String buildNaukriSearchUrl(String keyword, String location, Integer experience, Integer wfhType) {
        StringBuilder path = new StringBuilder("https://www.naukri.com/");
        StringBuilder params = new StringBuilder();
        if (keyword != null && !keyword.isBlank()) {
            path.append(keyword.toLowerCase().trim().replace(" ", "-")).append("-jobs");
        } else {
            path.append("jobs");
        }
        if (location != null && !location.isBlank()) {
            path.append("-in-").append(location.toLowerCase().trim().replace(" ", "-"));
        }
        path.append("?");
        if (keyword != null && !keyword.isBlank()) params.append("k=").append(encode(keyword)).append("&");
        if (location != null && !location.isBlank()) params.append("l=").append(encode(location)).append("&");
        if (experience != null) params.append("experience=").append(experience).append("&");
        if (wfhType != null) params.append("wfhType=").append(wfhType).append("&");
        params.append("nignbevent_src=jobsearchDeskGNB");
        return path.append(params).toString();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value.replace(" ", "%20");
        }
    }
}
