# HW2 Implementation Plan

## Overview
Build a customer support ticket management system with multi-format import, auto-classification, and comprehensive tests (>85% coverage).

**Tech Stack**: Kotlin + Spring Boot (consistent with HW1)  
**Key Complexity**: File parsing, classification logic, extensive testing, multi-level documentation

---

## Phase 1: Foundation & Setup (Tasks 1-3)

### Task 1.1: Project Structure & Dependencies
- [ ] Create `homework-2/build.gradle.kts` (Spring Boot, testing frameworks, XML/CSV libraries)
- [ ] Create OpenAPI spec: `openapi-spec/homework-2.yaml` (ticket model + endpoints)
- [ ] Configure code generation (org.openapi.generator)
- [ ] Set up spotless formatting

**Dependencies to add**:
- `org.apache.commons:commons-csv` (CSV parsing)
- `com.fasterxml.jackson.dataformat:jackson-dataformat-xml` (XML parsing)
- `org.junit.jupiter:junit-jupiter` (testing)
- `io.mockk:mockk` (mocking)
- `org.assertj:assertj-core` (assertions)


---

### Task 1.2: Ticket Model & DTOs
- [ ] Define `Ticket` data class with all fields (id, customer_id, category, priority, status, etc.)
- [ ] Define enum classes: `TicketCategory`, `TicketPriority`, `TicketStatus`, `TicketSource`, `DeviceType`
- [ ] Define `TicketMetadata` nested data class
- [ ] Generate DTOs from OpenAPI spec

**Key validation rules**:
- `subject`: 1-200 chars
- `description`: 10-2000 chars
- `customer_email`: valid email format
- Enums: only valid values


---

### Task 1.3: Core API Endpoints
- [ ] `POST /tickets` — Create single ticket
- [ ] `GET /tickets` — List with filtering (category, priority, status, customer_id)
- [ ] `GET /tickets/:id` — Get by ID
- [ ] `PUT /tickets/:id` — Update ticket
- [ ] `DELETE /tickets/:id` — Delete ticket

**Implementation pattern** (from HW1):
- Thin controller → Service → Validator pattern
- In-memory storage (ConcurrentHashMap)
- Meaningful error responses (400, 404, 422)


### Task 1.4: File Import Infrastructure
- [ ] Abstract `TicketImporter` interface
- [ ] `CsvTicketImporter` implementation
- [ ] `JsonTicketImporter` implementation  
- [ ] `XmlTicketImporter` implementation
- [ ] `ImportResult` DTO (total, successful, failed with error details)

**Error handling**:
- Parse failures → include line number + reason
- Validation failures → include field + expected format
- Return HTTP 400 with detailed summary


---

### Task 1.5: Bulk Import Endpoint
- [ ] `POST /tickets/import` — multipart file upload
- [ ] Detect file format (from MIME type or file extension)
- [ ] Delegate to appropriate importer
- [ ] Return `ImportResult` with per-record error details
- [ ] Handle concurrent uploads safely


---

## Phase 2: Auto-Classification (Tasks 2)

### Task 2.1: Classification Logic
- [ ] `TicketClassifier` service with rule-based categorization
- [ ] `PriorityAssigner` service with keyword matching
- [ ] `ClassificationResult` DTO (category, priority, confidence, keywords_found, reasoning)

**Rules engine**:
- Keywords for each category (e.g., "login", "password" → account_access)
- Keywords for each priority level
- Confidence score based on keyword match count
- Fallback: "other" category, "medium" priority


---

### Task 2.2: Auto-Classify Endpoint
- [ ] `POST /tickets/:id/auto-classify` — classify existing ticket
- [ ] Flag to run auto-classify on ticket creation (optional)
- [ ] Store classification result + confidence in ticket
- [ ] Allow manual override of auto-classification
---

## Phase 3: Testing (Task 3) — 56 Tests Total

### Task 3.1: Model & Validation Tests (15 tests)
```
tests/
├── unit/
│   ├── TicketModelTest (9 tests)
│   │   - Valid ticket creation
│   │   - Field length validation (subject, description)
│   │   - Email format validation
│   │   - Enum validation (category, priority, status)
│   │   - Null/empty field handling
│   │   - Timestamp handling
│   │   - Metadata validation
│   │   - Edge cases (min/max lengths, special chars)
│   │   - Invalid enum values
│   └── TicketValidatorTest (6 tests)
│       - Required fields
│       - String length bounds
│       - Email format
│       - Enum values
│       - Metadata structure
│       - Batch validation
```

---

### Task 3.2: File Parsing Tests (16 tests)
```
├── CsvImporterTest (5 tests)
│   - Valid CSV import (50 records)
│   - Malformed CSV (missing columns, extra columns)
│   - Invalid data in cells (email, length, enums)
│   - Empty CSV
│   - Edge cases (quotes, commas in values)
├── JsonImporterTest (5 tests)
│   - Valid JSON import (20 records)
│   - Invalid JSON syntax
│   - Missing required fields per record
│   - Type mismatches (string as number)
│   - Empty array
├── XmlImporterTest (6 tests)
│   - Valid XML import (30 records)
│   - Malformed XML (unclosed tags)
│   - Missing nested elements
│   - Attribute vs element parsing
│   - Empty XML document
│   - Namespace handling
```

**Fixtures**: Create sample valid/invalid files in `tests/fixtures/`

---

