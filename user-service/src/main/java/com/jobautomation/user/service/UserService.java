package com.jobautomation.user.service;

import com.jobautomation.common.dto.UserDto;
import com.jobautomation.user.entity.UserEntity;

public interface UserService {
    UserDto saveUser(UserEntity user);
    UserDto saveUser(UserDto userDto);
    UserDto getUser(Long userId);
    UserDto getUserByEmail(String email);
    UserDto getUserByNameAndEmail(String name, String email);
    void deleteUserByNameAndEmail(String name, String email);
}
