# SOSE Midterm Project 2025/2026: Ethics as a Service

**Author:** Marco Spada
**Application Domain:** University Services (Course Assignment)

## Project Overview
This project integrates Data as a Service (DaaS) and Ethics as a Service (EaaS) in a small service-oriented application.

Services included:
- Triplestore (Fuseki) for RDF data and SPARQL.
- DaaS service exposing university data via REST.
- EaaS service evaluating course-assignment proposals against external policies.
- Client orchestrator running an end-to-end demo.

## Build and Execution

### Prerequisites
- Docker installed and running (Docker Desktop or Docker Engine + Docker Compose).

### Start the system
From the project root (where `docker-compose.yml` is located), run:

```bash
docker compose up --build
```

Healthchecks are enabled: DaaS waits for Fuseki, and the client waits for DaaS and EaaS.

## Demo Instructions
After startup, the client runs automatically and executes three end-to-end scenarios:
1. Scenario 1 (expected: REJECT): student `S001` + course `C001`.
2. Scenario 2 (expected: PROCEED): student `S002` + course `C002`.
3. Scenario 3 (expected: ESCALATE): student `S003` + course `C003`.

For each scenario, the orchestrator fetches student/course data from DaaS, sends the proposal to EaaS, and prints the final audit result (decision, risk level, rationale). If `DEMO_PAUSE_MS` is set to a value greater than `0`, a pause is applied between scenarios.

To inspect EaaS audit traces in logs:

```bash
docker logs eaas-service
```

To stop and clean up:

```bash
docker compose down -v
```

## DaaS REST Endpoints & Dataset
The DaaS queries an RDF dataset (`dataset.ttl`) modeled with standard prefixes (`foaf:`) and a custom vocabulary (`ex:`).
Main classes are `foaf:Person` (Student) and `ex:Course` (Course).

DaaS is exposed on port `8081` (internal `daas:8080/api`) and provides at least these 6 endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Service health check (returns status `UP`). |
| `GET` | `/api/courses` | Returns the complete list of courses. |
| `GET` | `/api/courses/{id}` | Returns one course by id. |
| `GET` | `/api/courses/search` | Advanced search by optional filters: `workload` (`High`/`Low`) and `physical` (`true`/`false`). |
| `GET` | `/api/students` | Returns the complete list of students. |
| `GET` | `/api/students/{id}` | Returns one student by id. |

### API Documentation (Swagger UI)
Interactive API documentation is automatically generated via Swagger UI (OpenAPI v3).

Official and only URL to use:
- http://localhost:8081/api/docs

The OpenAPI specification consumed by the UI is:
- http://localhost:8081/openapi.json

Note: do not use `/api-docs` for the demo, because it may show the default Swagger example instead of the project APIs.

## EaaS Endpoints and Policies
EaaS is exposed on port `8082` and provides the following endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Service health check (returns status `UP`). |
| `GET` | `/api/audits` | Returns audit trail entries (supports optional query param `limit`, default 50). |
| `GET` | `/api/audits/{requestId}` | Returns one audit entry by request id. |
| `POST` | `/api/evaluate` | Evaluates a student-course proposal and returns outcome plus audit trace. |

Input for `/api/evaluate`: JSON containing student and course data.

Output: structured response including `outcome`, `auditId`, `timestamp`, `requiredActions`, and `audit_trace`.

Policies are externalized in `eaas/policies/corsi_policies.json`.