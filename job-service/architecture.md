# Job Service — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `job-service` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `job-service` |
| **Main Class** | `com.jobautomation.job.JobServiceApplication` |
| **Port (Local)** | `8083` |
| **Port (Docker)** | `8083` |
| **Database** | `linkedin_naukr_jobs` / `job_db` (MySQL) |
| **Tables Owned** | `jobs`, `job_applications` |

---

## Package Structure

```
com.jobautomation.job
├── JobServiceApplication.java
├── controller
│   ├── JobController.java             # /add-jobs/**
│   └── ApplyJobController.java        # /apply-job/**
├── entity
│   ├── JobEntity.java
│   └── JobApplicationEntity.java
├── repository
│   ├── JobRepository.java
│   └── JobApplicationRepository.java
├── service
│   ├── JobService.java                (interface)
│   ├── JobApplicationService.java     (interface)
│   ├── JobDeduplicationService.java   (core dedup engine)
│   └── impl
│       ├── JobServiceImpl.java
│       └── JobApplicationServiceImpl.java
└── mapper
    └── JobEntityMapper.java
```

---

## Configuration

**File:** `job-service/src/main/resources/application.properties`

```properties
server.port=8083
spring.application.name=job-service

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/job_db}
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

### Table: `jobs`

```sql
CREATE TABLE jobs (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                    VARCHAR(255),
    company                  VARCHAR(255),
    location                 TEXT,
    job_url                  TEXT,
    platform                 VARCHAR(255),
    job_posted               VARCHAR(255),
    job_applyed_count_status VARCHAR(255),
    description              TEXT,
    applied                  BOOLEAN,
    created_at               DATETIME NOT NULL
);
```

### Table: `job_applications`

```sql
CREATE TABLE job_applications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id       BIGINT NOT NULL,
    status       VARCHAR(255),         -- PENDING | APPLIED | FAILED | MATCHING | REJECTED
    applied_at   DATETIME,
    is_job_applied BOOLEAN,
    FOREIGN KEY (job_id) REFERENCES jobs(id)
);
```

---

## Entity Reference

### `JobEntity`

```java
@Entity @Table(name = "jobs")
public class JobEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String company;
    @Column(columnDefinition = "TEXT") private String location;
    private String job_posted;
    private String job_applyed_count_status;
    @Column(columnDefinition = "TEXT") private String jobUrl;
    private String platform;
    @Column(columnDefinition = "TEXT") private String description;
    private boolean applied;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### `JobApplicationEntity`

```java
@Entity @Table(name = "job_applications")
public class JobApplicationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;  // PENDING, APPLIED, FAILED, MATCHING, REJECTED
    private LocalDateTime appliedAt = LocalDateTime.now();
    private boolean isJobApplied;
}
```

---

## Repository Reference

### `JobRepository`

```java
List<JobEntity> findByJobUrl(String jobUrl);
Optional<JobEntity> findFirstByJobUrlOrderByIdAsc(String jobUrl);
```

### `JobApplicationRepository`

```java
// Standard JpaRepository<JobApplicationEntity, Long> — no custom queries.
```

---

## Service Reference

### `JobService`

| Method | Description |
|---|---|
| `getAllJobs()` | Return all jobs as DTO list |
| `getJobById(Long id)` | Return single job or throw RuntimeException |
| `deleteJob(Long id)` | Delete job by ID |
| `deleteJobList(List<Long>)` | Bulk delete jobs by IDs |
| `applyToJob(Long id)` | Placeholder — triggers future LinkedIn bot |
| `applyToJobs(List<Long>)` | Placeholder — bulk application trigger |
| `bulkSaveJobs(List<JobDto>)` | Delegates to `JobDeduplicationService` |

### `JobApplicationService`

| Method | Description |
|---|---|
| `updateApplicationStatus(Long, boolean)` | Set APPLIED/FAILED status on an application |

---

## Core Component: `JobDeduplicationService`

This is the central business logic component of job-service. It implements the deduplication pipeline that prevents duplicate jobs from being stored when scrapers rerun.

### Deduplication Key

```
company + "|" + title + "|" + jobUrl
```

