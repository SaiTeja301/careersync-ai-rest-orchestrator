package com.jobautomation.ai.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_responses")
@Data
public class AiResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String response;

    private LocalDateTime createdAt = LocalDateTime.now();
}
