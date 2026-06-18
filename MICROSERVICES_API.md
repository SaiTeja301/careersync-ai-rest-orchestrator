# Job Automation Platform: Microservices API Documentation

This document serves as a complete reference for all the independent microservices, their ports, and their REST endpoints, configured based on the `endpoints.txt` specification and gateway routing definitions.

---

## Service Port & Base Path Registry

When running locally, you can call services directly via their dedicated ports or route them through the **API Gateway** (recommended).

| Service Name | Port | Direct URL Base | API Gateway Route | Database / Storage |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | `http://localhost:8080` | N/A | None |
| **User Service** | `8082` | `http://localhost:8082` | `http://localhost:8080/users` & `/resume` | `user_db` (MySQL) |
| **Job Service** | `8083` | `http://localhost:8083` | `http://localhost:8080/add-jobs` & `/apply-job` | `job_db` (MySQL) |
| **LinkedIn Service** | `8084` | `http://localhost:8084` | `http://localhost:8080/linkedin-jobs` | Stateless (Selenium) |
| **Naukri Service** | `8085` | `http://localhost:8085` | `http://localhost:8080/naukri-jobs` | Stateless (Selenium) |
| **AI Recommendation Service** | `8086` | `http://localhost:8086` | `http://localhost:8080/aijobagent` | `ai_db` (MySQL) & Gemini |

---

## 1. API Gateway (`api-gateway` :8080)
Exposes all routing contexts on a single entry point (Port `8080`) and forwards requests downstream to the appropriate microservices.

### Routes Summary:
* `/users/**` & `/resume/**` $\rightarrow$ `user-service` (`http://localhost:8082`)
* `/add-jobs/**` & `/apply-job/**` $\rightarrow$ `job-service` (`http://localhost:8083`)
* `/linkedin-jobs/**` $\rightarrow$ `linkedin-service` (`http://localhost:8084`)
* `/naukri-jobs/**` $\rightarrow$ `naukri-service` (`http://localhost:8085`)
* `/aijobagent/**` $\rightarrow$ `ai-recommendation-service` (`http://localhost:8086`)

---

## 2. User Service (`user-service` :8082)
Manages candidate profiles, automation login credentials, and resume attachments.

### register/Update Profile
* **Endpoint:** `POST /users/add-user-info`
* **Gateway URL:** `POST http://localhost:8080/users/add-user-info`
* **Request Body (JSON):**
  ```json
  {
    "name": "user",
    "email": "user@gmail.com",
    "encryptedPassword": "********",
    "experienceYears": 3,
    "preferredRoles": "Java Developer",
    "preferredCompanies": "TCS, Infosys",
    "remote": false,
    "hybrid": true
  }
  ```
* **Description:** Save or update user login details used by LinkedIn/Naukri bots.

### Get User Info
* **Endpoint:** `GET /users/get-user-info/{userId}`
* **Gateway URL:** `GET http://localhost:8080/users/get-user-info/{userId}`
* **Parameters:** `userId` (Path Variable - Long)
* **Example:** `GET http://localhost:8080/users/get-user-info/1`

### Delete User
* **Endpoint:** `DELETE /users/delete-user-info/{name}/{email}`
* **Gateway URL:** `DELETE http://localhost:8080/users/delete-user-info/{name}/{email}`
* **Parameters:** `name` (Path Variable), `email` (Path Variable)

### Upload/Update Resume File
* **Endpoint:** `POST /resume/update-resume`
* **Gateway URL:** `POST http://localhost:8080/resume/update-resume`
* **Multipart Parameters:**
  - `file` (MultipartFile)
  - `userId` (Long)
* **Example (cURL):**
  ```bash
  curl -X POST http://localhost:8080/resume/update-resume \
    -F "file=@/path/to/resume.pdf" \
    -F "userId=1"
  ```

### Internal API: Fetch Profile by Email
* **Endpoint:** `GET /users/by-email/{email}`
* **Description:** Called internally by scraping microservices to retrieve bot passwords.

### Internal API: Fetch Resume Content
* **Endpoint:** `GET /resume/users/{userId}/resume`
* **Description:** Called by the AI service to fetch raw text content extracted from the resume.

---

## 3. Job Service (`job-service` :8083)
Manages the shared job list, deduplication store, and job application tracking.

### Fetch All Jobs
* **Endpoint:** `GET /add-jobs/get-all-jobs-list`
* **Gateway URL:** `GET http://localhost:8080/add-jobs/get-all-jobs-list`
* **Description:** Fetch all saved jobs across both platforms.

### Fetch Job By ID
* **Endpoint:** `GET /add-jobs/get-search-job/{id}`
* **Gateway URL:** `GET http://localhost:8080/add-jobs/get-search-job/{id}`
* **Parameters:** `id` (Path Variable - Long)

### Delete Job By ID
* **Endpoint:** `DELETE /add-jobs/delete-job/{id}`
* **Gateway URL:** `DELETE http://localhost:8080/add-jobs/delete-job/{id}`

