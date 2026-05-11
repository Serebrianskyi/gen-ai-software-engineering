# How to Run Homework 2: Customer Support Ticket Management System

## Prerequisites

- **Java**: OpenJDK 17+ (verify with `java -version`)
- **Gradle**: 8.0+ (included via wrapper)
- **Port**: 8080 available (or set `SERVER_PORT` env variable)
- **Disk Space**: ~500MB for build artifacts

## Step 1: Build the Project

```bash
cd /Users/rserebrianskyi/IdeaProjects/ai-workshops
./gradlew clean build -x test  # Skip tests on first build (optional)
```

**Expected Output:**
```
BUILD SUCCESSFUL in 45s
X actionable tasks: X executed
```

**Troubleshooting:**
```bash
# If build fails, try:
./gradlew clean
./gradlew build --info  # Detailed output
```

## Step 2: Run Application

### Option A: Using Gradle

```bash
./gradlew :homework-2:bootRun
```

Application will start and log:
```
Started Homework2Application in X.XXX seconds
Tomcat started on port 8080 with context path ''
```

### Option B: Using JAR (after build)

```bash
java -jar homework-2/build/libs/homework-2-2.0.0.jar
```

### Change Port

```bash
./gradlew :homework-2:bootRun --args='--server.port=8081'
```

## Step 3: Import Sample Data (Optional)

To populate the database with 100 test tickets:

```bash
# Import CSV (50 tickets)
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.csv"

# Import JSON (20 tickets)
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.json"

# Import XML (30 tickets)
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.xml"

# Verify: should return 100
curl http://localhost:8080/tickets | jq 'length'
```

**Full guide**: [`docs/SAMPLE_DATA_SETUP.md`](docs/SAMPLE_DATA_SETUP.md) — Swagger UI steps + all import commands

---

## Step 4: Verify Application is Running

### Health Check (HTTP)

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Swagger UI

Visit in browser: `http://localhost:8080/swagger-ui.html`

Interactive API explorer — try any endpoint directly without curl.

## Step 5: Run Tests

### All Tests

```bash
./gradlew :homework-2:test
```

**Expected Output:**
```
BUILD SUCCESSFUL
X tests passed
```

### Specific Test

```bash
./gradlew :homework-2:test --tests TicketControllerTest
```

### Coverage Report

```bash
./gradlew :homework-2:test jacocoTestReport
# Open: homework-2/build/reports/jacoco/test/html/index.html
```

## API Examples

### 1. Create a Ticket

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "cust-001",
    "customer_email": "john@example.com",
    "customer_name": "John Doe",
    "subject": "Cannot login to account",
    "description": "I cannot access my account after password reset",
    "category": "account_access",
    "priority": "high"
  }'
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "cust-001",
  "customer_email": "john@example.com",
  "customer_name": "John Doe",
  "subject": "Cannot login to account",
  "description": "I cannot access my account after password reset",
  "category": "account_access",
  "priority": "high",
  "status": "new",
  "created_at": "2024-05-02T10:30:00",
  "updated_at": "2024-05-02T10:30:00",
  "tags": []
}
```

### 1b. Create Ticket with Auto-Classification

Add `?auto_classify=true` to get inline classification result:

```bash
curl -X POST "http://localhost:8080/tickets?auto_classify=true" \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "cust-001",
    "customer_email": "john@example.com",
    "customer_name": "John Doe",
    "subject": "Cannot login to account",
    "description": "I cannot access my account after password reset"
  }'
```

**Expected Response:**
```json
{
  "ticket": { "id": "...", "status": "new", "..." },
  "classification": {
    "category": "account_access",
    "priority": "medium",
    "confidence": 0.30,
    "keywords_found": ["login", "access"],
    "reasoning": "Classified based on 2 keyword(s): login, access"
  }
}
```

### 2. List All Tickets

```bash
curl http://localhost:8080/tickets
```

**Expected Response:**
```json
[
  { "ticket1": "" },
  { "ticket2": "" }
]
```

### 3. List Tickets with Filters

```bash
# Filter by customer ID (string — always works)
curl "http://localhost:8080/tickets?customer_id=cust-001"

# Filter by status (enum — Spring may return 400 for some values; use customer_id for reliable filtering)
curl "http://localhost:8080/tickets?status=new"
```

> **Note:** Enum query params (`category`, `priority`, `status`) may return HTTP 400 depending on Spring's enum binding configuration. Use `customer_id` filtering reliably. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for details.

### 4. Get Specific Ticket

```bash
curl http://localhost:8080/tickets/{ticket_id}
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "cust-001",
  ...
}
```

### 5. Update Ticket

```bash
curl -X PUT http://localhost:8080/tickets/{ticket_id} \
  -H "Content-Type: application/json" \
  -d '{
    "status": "in_progress",
    "priority": "urgent",
    "assigned_to": "support-team@company.com"
  }'
