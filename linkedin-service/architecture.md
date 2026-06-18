# LinkedIn Service — Service Documentation

## Overview

| Property | Value |
|---|---|
| **Module** | `linkedin-service` |
| **Group ID** | `com.jobautomation` |
| **Artifact ID** | `linkedin-service` |
| **Main Class** | `com.jobautomation.linkedin.LinkedInServiceApplication` |
| **Port (Local)** | `8084` |
| **Port (Docker)** | `8084` |
| **Database** | None (stateless — no local DB) |
| **External Systems** | `LinkedIn.com` (Selenium), `user-service:8082`, `job-service:8083` |

---

## Package Structure

```
com.jobautomation.linkedin
├── LinkedInServiceApplication.java
├── automation
│   └── LinkedInLoginBot.java          # Selenium scraping bot
├── client
│   ├── UserServiceClient.java         # WebClient → user-service
│   └── JobServiceClient.java          # WebClient → job-service
├── config
│   ├── SeleniumConfig.java            # @Lazy ChromeDriver bean
│   └── WebClientConfig.java           # WebClient beans (10MB buffer)
├── controller
│   └── LinkedInController.java        # /linkedin-jobs/**
├── service
│   ├── LinkedInScrapingService.java   (interface)
│   └── impl
│       └── LinkedInScrapingServiceImpl.java
└── utils
    ├── Generic_Keywords.java          # Low-level Selenium helpers
    ├── KeyLibrary.java                # String/Jsoup parsing utilities
    ├── RateLimiter.java               # Randomised human-like delays
    ├── ScreenshotUtils.java           # Failure screenshot capture
    └── WaitUtils.java                 # Explicit condition-based waits
```

---

## Configuration

**File:** `linkedin-service/src/main/resources/application.properties`

```properties
server.port=8084
spring.application.name=linkedin-service

user-service.base-url=${USER_SERVICE_BASE_URL:http://localhost:8082}
job-service.base-url=${JOB_SERVICE_BASE_URL:http://localhost:8083}

selenium.headless=false

logging.level.com.jobautomation=DEBUG
```

---

## API Endpoints

### `LinkedInController` — `@RequestMapping("/linkedin-jobs")`

| Method | Path | Params | Description |
|---|---|---|---|
| GET | `/linkedin-jobs/scrape` | `linkedinUserEmailorUserName` | Scrape recommended + easy-apply LinkedIn jobs |
| GET | `/linkedin-jobs/scrape-with-filters` | `linkedinUserEmailorUserName`, `Title`, `timeHours` | Scrape jobs by keyword and time filter |
| POST | `/linkedin-jobs/apply-job/{id}` | — | Trigger application for one job (placeholder) |
| POST | `/linkedin-jobs/apply-jobs-list` | `List<Long>` | Trigger application for multiple jobs (placeholder) |

---

## Core Component: `LinkedInLoginBot`

A Selenium `@Component` that drives Chrome to interact with LinkedIn.

| Method | Description |
|---|---|
| `linkedinLogin(email, password)` | Navigate to login page, enter credentials, click Sign In, wait for session |
| `scrapeJobs()` | Scrape recommended + easy-apply jobs from LinkedIn home page |
| `scrapeLatestJobsByTitleandTime(title, hours)` | Search for jobs by keyword posted within N hours |
| `scrapeLinkedInJobDetails(List<String> jobUrls)` | Visit each job URL and extract structured `JobDto` |
| `quitWebDriver()` | Gracefully close Chrome browser |

### Scraping Strategy (`scrapeJobs`)

```
1. Click Jobs tab (or navigate directly to https://www.linkedin.com/jobs/?)
2. Parse "Show all" links for Easy Apply and Recommended collections
3. Generate paginated URLs for each collection (6 pages × 24 jobs each)
4. For each page URL:
   └── Extract /jobs/view/<id>/ links from .ember-view elements
5. Deduplicate job URLs
6. For each unique job URL:
   └── Navigate → wait for #workspace → parse via Jsoup
       ├── title: XPath //*[@id='workspace']//div[div/a]/following-sibling::div[1]/div/p
       ├── company: XPath (//*[@id='workspace']//div[div/a])[1]
       ├── location: XPath //*[@id='workspace']//div/div/div/div[1]/div/div/div[2]/div/div[1]/p/span[1]
       ├── description: XPath //section//div[3]/div[3]/div/div/div/div/div/p
       ├── jobPosted: span:nth-of-type(4) within //*[@id='workspace']//p
       └── applyCountStatus: //*[@id='workspace']//p/span[7]
```

### Scraping Strategy (`scrapeLatestJobsByTitleandTime`)

