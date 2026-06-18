package com.jobautomation.linkedin.service;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import java.util.List;

public interface LinkedInScrapingService {
    List<JobDto> scrapeAndSaveJobs(String linkedinUserEmail) throws InvalidUserException;
    List<JobDto> scrapeAndSaveJobs(String linkedinUserEmail, String title, Integer timeHours) throws InvalidUserException;
    void linkedinLogin(String linkedinUserEmail) throws InvalidUserException;
}
