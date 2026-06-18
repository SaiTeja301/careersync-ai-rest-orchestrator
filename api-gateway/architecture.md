# API Gateway — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `api-gateway` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `api-gateway` |
| **Main Class** | `com.jobautomation.gateway.ApiGatewayApplication` |
| **Port (Local)** | `8080` |
| **Port (Docker)** | `8080` |
| **Database** | None |
| **Framework** | Spring Cloud Gateway |

---

## Package Structure

```
com.jobautomation.gateway
└── ApiGatewayApplication.java         # @SpringBootApplication entry point
```

---

## Configuration

**File:** `api-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        - id: user-service-users
          uri: ${USER_SERVICE_URI:http://localhost:8082}
          predicates:
            - Path=/users/**

        - id: user-service-resume
          uri: ${USER_SERVICE_URI:http://localhost:8082}
          predicates:
            - Path=/resume/**

        - id: job-service-add-jobs
          uri: ${JOB_SERVICE_URI:http://localhost:8083}
          predicates:
            - Path=/add-jobs/**

        - id: job-service-apply-job
          uri: ${JOB_SERVICE_URI:http://localhost:8083}
          predicates:
            - Path=/apply-job/**

        - id: linkedin-service
          uri: ${LINKEDIN_SERVICE_URI:http://localhost:8084}
          predicates:
            - Path=/linkedin-jobs/**

        - id: naukri-service
          uri: ${NAUKRI_SERVICE_URI:http://localhost:8085}
          predicates:
            - Path=/naukri-jobs/**

        - id: ai-service
          uri: ${AI_SERVICE_URI:http://localhost:8086}
          predicates:
            - Path=/aijobagent/**
```

---

## Route Table

| Route ID | Path Predicate | Downstream Target | Env Var |
|---|---|---|---|
| `user-service-users` | `/users/**` | user-service:8082 | `USER_SERVICE_URI` |
| `user-service-resume` | `/resume/**` | user-service:8082 | `USER_SERVICE_URI` |
| `job-service-add-jobs` | `/add-jobs/**` | job-service:8083 | `JOB_SERVICE_URI` |
| `job-service-apply-job` | `/apply-job/**` | job-service:8083 | `JOB_SERVICE_URI` |
| `linkedin-service` | `/linkedin-jobs/**` | linkedin-service:8084 | `LINKEDIN_SERVICE_URI` |
| `naukri-service` | `/naukri-jobs/**` | naukri-service:8085 | `NAUKRI_SERVICE_URI` |
| `ai-service` | `/aijobagent/**` | ai-recommendation-service:8086 | `AI_SERVICE_URI` |

---

## Public API Catalogue (via Gateway)

All endpoints below are accessed through the gateway at `http://localhost:8080`.

### User Service Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/users/add-user-info` | Register or update a user |
| GET | `/users/get-user-info/{userId}` | Get user profile by ID |
| DELETE | `/users/delete-user-info/{name}/{email}` | Delete user by name and email |
| GET | `/users/by-email/{email}` | Get user by email (internal) |
| GET | `/users/{id}` | Get user by ID (internal) |
| POST | `/resume/update-resume` | Upload/replace resume |
| GET | `/resume/users/{userId}/resume` | Get resume for user (internal) |

### Job Service Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/add-jobs/get-all-jobs-list` | Get all jobs |
| GET | `/add-jobs/get-search-job/{id}` | Get job by ID |
| DELETE | `/add-jobs/delete-job/{id}` | Delete a job |
| POST | `/add-jobs/delete-job-list` | Delete multiple jobs by ID list |
| POST | `/add-jobs/bulk` | Bulk save scraped jobs (internal) |
| GET | `/add-jobs/jobs` | Get all jobs (internal for AI) |
| POST | `/apply-job/apply/update-job-status` | Update application status |
| POST | `/apply-job/apply-job/{id}` | Trigger application for one job |
| POST | `/apply-job/apply-jobs-list` | Trigger application for multiple jobs |
| GET | `/apply-job/jobs/applications` | Get all applications (internal for AI) |

### LinkedIn Service Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/linkedin-jobs/scrape?linkedinUserEmailorUserName=` | Scrape recommended LinkedIn jobs |
| GET | `/linkedin-jobs/scrape-with-filters?linkedinUserEmailorUserName=&Title=&timeHours=` | Scrape LinkedIn jobs with keyword/time filter |
| POST | `/linkedin-jobs/apply-job/{id}` | Trigger application for one LinkedIn job |
| POST | `/linkedin-jobs/apply-jobs-list` | Trigger application for multiple LinkedIn jobs |

### Naukri Service Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/naukri-jobs/scrape?naukriUserEmail=&keyword=&location=&experience=&wfhType=&pageCount=` | Scrape Naukri jobs by search filters |
| GET | `/naukri-jobs/scrape-recommended?naukriUserEmail=` | Scrape Naukri recommended jobs |

### AI Service Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/aijobagent/ask-agent?prompt=&userId=` | Ask AI agent a job-related question |
| GET | `/aijobagent/analyze-resume?userId=` | AI resume analysis against open jobs |

---

## Management Endpoints

| Endpoint | URL |
|---|---|
| Health Check | `http://localhost:8080/actuator/health` |
| Info | `http://localhost:8080/actuator/info` |
| Gateway Routes | `http://localhost:8080/actuator/gateway` |

---

## Docker Configuration

```yaml
api-gateway:
  build:
    context: ./api-gateway
    dockerfile: Dockerfile
  container_name: api-gateway
  ports:
    - "8080:8080"
  environment:
    - USER_SERVICE_URI=http://user-service:8082
    - JOB_SERVICE_URI=http://job-service:8083
    - LINKEDIN_SERVICE_URI=http://linkedin-service:8084
    - NAUKRI_SERVICE_URI=http://naukri-service:8085
    - AI_SERVICE_URI=http://ai-recommendation-service:8086
  depends_on:
    - user-service
    - job-service
    - linkedin-service
    - naukri-service
    - ai-recommendation-service
  networks:
    - job-automation-network
```

---

## Request Flow Diagram

```
Client
  │
  ▼
GET http://localhost:8080/naukri-jobs/scrape?naukriUserEmail=...
  │
  ▼
API Gateway (8080)
  │ Path matches /naukri-jobs/**
  │ Route: naukri-service
  │
  ▼
naukri-service (8085)
  /naukri-jobs/scrape → NaukriController.scrapeNaukriJobs()
```

---

## Notes

- No authentication or authorization filter is configured in the gateway. All routes are publicly accessible.
- The gateway uses predicate-only routing (no rewrite filters). Downstream services receive the exact path sent by the client.
- Logging is set to `DEBUG` for `org.springframework.cloud.gateway` in development.