```
1. Build URL: https://www.linkedin.com/jobs/search-results/?f_TPR=r<seconds>&keywords=<title>+Developer
2. Detect pagination buttons: .jobs-search-pagination__pages li button span
3. For each page (0 to maxPage × 25 offset):
   └── Extract job IDs from .job-card-job-posting-card-wrapper links
       → reconstruct as https://www.linkedin.com/jobs/view/<id>/
4. Scrape each job detail via scrapeLinkedInJobDetails()
```

---

## Utility Classes

### `RateLimiter`

Mimics human browsing patterns with randomised delays to avoid LinkedIn bot detection:

| Method | Range | Use Case |
|---|---|---|
| `microDelay()` | 300ms – 800ms | Between keystrokes, minor interactions |
| `humanDelay()` | 1.5s – 4s | Between page interactions and clicks |
| `longDelay()` | 5s – 12s | After page navigations |
| `loginDelay()` | 12s – 18s | After login submission |

### `WaitUtils`

| Method | Description |
|---|---|
| `waitForVisibility(driver, locator, seconds)` | Wait until element is visible |
| `waitForClickable(driver, locator, seconds)` | Wait until element is clickable |
| `waitForPresenceOfAll(driver, locator, seconds)` | Wait for all matching elements |
| `waitForPageLoad(driver, seconds)` | Wait for `document.readyState == "complete"` |
| `retryOnStaleElement(action, maxRetries)` | Retry action on `StaleElementReferenceException` |

### `ScreenshotUtils`

Captures screenshots to `./screenshots/<label>_<timestamp>.png` on failure.

### `SeleniumConfig`

```java
@Bean @Lazy
public WebDriver webDriver() {
    ChromeOptions options = new ChromeOptions();
    // headless=true in Docker, false in local
    options.addArguments("--disable-blink-features=AutomationControlled");
    options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
    return new ChromeDriver(options);
}
```

The `@Lazy` annotation ensures Chrome only opens on first request — not on Spring startup.

---

## REST Clients

### `UserServiceClient`

```java
// GET /users/by-email/{email}
UserDto getUserByEmail(String email)
```

- Throws `InvalidUserException` if user not found.

### `JobServiceClient`

```java
// POST /add-jobs/bulk
List<JobDto> postBulkJobs(List<JobDto> jobs)
```

- Returns empty list on error (logged, not rethrown).

---

## WebClient Configuration

**File:** `WebClientConfig.java`

```java
@Bean("userServiceWebClient")
public WebClient userServiceWebClient() {
    return WebClient.builder()
        .baseUrl(userServiceBaseUrl)                          // http://localhost:8082
        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))  // 10MB
        .defaultHeader("Content-Type", "application/json")
        .build();
}

@Bean("jobServiceWebClient")
public WebClient jobServiceWebClient() {
    return WebClient.builder()
        .baseUrl(jobServiceBaseUrl)                           // http://localhost:8083
        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))  // 10MB
        .defaultHeader("Content-Type", "application/json")
        .build();
}
```

---

## Full Request Flow: `GET /linkedin-jobs/scrape`

```
Client → API Gateway (8080)
  │ Route: /linkedin-jobs/** → linkedin-service (8084)
  ▼
LinkedInController.scrapeJobs(linkedinUserEmailorUserName)
  ▼
LinkedInScrapingServiceImpl.scrapeAndSaveJobs(email)
  │
  ├── 1. userServiceClient.getUserByEmail(email)
  │       └── GET http://user-service:8082/users/by-email/{email}
  │           Returns: UserDto { email, encryptedPassword }
  │
  ├── 2. linkedInLoginBot.linkedinLogin(email, encryptedPassword)
  │       └── Selenium: navigate → enter credentials → click Sign In → wait
  │
  ├── 3. linkedInLoginBot.scrapeJobs()
  │       └── Selenium: navigate collections → extract URLs → scrape details
  │           Returns: List<JobDto>
  │
  ├── 4. Set platform = "LinkedIn" on each job
  │
  └── 5. jobServiceClient.postBulkJobs(scrapedJobs)
          └── POST http://job-service:8083/add-jobs/bulk
              Returns: List<JobDto> (only newly inserted)
  ▼
Return: "Scraping completed. Saved N new jobs."
```

---

## Docker Configuration

```yaml
linkedin-service:
  ports: ["8084:8084"]
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

> [!IMPORTANT]
> In Docker, `SELENIUM_HEADLESS=true` is required. Chrome must run in headless mode since there is no display server. A ChromeDriver matching the installed Chrome version must be present in the image.

---

## Notes

- `encryptedPassword` from user-service is stored as AES-encrypted text. The `Generic_Keywords.decryptValues()` utility decrypts it using the key `publicKeymerchinsight123` before passing to the bot.
- LinkedIn DOM selectors are XPath-based and may need updates if LinkedIn changes their page structure.
- Pagination generates 6 pages per collection × 24–25 jobs per page = up to 300 job URLs per scrape run.