package com.jobautomation.naukri.controller;

import com.jobautomation.common.exceptions.InvalidUserException;
import com.jobautomation.naukri.service.NaukriScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Naukri automation endpoints.
 * Extracted from the monolith's AutomationController (Naukri sections only).
 */
@RestController
@RequestMapping("/naukri-jobs")
public class NaukriController {

    private final NaukriScrapingService naukriScrapingService;

    @Autowired
    public NaukriController(NaukriScrapingService naukriScrapingService) {
        this.naukriScrapingService = naukriScrapingService;
    }

    @GetMapping("/scrape")
    public String scrapeNaukriJobs(
            @RequestParam("naukriUserEmail") String naukriUserEmail,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "experience", required = false) Integer experience,
            @RequestParam(value = "wfhType", required = false) Integer wfhType,
            @RequestParam(value = "pageCount", required = false, defaultValue = "5") Integer pageCount)
            throws InvalidUserException {
        var savedJobs = naukriScrapingService.scrapeAndSaveNaukriJobs(
                naukriUserEmail, keyword, location, experience, wfhType, pageCount);
        return savedJobs.isEmpty()
                ? "Naukri scraping completed. No new jobs found."
                : "Naukri scraping completed. Saved/Updated " + savedJobs.size() + " jobs.";
    }

    @GetMapping(" ")
    public String scrapeNaukriRecommendedJobs(@RequestParam("naukriUserEmail") String naukriUserEmail)
            throws InvalidUserException {
        var savedJobs = naukriScrapingService.scrapeAndSaveRecommendedJobs(naukriUserEmail);
        return savedJobs.isEmpty()
                ? "Naukri recommended jobs scraping completed. No new jobs found."
                : "Naukri recommended jobs scraping completed. Saved/Updated " + savedJobs.size() + " jobs.";
    }
}
