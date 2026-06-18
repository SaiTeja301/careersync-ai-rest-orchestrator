# Common Library — Module Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `common-library` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `common-library` |
| **Type** | Shared Maven Library (JAR) |
| **Main Class** | None |
| **Database** | None (Stateless) |

---

## Package Structure

```
com.jobautomation.common
├── constants
│   └── JobConstants.java              # Job portals and URL constants
├── dto
│   ├── UserDto.java                   # User profile data DTO
│   ├── ResumeDto.java                 # User resume content DTO
│   ├── JobDto.java                    # Scraped job listing DTO
│   ├── JobApplicationDto.java         # Job application details DTO
│   └── AiResponseDto.java             # Google Gemini response DTO
└── exceptions
    ├── InvalidUserException.java      # Custom exception for credential resolution
    └── GlobalExceptionHandler.java    # Shared ControllerAdvice for mapping errors
```

---

## DTO Models

### `UserDto`

```java
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
```

### `ResumeDto`

```java
public class ResumeDto {
    private Long id;
    private Long userId;
    private String content;
    private String version;
    private LocalDateTime uploadedAt;
}
```

### `JobDto`

```java
public class JobDto {
    private Long id;
    private String title;
    private String company;
    private String location;
    private String jobUrl;
    private String platform;
    private String job_posted;
    private String job_applyed_count_status;
    private String description;
    private boolean applied;
    private LocalDateTime createdAt;
}
```

### `JobApplicationDto`

```java
public class JobApplicationDto {
    private Long id;
    private Long jobId;
    private String status;  // PENDING, APPLIED, FAILED, MATCHING, REJECTED
    private LocalDateTime appliedAt;
    private boolean jobApplied;
}
```

### `AiResponseDto`

```java
public class AiResponseDto {
    private Long id;
    private String prompt;
    private String response;
    private LocalDateTime createdAt;
}
```

---

## Exception Handling

The common library provides standard exception models and a `@ControllerAdvice` listener to ensure consistent JSON error responses across all REST services.

### `InvalidUserException`

A checked exception thrown when:
- User credentials cannot be resolved by the User Service.
- Credentials decrypted from the database are blank or invalid.
- Scraper bots fail to authenticate on career portals.

```java
public class InvalidUserException extends Exception {
    public InvalidUserException(String message) {
        super(message);
    }
}
```

### `GlobalExceptionHandler`

Maps standard Java and application-specific exceptions to REST responses:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "An unexpected error occurred");
        body.put("details", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "A runtime error occurred");
        body.put("details", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<Object> handleInvalidUserException(InvalidUserException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "Invalid User Error");
        body.put("details", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
```

---

## Shared Constants

### `JobConstants`

```java
public class JobConstants {
    public static final String JOB_PORTAL_LINKEDIN = "LinkedIn";
    public static final String JOB_PORTAL_NAUKRI = "Naukri";
    public static final String JOBS_LINKEDURL = "https://www.linkedin.com/jobs/?";
}
```
