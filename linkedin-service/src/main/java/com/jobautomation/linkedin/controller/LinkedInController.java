package com.jobautomation.linkedin.controller;

import com.jobautomation.common.exceptions.InvalidUserException;
import com.jobautomation.linkedin.service.LinkedInScrapingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * LinkedIn automation endpoints.
 * Extracted from the monolith's AutomationController (LinkedIn sections only).
 */
@RestController
@RequestMapping("/linkedin-jobs")
public class LinkedInController {

    private final LinkedInScrapingService linkedInScrapingService;

    @Autowired
    public LinkedInController(LinkedInScrapingService linkedInScrapingService) {
        this.linkedInScrapingService = linkedInScrapingService;
    }

    @GetMapping("/scrape")
    public String scrapeJobs(@RequestParam String linkedinUserEmailorUserName) throws InvalidUserException {
        var scrapedJobs = linkedInScrapingService.scrapeAndSaveJobs(linkedinUserEmailorUserName);
        return scrapedJobs.isEmpty()
                ? "Scraping completed. No new jobs found."
                : "Scraping completed. Saved " + scrapedJobs.size() + " new jobs.";
    }

    @GetMapping("/scrape-with-filters")
    public String scrapeJobsWithFilters(
            @RequestParam String linkedinUserEmailorUserName,
            @RequestParam String Title,
            @RequestParam Integer timeHours) throws InvalidUserException {
        var scrapedJobs = linkedInScrapingService.scrapeAndSaveJobs(linkedinUserEmailorUserName, Title, timeHours);
        return scrapedJobs.isEmpty()
                ? "Scraping completed. No new jobs found."
                : "Scraping completed. Saved " + scrapedJobs.size() + " new jobs.";
    }

    @PostMapping("/apply-job/{id}")
    public String applyToJob(@PathVariable("id") Long id) {
        // Placeholder: Auto-apply via LinkedIn bot will be wired here
        return "Triggered application for job ID: " + id;
    }

    @PostMapping("/apply-jobs-list")
    public String applyToJobs(@RequestBody List<Long> ids) {
        return "Triggered application for " + ids.size() + " jobs.";
    }
}
