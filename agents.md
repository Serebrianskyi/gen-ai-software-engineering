# Agent Guidance for AI-Assisted Development

### API Design Approaches (HW1 vs HW2+)

**HW1 Pattern (OpenAPI-First)**:
- Define API contract first in `openapi-spec/homework-X.yaml`
- Use `org.openapi.generator` to generate models and interfaces
- Spec is source of truth; implementation follows spec
- Best when: spec is provided upfront, API contracts are critical

**HW2+ Pattern (Traditional REST, Domain-First)**:
- Define data models first (case classes, enums, validation rules)
- Build endpoints that serve those models
- Create documentation AFTER implementation (describe what exists)
- No code generation; manual DTO management
- Best when: requirements evolve, testing/logic complexity is high, domain is the focus

**Key difference**: HW1 spec-drives-code; HW2 code-drives-spec.

### Multi-Module Architecture
- **Separation of concerns**: Keep API specification generation isolated from application logic
- **Module per homework**: Each homework gets its own module (`:homework-1`, `:homework-2`, etc.)
- **Reusable specs**: Specs in `openapi-spec/` can be shared; name files clearly (e.g., `homework-1.yaml`)
- **Clean dependencies**: Modules depend on generated specs, not on each other

### Spring Boot Best Practices
- **Framework conventions over configuration**: Use Spring's standard patterns
- **Validation at service layer**: Business rules and complex validation belong in services
- **Controllers are thin**: Controllers map HTTP to service calls; logic stays in services
- **Dependency injection**: Use Spring's @Service, @Repository, @Controller for lifecycle management

---

## 🏗️ Project Structure

```
ai-workshops/
├── agents.md                    # This file
├── settings.gradle.kts          # Multi-module configuration
├── build.gradle.kts             # Root build config
├── openapi-spec/                # API specification module
│   ├── build.gradle.kts
│   ├── homework-1.yaml          # HW1 API spec
│   ├── homework-2.yaml          # HW2 API spec 
│   └── build/generated/         # Generated code (read-only)
├── homework-1/
│   ├── build.gradle.kts
│   ├── README.md               # Implementation details
│   ├── HOWTORUN.md             # Setup & run instructions
│   ├── src/main/kotlin/
│   ├── src/main/resources/
│   └── docs/screenshots/       # Evidence of AI usage
├── homework-2/                  # (future)
└── ...
```

**Key rules**:
- Each homework gets a module in `homework-X/`
- Shared API specs live in `openapi-spec/` with homework-specific names
- Generated code goes to `build/generated/` (git-ignored)
- Documentation is per-module: `README.md` and `HOWTORUN.md`

---

## 🔧 Build & Development Setup

### Gradle Configuration
Each homework module must have a `build.gradle.kts` that:
1. Declares plugins: `kotlin("jvm")`, `kotlin("plugin.spring")`, `id("org.springframework.boot")`, `id("io.spring.dependency-management")`, code formatter (`spotless`)
2. Optionally includes `id("org.openapi.generator")` if generating from spec
3. Includes dependency management and testing setup

### OpenAPI Code Generation
When a homework uses OpenAPI generation:
```kotlin
plugins {
    id("org.openapi.generator")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("${projectDir}/path-to-spec.yaml")  // Spec location
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("com.banking.api")
    modelPackage.set("com.banking.model.generated")
    packageName.set("com.banking.generated")
    configOptions.set(mapOf(
        "useSpringBoot3" to "true",
        "documentationProvider" to "springdoc",
        // ... other options
    ))
}

// Ensure code generation runs before compilation
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

### Code Formatting
All modules use **ktlint** via **spotless** for consistent formatting:
```kotlin
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0").setEditorConfigPath(rootProject.file(".editorconfig"))
    }
}
```
The `.editorconfig` file at root defines formatting rules—**do not commit unformatted code**.

---

## 💻 Code Patterns & Conventions

### Package Structure
```
com/banking/
├── HW1Application.kt           # Spring Boot entry point
├── config/                     # Spring configuration
│   └── SwaggerConfig.kt        # OpenAPI/Swagger beans
├── controller/                 # REST endpoints (thin)
│   └── TransactionController.kt
├── service/                    # Business logic (thick)
│   └── TransactionService.kt
├── validator/                  # Input validation
│   └── TransactionValidator.kt
└── model/                      # Domain models & enums
    └── Transaction.kt
