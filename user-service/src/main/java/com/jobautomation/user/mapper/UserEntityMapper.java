package com.jobautomation.user.mapper;

import com.jobautomation.common.dto.ResumeDto;
import com.jobautomation.common.dto.UserDto;
import com.jobautomation.user.entity.ResumeEntity;
import com.jobautomation.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public UserDto toDto(UserEntity entity) {
        if (entity == null) return null;
        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setEncryptedPassword(entity.getEncryptedPassword());
        dto.setExperienceYears(entity.getExperienceYears());
        dto.setPreferredRoles(entity.getPreferredRoles());
        dto.setPreferredCompanies(entity.getPreferredCompanies());
        dto.setRemote(entity.isRemote());
        dto.setHybrid(entity.isHybrid());
        return dto;
    }

    public UserEntity toEntity(UserDto dto) {
        if (dto == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setEncryptedPassword(dto.getEncryptedPassword());
        entity.setExperienceYears(dto.getExperienceYears());
        entity.setPreferredRoles(dto.getPreferredRoles());
        entity.setPreferredCompanies(dto.getPreferredCompanies());
        entity.setRemote(dto.isRemote());
        entity.setHybrid(dto.isHybrid());
        return entity;
    }

    public ResumeDto resumeToDto(ResumeEntity entity) {
        if (entity == null) return null;
        ResumeDto dto = new ResumeDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setContent(entity.getContent());
        dto.setVersion(entity.getVersion());
        dto.setUploadedAt(entity.getUploadedAt());
        return dto;
    }
}
