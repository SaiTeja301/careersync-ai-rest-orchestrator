package com.jobautomation.job.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;

    @Column(columnDefinition = "TEXT")
    private String location;

    private String job_posted;
    private String job_applyed_count_status;

    @Column(columnDefinition = "TEXT")
    private String jobUrl;

    private String platform;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean applied;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