```

### 6. Delete Ticket

```bash
curl -X DELETE http://localhost:8080/tickets/{ticket_id}
```

**Expected Response:** 204 No Content

### 7. Import Tickets from CSV

**Create test file** `tickets.csv`:
```csv
customer_id,customer_email,customer_name,subject,description,category,priority
cust-001,john@example.com,John Doe,Login Issue,Cannot login to account,account_access,high
cust-002,jane@example.com,Jane Smith,App Crashes,Application crashes on upload,bug_report,urgent
cust-003,bob@example.com,Bob Johnson,Billing,Double charge on subscription,billing_question,high
```

**Upload file:**
```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@tickets.csv"
```

**Expected Response:**
```json
{
  "total_records": 3,
  "successful": 3,
  "failed": 0,
  "errors": []
}
```

### 8. Import Tickets from JSON

**Create test file** `tickets.json`:
```json
[
  {
    "customer_id": "cust-001",
    "customer_email": "john@example.com",
    "customer_name": "John Doe",
    "subject": "Login Issue",
    "description": "Cannot login to account",
    "category": "account_access",
    "priority": "high"
  }
]
```

**Upload file:**
```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@tickets.json"
```

### 9. Import Tickets from XML

**Create test file** `tickets.xml`:
```xml
<?xml version="1.0"?>
<tickets>
  <ticket>
    <customer_id>cust-001</customer_id>
    <customer_email>john@example.com</customer_email>
    <customer_name>John Doe</customer_name>
    <subject>Login Issue</subject>
    <description>Cannot login to account</description>
    <category>account_access</category>
    <priority>high</priority>
  </ticket>
</tickets>
```

**Upload file:**
```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@tickets.xml"
```

### 10. Auto-Classify Ticket

```bash
curl -X POST http://localhost:8080/tickets/{ticket_id}/auto-classify
```

**Expected Response:**
```json
{
  "category": "account_access",
  "priority": "high",
  "confidence": 0.85,
  "keywords_found": ["login", "password", "access"],
  "reasoning": "Classified based on 3 matching keywords"
}
```

## Error Handling Examples

### Invalid Email

```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "customer_id": "cust-001",
    "customer_email": "invalid-email",
    "customer_name": "John",
    "subject": "Subject",
    "description": "Description with sufficient length"
  }'
```

**Response:** 400 Bad Request
```json
{
  "error": "Validation Error",
  "message": "customer_email: invalid email format",
  "timestamp": "2024-05-02T10:30:00",
  "path": "/tickets"
}
```

### Ticket Not Found

```bash
curl http://localhost:8080/tickets/non-existent-id
```

**Response:** 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Ticket with id non-existent-id not found",
  "timestamp": "2024-05-02T10:30:00",
  "path": "/tickets/non-existent-id"
}
```

### Empty File Import

```bash
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@empty.csv"
```

**Response:** 400 Bad Request
```json
{
  "error": "File is empty",
  "total_records": 0,
  "successful": 0,
  "failed": 1
}
```

## Code Quality Checks

### Format Check

```bash
./gradlew :homework-2:spotlessCheck
```

### Auto-Format

```bash
./gradlew :homework-2:spotlessApply
```

### Full Quality Gates

```bash
./gradlew clean build spotlessCheck test
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process or use different port
export SERVER_PORT=8081
./gradlew :homework-2:bootRun
```

### Out of Memory

```bash
export GRADLE_OPTS="-Xmx1024m"
./gradlew clean build
```

### Gradle Build Cache Issues

```bash
./gradlew clean
rm -rf .gradle
./gradlew build
```

### Tests Failing

```bash
# Run with debug output
./gradlew test --info

# Run single test
./gradlew test --tests TicketControllerTest
```

## Performance Testing

### Load Test (100 requests)

```bash
for i in {1..100}; do
  curl http://localhost:8080/tickets?category=account_access &
done
wait
```

### Benchmark Import

```bash
# Create large CSV (1000 records)
seq 1 1000 | while read i; do
  echo "cust-$i,user$i@example.com,User $i,Subject,Description,account_access,high"
done > large.csv

# Time the import
time curl -X POST http://localhost:8080/tickets/import -F "file=@large.csv"
```

## Project Structure

```
homework-2/
├── src/main/kotlin/com/ai/homework/
│   ├── controller/         # REST endpoints
│   ├── service/            # Business logic
│   ├── importer/           # File parsers
│   ├── validator/          # Input validation
│   ├── model/              # Domain classes
│   └── dto/                # Data transfer objects
├── src/test/kotlin/        # 320+ unit/integration tests
├── src/test/resources/     # Test fixtures
├── build.gradle.kts        # Gradle configuration
└── docs/                   # Documentation
```

## Next Steps

1. **Explore API**: Run curl examples above
2. **Review Code**: Check `src/main/kotlin/` structure
3. **Run Tests**: `./gradlew test`
4. **Check Coverage**: Open `build/reports/jacoco/test/html/index.html`
5. **Review Docs**: See `README.md` for architecture details

---

**Status**: ✅ Ready to Run  
**Last Verified**: May 10, 2026