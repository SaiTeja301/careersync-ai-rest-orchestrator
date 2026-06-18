package com.jobautomation.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiResponseDto {
    private Long id;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;
}
