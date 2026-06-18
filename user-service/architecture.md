# User Service — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `user-service` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `user-service` |
| **Main Class** | `com.jobautomation.user.UserServiceApplication` |
| **Port (Local)** | `8082` |
| **Port (Docker)** | `8082` |
| **Database** | `linkedin_naukr_jobs` / `user_db` (MySQL) |
| **Tables Owned** | `users`, `resumes` |

---

## Package Structure

```
com.jobautomation.user
├── UserServiceApplication.java
├── controller
│   ├── UserController.java            # /users/**
│   └── ResumeController.java          # /resume/**
├── entity
│   ├── UserEntity.java
│   └── ResumeEntity.java
├── repository
│   ├── UserRepository.java
│   └── ResumeRepository.java
├── service
│   ├── UserService.java               (interface)
│   ├── ResumeService.java              (interface)
│   └── impl
│       ├── UserServiceImpl.java
│       └── ResumeServiceImpl.java
└── mapper
    └── UserEntityMapper.java
```

---

## Configuration

**File:** `user-service/src/main/resources/application.properties`

```properties
server.port=8082
spring.application.name=user-service

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/user_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

logging.level.com.jobautomation=DEBUG
```

---

## Database Schema

### Table: `users`

```sql
CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    encrypted_password   TEXT,
    experience_years     INT,
    preferred_roles      TEXT,
    preferred_companies  TEXT,
    remote              BOOLEAN,
    hybrid              BOOLEAN
);
```

### Table: `resumes`

```sql
CREATE TABLE resumes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    content       TEXT,
    version       VARCHAR(255),
    version_count INT,
    uploaded_at   DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Entity Reference

### `UserEntity`

```java
@Entity @Table(name = "users")
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(unique = true, nullable = false) private String email;
    @Column(columnDefinition = "TEXT") private String encryptedPassword;
    private Integer experienceYears;
    @Column(columnDefinition = "TEXT") private String preferredRoles;
    @Column(columnDefinition = "TEXT") private String preferredCompanies;
    private boolean remote;
    private boolean hybrid;
}
```

### `ResumeEntity`

```java
@Entity @Table(name = "resumes")
public class ResumeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(columnDefinition = "TEXT") private String content;
    private String version;
    @Version private Integer versionCount;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
```

---

## Repository Reference

### `UserRepository`

```java
Optional<UserEntity> findByEmail(String email);
Optional<UserEntity> findByNameAndEmail(String name, String email);
```

### `ResumeRepository`

```java
List<ResumeEntity> findByUserId(Long userId);
```

---

## Service Reference

### `UserService`

| Method | Description |
|---|---|
| `saveUser(UserDto)` | Save or update user profile details |
| `getUser(Long)` | Fetch user DTO by user ID |
| `getUserByEmail(String)` | Fetch user DTO by email |
| `deleteUserByNameAndEmail(String, String)` | Delete user and cascade delete their resumes |

### `ResumeService`

| Method | Description |
|---|---|
| `updateResume(MultipartFile, Long)` | Upload new resume file, parse content to String and save |
| `getResumeByUserId(Long)` | Fetch latest resume version content for a user |

---

## API Endpoints

### `UserController` — `@RequestMapping("/users")`

| Method | Path | Body / Params | Description |
|---|---|---|---|
| POST | `/users/add-user-info` | `@RequestBody UserDto` | Register or update user details |
| GET | `/users/get-user-info/{userId}` | PathVariable: `userId` | Public: Get user details by ID |
| DELETE | `/users/delete-user-info/{name}/{email}` | PathVariables: `name`, `email` | Public: Delete user by name + email |
| GET | `/users/by-email/{email}` | PathVariable: `email` | **Internal:** Called by scraper services |
| GET | `/users/{id}` | PathVariable: `id` | **Internal:** Called by AI service |

### `ResumeController` — `@RequestMapping("/resume")`

| Method | Path | Params | Description |
|---|---|---|---|
| POST | `/resume/update-resume` | `file` (Multipart), `userId` | Public: Upload/replace resume |
| GET | `/resume/users/{userId}/resume` | PathVariable: `userId` | **Internal:** Called by AI service |

---

## Mapper: `UserEntityMapper`

| Method | Description |
|---|---|
| `toDto(UserEntity)` | `UserEntity` → `UserDto` |
| `toEntity(UserDto)` | `UserDto` → `UserEntity` |
| `resumeToDto(ResumeEntity)` | `ResumeEntity` → `ResumeDto` |

---

## Docker Configuration

```yaml
user-service:
  ports: ["8082:8082"]
  environment:
    - SPRING_DATASOURCE_URL=jdbc:mysql://user-db-mysql:3306/${USER_DB_NAME:user_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:}
    - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:}
  depends_on:
    user-db-mysql:
      condition: service_healthy
  networks:
    - job-automation-network
```