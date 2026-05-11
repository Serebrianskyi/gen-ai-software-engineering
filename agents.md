# Agent Guidance for AI-Assisted Development

### API Design Approaches (HW1 vs HW2+)

**HW1 Pattern (OpenAPI-First)**:
- Define API contract first in `openapi-spec/homework-X.yaml`
- Use `org.openapi.generator` to generate models and interfaces
- Spec is source of truth; implementation follows spec
- Best when: spec is provided upfront, API contracts are critical

**HW2+ Pattern (Traditional REST, Domain-First)**:
- Define data models first (data classes, enums, validation rules)
- Build endpoints that serve those models
- Create documentation AFTER implementation (describe what exists)
- No code generation; manual DTO management
- Swagger/OpenAPI via `springdoc-openapi` (auto-generated from annotations)
- Best when: requirements evolve, testing/logic complexity is high, domain is the focus

**Key difference**: HW1 spec-drives-code; HW2+ code-drives-spec.

### Multi-Module Architecture
- **Separation of concerns**: Keep API specification generation isolated from application logic
- **Module per homework**: Each homework gets its own module (`:homework-1`, `:homework-2`, etc.)
- **Reusable specs**: Specs in `openapi-spec/` can be shared; name files clearly (e.g., `homework-1.yaml`)
- **Clean dependencies**: Modules depend on generated specs, not on each other
- **Version alignment**: All modules must use the same Kotlin and Spring Boot versions (root `build.gradle.kts` is source of truth)

### Spring Boot Best Practices
- **Framework conventions over configuration**: Use Spring's standard patterns
- **Validation at service layer**: Business rules and complex validation belong in services
- **Controllers are thin**: Controllers map HTTP to service calls; logic stays in services
- **Dependency injection**: Use Spring's @Service, @Repository, @Controller for lifecycle management
- **Thread-safe storage**: Use `ConcurrentHashMap` for in-memory state; never use `HashMap` in a Spring singleton

---

## 🏗️ Project Structure

```
ai-workshops/
├── agents.md                    # This file
├── QUALITY_GATES.md             # Quality standards for all homeworks
├── settings.gradle.kts          # Multi-module configuration
├── build.gradle.kts             # Root build config (versions + spotless)
├── .editorconfig                # Formatting rules for ktlint
├── openapi-spec/                # API specification module (HW1)
│   ├── build.gradle.kts
│   ├── homework-1.yaml          # HW1 API spec
│   └── build/generated/         # Generated code (read-only, git-ignored)
├── homework-1/                  # Banking transaction API (OpenAPI-first)
│   ├── build.gradle.kts
│   ├── README.md
│   ├── HOWTORUN.md
│   ├── src/main/kotlin/
│   ├── src/main/resources/
│   └── docs/screenshots/
├── homework-2/                  # Customer support ticket system (domain-first)
│   ├── build.gradle.kts
│   ├── README.md
│   ├── HOWTORUN.md
│   ├── PR_CHECKLIST.md
│   ├── demo/                    # Sample data files (CSV, JSON, XML)
│   ├── docs/                    # API reference, architecture, diagrams
│   └── src/
└── ...
```

**Key rules**:
- Each homework gets a module in `homework-X/`
- HW1 shared API specs live in `openapi-spec/`; HW2+ embed docs in `docs/`
- Generated code goes to `build/generated/` (git-ignored)
- Documentation is per-module: `README.md`, `HOWTORUN.md`, `PR_CHECKLIST.md`
- Sample data lives in `demo/` for import-capable projects

---

## 🔧 Build & Development Setup

### Gradle Configuration
Each homework module must have a `build.gradle.kts` that:
1. Declares plugins: `kotlin("jvm")`, `kotlin("plugin.spring")`, `id("org.springframework.boot")`, `id("io.spring.dependency-management")`
2. Optionally includes `id("org.openapi.generator")` if generating from spec (HW1 pattern)
3. Optionally includes `jacoco` for coverage reports
4. Includes dependency management and testing setup

