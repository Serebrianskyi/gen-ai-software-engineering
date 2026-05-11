# Architecture — Customer Support Ticket System

## High-Level Overview

```mermaid
C4Context
    title Customer Support Ticket System — Context Diagram

    Person(user, "API Consumer", "Developer or support agent using the REST API")
    System(ticketSystem, "Ticket System", "Spring Boot application for managing support tickets")
    System_Ext(fileSystem, "File System", "CSV / JSON / XML import files")

    Rel(user, ticketSystem, "CRUD operations, bulk import, auto-classify", "HTTP/JSON")
    Rel(user, fileSystem, "Uploads files")
    Rel(ticketSystem, fileSystem, "Reads during import")
```

## Component Diagram

```mermaid
C4Component
    title Internal Components

    Container_Boundary(api, "Spring Boot App") {
        Component(ctrl, "TicketController", "REST Controller", "Routes HTTP requests, handles status codes")
        Component(importCtrl, "ImportController", "REST Controller", "Handles multipart file uploads")
        Component(classCtrl, "ClassificationController", "REST Controller", "Exposes auto-classify endpoint")

        Component(svc, "TicketService", "Service", "Business logic: CRUD, filtering, resolved_at timestamps")
        Component(classSvc, "ClassificationService", "Service", "Keyword-based category and priority classification")

        Component(validator, "TicketValidator", "Validator", "Email, length, enum validation")

        Component(csvImp, "CsvImporter", "Importer", "Apache Commons CSV parsing")
        Component(jsonImp, "JsonImporter", "Importer", "Jackson JSON array parsing")
        Component(xmlImp, "XmlImporter", "Importer", "Jackson XML parsing")

        Component(store, "In-Memory Store", "ConcurrentHashMap", "Thread-safe ticket storage")
    }

    Rel(ctrl, svc, "uses")
    Rel(ctrl, classSvc, "auto-classify on create")
    Rel(ctrl, validator, "validates requests")
    Rel(importCtrl, csvImp, "delegates by content-type")
    Rel(importCtrl, jsonImp, "delegates by content-type")
    Rel(importCtrl, xmlImp, "delegates by content-type")
    Rel(importCtrl, svc, "persists valid tickets")
    Rel(classCtrl, classSvc, "classifies ticket")
    Rel(classCtrl, svc, "looks up ticket")
    Rel(svc, store, "reads / writes")
```

## Request Flow — Create Ticket

```mermaid
sequenceDiagram
    participant Client
    participant Controller as TicketController
    participant Validator as TicketValidator
    participant Service as TicketService
    participant Classifier as ClassificationService
    participant Store as ConcurrentHashMap

    Client->>Controller: POST /tickets [?auto_classify=true]
    Controller->>Validator: validate(request)
    alt validation fails
        Validator-->>Controller: errors list
        Controller-->>Client: 400 Bad Request
    else validation passes
        Validator-->>Controller: empty list
        Controller->>Service: createTicket(request)
        Service->>Store: put(ticket)
        Store-->>Service: ok
        Service-->>Controller: ticket
        alt auto_classify=true
            Controller->>Classifier: classify(ticket)
            Classifier-->>Controller: ClassificationResult
            Controller-->>Client: 201 {ticket, classification}
        else
            Controller-->>Client: 201 TicketResponse
        end
    end
```

## Request Flow — Bulk Import

```mermaid
sequenceDiagram
    participant Client
    participant Controller as ImportController
    participant Importer as CsvImporter / JsonImporter
    participant Validator as TicketValidator
    participant Service as TicketService

    Client->>Controller: POST /tickets/import (multipart)
    Controller->>Importer: import(inputStream)
    loop for each record
        Importer->>Validator: validate(request)
        alt valid
            Importer->>Service: createTicket(request)
        else invalid
            Importer->>Importer: add to errors list
        end
    end
    Importer-->>Controller: ImportResult
    Controller-->>Client: 200 {total, successful, failed, errors}
```

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage | `ConcurrentHashMap` | Zero dependencies, thread-safe, sufficient for demo scope |
| Import routing | Content-Type header | Standard HTTP convention; avoids filename-based guessing |
| Classification | Keyword matching | Simple, deterministic, fully testable without LLM dependency |
| Enum serialization | Jackson `@JsonProperty` with lowercase values | REST convention; Spring `@JsonValue` drives consistent deserialization |
| ID generation | `UUID.randomUUID()` | Globally unique, no coordination needed |
| `resolved_at` | Auto-set on status → RESOLVED | Business invariant enforced in service layer, not controller |

## Trade-offs and Limitations

- **No persistence** — data is lost on restart; replace `ConcurrentHashMap` with JPA + PostgreSQL for production
- **XML coverage gap** — Jackson XML + Gradle 8.x compatibility limits XML importer test coverage to ~10%; CSV and JSON are fully tested
- **Enum query params** — Spring cannot deserialize lowercase enum values from `@RequestParam` without a custom converter; filter endpoints accept `null` for enum params
- **Classification** — purely keyword-based; no ML model; confidence score is proportional to keyword match count, not probabilistic

## Security Considerations

- No authentication — add Spring Security + JWT for production
- Input validation at controller boundary prevents injection via description/subject fields
- In-memory storage has no encryption at rest
- Rate limiting not implemented — required for public-facing deployments
