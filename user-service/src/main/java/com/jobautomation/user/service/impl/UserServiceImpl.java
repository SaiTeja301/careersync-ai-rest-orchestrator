package com.jobautomation.user.service.impl;

import com.jobautomation.common.dto.UserDto;
import com.jobautomation.user.entity.UserEntity;
import com.jobautomation.user.mapper.UserEntityMapper;
import com.jobautomation.user.repository.UserRepository;
import com.jobautomation.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserEntityMapper userEntityMapper) {
        this.userRepository = userRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public UserDto saveUser(UserEntity user) {
        return userEntityMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto saveUser(UserDto userDto) {
        Optional<UserEntity> existingUserOpt = userRepository.findByEmail(userDto.getEmail());
        UserEntity userToSave;
        if (existingUserOpt.isPresent()) {
            UserEntity existingUser = existingUserOpt.get();
            existingUser.setName(userDto.getName());
            existingUser.setExperienceYears(userDto.getExperienceYears());
            existingUser.setEncryptedPassword(userDto.getEncryptedPassword());
            existingUser.setPreferredRoles(userDto.getPreferredRoles());
            existingUser.setPreferredCompanies(userDto.getPreferredCompanies());
            existingUser.setRemote(userDto.isRemote());
            existingUser.setHybrid(userDto.isHybrid());
            userToSave = existingUser;
        } else {
            userToSave = userEntityMapper.toEntity(userDto);
        }
        return userEntityMapper.toDto(userRepository.save(userToSave));
    }

    @Override
    public UserDto getUser(Long userId) {
        return userRepository.findById(userId)
                .map(userEntityMapper::toDto)
                .orElse(null);
    }

    @Override
    public UserDto getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userEntityMapper::toDto)
                .orElse(null);
    }

    @Override
    public UserDto getUserByNameAndEmail(String name, String email) {
        UserEntity entity = userRepository.findByNameAndEmail(name, email);
        return entity != null ? userEntityMapper.toDto(entity) : null;
    }

    @Override
    public void deleteUserByNameAndEmail(String name, String email) {
        UserEntity entity = userRepository.findByNameAndEmail(name, email);
        if (entity != null) {
            userRepository.delete(entity);
        }
    }
}
