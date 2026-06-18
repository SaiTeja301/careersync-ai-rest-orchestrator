package com.jobautomation.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobApplicationDto {
    private Long id;
    private Long jobId;
    private String status;
    private LocalDateTime appliedAt;
    private boolean jobApplied;
}