**Version alignment rule**: Plugin versions in submodule `build.gradle.kts` must match root `build.gradle.kts`. Mismatched Kotlin versions cause classpath conflicts.

### OpenAPI Code Generation (HW1 Pattern)
```kotlin
plugins {
    id("org.openapi.generator")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${projectDir}/path-to-spec.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("com.banking.api")
    modelPackage.set("com.banking.model.generated")
    packageName.set("com.banking.generated")
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "documentationProvider" to "springdoc",
    ))
}

tasks.compileKotlin {
    dependsOn("openApiGenerate")
}

sourceSets {
    main {
        kotlin {
            srcDir("$buildDir/generated/openapi/src/main/kotlin")
        }
    }
}
```

### Swagger UI Integration (HW2+ Pattern)
Instead of OpenAPI generation, use `springdoc-openapi` for auto-generated Swagger UI:
```kotlin
// build.gradle.kts
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.4")
```
Swagger UI is then available at `http://localhost:8080/swagger-ui.html` with no additional config.

For file upload endpoints, add `consumes` to ensure correct file picker widget:
```kotlin
@PostMapping("/import", consumes = ["multipart/form-data"])
fun importData(@RequestParam file: MultipartFile): ResponseEntity<Unit> = TODO()
```

### Code Formatting
Spotless runs at root level targeting `src/main/**/*.kt` only (test files are exempt from wildcard import rule):
```kotlin
// root build.gradle.kts
spotless {
    kotlin {
        target("homework-1/src/main/**/*.kt")
        ktlint("0.50.0")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint("0.50.0")
    }
}
```
The `.editorconfig` file at root defines formatting rules — **do not commit unformatted code**.

Pre-commit hook runs `./gradlew spotlessApply` automatically before every commit.

### JaCoCo Coverage Reports
```kotlin
// build.gradle.kts
plugins { jacoco }

tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports { xml.required.set(true); html.required.set(true) }
}
```

---

## 💻 Code Patterns & Conventions

### Package Structure (HW2 Domain-First)
```
com/ai/homework/
├── TicketsApplication.kt        # Spring Boot entry point
├── controller/                  # REST endpoints (thin)
│   ├── TicketController.kt
│   ├── ImportController.kt
│   └── ClassificationController.kt
├── service/                     # Business logic (thick)
│   ├── TicketService.kt
│   ├── ImportService.kt
│   └── ClassificationService.kt
├── importer/                    # Format-specific parsers
│   ├── TicketImporter.kt        # Interface
│   ├── CsvTicketImporter.kt
│   ├── JsonTicketImporter.kt
│   └── XmlTicketImporter.kt
├── validator/                   # Input validation
│   └── TicketValidator.kt
├── model/                       # Domain models & enums
│   └── Ticket.kt
└── dto/                         # Request/response shapes
    └── TicketDtos.kt
```

### Controllers
- **Minimal logic**: Map HTTP requests to service calls
- **Return `ResponseEntity<Any>`** when response type varies (success vs error)
- **Query param filtering**: Use nullable params; Spring cannot bind lowercase enums from `@RequestParam` without a custom converter — test with string params first
- **Optional features via query params**: `?auto_classify=true` pattern for opt-in behavior

Example (HW2 pattern with optional feature flag):
```kotlin
@PostMapping
fun createTicket(
    @RequestBody request: TicketCreateRequest,
    @RequestParam(name = "auto_classify", defaultValue = "false") autoClassify: Boolean
): ResponseEntity<Any> {
    val ticket = ticketService.createTicket(request)
    return if (autoClassify) {
        val classification = classificationService.classify(ticket)
        ResponseEntity(TicketCreateWithClassificationResponse(ticket.toResponse(), classification), HttpStatus.CREATED)
    } else {
        ResponseEntity(ticket.toResponse(), HttpStatus.CREATED)
    }
}
```

### Response Wrapper Pattern
When adding metadata to list responses (e.g., count), wrap in a DTO:
```kotlin
data class TicketListResponse(
    @JsonProperty("count") val count: Int,
    @JsonProperty("tickets") val tickets: List<TicketResponse>
)
// GET /tickets returns { count: N, tickets: [...] }
```

