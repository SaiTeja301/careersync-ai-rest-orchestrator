package com.jobautomation.job.mapper;

import com.jobautomation.common.dto.JobApplicationDto;
import com.jobautomation.common.dto.JobDto;
import com.jobautomation.job.entity.JobApplicationEntity;
import com.jobautomation.job.entity.JobEntity;
import org.springframework.stereotype.Component;

@Component
public class JobEntityMapper {

    public JobDto toDto(JobEntity entity) {
        if (entity == null) return null;
        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setCompany(entity.getCompany());
        dto.setLocation(entity.getLocation());
        dto.setJobUrl(entity.getJobUrl());
        dto.setPlatform(entity.getPlatform());
        dto.setJob_posted(entity.getJob_posted());
        dto.setJob_applyed_count_status(entity.getJob_applyed_count_status());
        dto.setDescription(entity.getDescription());
        dto.setApplied(entity.isApplied());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public JobEntity toEntity(JobDto dto) {
        if (dto == null) return null;
        JobEntity entity = new JobEntity();
        entity.setTitle(dto.getTitle());
        entity.setCompany(dto.getCompany());
        entity.setLocation(dto.getLocation());
        entity.setJobUrl(dto.getJobUrl());
        entity.setPlatform(dto.getPlatform());
        entity.setJob_posted(dto.getJob_posted());
        entity.setJob_applyed_count_status(dto.getJob_applyed_count_status());
        entity.setDescription(dto.getDescription());
        entity.setApplied(false);
        return entity;
    }

    public JobApplicationDto applicationToDto(JobApplicationEntity entity) {
        if (entity == null) return null;
        JobApplicationDto dto = new JobApplicationDto();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJob() != null ? entity.getJob().getId() : null);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setAppliedAt(entity.getAppliedAt());
        dto.setJobApplied(entity.isJobApplied());
        return dto;
    }
}
