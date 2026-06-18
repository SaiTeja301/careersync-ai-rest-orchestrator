package com.jobautomation.job.controller;

import com.jobautomation.common.dto.JobDto;
import com.jobautomation.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/add-jobs")
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/get-all-jobs-list")
    public ResponseEntity<List<JobDto>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/get-search-job/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @DeleteMapping("/delete-job/{id}")
    public ResponseEntity<String> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok("Job deleted successfully");
    }

    @PostMapping("/delete-job-list")
    public ResponseEntity<String> deleteJobList(@RequestBody List<Long> ids) {
        jobService.deleteJobList(ids);
        return ResponseEntity.ok("Jobs deleted successfully");
    }

    /** Internal endpoint: Called by linkedin-service and naukri-service to bulk save scraped jobs */
    @PostMapping("/bulk")
    public ResponseEntity<List<JobDto>> bulkSaveJobs(@RequestBody List<JobDto> jobs) {
        return ResponseEntity.ok(jobService.bulkSaveJobs(jobs));
    }

    /** Internal endpoint: Called by ai-recommendation-service */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobDto>> getJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }
}