### Services
- **Thread-safe storage**: Always use `ConcurrentHashMap` for in-memory singletons
- **Expose store method for importers**: `fun storeTicket(ticket: Ticket): Ticket` allows importers to persist directly without going through request conversion
- **Business logic**: All validation rules and computations live here
- **Exception handling**: Return null or throw; let controllers decide response codes

```kotlin
@Service
class TicketService {
    private val tickets = ConcurrentHashMap<String, Ticket>()

    fun createTicket(request: TicketCreateRequest): Ticket { }
    fun storeTicket(ticket: Ticket): Ticket { tickets[ticket.id] = ticket; return ticket }
    fun getTicket(id: String): Ticket? = tickets[id]
    fun ticketExists(id: String): Boolean = tickets.containsKey(id)
}
```

### Validators
- **Return error list, don't throw**: Collect all errors and return; let controller format the 400 response
- **Separate concerns**: Email format, string lengths, enum validity — each in its own check
- **Clear error messages**: Include field name and constraint in the message

```kotlin
@Component
class TicketValidator {
    fun validate(request: TicketCreateRequest): List<String> {
        val errors = mutableListOf<String>()
        if (!request.customerEmail.matches(EMAIL_REGEX)) errors.add("Invalid email format")
        if (request.subject.length !in 1..200) errors.add("Subject must be 1-200 characters")
        if (request.description.length !in 10..2000) errors.add("Description must be 10-2000 characters")
        return errors
    }
}
```

### Models
- **Generated DTOs** (HW1): From OpenAPI spec; do not hand-edit
- **Domain models** (HW2+): Custom Kotlin data classes; use `val` everywhere
- **Enums**: Use for fixed sets; name in SCREAMING_SNAKE_CASE for enum values, lowercase snake_case in JSON via `@JsonProperty`
- **Nullable fields**: Use `?` for optional metadata; Jackson handles null serialization

### No Comments on "What"
- Code should be self-documenting: clear names, intent visible
- **Only add comments on "Why"**: architectural decisions, non-obvious constraints, workarounds
- **No comments on generated code**: Generators will overwrite them

### File Parsing & Import (HW2 Pattern)
When implementing multi-format imports (CSV, JSON, XML):

**Strategy**:
- Abstract importer interface: `interface TicketImporter { fun import(content: String): ImportResult }`
- Concrete implementations: `CsvTicketImporter`, `JsonTicketImporter`, `XmlTicketImporter`
- Each importer calls `ticketService.storeTicket()` directly — importers must be injected with `TicketService`
- Accumulate errors; don't fail on first bad record

**Error handling**:
- Return `ImportResult` with summary: `{ totalRecords: Int, successful: Int, failed: Int, errors: List<ImportError> }`
- Include row number + field + reason in each `ImportError`
- Distinguish parse errors from validation errors

**Jackson XML pitfalls**:
- Make all XML-mapped fields nullable (`val tags: List<String>? = null`) — Jackson XML cannot inject non-null defaults
- Add `@JacksonXmlElementWrapper` and `@JacksonXmlProperty` for list fields
- Gradle 8.x + Jackson XML has compatibility issues — keep XML test coverage separate and document the limitation

**CSV pitfalls**:
- Descriptions containing commas break naive CSV parsing — use Apache Commons CSV (`commons-csv:1.10.0`)
- Never include commas in sample data description fields
- Validate all fields before accepting a row; partial rows (missing columns) cause index-out-of-bounds

### Classification & Rule-Based Logic (HW2 Pattern)
When implementing automated categorization or priority assignment:

**Strategy**:
- Lowercase and tokenize input text (subject + description combined)
- Match against predefined keyword maps per category and priority
- Score: `confidence = keywords_matched / total_keywords_in_winning_category`
- Return classification even when confidence is low; let caller decide threshold

**Implementation**:
```kotlin
data class ClassificationResult(
    val category: TicketCategory,
    val priority: TicketPriority,
    val confidence: Double,          // 0.0–1.0
    val keywordsFound: List<String>,
    val reasoning: String
)
```