### Task 3.3: API & Integration Tests (25 tests)
```
├── TicketControllerTest (11 tests)
│   - Create ticket (201 Created)
│   - Create with validation error (400 Bad Request)
│   - Get existing ticket (200)
│   - Get non-existent (404)
│   - List all tickets (200)
│   - List with filters (category, priority, status)
│   - Update ticket (200)
│   - Delete ticket (204)
│   - Delete non-existent (404)
│   - Concurrent requests (thread-safe)
│   - Health check
├── ImportControllerTest (5 tests)
│   - Import CSV successfully
│   - Import with partial failures
│   - Import invalid file format
│   - Concurrent imports
│   - Large file import (100+ records)
├── ClassificationTest (9 tests)
│   - Auto-classify endpoint
│   - Category detection (each category 1 test)
│   - Priority assignment (4 levels)
│   - Confidence scoring
│   - Override classification
│   - Edge cases (empty description, null fields)
```

---

## Phase 4: Documentation (Task 4)

### Task 4.1: README.md
- Project overview & features
- Architecture overview (Mermaid diagram)
- Tech stack & setup
- Build & run instructions
- Test execution


---

### Task 4.2: API_REFERENCE.md
- All 7 endpoints documented
- Request/response schemas
- cURL examples for each
- Error response formats
- Sample data

---

### Task 4.3: ARCHITECTURE.md
- System design (Mermaid C4/flowchart)
- Component responsibilities
- Data flow (sequence diagrams)
- Design decisions (why Spring Boot, why in-memory, classification strategy)
- Security & performance notes

---

### Task 4.4: TESTING_GUIDE.md
- Test pyramid (unit/integration/e2e)
- Coverage breakdown by component
- How to run tests locally
- Sample data locations
- Manual testing checklist
- Performance benchmark results

---

### Task 4.5: HOWTORUN.md
- Prerequisites
- Build instructions
- Run instructions (bootRun, JAR)
- Verification (health, Swagger UI, sample endpoints)
- Troubleshooting


---

## Phase 5: Performance & Integration Tests (Task 5)

### Task 5.1: Performance Tests
- [ ] Import 1000+ records benchmark
- [ ] Concurrent requests (20+ simultaneous)
- [ ] Memory usage under load
- [ ] Response time p95/p99

---

### Task 5.2: End-to-End Workflows
- [ ] Complete ticket lifecycle (create → classify → update → resolve)
- [ ] Bulk import → verify auto-classification
- [ ] Filtering with multiple criteria
- [ ] Error recovery & retries

**Duration**: ~2 hours

---

## Phase 6: Quality Gates & Submission

### Task 6.1: Verification
- [ ] Build: `./gradlew clean build` passes
- [ ] Format: `./gradlew spotlessCheck` passes
- [ ] Tests: >85% coverage, all pass
- [ ] App starts: `./gradlew :homework-2:bootRun`
- [ ] Health check: `/actuator/health` responds
- [ ] API contract: All endpoints respond correctly

**Duration**: ~1 hour

---

### Task 6.2: Screenshots & Evidence
- [ ] App startup screenshot
- [ ] Swagger UI with all endpoints
- [ ] Test coverage report (>85%)
- [ ] Sample API requests (POST /tickets, POST /tickets/import, POST /tickets/:id/auto-classify)
- [ ] Error handling (invalid input)

---

## Task Summary

| Phase | Tasks | Est. Hours |
|-------|-------|-----------|
| 1. Foundation | Project setup, model, endpoints, import | 10-12 |
| 2. Classification | Logic + endpoint | 3 |
| 3. Testing | 56 tests, 85%+ coverage | 12-15 |
| 4. Documentation | 5 docs (README, API, ARCH, TESTING, HOWTORUN) | 7-8 |
| 5. Performance | Load tests, e2e tests | 4 |
| 6. QA & Submission | Verification, screenshots, PR | 2 |
| **Total** | | **38-44 hours** |

---

## Execution Order

1. ✅ Task 1.1: Setup (dependencies, build config)
2. ✅ Task 1.2: Models & DTOs
3. ✅ Task 1.3: API endpoints (create, list, get, update, delete)
4. ✅ Task 1.4: File importers (CSV, JSON, XML)
5. ✅ Task 1.5: Import endpoint + tests
6. ✅ Task 2: Auto-classification
7. ✅ Task 3: Comprehensive test suite (56 tests)
8. ✅ Task 4: Documentation (5 files)
9. ✅ Task 5: Performance & integration tests
10. ✅ Task 6: QA & submission

**Parallel opportunities**:
- Documentation can start after API endpoints are stable
- Tests can be written alongside implementation

---

## Key Differences from HW1

1. **No OpenAPI-first**: HW2 uses traditional REST design (not spec-driven)
2. **Heavy testing focus**: >85% coverage required (vs. basic testing in HW1)
3. **File parsing complexity**: CSV/JSON/XML handling (new domain)
4. **Classification logic**: Keyword-based categorization (new algorithmic challenge)
5. **Multi-level documentation**: 5 docs for different audiences (expanded from 2)
6. **Performance testing**: Concurrent requests, load testing (new)

---

## Optimization Notes

- **Parallel imports**: Design `TicketImporter` to support parallel file processing
- **Batch validation**: Accumulate errors before returning, don't fail on first error
- **Efficient search**: Use indexes for filtering (category, priority, status)
- **Memory management**: Stream large files instead of loading into memory
- **Test data**: Use fixtures instead of hardcoding sample data

---