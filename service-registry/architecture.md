# Service Registry

## Overview
- **Purpose:** Netflix Eureka registry for service registration and discovery (Proposed).
- **Port:** `8761`
- **Technology Stack:** Spring Cloud Netflix Eureka Server.

## Package Structure (Proposed)
```text
com.jobautomation.registry
└── ServiceRegistryApplication.java
```

## Request Flow
```mermaid
---
config:
  theme: base
  themeVariables:
    actorBkg: "#0F766E"
    actorBorder: "#99F6E4"
    actorTextColor: "#FFFFFF"
---
sequenceDiagram
    JobService->>Eureka: Register instance
    NaukriService->>Eureka: Lookup "job-service" hostname
    Eureka-->>NaukriService: Return target IP
```

## Key Takeaways
- Eliminates hardcoded hostname settings inside scraper properties.