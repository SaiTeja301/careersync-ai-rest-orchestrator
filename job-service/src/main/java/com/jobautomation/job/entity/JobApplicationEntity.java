package com.jobautomation.job.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Data
public class JobApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(255)")
    private ApplicationStatus status;

    private LocalDateTime appliedAt = LocalDateTime.now();

    private boolean isJobApplied;

    public enum ApplicationStatus {
        PENDING, APPLIED, FAILED, MATCHING, REJECTED
    }
}
