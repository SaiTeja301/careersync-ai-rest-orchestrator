package com.jobautomation.user.service.impl;

import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.user.entity.ResumeEntity;
import com.jobautomation.user.entity.UserEntity;
import com.jobautomation.user.mapper.UserEntityMapper;
import com.jobautomation.user.repository.ResumeRepository;
import com.jobautomation.user.repository.UserRepository;
import com.jobautomation.user.service.ResumeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    @Autowired
    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             UserRepository userRepository,
                             UserEntityMapper userEntityMapper) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public ResumeDto updateResume(MultipartFile file, Long userId) throws IOException {
        String content = new String(file.getBytes());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        ResumeEntity resume = new ResumeEntity();
        resume.setUser(user);
        resume.setContent(content);
        resume.setVersion("v" + System.currentTimeMillis());
        return userEntityMapper.resumeToDto(resumeRepository.save(resume));
    }

    @Override
    public ResumeDto getResumeByUserId(Long userId) {
        List<ResumeEntity> resumes = resumeRepository.findByUserId(userId);
        if (resumes.isEmpty()) {
            return null;
        }
        return userEntityMapper.resumeToDto(resumes.get(resumes.size() - 1));
    }
}
