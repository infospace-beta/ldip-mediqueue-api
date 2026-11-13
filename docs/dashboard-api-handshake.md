# Dashboard API handshake

This contract is derived from the Spring Boot dashboard controllers and
response DTOs in this repository. The Vue client in
`ldip-mediqueue-app/frontend` calls these endpoints through its MediQueue API
service.

All endpoints require a valid bearer token. Responses are JSON and use
camel-case property names. These endpoints do not accept a request body.

| Method and path | Allowed role(s) | Response |
|---|---|---|
| `GET /api/v1/dashboards/hospitals/{hospitalId}` | Authenticated users authorized for that hospital | `HospitalDashboardResponse` |
| `GET /api/v1/dashboards/doctor` | `DOCTOR` | `DoctorDashboardResponse` |
| `GET /api/v1/dashboards/staff` | `RECEPTIONIST`, `MEDICAL_STAFF` | `StaffDashboardResponse` |
| `GET /api/v1/dashboards/responder` | `RESPONDER` | `ResponderDashboardResponse` |

## Response fields

### Hospital dashboard

```json
{
  "hospitalId": 1,
  "waitingTickets": 12,
  "averageWaitMinutes": 18.5,
  "openEmergencyCases": 2,
  "onDutyDoctors": 4
}
```

`averageWaitMinutes` may be `null` when no average can be calculated.

### Doctor dashboard

```json
{
  "hospitalId": 1,
  "onDuty": true,
  "waitingTickets": 12,
  "openEmergencyCases": 2
}
```

### Staff dashboard

```json
{
  "hospitalId": 1,
  "waitingTickets": 12,
  "openEmergencyCases": 2,
  "onDutyDoctors": 4
}
```

### Responder dashboard

```json
{
  "hospitalId": 1,
  "assignedCases": 3,
  "respondingCases": 1
}
```

The `hospitalId` path parameter must be the numeric ID of an existing
hospital. The hospital dashboard endpoint additionally checks that the
authenticated user may access that hospital. Role-specific endpoints derive
the current account from the bearer token; clients should not send a user ID
or username in the request.

The patient dashboard loads its supporting visit, appointment, notification,
and emergency data from the corresponding APIs. Patient emergency history is
available at `GET /api/v1/patients/{patientId}/emergency-cases`, returning a
page of `EmergencyResponse` objects. Access is limited to that patient and
system administrators.

The backend tests for these endpoints are
`DashboardControllerTest` and `DashboardServiceTest`.

## Integration verification

- The frontend service defines callers for all four dashboard endpoints.
- The staff dashboard selects the doctor, staff, or hospital summary endpoint
  based on the signed-in user's role.
- The responder dashboard loads the responder summary, emergency cases, and
  notifications from the API.
- The patient dashboard loads visit, appointment, notification, and emergency
  history from the API.
- Backend controller/service tests passed (7 tests), and the frontend
  production build and type-check passed.
- No request-path or response-field mismatch was found during source and build
  verification.

Live end-to-end verification against a running API and PostgreSQL database is
still pending; it requires the backend and database to be started with the
local environment configuration. Any runtime integration issue should be
recorded here once observed.
