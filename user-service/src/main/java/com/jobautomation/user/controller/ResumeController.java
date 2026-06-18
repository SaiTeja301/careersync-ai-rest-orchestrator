package com.jobautomation.user.controller;

import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.user.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeService resumeService;

    @Autowired
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /** Public endpoint: Upload or replace resume for a user */
    @PostMapping("/update-resume")
    public ResponseEntity<ResumeDto> updateResume(@RequestParam MultipartFile file,
                                                   @RequestParam Long userId) throws IOException {
        return ResponseEntity.ok(resumeService.updateResume(file, userId));
    }

    /** Internal endpoint: Called by ai-recommendation-service to fetch resume content */
    @GetMapping("/users/{userId}/resume")
    public ResponseEntity<ResumeDto> getResumeByUserId(@PathVariable("userId") Long userId) {
        ResumeDto resume = resumeService.getResumeByUserId(userId);
        return resume != null ? ResponseEntity.ok(resume) : ResponseEntity.notFound().build();
    }
}
