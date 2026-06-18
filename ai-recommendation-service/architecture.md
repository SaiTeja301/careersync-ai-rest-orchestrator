# AI Recommendation Service — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `ai-recommendation-service` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `ai-recommendation-service` |
| **Main Class** | `com.jobautomation.ai.AiServiceApplication` |
| **Port (Local)** | `8086` |
| **Port (Docker)** | `8086` |
| **Database** | `ai_db` (MySQL) |
| **Tables Owned** | `ai_responses` |

---

## Package Structure

```
com.jobautomation.ai
├── AiServiceApplication.java
├── client
│   ├── UserServiceClient.java         # WebClient → user-service
│   └── JobServiceClient.java          # WebClient → job-service
├── config
│   ├── GeminiConfig.java              # Google Gemini LLM WebClient Bean
│   └── WebClientConfig.java           # Internal service WebClient Config
├── controller
│   └── AgentController.java           # /aijobagent/**
├── entity
│   └── AiResponseEntity.java
├── mapper
│   └── AiEntityMapper.java
├── repository
│   └── AiResponseRepository.java
└── service
    ├── AiAgentService.java            (interface)
    ├── PromptBuilderService.java       (context prompt builder)
    └── impl
        └── AiAgentServiceImpl.java
```

---

## Configuration

**File:** `ai-recommendation-service/src/main/resources/application.properties`

```properties
server.port=8086
spring.application.name=ai-recommendation-service

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/ai_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

user-service.base-url=${USER_SERVICE_BASE_URL:http://localhost:8082}
job-service.base-url=${JOB_SERVICE_BASE_URL:http://localhost:8083}

# Gemini API Endpoint Configuration
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
gemini.api.key=${GEMINI_API_KEY}

logging.level.com.jobautomation=DEBUG
```

---

## Database Schema

### Table: `ai_responses`

```sql
CREATE TABLE ai_responses (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt     TEXT,
    response   TEXT,
    created_at DATETIME NOT NULL
);
```

---

## Entity Reference

### `AiResponseEntity`

```java
@Entity @Table(name = "ai_responses")
public class AiResponseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT") private String prompt;
    @Column(columnDefinition = "TEXT") private String response;
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

## Repository Reference

### `AiResponseRepository`

Standard `JpaRepository<AiResponseEntity, Long>` — no custom queries.

---

## Service Reference

### `AiAgentService`

| Method | Return Type | Description |
|---|---|---|
| `askAgent(String prompt, Long userId)` | `Mono<AiResponseDto>` | Parallel fetch candidate profile, resume, scraped jobs and applications, construct prompt, and call Gemini. |
| `analyzeResume(Long userId)` | `Mono<AiResponseDto>` | Fetch candidate resume and scraped jobs, build comparison matrix, and prompt Gemini for fit. |

### `PromptBuilderService`

Matches candidate profiles against the job database to assemble the prompt payload.

- Constructs the list of scraped jobs (mapping title, company, location, description).
- Includes candidate's resume content, experience years, preferred roles and companies.
- Appends the candidate's custom query prompt.
- Builds instructions for Gemini to output recommendations in structured markdown.

---

## API Endpoints

### `AgentController` — `@RequestMapping("/aijobagent")`

| Method | Path | Params | Description |
|---|---|---|---|
| POST | `/aijobagent/ask-agent` | `prompt` (String), `userId` (Long) | Ask AI agent advice (returns reactive Mono) |
| GET | `/aijobagent/analyze-resume` | `userId` (Long) | Trigger AI matching analysis for user resume |

---

## Request Flow (Reactive Pipeline)

```
Client → API Gateway (8080)
  │ Route: /aijobagent/** → ai-recommendation-service (8086)
  ▼
AgentController.askAgent(userPrompt, userId)
  ▼
AiAgentServiceImpl.askAgent()
  │
  ├── 1. Call jobServiceClient.getJobs() [Non-blocking]
  │      Call jobServiceClient.getApplications() [Non-blocking]
  │      Call userServiceClient.getUser(userId) [Non-blocking]
  │      Call userServiceClient.getResume(userId) [Non-blocking]
  │
  ├── 2. Mono.zip() chains all 4 responses into tuple
  │
  ├── 3. promptBuilderService.buildAgentPrompt(...)
  │      Constructs the context prompt payload string.
  │
  ├── 4. Call callGemini(fullPrompt, userPrompt)
  │      └── POST https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
  │          Header: Content-Type=application/json
  │          Params: key=${GEMINI_API_KEY}
  │          Body: { "contents": [{ "parts": [{ "text": fullPrompt }] }] }
  │
  ├── 5. Map response and save AiResponseEntity to ai_responses table.
  │
  ▼
Return Mono<AiResponseDto> to Client
```

---

## Docker Configuration

```yaml
ai-recommendation-service:
  ports: ["8086:8086"]
  environment:
    - USER_SERVICE_BASE_URL=http://user-service:8082
    - JOB_SERVICE_BASE_URL=http://job-service:8083
    - GEMINI_API_KEY=${GEMINI_API_KEY}
    - SPRING_DATASOURCE_URL=jdbc:mysql://ai-db-mysql:3306/${AI_DB_NAME:ai_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    - SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:}
    - SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:}
  depends_on:
    ai-db-mysql:
      condition: service_healthy
  networks:
    - job-automation-network
```