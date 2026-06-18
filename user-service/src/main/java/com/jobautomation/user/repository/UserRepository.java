package com.jobautomation.user.repository;

import com.jobautomation.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    UserEntity findByNameAndEmail(String name, String email);
}
