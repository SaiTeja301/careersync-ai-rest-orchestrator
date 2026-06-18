# Config Server

## Overview
- **Purpose:** Centralized, git-backed microservices configuration registry (Proposed).
- **Port:** `8888`
- **Dependencies:** GitHub Repository.
- **Technology Stack:** Spring Cloud Config Server.

## Package Structure (Proposed)
```text
com.jobautomation.configserver
└── ConfigServerApplication.java
```

## APIs
- Exposes property configurations to boot microservices.

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
    UserService->>ConfigServer: Fetch bootstrap properties
    ConfigServer-->>UserService: Return application config
```

## Dependencies
- **Inbound:** Core services.
- **Outbound:** Git repository.

## Security
- Property decryption via symmetric keys.

## Docker
- Alpine build wrapper.

## Key Takeaways
- Enables dynamic property refresh without redeploying microservices.