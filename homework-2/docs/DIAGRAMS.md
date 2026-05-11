# System Diagrams — Customer Support Ticket System

All diagrams use [Mermaid](https://mermaid.js.org/) syntax. Render in GitHub, GitLab, or any Mermaid-compatible viewer.

---

## 1. Component Architecture

Shows how the main building blocks connect.

```mermaid
graph TD
    Client([API Consumer])

    subgraph Controllers["REST Layer"]
        TC["TicketController<br/>/tickets"]
        IC["ImportController<br/>/tickets/import"]
        CC["ClassificationController<br/>/tickets/:id/auto-classify"]
    end

    subgraph Services["Service Layer"]
        TS["TicketService<br/>CRUD · filtering · lifecycle"]
        CS["ClassificationService<br/>keyword matching · confidence"]
    end

    subgraph Importers["Importers"]
        CSV["CsvImporter<br/>Apache Commons CSV"]
        JSON["JsonImporter<br/>Jackson"]
        XML["XmlImporter<br/>Jackson XML"]
    end

    TV["TicketValidator<br/>email · lengths · enums"]
    Store[(ConcurrentHashMap)]

    Client --> TC & IC & CC

    TC --> TV
    TC --> TS
    TC --> CS

    IC -->|"content-type: text/csv"| CSV
    IC -->|"content-type: application/json"| JSON
    IC -->|"content-type: application/xml"| XML
    CSV & JSON & XML --> TV
    CSV & JSON & XML --> TS

    CC --> CS
    CC --> TS

    TS --> Store

    style Store fill:#f0f4f8,stroke:#90a0b0
```

---

## 2. Ticket Lifecycle (State Machine)

```mermaid
stateDiagram-v2
    [*] --> new : POST /tickets (create)

    new --> in_progress : PUT status=in_progress
    new --> waiting_customer : PUT status=waiting_customer
    new --> closed : PUT status=closed

    in_progress --> waiting_customer : PUT status=waiting_customer
    in_progress --> resolved : PUT status=resolved
    in_progress --> closed : PUT status=closed

    waiting_customer --> in_progress : PUT status=in_progress
    waiting_customer --> resolved : PUT status=resolved
    waiting_customer --> closed : PUT status=closed

    resolved --> closed : PUT status=closed
    resolved --> in_progress : PUT status=in_progress (re-open)

    closed --> [*]

    note right of resolved
        resolved_at timestamp
        is set automatically
    end note
```

---

## 3. Create Ticket — Request Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant CT as TicketController
    participant V as TicketValidator
    participant S as TicketService
    participant CL as ClassificationService
    participant DB as ConcurrentHashMap

    C->>CT: POST /tickets [?auto_classify=true]
    CT->>V: validate(request)

    alt validation fails
        V-->>CT: [error list]
        CT-->>C: 400 Bad Request<br/>{"error":"Validation Error","message":"..."}
    else validation passes
        V-->>CT: []
        CT->>S: createTicket(request)
        S->>DB: put(uuid, ticket)
        DB-->>S: ok
        S-->>CT: Ticket

        alt auto_classify=true
            CT->>CL: classify(ticket)
            CL-->>CT: ClassificationResult
            CT-->>C: 201 Created<br/>{"ticket":{...},"classification":{...}}
        else auto_classify=false (default)
            CT-->>C: 201 Created<br/>TicketResponse{...}
        end
    end
```

---

## 4. Bulk Import — Request Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant IC as ImportController
    participant IMP as Importer (CSV/JSON/XML)
    participant V as TicketValidator
    participant S as TicketService

    C->>IC: POST /tickets/import<br/>Content-Type: text/csv<br/>file: @tickets.csv

    IC->>IMP: route by Content-Type
    note over IMP: Parse entire file<br/>into raw records

    loop for each record
        IMP->>V: validate(record)
        alt valid
            IMP->>S: createTicket(record)
            S-->>IMP: ticket saved
        else invalid
            IMP->>IMP: append ImportError{row, field, message}
        end
    end

    IMP-->>IC: ImportResult{total, successful, failed, errors[]}
    IC-->>C: 200 OK<br/>{"total_records":50,"successful":47,"failed":3,"errors":[...]}
```

---

## 5. Auto-Classification Logic

```mermaid
flowchart TD
    Start([ticket.subject + ticket.description]) --> Lower[lowercase combined text]

    Lower --> CatLoop{for each category<br/>count keyword matches}

    CatLoop -->|ACCOUNT_ACCESS keywords<br/>login, password, 2fa, access...| CatScore
    CatLoop -->|TECHNICAL_ISSUE keywords<br/>error, crash, bug, timeout...| CatScore
    CatLoop -->|BILLING_QUESTION keywords<br/>invoice, payment, refund...| CatScore
    CatLoop -->|FEATURE_REQUEST keywords<br/>feature, enhancement, suggest...| CatScore
    CatLoop -->|BUG_REPORT keywords<br/>bug, defect, reproduce...| CatScore

    CatScore[pick category with most matches] --> BestCat{any matches?}
    BestCat -->|yes| AssignCat[category = best match]
    BestCat -->|no| OtherCat[category = OTHER]

    AssignCat & OtherCat --> PriLoop{check priority keywords<br/>in order: URGENT → HIGH → LOW}

    PriLoop -->|urgent, critical, security,<br/>production down, can't access| PriUrgent[priority = URGENT]
    PriLoop -->|important, blocking,<br/>asap, high priority| PriHigh[priority = HIGH]
    PriLoop -->|minor, cosmetic,<br/>suggestion, low priority| PriLow[priority = LOW]
    PriLoop -->|no match| PriMed[priority = MEDIUM]

    PriUrgent & PriHigh & PriLow & PriMed --> Confidence

    Confidence["confidence = min(1.0, totalKeywordsMatched × 0.15)"]
    Confidence --> Result([ClassificationResult<br/>category · priority · confidence<br/>keywordsFound · reasoning])
```

---

## 6. Data Model

```mermaid
erDiagram
    Ticket {
        string id PK "UUID"
        string customer_id
        string customer_email
        string customer_name
        string subject "1-200 chars"
        string description "10-2000 chars"
        TicketCategory category
        TicketPriority priority
        TicketStatus status
        datetime created_at
        datetime updated_at
        datetime resolved_at "nullable"
        string assigned_to "nullable"
        stringArray tags
    }

    TicketMetadata {
        TicketSource source
        string browser "nullable"
        DeviceType device_type "nullable"
    }

    Ticket ||--o| TicketMetadata : has

    TicketCategory {
        enum account_access
        enum technical_issue
        enum billing_question
        enum feature_request
        enum bug_report
        enum other
    }

    TicketPriority {
        enum urgent
        enum high
        enum medium
        enum low
    }

    TicketStatus {
        enum new
        enum in_progress
        enum waiting_customer
        enum resolved
        enum closed
    }

    TicketSource {
        enum web_form
        enum email
        enum api
        enum chat
        enum phone
    }

    DeviceType {
        enum desktop
        enum mobile
        enum tablet
    }
```

---

## 7. Test Coverage by Layer

```mermaid
pie title Test Coverage Distribution (320+ tests)
    "TicketValidator (97%)" : 35
    "CsvImporter (90%)" : 50
    "JsonImporter (90%)" : 45
    "ImporterEdgeCases" : 60
    "TicketService (97%)" : 25
    "ClassificationService (97%)" : 20
    "Controller/Integration (89%)" : 55
    "DTOs (100%)" : 30
```
