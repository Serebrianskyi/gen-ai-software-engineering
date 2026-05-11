# Quality Gates & Acceptance Criteria

This document defines the quality standards, acceptance criteria, and verification steps for all homework submissions. Every submission must pass these gates before being considered complete and ready for review.

---

## Overview

Quality gates are organized into five categories:
1. **Build & Compilation** — Code must compile and build
2. **Code Quality** — Formatting, style, and best practices
3. **Testing & Validation** — Functional correctness
4. **Documentation** — Completeness and accuracy
5. **Deployment & Runtime** — Application runs and responds correctly

All gates must be **GREEN** before submitting a pull request.

---

## 🔨 1. Build & Compilation

### Gate: Successful Clean Build
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew clean build
```

**Acceptance Criteria**:
- [ ] No compilation errors
- [ ] No warnings (unless pre-existing from framework)
- [ ] All tasks complete successfully
- [ ] JAR/WAR artifact is created in `build/libs/`

**Expected Output**:
```
BUILD SUCCESSFUL in Xs
X actionable tasks: X executed
```

**Why it matters**: Code that doesn't compile has zero functional value. This is a hard requirement.

---

### Gate: OpenAPI Spec Validation
**Status**: ✅ **REQUIRED for API projects** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew :openapi-spec:openApiGenerate
```

**Acceptance Criteria**:
- [ ] YAML syntax is valid
- [ ] OpenAPI 3.0 spec conforms to schema
- [ ] Generator produces code without errors
- [ ] Generated code compiles cleanly

**Expected Output**:
```
> Task :openapi-spec:openApiGenerate
Successfully generated code to .../build/generated/openapi
```

**Why it matters**: Invalid specs cause code generation to fail; bad specs create unusable generated models.

---

### Gate: No Dependency Conflicts
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew dependencies --configuration compileClasspath
```

**Acceptance Criteria**:
- [ ] No dependency version conflicts
- [ ] All transitive dependencies resolve
- [ ] No circular dependencies between modules
- [ ] No deprecated/EOL library versions

**Common Issues**:
- Mismatched Spring Boot versions across modules
- Jackson version conflicts with Spring Data
- Kotlin stdlib vs kotlin-reflect version mismatch

**Why it matters**: Version conflicts cause runtime ClassNotFoundExceptions and hard-to-debug failures.

---

## 🎨 2. Code Quality

### Gate: Spotless Formatting
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew spotlessCheck
```

**Acceptance Criteria**:
- [ ] All Kotlin files pass ktlint (0.50.0)
- [ ] All Gradle build files follow style rules
- [ ] No trailing whitespace
- [ ] Proper indentation (4 spaces)

**Auto-fix**:
```bash
./gradlew spotlessApply
```

**Why it matters**: Consistent formatting makes code readable and reviewable; it's non-negotiable for team work.

---

### Gate: No Auto-Generated Code Modifications
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
- [ ] No hand edits to files in `build/generated/`
- [ ] DTOs and API interfaces match generated code exactly
- [ ] No custom extensions to generated models (inline instead)

**What to do if generated code is wrong**:
1. Update the OpenAPI spec (e.g., `homework-X.yaml`)
2. Delete `build/generated/`
3. Re-run `./gradlew openApiGenerate`
4. Rebuild and verify

**Why it matters**: Generated code is idempotent and reproducible. Hand edits get lost on regeneration and create inconsistency.

---

### Gate: Clear & Minimal Comments
**Status**: ✅ **REQUIRED** | ⚠️ **WARNING**

**Verification**:
- [ ] Comments explain **WHY**, not **WHAT**
- [ ] No comments stating obvious code behavior (e.g., "increment counter")
- [ ] No TODO/FIXME comments without context
- [ ] Architectural decisions documented in README, not code

**Good Comments**:
```kotlin
// Map HTTP 201 Created to match OpenAPI spec endpoint response
// (Spring's default is 200 OK, but spec requires 201)
@PostMapping
fun create(): ResponseEntity<Transaction> { }
```

**Bad Comments**:
```kotlin
// Create a transaction
fun create() { }

// Loop through transactions
for (t in transactions) { }
```

