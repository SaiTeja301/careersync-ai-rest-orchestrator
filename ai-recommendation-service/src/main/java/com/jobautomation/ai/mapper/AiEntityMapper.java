package com.jobautomation.ai.mapper;

import com.jobautomation.ai.entity.AiResponseEntity;
import com.jobautomation.common.dto.AiResponseDto;
import org.springframework.stereotype.Component;

@Component
public class AiEntityMapper {

    public AiResponseDto toDto(AiResponseEntity entity) {
        if (entity == null) return null;
        AiResponseDto dto = new AiResponseDto();
        dto.setId(entity.getId());
        dto.setPrompt(entity.getPrompt());
        dto.setResponse(entity.getResponse());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public AiResponseEntity toEntity(AiResponseDto dto) {
        if (dto == null) return null;
        AiResponseEntity entity = new AiResponseEntity();
        entity.setPrompt(dto.getPrompt());
        entity.setResponse(dto.getResponse());
        return entity;
    }
}
