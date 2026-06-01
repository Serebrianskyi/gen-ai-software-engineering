# Generate README Skill

A custom Claude Code skill that generates comprehensive README.md files for homework assignments based on HW1 and HW2 templates.

## How to Use

1. Run the skill with your homework details
2. The skill will create a README.md based on whether it's a full template or student template
3. Customize the generated file with your specific project details

## Full Template (HW1/HW3+ Style)

```markdown
# {title}

> **Student Name**: {student-name}

**Status**: ✅ Complete and Running  
**Technology**: {technology} | OpenAPI 3.0 | Gradle Multi-Module

{description}

---

## 🚀 Startup Guide

### Prerequisites

- **Java 17+** - Check with `java -version`
- **Gradle** - Included via wrapper (`./gradlew`)
- **Git** - For version control

### Clone & Setup

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/ai-workshops.git
cd ai-workshops
```

### Run the Application

```bash
# Option 1: Run via Gradle (recommended for development)
./gradlew :homework-{number}:bootRun

# Option 2: Build and run JAR directly (faster startup)
./gradlew :homework-{number}:bootJar
java -jar homework-{number}/build/libs/homework-{number}-1.0.0.jar
```

**Expected Output:**
```
Started Application in X.XXX seconds
Tomcat started on port 8080
```

### Verify Installation & Access Swagger UI

Once running, access the interactive API documentation:

#### 🎨 Swagger UI (Interactive API Explorer)
- **Main**: http://localhost:8080/swagger-ui.html

#### 📋 API Specification
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/api-docs.yaml
- **Source**: [`openapi-spec/homework-{number}.yaml`](../openapi-spec/homework-{number}.yaml)

#### ✅ Health Check
- **Endpoint**: http://localhost:8080/actuator/health
- **Expected**: `{"status":"UP"}`

### Build & Test

```bash
# Build the project (with tests)
./gradlew clean build

# Build without tests (faster)
./gradlew build -x test

# Run tests only
./gradlew test
```

### Project Structure

```
homework-{number}/
├── build.gradle.kts                      # Module build configuration
├── src/
│   ├── main/
│   │   ├── kotlin/com/
│   │   │   ├── Application.kt            # Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   └── SwaggerConfig.kt      # OpenAPI/Swagger configuration
│   │   │   ├── controller/
│   │   │   │   └── YourController.kt     # REST API endpoints
│   │   │   ├── service/
│   │   │   │   └── YourService.kt        # Business logic & data management
│   │   │   ├── validator/
│   │   │   │   └── YourValidator.kt      # Input validation
│   │   │   └── model/
│   │   │       └── YourModel.kt          # Domain models & enums
│   │   └── resources/
│   │       └── application.yml           # Spring Boot configuration
│   └── test/                             # Unit tests
└── build/
    └── libs/homework-{number}-1.0.0.jar  # Compiled JAR artifact
```

---

## 🔗 Dependency Architecture Scheme

```
┌─────────────────────────────────────────────────────────────┐
│              openapi-spec/homework-{number}.yaml                   │
│              (API Contract - Source of Truth)               │
└────────────────────────┬────────────────────────────────────┘
                         │ generates via
                         ↓ openapi-generator plugin
┌─────────────────────────────────────────────────────────────┐
│              :openapi-spec Module (v0.0.1)                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Generated DTOs & Models (from OpenAPI schemas)        │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────┬──────────────────────────────────────────┘
                 │ depends on (project dependency)
                 ↓
┌─────────────────────────────────────────────────────────────┐
│            :homework-{number} Module (v1.0.0)                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Application.kt                                        │  │
│  │ YourController.kt                                     │  │
│  │ YourService.kt                                        │  │
│  │ YourValidator.kt                                      │  │
│  │ SwaggerConfig.kt                                      │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────┬──────────────────────────────────────────┘
                 │ provides REST API on
                 ↓
          http://localhost:8080/api/v{number}/*
```

**Benefits of this architecture**:
- ✅ **Separation of Concerns** - Spec generation isolated from business logic
- ✅ **Single Source of Truth** - OpenAPI spec drives all API contracts
- ✅ **Version Independence** - Spec (v0.0.1) and app (v1.0.0) evolve separately
- ✅ **Type Safety** - Generated models with full validation
- ✅ **Auto-Documentation** - Swagger UI auto-generated from spec
- ✅ **Reusability** - :openapi-spec can be used by multiple applications

---

## 📚 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v{number}/endpoint` | [Description] |
| `POST` | `/api/v{number}/endpoint` | [Description] |
| `GET` | `/api/v{number}/endpoint/{id}` | [Description] |

## 🔍 Example Requests

```bash
# Example GET request
curl http://localhost:8080/api/v{number}/endpoint

# Example POST request
curl -X POST http://localhost:8080/api/v{number}/endpoint \
  -H "Content-Type: application/json" \
  -d '{ "field": "value" }'
```

---

## ✅ Validation Rules

| Field | Rule |
|-------|------|
| **Field** | Description |

---

## 📊 Data Model

### Model Name
```json
{
  "field": "value"
}
```

---

## 📝 Implementation Notes

### Design Decisions

1. **Design Decision 1**: Explanation
2. **Design Decision 2**: Explanation

## 📄 Files Overview

| File | Purpose |
|------|---------|
| `openapi-spec/homework-{number}.yaml` | API specification |
| `Application.kt` | Spring Boot entry point |
| `SwaggerConfig.kt` | OpenAPI/Swagger setup |
| `YourController.kt` | REST endpoints |
| `YourService.kt` | Business logic & data |
| `YourValidator.kt` | Input validation |
| `application.yml` | Spring Boot config |

<div align="center">

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>

</div>
```

---

## Student Template (HW2 Style)

```markdown
# 🏦 {title}

> **Student Name**: {student-name}
> **Date Submitted**: [Date]
> **AI Tools Used**: [List tools, e.g., Claude Code, GitHub Copilot]

---

## 📋 Project Overview

{description}

### Key Features

- [ ] Feature 1
- [ ] Feature 2
- [ ] Feature 3

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Gradle
- Git

### Installation

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/ai-workshops.git
cd ai-workshops
```

### Running the Application

```bash
./gradlew :homework-{number}:bootRun
```

---

## 📚 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v{number}/... | [Description] |
| POST | /api/v{number}/... | [Description] |

---

## ✅ Validation Rules

| Field | Rule |
|-------|------|
| Field 1 | Rule 1 |
| Field 2 | Rule 2 |

---

## 📊 Data Model

[Add your data models here]

---

## 📝 Implementation Notes

### Design Decisions

1. **Decision 1**: Explanation
2. **Decision 2**: Explanation

### Project Structure

```
homework-{number}/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── [Your implementation]
│   │   └── resources/
│   └── test/
└── README.md
```

---

<div align="center">

*This project was completed as part of the AI-Assisted Development course.*

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>

</div>
```

---

## Usage Instructions

1. **For Full Template (HW1/HW3+)**: Use the full template with comprehensive sections
2. **For Student Template (HW2)**: Use the student template for assignment submissions
3. **Customize**: Replace placeholders like `{title}`, `{number}`, `{description}`, `{technology}` with your actual values

## Variables to Replace

- `{number}` - Homework number
- `{title}` - Project title
- `{student-name}` - Student name (auto-populated if provided)
- `{description}` - Project description
- `{technology}` - Technology stack (e.g., "Kotlin + Spring Boot 3.2.3")