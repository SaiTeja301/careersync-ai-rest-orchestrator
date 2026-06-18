# Naukri Service — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `naukri-service` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `naukri-service` |
| **Main Class** | `com.jobautomation.naukri.NaukriServiceApplication` |
| **Port (Local)** | `8085` |
| **Port (Docker)** | `8085` |
| **Database** | None (stateless — no local DB) |
| **External Systems** | `Naukri.com` (Selenium), `user-service:8082`, `job-service:8083` |

---

## Package Structure

```
com.jobautomation.naukri
├── NaukriServiceApplication.java
├── automation
│   └── NaukriJobBot.java              # Selenium scraping bot
├── client
│   ├── UserServiceClient.java         # WebClient → user-service
│   └── JobServiceClient.java          # WebClient → job-service
├── config
│   ├── SeleniumConfig.java            # @Lazy ChromeDriver bean
│   └── WebClientConfig.java           # WebClient beans (10MB buffer)
├── controller
│   └── NaukriController.java          # /naukri-jobs/**
├── service
│   ├── NaukriScrapingService.java     (interface)
│   └── impl
│       └── NaukriScrapingServiceImpl.java
└── utils
    ├── Generic_Keywords.java          # Low-level Selenium helpers
    ├── KeyLibrary.java                # String/Jsoup parsing utilities
    ├── RateLimiter.java               # Randomised human-like delays
    ├── ScreenshotUtils.java           # Failure screenshot capture
    └── WaitUtils.java                 # Explicit condition-based waits
```

---

## Configuration

**File:** `naukri-service/src/main/resources/application.properties`

```properties
server.port=8085
spring.application.name=naukri-service

user-service.base-url=${USER_SERVICE_BASE_URL:http://localhost:8082}
job-service.base-url=${JOB_SERVICE_BASE_URL:http://localhost:8083}

selenium.headless=false

naukri.max.pages=5

logging.level.com.jobautomation=DEBUG
```

---

## API Endpoints

### `NaukriController` — `@RequestMapping("/naukri-jobs")`

| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/naukri-jobs/scrape` | `naukriUserEmail`, `keyword`, `location`, `experience`, `wfhType`, `pageCount` | Scrape Naukri jobs by search filters and page count |
| GET | `/naukri-jobs/scrape-recommended` | `naukriUserEmail` | Scrape Naukri personalized recommended jobs |

---

## Core Component: `NaukriJobBot`

A Selenium `@Component` that drives Chrome to interact with Naukri.com.

| XPath/CSS Selector | Element / Match Target | Purpose |
|---|---|---|
| `//*[contains(@class,'jd-header-title')]` | Job Title | Extract job title |
| `(//*[contains(@class,'comp-name')]//a)[1]` | Company Name | Extract company name |
| `//*[contains(@class,'styles_jhc__location__W_pVs')]` | Location | Extract job location |
| `//*[contains(@class,'job-desc')]` | Job Description | Extract job description |
| `(//*[contains(@class,'styles_jhc__stat__PgY67')])[1]` | Date Posted | Extract posted time |
| `(//*[contains(@class,'styles_jhc__stat__PgY67')])[3]` | Apply Count Status | Extract application count / status |
| `.srp-jobtuple-wrapper[data-job-id]` | Job card tuple | Extract unique job IDs from search list |

### Scraping Strategy (`scrapeJobsBySearchUrl`)

```
1. Build URL: https://www.naukri.com/<keyword>-jobs-in-<location>?k=<keyword>&l=<location>&experience=<exp>&wfhType=<wfh>
2. For each page (1 to pageCount):
   └── Navigate to search results URL + "/" + pageNumber
   └── Wait for .srp-jobtuple-wrapper[data-job-id] element list
   └── Extract unique data-job-id attribute values
3. Reconstruct job detail URLs:
   └── https://www.naukri.com/job-listings-<data-job-id>
4. For each unique job URL:
   └── Navigate ChromeDriver → Wait for page load
   └── Fallback: Try Jsoup connection if Selenium parsing times out
   └── Extract Title, Company, Location, Description, Posted Date, and Apply Count Status
5. Map to List<JobDto> and return
```

### Search URL Builder Algorithm

```
Keyword: "java spring boot", Location: "hyderabad", Experience: 3, WfhType: 1 (Hybrid)
  │
  ├── 1. Build Path:
  │      keyword → "java-spring-boot-jobs"
  │      location → "java-spring-boot-jobs-in-hyderabad"
  │
  ├── 2. Build Query Params:
  │      k=java%20spring%20boot
  │      l=hyderabad
  │      experience=3
  │      wfhType=1
  │      nignbevent_src=jobsearchDeskGNB
  │
  └── 3. Reconstruct final URL:
         https://www.naukri.com/java-spring-boot-jobs-in-hyderabad?k=java%20spring%20boot&l=hyderabad&experience=3&wfhType=1&nignbevent_src=jobsearchDeskGNB
```

---

## Utility Classes

### `RateLimiter`

Uses randomized delays to simulate human behavior and bypass anti-bot detections:

- `microDelay()`: 300ms - 800ms (typing input)
- `humanDelay()`: 1.5s - 4.0s (clicks, pagination)
- `longDelay()`: 5.0s - 12.0s (page navigation)
- `loginDelay()`: 12.0s - 18.0s (credential validation)

---

## REST Clients

### `UserServiceClient`

```java
// GET /users/by-email/{email}
UserDto getUserByEmail(String email)
```

### `JobServiceClient`

```java
// POST /add-jobs/bulk
List<JobDto> postBulkJobs(List<JobDto> jobs)
```

---

## Full Request Flow: `GET /naukri-jobs/scrape`

```
Client → API Gateway (8080)
  │ Route: /naukri-jobs/** → naukri-service (8085)
  ▼
NaukriController.scrapeNaukriJobs(naukriUserEmail, keyword, location, experience, wfhType, pageCount)
  ▼
NaukriScrapingServiceImpl.scrapeAndSaveNaukriJobs(...)
  │
  ├── 1. userServiceClient.getUserByEmail(naukriUserEmail)
  │       └── GET http://user-service:8082/users/by-email/{email}
  │           Returns: UserDto { email, encryptedPassword }
  │
  ├── 2. naukriJobBot.naukriLogin(email, decryptedPassword)
  │       └── Selenium: Navigate to login modal → fill fields → submit → wait for cookie session
  │
  ├── 3. naukriJobBot.scrapeJobsBySearchUrl(searchUrl, pageCount)
  │       └── Selenium: Iterate search lists → pull data-job-id → navigate details → parse selectors
  │           Returns: List<JobDto>
  │
  ├── 4. Set platform = "Naukri" on each job
  │
  └── 5. jobServiceClient.postBulkJobs(scrapedJobs)
          └── POST http://job-service:8083/add-jobs/bulk
              Returns: List<JobDto> (newly saved only)
  ▼
Return: "Naukri scraping completed. Saved/Updated N jobs."
```

---

## Docker Configuration

```yaml
naukri-service:
  ports: ["8085:8085"]
  environment:
    - USER_SERVICE_BASE_URL=http://user-service:8082
    - JOB_SERVICE_BASE_URL=http://job-service:8083
    - SELENIUM_HEADLESS=true
  depends_on:
    - user-service
    - job-service
  networks:
    - job-automation-network
```