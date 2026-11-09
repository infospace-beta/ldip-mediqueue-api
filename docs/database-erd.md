# MediQueue database ER diagram

The diagram reflects the PostgreSQL tables created by Flyway migrations V1–V9.
MediQueue models patients, doctors, staff, responders, and administrators as
rows in `users`; the `role` column distinguishes account types. Queue tickets
are the visit/history records returned by the API. There is no separate
`patients`, `visits`, or `roles` table.

```mermaid
erDiagram
    HOSPITALS ||--o{ DEPARTMENTS : contains
    HOSPITALS o|--o{ USERS : assigns
    USERS ||--o{ QUEUE_TICKETS : patient
    DEPARTMENTS ||--o{ QUEUE_TICKETS : queues
    HOSPITALS ||--o{ EMERGENCY_CASES : receives
    USERS o|--o{ EMERGENCY_CASES : patient
    USERS ||--o{ EMERGENCY_CASES : reports
    USERS o|--o{ EMERGENCY_CASES : responds

    AUTHORS ||--o{ BOOKS : writes
    USERS ||--o{ BOOKMARKS : saves
    BOOKS ||--o{ BOOKMARKS : bookmarked

    HOSPITALS {
        bigint id PK
        string name
        string registration_number UK
        string hospital_type
        string status
    }

    DEPARTMENTS {
        bigint id PK
        bigint hospital_id FK
        string name
        string code
        string status
    }

    USERS {
        bigint id PK
        bigint hospital_id FK
        string username UK
        string email UK
        string password_hash
        string role
        boolean enabled
        boolean on_duty
    }

    QUEUE_TICKETS {
        bigint id PK
        bigint patient_id FK
        bigint department_id FK
        string status
        timestamp called_at
        timestamp completed_at
    }

    EMERGENCY_CASES {
        bigint id PK
        bigint hospital_id FK
        bigint patient_id FK
        bigint reported_by_id FK
        bigint assigned_responder_id FK
        string severity
        string status
        timestamp resolved_at
    }

    AUTHORS {
        bigint id PK
        string name
    }

    BOOKS {
        bigint id PK
        bigint author_id FK
        string title
        string isbn UK
    }

    BOOKMARKS {
        bigint id PK
        bigint user_id FK
        bigint book_id FK
        string note
    }
```

`AUTHORS`, `BOOKS`, and `BOOKMARKS` are retained from the repository's original
library starter schema. The MediQueue API does not currently use them.