**Why it matters**: Self-documenting code is more maintainable; comments become stale and misleading.

---

### Gate: No Unused Imports or Variables
**Status**: ✅ **REQUIRED** | ⚠️ **WARNING**

**Verification**:
```bash
./gradlew build  # IDE will highlight unused
```

**Acceptance Criteria**:
- [ ] No unused imports (IDE will flag in red)
- [ ] No unused variables (compiler warnings)
- [ ] No dead code
- [ ] ktlint will enforce this

**Why it matters**: Unused code is noise; it confuses readers and suggests incomplete refactoring.

---

## 🧪 3. Testing & Validation

### Gate: All Tests Pass
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew test
```

**Acceptance Criteria**:
- [ ] All unit tests pass (0 failures)
- [ ] All integration tests pass (0 failures)
- [ ] Test execution time < 30 seconds (warn if > 1 minute)
- [ ] No flaky tests (tests pass consistently)

**Expected Output**:
```
> Task :homework-1:test
BUILD SUCCESSFUL
X tests passed
```

**Why it matters**: Tests catch regressions and verify correctness. Failing tests indicate incomplete or broken implementation.

---

### Gate: Test Coverage for Business Logic
**Status**: ⚠️ **STRONGLY RECOMMENDED** | 📊 **METRIC**

**Verification** (if configured):
```bash
./gradlew test jacocoTestReport  # if Jacoco plugin is added
```

**Acceptance Criteria**:
- [ ] Service layer: ≥ 80% line coverage
- [ ] Validator layer: ≥ 90% coverage (critical for correctness)
- [ ] Controllers: ≥ 50% coverage (integration tests preferred)
- [ ] Generated code: excluded from coverage
- [ ] Utility/model classes: ≥ 70% coverage

**Why it matters**: High coverage reduces production bugs. Untested paths are likely broken.

---

### Gate: API Contract Validation
**Status**: ✅ **REQUIRED for API projects** | 🚫 **BLOCKING**

**Verification**:
1. Start the application: `./gradlew :homework-1:bootRun`
2. Check Swagger UI responds: `curl http://localhost:8080/swagger-ui.html`
3. Verify OpenAPI spec endpoint: `curl http://localhost:8080/api-docs`

**Acceptance Criteria**:
- [ ] Application starts without errors
- [ ] Health check passes: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] All endpoints in Swagger match OpenAPI spec
- [ ] Sample requests in HOWTORUN.md execute successfully
- [ ] Response bodies match documented schemas

**Test Sample Requests**:
```bash
# Each homework should include curl examples in HOWTORUN.md
curl -X GET http://localhost:8080/api/v1/transactions
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Why it matters**: API contracts are the promise to clients. Broken contracts cause integration failures downstream.

---

### Gate: Input Validation
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
Test invalid inputs to verify rejection:

**Acceptance Criteria**:
- [ ] Invalid JSON is rejected (HTTP 400)
- [ ] Missing required fields are rejected
- [ ] Out-of-range values are rejected (negative amounts, invalid IDs, etc.)
- [ ] Invalid enum values are rejected
- [ ] Error response includes clear error message (e.g., "Amount must be positive")

**Test Examples** (from HW1 pattern):
```bash
# Missing required field
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-123"}'
# Expected: 400 Bad Request with error details