**Auto-classify on creation**: Expose as optional query param (`?auto_classify=true`) returning a combined response DTO. Don't force classification — make it opt-in.

**Testing**:
- Test each category with its canonical keywords
- Test priority levels independently from category
- Test confidence scoring: 0 keywords → low confidence, all keywords → high
- Test edge cases: empty text, all caps, mixed languages

---

## 🧪 Testing & Validation

### Coverage Targets (from HW2 baseline)
| Layer | Target | HW2 Achieved |
|-------|--------|--------------|
| Service | ≥ 90% | 97% |
| Validator | ≥ 90% | 97% |
| Controllers | ≥ 70% | 89% |
| DTOs/Models | ≥ 80% | 100% |
| Overall | ≥ 85% | 85% |

Aim to exceed these; use JaCoCo HTML report to find uncovered paths.

### Unit Tests
- **Test service logic**: Cover happy path + every validation branch
- **Test validators**: Every field constraint gets its own test (boundary values: min-1, min, max, max+1)
- **Test importers**: Valid input, missing fields, invalid enums, malformed format, unicode, empty file
- **No mocks for in-memory storage**: Test against the real in-memory store; mocks hide integration bugs

### Integration Tests (MockMvc)
- **Full controller tests**: Use `@SpringBootTest` + `@AutoConfigureMockMvc`
- **Test all HTTP methods**: GET, POST, PUT, DELETE with success and error cases
- **Test status codes explicitly**: `.andExpect(status().isCreated())`, `.andExpect(status().isNotFound())`
- **Test response body structure**: Use `.andExpect(jsonPath("$.field").value(...))`
- **No wildcard imports in test files**: ktlint enforces this; import each MockMvc method explicitly

```text
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
```

### Test Organization
- One test class per source class (mirrors the source tree)
- `@DisplayName` on class and each test for readable reports
- Edge case tests grouped in a dedicated `*EdgeCasesTest` class
- Keep tests isolated: each test creates its own data, doesn't rely on order

### API Verification
- **Swagger UI**: Verify endpoints are documented, file upload shows "Choose File" (not text input)
- **Curl tests**: Include working examples in `HOWTORUN.md`
- **Health checks**: Always include `/actuator/health` endpoint

---

## 📝 Documentation Requirements

### Per-Homework Files

#### `README.md` (Implementation Details)
- Explain architecture and design decisions with Mermaid diagram
- Document all API endpoints in a table (method, path, description, status codes)
- Show example requests/responses
- Link to docs/ for detailed API reference and architecture docs

#### `HOWTORUN.md` (Setup & Execution)
- Prerequisites (Java version, tools, ports)
- Build + run instructions with exact Gradle commands
- How to import sample data (Swagger UI steps + curl)
- Verification steps (health check, Swagger URL, sample curl calls)
- Troubleshooting (common errors: port conflicts, enum deserialization, XML parsing)
- Note known limitations (e.g., enum query param deserialization quirks)

#### `PR_CHECKLIST.md` (Submission Evidence)
- Implemented features mapped to tasks
- Test count and coverage breakdown
- Sample data summary
- Challenges encountered and how resolved
- Improvements beyond initial plan

#### `docs/` (Extended Documentation)
- `API_REFERENCE.md` — all endpoints with full cURL examples and response schemas
- `ARCHITECTURE.md` — C4 diagrams (context, component, sequence), design decisions
- `DIAGRAMS.md` — Mermaid diagrams for state machines, flows, data model
- `TESTING_GUIDE.md` — test pyramid, coverage breakdown, manual checklist

#### `demo/` (Sample Data)
- `sample_tickets.csv` — valid records covering all enum values
- `sample_tickets.json` — valid records in JSON array format
- `sample_tickets.xml` — valid records in XML format
- `invalid_tickets.csv` — error cases (missing fields, bad enums, length violations)
- `invalid_tickets.json` — same error cases in JSON

---

## 🔄 AI-Assisted Workflow

### Effective Prompts for Code Generation
When working with Claude or other AI agents:

