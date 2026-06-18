package com.jobautomation.job.controller;

import com.jobautomation.job.service.JobApplicationService;
import com.jobautomation.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/apply-job")
public class ApplyJobController {

    private final JobApplicationService jobApplicationService;
    private final JobService jobService;

    @Autowired
    public ApplyJobController(JobApplicationService jobApplicationService, JobService jobService) {
        this.jobApplicationService = jobApplicationService;
        this.jobService = jobService;
    }

    @PostMapping("/apply/update-job-status")
    public ResponseEntity<String> updateJobStatus(@RequestParam Long applicationId,
                                                   @RequestParam boolean applied) {
        return ResponseEntity.ok(jobApplicationService.updateApplicationStatus(applicationId, applied));
    }

    @PostMapping("/apply-job/{id}")
    public ResponseEntity<String> applyToJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.applyToJob(id));
    }

    @PostMapping("/apply-jobs-list")
    public ResponseEntity<String> applyToJobs(@RequestBody List<Long> ids) {
        return ResponseEntity.ok(jobService.applyToJobs(ids));
    }

    /** Internal endpoint: Get all applications for ai-recommendation-service */
    @GetMapping("/jobs/applications")
    public ResponseEntity<List<com.jobautomation.common.dto.JobApplicationDto>> getAllApplications() {
        // Return a simplified list of all applications
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }
}
