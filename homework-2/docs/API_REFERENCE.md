# API Reference — Customer Support Ticket System

Base URL: `http://localhost:8080`

---

## Data Models

### Ticket

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string (UUID) | auto | Auto-generated |
| `customer_id` | string | yes | |
| `customer_email` | email | yes | Must be valid email format |
| `customer_name` | string | yes | |
| `subject` | string | yes | 1–200 characters |
| `description` | string | yes | 10–2000 characters |
| `category` | enum | no | `account_access`, `technical_issue`, `billing_question`, `feature_request`, `bug_report`, `other` |
| `priority` | enum | no | `urgent`, `high`, `medium`, `low` |
| `status` | enum | auto | `new`, `in_progress`, `waiting_customer`, `resolved`, `closed` |
| `created_at` | datetime | auto | ISO 8601 |
| `updated_at` | datetime | auto | ISO 8601 |
| `resolved_at` | datetime | no | Set automatically when status → `resolved` |
| `assigned_to` | string | no | Agent identifier |
| `tags` | string[] | no | |
| `metadata.source` | enum | no | `web_form`, `email`, `api`, `chat`, `phone` |
| `metadata.browser` | string | no | |
| `metadata.device_type` | enum | no | `desktop`, `mobile`, `tablet` |

---

## Endpoints

### POST /tickets

Create a new ticket.

**Query params:** `auto_classify=true` — run auto-classification and include result in response.

**Request:**
```json
{
  "customer_id": "cust-001",
  "customer_email": "user@example.com",
  "customer_name": "Jane Doe",
  "subject": "Login not working",
  "description": "I cannot log into my account after the password reset.",
  "category": "account_access",
  "priority": "high",
  "tags": ["login", "auth"],
  "metadata": {
    "source": "web_form",
    "browser": "Chrome 120",
    "device_type": "desktop"
  }
}
```

**Response 201:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customer_id": "cust-001",
  "customer_email": "user@example.com",
  "customer_name": "Jane Doe",
  "subject": "Login not working",
  "description": "I cannot log into my account after the password reset.",
  "category": "account_access",
  "priority": "high",
  "status": "new",
  "created_at": "2024-03-15T10:30:00",
  "updated_at": "2024-03-15T10:30:00",
  "resolved_at": null,
  "assigned_to": null,
  "tags": ["login", "auth"],
  "metadata": {
    "source": "web_form",
    "browser": "Chrome 120",
    "device_type": "desktop"
  }
}
```

**With auto_classify=true — Response 201:**
```json
{
  "ticket": { "...ticket fields..." },
  "classification": {
    "category": "account_access",
    "priority": "high",
    "confidence": 0.45,
    "keywords_found": ["login"],
    "reasoning": "Classified based on 1 keyword(s): login"
  }
}
```

**Response 400:**
```json
{
  "error": "Validation Error",
  "message": "customer_email: invalid email format; subject: must be between 1 and 200 characters",
  "timestamp": "2024-03-15T10:30:00",
  "path": "/tickets"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"cust-001","customer_email":"user@example.com","customer_name":"Jane Doe","subject":"Login not working","description":"I cannot log into my account after the password reset."}'
```

---

### GET /tickets

List tickets with optional filters.

**Query params:**

| Param | Type | Example |
|-------|------|---------|
| `category` | enum | `account_access` |
| `priority` | enum | `high` |
| `status` | enum | `new` |
| `customer_id` | string | `cust-001` |

**Response 200:** Array of ticket objects.

**cURL:**
```bash
# All tickets
curl http://localhost:8080/tickets

# Filtered
curl "http://localhost:8080/tickets?customer_id=cust-001&status=new"
```

---

### GET /tickets/{id}

Get a specific ticket by ID.

**Response 200:** Ticket object.

**Response 404:**
```json
{
  "error": "Not Found",
  "message": "Ticket with id abc123 not found",
  "timestamp": "2024-03-15T10:30:00",
  "path": "/tickets/abc123"
}
```

**cURL:**
```bash
curl http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000
```

---

### PUT /tickets/{id}

Update a ticket (partial update — only provided fields are changed).

**Request:**
```json
{
  "status": "in_progress",
  "priority": "urgent",
  "category": "technical_issue",
  "assigned_to": "agent@support.com",
  "tags": ["escalated", "production"]
}
```

**Response 200:** Updated ticket object.  
**Response 404:** Not Found error.

**cURL:**
```bash
curl -X PUT http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{"status":"in_progress","assigned_to":"agent@support.com"}'
```

---

### DELETE /tickets/{id}

Delete a ticket.

**Response 204:** No content.  
**Response 404:** Not Found error.

**cURL:**
```bash
curl -X DELETE http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000
```

---

### POST /tickets/import

Bulk import tickets from CSV, JSON, or XML file.

**Request:** `multipart/form-data` with field `file`.

**Supported formats:**
- `text/csv` — headers: `customer_id, customer_email, customer_name, subject, description, [category, priority, status, tags, source, browser, device_type]`
- `application/json` — array of ticket objects
- `application/xml` — `<tickets><ticket>...</ticket></tickets>`

**Response 200:**
```json
{
  "total_records": 50,
  "successful": 47,
  "failed": 3,
  "errors": [
    { "row": 5, "field": "customer_email", "message": "invalid email format" },
    { "row": 12, "field": "subject", "message": "must not be blank" },
    { "row": 31, "field": "description", "message": "must be between 10 and 2000 characters" }
  ]
}
```

**cURL:**
```bash
# CSV import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.csv;type=text/csv"

# JSON import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.json;type=application/json"

# XML import
curl -X POST http://localhost:8080/tickets/import \
  -F "file=@demo/sample_tickets.xml;type=application/xml"
```

---

### POST /tickets/{id}/auto-classify

Classify an existing ticket using keyword-based rules.

**Response 200:**
```json
{
  "category": "account_access",
  "priority": "urgent",
  "confidence": 0.75,
  "keywords_found": ["login", "cannot access", "urgent"],
  "reasoning": "Classified based on 3 matching keywords"
}
```

**Response 404:** Ticket not found.

**cURL:**
```bash
curl -X POST http://localhost:8080/tickets/550e8400-e29b-41d4-a716-446655440000/auto-classify
```

---

## Error Response Format

All errors follow the same format:

```json
{
  "error": "string — error type",
  "message": "string — human-readable detail",
  "timestamp": "2024-03-15T10:30:00",
  "path": "/tickets/..."
}
```

| HTTP Status | When |
|-------------|------|
| 201 | Resource created |
| 204 | Resource deleted |
| 400 | Validation failure or malformed request |
| 404 | Resource not found |
| 415 | Unsupported file format on import |