```

### Controllers
- **Minimal logic**: Map HTTP requests to service calls
- **Parameter validation**: Use `@Valid` and `@PathVariable` with validation annotations
- **Response mapping**: Convert service results to DTOs
- **Error handling**: Let exceptions propagate; handle globally if needed

Example:
```kotlin
@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(val service: TransactionService) {
    @PostMapping
    fun createTransaction(@Valid @RequestBody request: CreateTransactionRequest): Transaction {
        return service.create(request)
    }
}
```

### Services
- **Business logic**: All validation rules and computations live here
- **Data management**: Services manage in-memory or database state
- **Exception handling**: Throw meaningful exceptions; let controllers decide response codes
- **Stateless where possible**: Prefer immutability; manage shared state carefully

Example:
```kotlin
@Service
class TransactionService {
    private val transactions = mutableMapOf<String, Transaction>()
    
    fun create(request: CreateTransactionRequest): Transaction {
        validate(request)  // Delegate to validator or inline
        val transaction = Transaction(id = UUID.randomUUID().toString())
        transactions[transaction.id] = transaction
        return transaction
    }
}
```

### Validators
- **Encapsulate validation**: Separate complex validation logic from services
- **Reusable rules**: Define validators as standalone classes or functions
- **Clear error messages**: Validation exceptions should explain what's wrong

Example:
```kotlin
object TransactionValidator {
    fun validate(request: CreateTransactionRequest) {
        require(request.amount > 0) { "Amount must be positive" }
        require(request.currency.matches(ISO_CURRENCY_PATTERN)) { "Invalid currency" }
        // ...
    }
}
```

### Models
- **Generated DTOs**: From OpenAPI spec; do not hand-edit
- **Domain models**: Custom Kotlin data classes for internal use
- **Enums**: Use for fixed sets (TransactionType, TransactionStatus, etc.)
- **Immutability**: Prefer `val` and data classes; avoid mutable state

### No Comments on "What"
- Code should be self-documenting: clear names, intent visible
- **Only add comments on "Why"**: architectural decisions, non-obvious constraints, workarounds
- **No comments on generated code**: Generators will overwrite them

### File Parsing & Import (HW2 Pattern)
When implementing multi-format imports (CSV, JSON, XML):

**Strategy**:
- Abstract importer interface: `interface TicketImporter { fun import(content: String): List<Ticket> }`
- Concrete implementations: `CsvTicketImporter`, `JsonTicketImporter`, `XmlTicketImporter`
- Factory or switch statement to select importer based on file type
- Accumulate errors; don't fail on first bad record

**Error handling**:
- Return `ImportResult` with summary: `{ total: Int, successful: Int, failed: Int, errors: List<String> }`
- Include line/record number + reason in error messages
- Distinguish parse errors from validation errors
- Return HTTP 400 with details (allow client to fix and retry)

**Performance**:
- Stream large files instead of loading into memory
- Batch validation instead of validating each record individually
- Parallel processing if handling multiple files simultaneously

### Classification & Rule-Based Logic (HW2 Pattern)
When implementing automated categorization or priority assignment:

**Strategy**:
- Extract keywords from text (subject + description)
- Match against predefined keyword lists (e.g., ["login", "password"] → account_access)
- Return classification with confidence score (0-1) based on keyword matches
- Allow manual overrides; log all decisions

**Implementation**:
```kotlin
data class ClassificationResult(
    val category: String,
    val priority: String,
    val confidence: Double,  // keywords_matched / total_keywords
    val keywordsFound: List<String>,
    val reasoning: String
)
```

**Testing**:
- Test each category with typical keywords
- Test edge cases (empty text, all caps, typos)
- Test priority levels independently
- Verify confidence scoring is accurate

---

## 🧪 Testing & Validation

### Unit Tests
- **Test service logic**: Service layer tests should cover business rules
- **Mock external dependencies**: In-memory stubs for persistence, external APIs
- **Use Spring Boot Test**: `@SpringBootTest` for integration tests if needed
- **Test validators**: Separately test validation rules with edge cases

### Integration Tests
- **Test full request/response**: Controller → Service → Response
- **Use test containers** if needing real database/external service
- **Keep tests isolated**: Each test should be independent

### API Verification
- **Swagger UI**: Verify endpoints are documented and responsive
- **Curl tests**: Include simple curl examples in `HOWTORUN.md`
- **Health checks**: Always include `/actuator/health` endpoint

---

## 📝 Documentation Requirements

### Per-Homework Files

#### `README.md` (Implementation Details)
- Explain architecture and design decisions
- Document API endpoints and their business logic
- Show example requests/responses
- Link to the OpenAPI spec file
- Include dependency diagram if multi-module

#### `HOWTORUN.md` (Setup & Execution)
- Prerequisites (Java version, tools)
- Build instructions (with Gradle commands)
- Run instructions (bootRun, JAR, etc.)
- How to verify (Swagger UI, health check, curl examples)
- Troubleshooting (common errors, port conflicts, etc.)
- Project structure overview

#### `docs/screenshots/` (Evidence of AI Usage)
- Prompts given to AI tools
- Responses and suggestions received
- Code generated or verified by AI
- Final working application (Swagger UI, endpoints, responses)
- Test results if applicable

---

## 🔄 AI-Assisted Workflow

### Effective Prompts for Code Generation
When working with Claude or other AI agents:

1. **Provide Context**
    - Link to the OpenAPI spec or include the relevant schema
    - Reference existing patterns from HW1
    - Explain the business requirement clearly

2. **Request Specific Implementation**
    - "Generate a Kotlin Spring service that validates and stores transactions per [schema]"
    - "Create a REST controller that maps to the /api/v1/accounts/{id}/balance endpoint"
    - "Write unit tests for the TransactionValidator"

3. **Ask for Verification**
    - "Does this implementation match the OpenAPI schema?"
    - "Are there edge cases I'm missing in validation?"
    - "Is this following Spring best practices?"

4. **Iterate with Evidence**
    - Run the code and capture screenshots
    - Ask AI to review failing tests or build errors
    - Request refactoring based on actual runtime behavior

### Common Patterns to Reference
- **OpenAPI-first**: "Follow the pattern from HW1: OpenAPI spec → generated models → service layer implementation"
- **Validation**: "Use the validator pattern from HW1 TransactionValidator.kt"
- **Services with state**: "Use the in-memory Map pattern from HW1 TransactionService"
- **Controllers**: "Keep it thin—just map requests to service calls like HW1 TransactionController"

### Token Efficiency & Concise Communication
Agents working on this project should optimize for clarity and brevity:

**When generating code**:
- Output only the code block needed; don't repeat the entire file if editing
- Use diffs or line ranges when modifying existing files
- Avoid unnecessary explanatory paragraphs; let code speak for itself
- Skip boilerplate comments on "what" the code does

**When responding to user requests**:
- Lead with the action taken (e.g., "Updated `homework-1/README.md` with..."')
- Use bullet points for multiple items, not paragraphs
- One sentence per update; silence is acceptable if nothing changed
- Summarize only if the user asks for it

**In prompts to the user**:
- Direct questions: "Should I also update QUALITY_GATES.md?" not "Would you like me to consider also updating QUALITY_GATES.md?"
- Avoid filler: no "Let me..." preambles, no "As requested...", no "Thanks for asking!"
- Status updates: "Build succeeded, app runs, health check responds" beats "The build was successful, the application started without errors, and the health check endpoint responded correctly"

**Symbol & word economy**:
- Use `code blocks` instead of descriptions where possible
- Avoid: "In order to", "Due to the fact that", "As a result of"
- Use: "To", "Because", "So"
- Skip markdown where plain text works: `filename.txt` not **filename.txt**

**Goal**: Maximize signal, minimize noise. Every token should add value.

### Verification Against Quality Gates
Every homework submission must pass all gates in [QUALITY_GATES.md](./QUALITY_GATES.md) before being considered complete:

**Before final submission, verify**:
- [ ] Build & Compilation: `./gradlew clean build` succeeds
- [ ] Code Quality: `./gradlew spotlessCheck` passes, no hand-edited generated code
- [ ] Testing: `./gradlew test` passes with 0 failures
- [ ] Documentation: README.md, HOWTORUN.md, screenshots all complete
- [ ] Deployment: App starts, health check responds, API contracts validated

**If any gate is RED**: Fix the underlying issue, re-verify, and only then proceed to PR.

Do not skip quality gates to move faster. Gates exist to prevent rework and ensure submission readiness.

### Planning Phase for New Homeworks
When starting work on a new homework (HW2, HW3, etc.):

**Step 1: Create a plan**
- Break down the homework into discrete tasks
- Reference the HW1 patterns and QUALITY_GATES
- Estimate effort for each task
- Identify dependencies (e.g., "spec must be complete before code generation")
- Ask the user to review and approve the plan before starting implementation

**Step 2: Optimize the plan**
- Compress overlapping tasks where possible
- Reorder tasks to minimize context switching
- Identify parallel work opportunities (spec + tests can run in parallel)
- Lock in the optimized plan with user approval

**Step 3: Execute in order**
- Follow the optimized plan strictly
- Update plan status as tasks complete
- If blockers arise, surface them immediately rather than pivoting
- Verify each task against QUALITY_GATES before moving on

**Example task breakdown for HW2**:
1. Define OpenAPI spec (`openapi-spec/homework-2.yaml`)
2. Verify spec is valid (run openApiGenerate)
3. Implement service layer (follow HW1 TransactionService pattern)
4. Implement controller layer (thin, delegate to service)
5. Write unit tests (service + validator coverage ≥ 80%)
6. Write integration tests (API contract validation)
7. Document in README.md (architecture, endpoints, examples)
8. Document in HOWTORUN.md (setup, run, troubleshoot)
9. Add screenshots to `docs/screenshots/`
10. Verify against QUALITY_GATES
11. Prepare PR (detailed description, link to docs)

Always plan before coding. A good plan prevents rework.

---

## ✅ Checklist for Each Homework

Before submitting, ensure:

- [ ] **API Spec**: Updated and valid OpenAPI YAML in `openapi-spec/homework-X.yaml`
- [ ] **Code Generation**: Runs successfully; no manual edits to generated code
- [ ] **Build**: `./gradlew build` succeeds without warnings or errors
- [ ] **Formatting**: `spotless` passes; use `./gradlew spotlessApply` to fix
- [ ] **Tests**: All unit and integration tests pass
- [ ] **Running App**: Starts without errors; responds to health checks
- [ ] **Swagger UI**: Accessible at `http://localhost:8080/swagger-ui.html` (or configured port)
- [ ] **README.md**: Documents design, endpoints, and AI usage
- [ ] **HOWTORUN.md**: Clear setup and run instructions with examples
- [ ] **Screenshots**: Evidence of working app and AI-assisted development
- [ ] **Pull Request**: Detailed description, links to docs, clear title

