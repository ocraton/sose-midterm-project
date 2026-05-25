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
After startup, the client runs automatically and:
1. Fetches student `S001` from DaaS.
2. Fetches course `C001` from DaaS.
3. Sends the proposal to EaaS.
4. Prints the final audit result (decision, risk level, rationale) to console.

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

DaaS is exposed on port `8081` (internal `daas:8080/api`) and provides at least these 5 endpoints:

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/courses` | Returns the complete list of courses. |
| `GET` | `/api/courses/{id}` | Returns one course by id. |
| `GET` | `/api/students` | Returns the complete list of students. |
| `GET` | `/api/students/{id}` | Returns one student by id. |
| `GET` | `/api/courses/search` | Advanced search with multiple filters. |

All endpoints return JSON.

## EaaS Endpoint and Policies
EaaS is exposed on port `8082` with endpoint:

```text
POST /api/evaluate
```

Input: JSON containing student and course data.

Output: structured audit trail including `auditId`, `timestamp`, `decision`, `riskLevel`, and `rationale`.

Policies are externalized in `eaas/policies/corsi_policies.json`.