# Invalid amount
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-123","toAccount":"ACC-456","amount":-100}'
# Expected: 400 Bad Request, "Amount must be positive"
```

**Why it matters**: Validation prevents bad data from entering the system and corrupting state.

---

## 📚 4. Documentation

### Gate: README.md Completeness
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**: Review `homework-X/README.md`

**Acceptance Criteria**:
- [ ] Project description (what it does)
- [ ] Architecture diagram or explanation (e.g., OpenAPI → Models → Services)
- [ ] Technology stack listed (Kotlin, Spring Boot, etc.)
- [ ] All API endpoints documented with method, path, description
- [ ] Example request/response for at least 3 endpoints
- [ ] Data models documented (JSON schema or table)
- [ ] Design decisions explained (why OpenAPI-first? why this structure?)
- [ ] AI assistance documented (tools used, prompts, verification)
- [ ] Links to HOWTORUN.md and relevant specs

**Length**: Typically 500–1500 words

**Why it matters**: README is the first thing reviewers read. Poor docs suggest incomplete understanding.

---

### Gate: HOWTORUN.md Completeness
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**: Follow steps in `homework-X/HOWTORUN.md` and verify they work

**Acceptance Criteria**:
- [ ] Prerequisites listed (Java version, tools, ports)
- [ ] Clone/setup instructions (where to run commands)
- [ ] Build instructions (./gradlew commands)
- [ ] Run instructions (bootRun, JAR, etc.)
- [ ] Verification steps (health check, Swagger URL, sample curl calls)
- [ ] Troubleshooting section (common errors, solutions)
- [ ] Project structure diagram
- [ ] At least 5 working curl examples
- [ ] Expected output shown for each step

**Length**: Typically 200–500 words

**Test**: Follow the HOWTORUN.md exactly as an outsider; if you get stuck, it's incomplete.

**Why it matters**: Good instructions = replicable results. Bad instructions = grader can't verify your work.

---

### Gate: Screenshots & Evidence
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**: Check `homework-X/docs/screenshots/`

**Acceptance Criteria**:
- [ ] Application running screenshot (Swagger UI or curl output)
- [ ] API working screenshot (example endpoint response)
- [ ] Health check passing screenshot
- [ ] Swagger UI showing all endpoints
- [ ] At least one example of AI interaction (prompt + response)
- [ ] Test results (if applicable)
- [ ] Code snippet showing key implementation (e.g., service logic)

**Directory Structure**:
```
homework-X/docs/screenshots/
├── 01-app-startup.png
├── 02-swagger-ui.png
├── 03-api-response.png
├── 04-validation-error.png
├── 05-ai-prompt.png
├── 06-ai-response.png
├── 07-test-results.png
└── README.md (brief captions)
```

**Why it matters**: Visual evidence is proof of working solution. Screenshots are grading criteria.

---

### Gate: API Spec Documentation
**Status**: ✅ **REQUIRED for API projects** | 🚫 **BLOCKING**

**Verification**: Check `openapi-spec/homework-X.yaml`

**Acceptance Criteria**:
- [ ] Valid OpenAPI 3.0.0 YAML
- [ ] All endpoints documented
- [ ] All request/response schemas defined
- [ ] Enums documented (e.g., TransactionType, TransactionStatus)
- [ ] HTTP status codes documented (200, 400, 404, 500, etc.)
- [ ] Examples provided for key schemas

**Validation**:
```bash
./gradlew :openapi-spec:openApiGenerate  # Should succeed
```

**Why it matters**: Specs are contracts. Incomplete specs lead to mismatched implementation.

---

## 🚀 5. Deployment & Runtime

### Gate: Application Startup
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
./gradlew :homework-1:bootRun
# Wait for startup message
```

**Acceptance Criteria**:
- [ ] Application starts in < 5 seconds
- [ ] No errors or stack traces in logs
- [ ] Listening on expected port (default: 8080)
- [ ] Can be cleanly stopped with Ctrl+C

**Expected Log Output**:
```
Started BankingApplication in X.XXX seconds
Tomcat started on port 8080
```

**Why it matters**: An app that won't start is worthless.

---

### Gate: Health & Liveness
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
```bash
curl http://localhost:8080/actuator/health
```

**Acceptance Criteria**:
- [ ] Health endpoint responds with 200 OK
- [ ] Response body: `{"status":"UP"}`
- [ ] Response time < 100ms
- [ ] Works immediately after startup

**Why it matters**: Health checks are how deployment systems know if your app is alive.

---

### Gate: Memory & Performance
**Status**: ⚠️ **STRONGLY RECOMMENDED** | 📊 **METRIC**

**Verification**:
```bash
# Monitor while running: ./gradlew :homework-1:bootRun
watch -n 1 'ps aux | grep java'
```