### Bulk Delete Jobs
* **Endpoint:** `POST /add-jobs/delete-job-list`
* **Gateway URL:** `POST http://localhost:8080/add-jobs/delete-job-list`
* **Request Body (JSON):** List of IDs (e.g. `[1, 2, 3]`)

### Update Job Application Status
* **Endpoint:** `POST /apply-job/update-job-status`
* **Gateway URL:** `POST http://localhost:8080/apply-job/update-job-status`
* **Parameters:** 
  - `applicationId` (Query Param - Long)
  - `applied` (Query Param - boolean)
* **Example:** `POST http://localhost:8080/apply-job/update-job-status?applicationId=1&applied=true`

### Internal API: Bulk Save & Deduplicate
* **Endpoint:** `POST /add-jobs/bulk`
* **Description:** Called by `naukri-service` and `linkedin-service` to process and save scraped jobs. Automatically performs deduplication and returns *only* newly saved jobs.

---

## 4. LinkedIn Service (`linkedin-service` :8084)
Drives Selenium simulations to scrape LinkedIn and apply to easy-apply job listings.

### Trigger Recommended LinkedIn Scrape
* **Endpoint:** `GET /linkedin-jobs/scrape`
* **Gateway URL:** `GET http://localhost:8080/linkedin-jobs/scrape`
* **Parameters:** `linkedinUserEmailorUserName` (Query Param - String)
* **Example:** `GET http://localhost:8080/linkedin-jobs/scrape?linkedinUserEmailorUserName=test@example.com`

### Scrape With Search Filters
* **Endpoint:** `GET /linkedin-jobs/scrape-with-filters`
* **Gateway URL:** `GET http://localhost:8080/linkedin-jobs/scrape-with-filters`
* **Parameters:** 
  - `linkedinUserEmailorUserName` (Query Param - String)
  - `Title` (Query Param - String)
  - `timeHours` (Query Param - Integer)
* **Example:** `GET http://localhost:8080/linkedin-jobs/scrape-with-filters?linkedinUserEmailorUserName=test@example.com&Title=java&timeHours=24`

### Trigger Auto-Apply (Single Job)
* **Endpoint:** `POST /linkedin-jobs/apply-job/{id}`
* **Gateway URL:** `POST http://localhost:8080/linkedin-jobs/apply-job/{id}`
* **Parameters:** `id` (Path Variable - Long)

### Trigger Auto-Apply (Batch list)
* **Endpoint:** `POST /linkedin-jobs/apply-jobs-list`
* **Gateway URL:** `POST http://localhost:8080/linkedin-jobs/apply-jobs-list`
* **Request Body (JSON):** List of IDs (e.g. `[1, 2, 3]`)

---

## 5. Naukri Service (`naukri-service` :8085)
Drives Selenium simulations to scrape keyword search results and recommended job pages from Naukri.

### Scrape Keywords & Filters (Full Support — 27 Scenarios)
* **Endpoint:** `GET /naukri-jobs/scrape`
* **Gateway URL:** `GET http://localhost:8080/naukri-jobs/scrape`
* **Parameters:**
  - `naukriUserEmail` (Query Param - String) [Required]
  - `keyword` (Query Param - String) [Optional]
  - `location` (Query Param - String) [Optional]
  - `experience` (Query Param - Integer) [Optional]
  - `wfhType` (Query Param - Integer) [Optional - 0=WFO, 1=Hybrid, 2=Remote]
  - `pageCount` (Query Param - Integer) [Optional, default=5]
* **Example (Hybrid & Experience):**
  ```http
  http://localhost:8080/naukri-jobs/scrape?naukriUserEmail=venkatasaitejaakula@gmail.com&keyword=java spring boot&location=hyderabad&experience=3&wfhType=1&pageCount=3
  ```

### Scrape Personalized Recommended Jobs
* **Endpoint:** `GET /naukri-jobs/scrape-recommended`
* **Gateway URL:** `GET http://localhost:8080/naukri-jobs/scrape-recommended`
* **Parameters:** `naukriUserEmail` (Query Param - String) [Required]
* **Example:**
  ```http
  http://localhost:8080/naukri-jobs/scrape-recommended?naukriUserEmail=venkatasaitejaakula@gmail.com
  ```

---

## 6. AI Recommendation Service (`ai-recommendation-service` :8086)
Interfaces with Google Gemini LLM using reactive Mono endpoints to analyze resumes and suggest jobs.

### Ask AI Agent
* **Endpoint:** `POST /aijobagent/ask-agent`
* **Gateway URL:** `POST http://localhost:8080/aijobagent/ask-agent`
* **Parameters:**
  - `prompt` (Query Param - String)
  - `userId` (Query Param - Long)
* **Example:**
  ```http
  http://localhost:8080/aijobagent/ask-agent?prompt=How do my skills match saved jobs?&userId=1
  ```

### Analyze Resume
* **Endpoint:** `GET /aijobagent/analyze-resume`
* **Gateway URL:** `GET http://localhost:8080/aijobagent/analyze-resume`
* **Parameters:** `userId` (Query Param - Long)
* **Example:**
  ```http
  http://localhost:8080/aijobagent/analyze-resume?userId=1
  ```
