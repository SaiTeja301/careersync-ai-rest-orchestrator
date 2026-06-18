package com.jobautomation.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String version;

    @Version
    private Integer versionCount;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}
