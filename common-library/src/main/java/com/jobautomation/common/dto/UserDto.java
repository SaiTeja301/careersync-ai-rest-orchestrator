package com.jobautomation.common.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String encryptedPassword;
    private Integer experienceYears;
    private String preferredRoles;
    private String preferredCompanies;
    private boolean remote;
    private boolean hybrid;
}
