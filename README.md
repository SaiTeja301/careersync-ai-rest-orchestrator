# 🏗️ Job Automation Platform — Microservices Setup & Running Guide

Welcome to the newly decomposed **Job Automation Platform**. This multi-module Spring Boot application automates job searching, scraping, deduplication, and applying on LinkedIn and Naukri using Selenium, and provides AI-powered resume and career advice using Google Gemini.

---

## 🗺️ Microservice Architecture & Port Map

The monolith has been migrated into **6 microservices** and **1 shared library**:

| Service/Module | Port | Database | Primary Responsibility |
|---|---|---|---|
| 📦 `common-library` | *None (JAR)* | — | Shared DTOs, Exceptions (`InvalidUserException`), and Constants |
| 🔀 `api-gateway` | `8080` | — | Spring Cloud Gateway – routes external traffic to backend services |
| 👤 `user-service` | `8082` | `user_db` | Manages users, credentials, preferences, and resumes |
| 💼 `job-service` | `8083` | `job_db` | Job catalog, status tracking, and the job deduplication engine |
| 🔗 `linkedin-service` | `8084` | — | Stateless Selenium worker for LinkedIn login, scraping, and easy-apply |
| 📋 `naukri-service` | `8085` | — | Stateless Selenium worker for Naukri login, scraping, and search URL building |
| 🤖 `ai-recommendation-service` | `8086` | `ai_db` | Gemini LLM integration, prompt assembly, and response store |

---

## 🗄️ Database Setup

Ensure MySQL is running locally. The platform relies on three separate schemas:
1. `user_db`
2. `job_db`
3. `ai_db`

Spring Boot's `ddl-auto=update` is active across all services, so schemas and tables will be created automatically upon startup if they do not exist.

> [!NOTE]
> Database configurations can be modified in each service's `application.properties` under `src/main/resources/`. The default configuration assumes:
> - **URL:** `jdbc:mysql://localhost:3306/<db_name>`
> - **Username:** `root`
> - **Password:** `root`

---

## 🛠️ Build and Compilation

To compile the entire project, run the following Maven command from the root directory (`job-automation-platform/`):

```bash
mvn clean compile
```

To build and package the services into runnable JARs:

```bash
mvn clean package -DskipTests
```

---

## 🚀 Running the Services

### Prerequisites
1. **Google Gemini API Key**: The AI service requires a Gemini API key. Set it in your environment variables:
   - **Windows (PowerShell):** `$env:GEMINI_API_KEY="your_api_key"`
   - **Linux/macOS:** `export GEMINI_API_KEY="your_api_key"`
2. **ChromeDriver**: Ensure you have Chrome and ChromeDriver installed for the Selenium bots (`linkedin-service` and `naukri-service`).

### Startup Order
For local execution, run the services in the following order:

1. **User Service** (Port 8082)
   ```bash
   cd user-service
   mvn spring-boot:run
   ```
2. **Job Service** (Port 8083)
   ```bash
   cd job-service
   mvn spring-boot:run
   ```
3. **LinkedIn Service** (Port 8084)
   ```bash
   cd linkedin-service
   mvn spring-boot:run
   ```
4. **Naukri Service** (Port 8085)
   ```bash
   cd naukri-service
   mvn spring-boot:run
   ```
5. **AI Recommendation Service** (Port 8086)
   ```bash
   cd ai-recommendation-service
   mvn spring-boot:run
   ```
6. **API Gateway** (Port 8080)
   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```

---

## 🔗 Inter-Service Communication Details

The services interact via synchronous REST calls using Spring's `WebClient`. Key paths are:
- `user-service` exposes `/users/by-email/{email}` to return decrypted login credentials.
- `job-service` exposes `/add-jobs/jobs/bulk` (a POST endpoint) for scrapers to publish newly crawled items.
- `ai-recommendation-service` reads user profiles from `user-service` (`/users/{id}`), resume documents from `user-service` (`/resume/users/{userId}/resume`), queries jobs and application statuses from `job-service` (`/add-jobs/jobs` and `/apply-job/jobs/applications`), and calls the Gemini API to construct recommendations.

---

## 📡 Gateway Routing Rules (`localhost:8080`)

External clients should call backend endpoints through the API Gateway at port `8080`. The Gateway routes requests as follows:

- **User operations:** `/users/**` and `/resume/**` → `user-service` (Port `8082`)
- **Job operations:** `/add-jobs/**` and `/apply-job/**` → `job-service` (Port `8083`)
- **LinkedIn operations:** `/linkedin-jobs/**` → `linkedin-service` (Port `8084`)
- **Naukri operations:** `/naukri-jobs/**` → `naukri-service` (Port `8085`)
- **AI Recommendation Engine:** `/aijobagent/**` → `ai-recommendation-service` (Port `8086`)
