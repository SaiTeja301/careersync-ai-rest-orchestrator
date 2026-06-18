package com.jobautomation.naukri.service;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.common.exceptions.InvalidUserException;
import java.util.List;

public interface NaukriScrapingService {
    void naukriLogin(String naukriUserEmail) throws InvalidUserException;
    List<JobDto> scrapeAndSaveNaukriJobs(String naukriUserEmail, String keyword, String location,
                                         Integer experience, Integer wfhType, Integer pageCount)
            throws InvalidUserException;
    List<JobDto> scrapeAndSaveRecommendedJobs(String naukriUserEmail) throws InvalidUserException;
}