### Algorithm (3-step pipeline)

```
Step 1: Detect In-Batch Duplicates
  ├── Build HashMap<key, JobDto> from incoming list
  └── Track URLs that appear more than once → inBatchDuplicateUrls

Step 2: Check DB Duplicates
  ├── For each job NOT in inBatchDuplicateUrls:
  │   └── findFirstByJobUrlOrderByIdAsc(jobUrl)
  │       ├── if found → update job_applyed_count_status + job_posted → add to duplicatesToRemove
  │       └── if not found → keep in list

Step 3: Save New Jobs
  ├── Remove all duplicatesToRemove from incoming list
  ├── Map remaining DTOs → entities
  ├── saveAllAndFlush(newEntities)
  └── Return only newly saved jobs as DTO list
```

> [!NOTE]
> The API response count from naukri-service/linkedin-service reflects **only newly inserted jobs**, not total scraped. DB duplicates are updated (status fields only) but not counted in the response.

---

## API Endpoints

### `JobController` — `@RequestMapping("/add-jobs")`

| Method | Path | Body | Description |
|---|---|---|---|
| GET | `/add-jobs/get-all-jobs-list` | — | Get all jobs |
| GET | `/add-jobs/get-search-job/{id}` | — | Get job by ID |
| DELETE | `/add-jobs/delete-job/{id}` | — | Delete one job |
| POST | `/add-jobs/delete-job-list` | `List<Long>` | Delete multiple jobs |
| POST | `/add-jobs/bulk` | `List<JobDto>` | **Bulk save + deduplicate** (called by scraper services) |
| GET | `/add-jobs/jobs` | — | Get all jobs (internal — called by AI service) |

### `ApplyJobController` — `@RequestMapping("/apply-job")`

| Method | Path | Params | Description |
|---|---|---|---|
| POST | `/apply-job/apply/update-job-status` | `applicationId`, `applied` | Update application APPLIED/FAILED |
| POST | `/apply-job/apply-job/{id}` | — | Trigger application for one job |
| POST | `/apply-job/apply-jobs-list` | `List<Long>` | Trigger application for multiple jobs |
| GET | `/apply-job/jobs/applications` | — | Get all applications (internal — AI service) |

---

## Request Flow: Bulk Save (Scraper → Job Service)

```
linkedin-service / naukri-service
  │ POST /add-jobs/bulk
  │ Body: List<JobDto> (N scraped jobs)
  ▼
JobController.bulkSaveJobs(List<JobDto>)
  ▼
JobServiceImpl.bulkSaveJobs()
  ▼
JobDeduplicationService.deduplicateAndSave()
  ├── Step 1: Detect in-batch duplicates
  ├── Step 2: Check DB for existing job_url
  │           → update status on duplicates
  └── Step 3: saveAllAndFlush() on new jobs
  ▼
Return List<JobDto> (only newly saved jobs)
  ▼
Scraper service logs: "Saved X new jobs via job-service"
```

---

## Mapper: `JobEntityMapper`

| Method | Description |
|---|---|
| `toDto(JobEntity)` | Entity → `JobDto` |
| `toEntity(JobDto)` | `JobDto` → new `JobEntity` (sets `applied=false`) |
| `applicationToDto(JobApplicationEntity)` | Application entity → `JobApplicationDto` |

---

## Docker Configuration

```yaml
job-service:
  ports: ["8083:8083"]
  environment:
    - SPRING_DATASOURCE_URL=jdbc:mysql://job-db-mysql:3306/${JOB_DB_NAME:job_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:}
    - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:}
  depends_on:
    job-db-mysql:
      condition: service_healthy
  networks:
    - job-automation-network
```

---

## Called By

| Caller | Path | Purpose |
|---|---|---|
| `linkedin-service` | POST `/add-jobs/bulk` | Save scraped LinkedIn jobs |
| `naukri-service` | POST `/add-jobs/bulk` | Save scraped Naukri jobs |
| `ai-recommendation-service` | GET `/add-jobs/jobs` | Fetch all jobs for AI context |
| `ai-recommendation-service` | GET `/apply-job/jobs/applications` | Fetch application statuses |