---

## 🚨 Common Pitfalls to Avoid

1. **Hand-editing generated code**: Regenerate; don't patch DTOs manually
2. **Logic in controllers**: Move to services; keep controllers thin
3. **No validation**: Validate early in services; fail fast with clear errors
4. **Ignoring the spec**: Always keep OpenAPI spec in sync with implementation
5. **Skipping documentation**: Screenshots and HOWTORUN are grading criteria
6. **Unformatted code**: Run `spotlessApply` before committing
7. **Missing error handling**: Catch exceptions; return meaningful HTTP status codes
8. **Stale dependencies**: Keep versions aligned across modules (see `build.gradle.kts`)

---

## 📚 Reference & Examples

**From Homework 1:**
- API Spec: `openapi-spec/homework-1.yaml`
- Service Pattern: `homework-1/src/main/kotlin/com/banking/service/TransactionService.kt`
- Validator Pattern: `homework-1/src/main/kotlin/com/banking/validator/TransactionValidator.kt`
- Controller Pattern: `homework-1/src/main/kotlin/com/banking/controller/TransactionController.kt`
- Build Config: `homework-1/build.gradle.kts`

**External Resources:**
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.0)
- [OpenAPI Generator Docs](https://openapi-generator.tech/)
- [Kotlin Language Reference](https://kotlinlang.org/docs/reference/)

---

## 🎯 Summary

The HW1 approach is:
1. **Define the API spec first** (OpenAPI YAML)
2. **Generate models from the spec** (automatic, idempotent)
3. **Implement services** (thick business logic)
4. **Wire controllers** (thin HTTP mapping)
5. **Validate inputs** (reusable validators)
6. **Document thoroughly** (README, HOWTORUN, screenshots)
7. **Format & test** (spotless, unit/integration tests)
8. **Commit with evidence** (PR with detailed description & screenshots)

Follow this for HW2–HW6 and maintain consistency across the codebase.

---

**Last Updated**: May 2, 2026  
**Established from**: Homework 1 Implementation
