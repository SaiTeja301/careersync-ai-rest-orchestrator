package com.jobautomation.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResumeDto {
    private Long id;
    private Long userId;
    private String content;
    private String version;
    private LocalDateTime uploadedAt;
}
