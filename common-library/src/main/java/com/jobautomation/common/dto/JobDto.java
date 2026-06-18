package com.jobautomation.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDto {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String jobUrl;
    private String platform;
    private String job_posted;
    private String job_applyed_count_status;
    private String description;
    private boolean applied;
    private LocalDateTime createdAt;
}