**Acceptance Criteria**:
- [ ] Startup memory: < 500MB
- [ ] Idle memory: < 400MB
- [ ] API response time: < 200ms (p95)
- [ ] No memory leaks (memory stable after 1000 requests)
- [ ] No GC pauses > 100ms

**Benchmark Sample**:
```bash
# Load test with 100 requests
for i in {1..100}; do
  curl http://localhost:8080/api/v1/transactions
done
```

**Why it matters**: Bloated, slow apps are bad for production. Performance issues suggest inefficient code.

---

### Gate: Error Handling
**Status**: ✅ **REQUIRED** | 🚫 **BLOCKING**

**Verification**:
Test invalid requests and verify graceful failure:

**Acceptance Criteria**:
- [ ] Invalid input → 400 Bad Request (not 500)
- [ ] Not found → 404 Not Found (not 500)
- [ ] Server errors → 500 with helpful message (not stack trace dump)
- [ ] No unhandled exceptions crash the app
- [ ] Error responses include a message field

**Test Bad Requests**:
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"invalid":"request"}'
# Expected: 400 Bad Request with error message
```

**Why it matters**: Poor error handling confuses clients and hides problems.

---

## ✅ Pre-Submission Checklist

Use this checklist before opening a PR:

### Build & Compilation
- [ ] `./gradlew clean build` passes
- [ ] `./gradlew spotlessCheck` passes
- [ ] No dependency conflicts
- [ ] Generated code is fresh (not hand-edited)

### Testing
- [ ] `./gradlew test` passes (0 failures)
- [ ] Manual API tests from HOWTORUN.md all work
- [ ] Invalid inputs are rejected gracefully

### Documentation
- [ ] README.md is complete and accurate
- [ ] HOWTORUN.md has working instructions
- [ ] Screenshots in `docs/screenshots/` exist
- [ ] API spec (`homework-X.yaml`) is valid and complete

### Runtime
- [ ] Application starts cleanly
- [ ] Health check responds
- [ ] All endpoints respond correctly
- [ ] Swagger UI is accessible

### Pull Request
- [ ] Branch name: `homework-X-submission`
- [ ] PR title: Clear, concise (< 70 chars)
- [ ] PR description: Detailed, with links to README/HOWTORUN
- [ ] At least 3 screenshots embedded or linked
- [ ] Assigned to reviewer (instructor)

---

## 🚫 Failure Modes & Recovery

### If Build Fails
**Action**:
1. Check error message carefully
2. Run `./gradlew clean build --info` for details
3. Fix compilation errors (missing imports, syntax)
4. Verify OpenAPI spec is valid
5. Check Java version: `java -version` (must be 17+)

### If Tests Fail
**Action**:
1. Run failing test in isolation: `./gradlew test --tests ClassName`
2. Check test logs for assertion failures
3. Verify test data matches expectations
4. Run with `--info` flag for details

### If App Won't Start
**Action**:
1. Check for port conflicts: `lsof -i :8080`
2. Check logs for missing dependencies
3. Verify Java version and classpath
4. Try: `./gradlew clean build :homework-X:bootRun`

### If Formatting Fails
**Action**:
```bash
./gradlew spotlessApply  # Auto-fix
./gradlew spotlessCheck  # Verify
```

---

## 📊 Grading Impact

Quality gates map to grading rubric:

| Gate | Weight | Criteria |
|------|--------|----------|
| Build Success | 10% | Code compiles and builds |
| Code Quality | 15% | Formatting, style, best practices |
| Testing | 20% | Tests pass, coverage, API contract |
| Documentation | 25% | README, HOWTORUN, screenshots, spec |
| Functionality | 30% | Features work as required, no bugs |

**Green gates = passing rubric item**  
**Red gate = risk of failing that rubric item**

---

## 🔗 Related Documentsb

- **[agents.md](./agents.md)** — Guidance for AI-assisted development
- **[homework-1/README.md](./homework-1/README.md)** — Example of complete documentation
- **[homework-1/HOWTORUN.md](./homework-1/HOWTORUN.md)** — Example of runnable instructions

---

**Last Updated**: May 2, 2026  
**Applies to**: All Homework Submissions (HW1–HW6)
