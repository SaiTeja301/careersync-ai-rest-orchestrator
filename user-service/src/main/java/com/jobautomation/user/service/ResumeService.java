package com.jobautomation.user.service;

import com.jobautomation.common.dto.ResumeDto;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ResumeService {
    ResumeDto updateResume(MultipartFile file, Long userId) throws IOException;
    ResumeDto getResumeByUserId(Long userId);
}