1. **Provide Context**
    - Link to the relevant TASKS.md or spec
    - Reference existing patterns from prior homeworks
    - Explain the business requirement clearly

2. **Request Specific Implementation**
    - "Generate a Kotlin Spring service that validates and stores tickets per [schema]"
    - "Create a REST controller that maps to the /tickets/{id}/auto-classify endpoint"
    - "Write unit tests for the TicketValidator covering all boundary values"

3. **Ask for Verification**
    - "Does this implementation match the task requirements?"
    - "Are there edge cases I'm missing in this CSV parser?"
    - "Is this thread-safe for concurrent requests?"

4. **Iterate with Evidence**
    - Run the code and capture screenshots
    - Ask AI to review failing tests or build errors
    - Request refactoring based on actual runtime behavior

### Common Patterns to Reference
- **OpenAPI-first (HW1)**: OpenAPI spec → generated models → service layer
- **Domain-first (HW2+)**: Domain models → services → controllers → springdoc Swagger
- **Validation**: Return error list from validator; controller formats 400 response
- **Thread-safe storage**: `ConcurrentHashMap` in service singleton
- **Import pipeline**: `TicketImporter` interface → format-specific impl → `storeTicket()`
- **Auto-classify flag**: Optional `?auto_classify=true` query param returning combined DTO

### Token Efficiency & Concise Communication
**When generating code**:
- Output only the code block needed; don't repeat the entire file if editing
- Use diffs or line ranges when modifying existing files
- Skip boilerplate comments on "what" the code does

**When responding to user requests**:
- Lead with the action taken
- Use bullet points for multiple items
- One sentence per update
- Summarize only if the user asks

**In prompts to the user**:
- Direct questions: "Should I also update QUALITY_GATES.md?" not "Would you like me to consider..."
- Avoid filler: no "Let me..." preambles, no "As requested...", no "Thanks for asking!"

**Goal**: Maximize signal, minimize noise. Every token should add value.

### Verification Against Quality Gates
Every homework submission must pass all gates in [QUALITY_GATES.md](./QUALITY_GATES.md):

- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew spotlessCheck` passes
- [ ] `./gradlew test` passes with 0 failures
- [ ] JaCoCo coverage ≥ 85% overall
- [ ] README.md, HOWTORUN.md, screenshots all complete
- [ ] App starts, health check responds, Swagger UI loads
- [ ] Sample data imports successfully (if applicable)

Do not skip quality gates. Gates exist to prevent rework.

### Planning Phase for New Homeworks
**Step 1: Create a plan**
- Read TASKS.md carefully; identify all deliverables
- Reference prior homework patterns and QUALITY_GATES
- Break into discrete tasks with dependencies
- Get user approval before coding

**Step 2: Optimize the plan**
- Identify parallel work opportunities
- Lock in the plan with user approval

**Step 3: Execute in order**
- Follow the plan strictly; surface blockers immediately
- Verify each task against QUALITY_GATES before moving on

**Example task breakdown for domain-first homework (HW2 style)**:
1. Define domain models (data classes, enums)
2. Implement validator (all field constraints)
3. Implement service layer with ConcurrentHashMap storage
4. Implement controller (CRUD endpoints)
5. Implement importers (CSV, JSON, XML) + inject TicketService
6. Implement classification/business logic service
7. Add Swagger (springdoc dependency + consumes annotation for file upload)
8. Write unit tests (service, validator, importers — edge cases)
9. Write integration tests (MockMvc, full CRUD + error scenarios)
10. Add JaCoCo; verify ≥ 85% coverage
11. Create sample data files (valid + invalid for each format)
12. Document: README, HOWTORUN, PR_CHECKLIST, docs/
13. Verify all quality gates
14. Prepare PR

Always plan before coding. A good plan prevents rework.

---

## ✅ Checklist for Each Homework

Before submitting, ensure:

- [ ] **Build**: `./gradlew clean build` succeeds
- [ ] **Formatting**: `./gradlew spotlessCheck` passes
- [ ] **Tests**: All pass; coverage ≥ 85%
- [ ] **Running App**: Starts without errors; health check responds
- [ ] **Swagger UI**: Loads at `/swagger-ui.html`; file upload shows "Choose File" button
- [ ] **README.md**: Architecture, endpoints table, design decisions, AI usage
- [ ] **HOWTORUN.md**: Working instructions with curl examples
- [ ] **PR_CHECKLIST.md**: Features mapped to tasks, test count, challenges
- [ ] **Screenshots**: Working app, Swagger UI, test results in `docs/screenshots/`
- [ ] **Sample data**: Valid + invalid files in `demo/` (if import feature exists)
- [ ] **Pull Request**: Detailed description, links to docs, clear title

---

## 🚨 Common Pitfalls to Avoid

**HW1 (OpenAPI-first)**:
1. Hand-editing generated code — regenerate; don't patch DTOs manually
2. Ignoring the spec — keep OpenAPI spec in sync with implementation

**HW2+ (Domain-first)**:
3. Using `HashMap` instead of `ConcurrentHashMap` — causes race conditions
4. Importers that don't persist — inject `TicketService` and call `storeTicket()`
5. Wildcard imports in test files — ktlint blocks commit; import each method explicitly
6. CSV with commas in descriptions — use Apache Commons CSV and avoid commas in sample data
7. XML fields as non-null — make all Jackson XML-mapped fields nullable
8. Enum deserialization from `@RequestParam` — Spring needs custom converter for lowercase enums; test with string params
9. Gradle version conflicts — all modules must use same Kotlin/Spring Boot versions as root

**Universal**:
10. Logic in controllers — move to services
11. Missing error handling — return 400/404, not 500
12. Skipping documentation — screenshots and HOWTORUN are grading criteria
13. Unformatted code — run `./gradlew spotlessApply` before committing
14. Committing build artifacts — ensure `.gradle/`, `build/` are in `.gitignore`

---

## 📚 Reference & Examples

**From Homework 1:**
- API Spec: `openapi-spec/homework-1.yaml`
- Service Pattern: `homework-1/src/main/kotlin/com/banking/service/TransactionService.kt`
- Validator Pattern: `homework-1/src/main/kotlin/com/banking/validator/TransactionValidator.kt`
- Controller Pattern: `homework-1/src/main/kotlin/com/banking/controller/TransactionController.kt`

**From Homework 2:**
- Thread-safe service: `homework-2/src/main/kotlin/com/ai/homework/service/TicketService.kt`
- Importer interface + CSV/JSON/XML: `homework-2/src/main/kotlin/com/ai/homework/importer/`
- Classification with confidence: `homework-2/src/main/kotlin/com/ai/homework/service/ClassificationService.kt`
- DTO patterns + response wrappers: `homework-2/src/main/kotlin/com/ai/homework/dto/TicketDtos.kt`
- MockMvc integration tests: `homework-2/src/test/kotlin/com/ai/homework/controller/`
- Sample data: `homework-2/demo/`

**External Resources:**
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [springdoc-openapi](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.0)
- [Kotlin Language Reference](https://kotlinlang.org/docs/reference/)
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/)

---

## 🎯 Summary

**HW1 approach** (OpenAPI-first):
1. Define the API spec (OpenAPI YAML)
2. Generate models from the spec (automatic)
3. Implement services (thick business logic)
4. Wire controllers (thin HTTP mapping)
5. Validate inputs, format & test, document, PR

**HW2+ approach** (Domain-first):
1. Define domain models and enums
2. Implement validator (return error list)
3. Implement service with `ConcurrentHashMap`
4. Implement importers (inject service, call `storeTicket`)
5. Add classification/business logic
6. Add springdoc Swagger (`springdoc-openapi-starter-webmvc-ui`)
7. Write 85%+ coverage tests (unit + MockMvc integration)
8. Create sample data (`demo/`)
9. Document (`docs/`, `PR_CHECKLIST.md`)
10. Verify all quality gates, then PR

---

**Last Updated**: May 11, 2026
**Established from**: Homework 1 & Homework 2 Implementations
