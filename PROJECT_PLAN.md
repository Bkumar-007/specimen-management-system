# Specimen Management System — Project Plan (v1)

Portfolio project: a **lab Specimen Management System** covering order → accession → status tracking → result entry, with a clear path to add a **simulated instrument interface** later.

> **Honesty rule:** This is a **specimen / lab workflow system you build**. Do **not** call it STARLIMS or Outreach on the resume unless you worked on those products. Recruiter language you *can* use: specimen management, LIMS-style workflow, instrument interface (after Phase 4).

---

## 1. Vision

| Capability | Description |
|------------|-------------|
| **Lab orders** | Create orders with patient demographics, client/site, and ordered tests. |
| **Specimen accession** | Receive specimen, assign barcode, tube type, collected/received times. |
| **Status lifecycle** | Track: `ORDERED` → `COLLECTED` → `RECEIVED` → `IN_TESTING` → `RESULTED` → `REPORTED` (plus `CANCELLED` / `REJECTED`). |
| **Results** | Enter or attach results to a specimen/test; keep audit of who changed what. |
| **Search** | Find by barcode, patient name/MRN, order id, or status. |
| **Later: instrument feed** | Accept a simulated analyzer message (file or HTTP) and auto-post a result onto an existing specimen. |

**Why this project fits interviews:** clear domain model, status rules, REST APIs, audit, and a demo UI — same skills used in real LIMS / specimen work without copying a commercial product.

---

## 2. Stack (locked for MVP)

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 3, Spring Security (JWT), JPA |
| Data | PostgreSQL (Neon free tier) |
| Frontend | Next.js (JavaScript) |
| Local infra | Neon cloud DB + env vars (`.env` / `application.yml`) |
| Optional later | Redis cache, message queue for instrument ingest |

Reuse patterns you already know from the HR assistant project (layered backend, thin Next.js routes, JWT auth). Keep this repo **separate** — do not copy employer or confidential lab data.

---

## 3. High-level architecture

```text
Next.js UI  →  Spring Boot API  →  Neon PostgreSQL (cloud)
                  │
                  ├── Auth (JWT + roles)
                  ├── Orders / Specimens / Results
                  ├── Status transition rules
                  ├── Audit log
                  └── (Phase 4) Instrument ingest endpoint / file watcher
```

**Roles (MVP):**

| Role | Can do |
|------|--------|
| `LAB_TECH` | Accession specimens, update status, enter results |
| `LAB_ADMIN` | Manage tests/clients, cancel/reject, view audit |
| `CLIENT_USER` | Create/view own orders (optional Phase 3+) |

---

## 4. Domain model (MVP)

```text
Client (site / clinic)
  └── Order
        ├── Patient (name, DOB, MRN — fictional seed data only)
        ├── OrderTest[] (ordered panels/analytes)
        └── Specimen
              ├── barcode, tubeType, collectedAt, receivedAt
              ├── status
              └── Result[] (analyte, value, unit, flagged, resultedAt)
```

**Status rules (enforce in service layer):**

- Cannot jump `ORDERED` → `RESULTED` without `RECEIVED`.
- `CANCELLED` / `REJECTED` are terminal.
- Result entry allowed only from `RECEIVED` or `IN_TESTING`.
- Moving to `REPORTED` requires at least one result (or explicit override by admin — decide in Phase 2).

---

## 5. Repository layout (target)

```text
specimen-management-system/
  PROJECT_PLAN.md
  README.md
  .env.example             DB + JWT placeholders (no secrets committed)
  backend/                 Spring Boot API
    src/main/java/.../
      config/
      domain/              entities, enums, repositories
      application/         use-cases (order, specimen, result, auth)
      security/
      web/                 controllers + DTOs
  frontend/                Next.js
    app/                   routes: login, orders, specimens, results
    src/                   components, api helpers
  docs/
    LEARNING_LOG.md        short notes per phase
    SAMPLE_DATA.md         fictional patients / tests used in demos
    NEON_SETUP.md          step-by-step Neon free DB setup
```

---

## 6. Core APIs (MVP sketch)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/login` | JWT login |
| GET/POST | `/api/clients` | List/create clients |
| GET/POST | `/api/orders` | List/create orders |
| GET | `/api/orders/{id}` | Order detail + specimens |
| POST | `/api/orders/{id}/specimens` | Accession specimen |
| PATCH | `/api/specimens/{id}/status` | Validated status change |
| GET | `/api/specimens?barcode=` | Search by barcode |
| POST | `/api/specimens/{id}/results` | Add result |
| GET | `/api/audit?entityType=&entityId=` | Audit trail |

Exact DTO shapes can be finalized when coding Phase 1–2.

---

## 7. Build phases

### Phase 0 — Repo bootstrap (1 sitting)
- [x] Create Neon project + copy connection string (see `docs/NEON_SETUP.md`)
- [x] `.env.example` + Spring Boot skeleton wired to Neon via env vars + JWT auth stub
- [x] Next.js skeleton + login page shell
- [x] README with run steps
- [x] Seed one admin + one tech user

### Phase 1 — Orders + patients + clients
- [ ] Entities: Client, Patient, Order, OrderTest, TestCatalog
- [ ] CRUD APIs for clients and orders
- [ ] Seed fictional catalog (CBC, CMP, TSH, etc.)
- [ ] UI: create order, list orders

### Phase 2 — Specimens + status engine
- [ ] Specimen entity + barcode generation
- [ ] Status transition service (reject illegal moves)
- [ ] Accession UI + status timeline on detail page
- [ ] Search by barcode / MRN / status

### Phase 3 — Results + audit
- [ ] Result entry API + UI
- [ ] Audit log on create/update/status change
- [ ] Role checks (tech vs admin)
- [ ] Demo script in README (happy path)

### Phase 4 — Simulated instrument interface (optional but strong)
- [ ] `POST /api/instruments/ingest` accepting a simple JSON payload
- [ ] Match by barcode → attach result → set `RESULTED`
- [ ] Optional: drop a sample `.json` / `.hl7-lite` file into `inbox/` for a watcher
- [ ] Document as “lab instrument interface (simulated analyzer)” on resume

### Phase 5 — Polish for portfolio
- [ ] Validation, error messages, basic tests
- [ ] Deploy notes (optional: Azure / Render / Railway)
- [ ] Short architecture diagram in README
- [ ] Resume bullets drafted from real features only

---

## 8. Demo script (what you show in interviews)

1. Login as lab tech.
2. Create order for fictional patient + CBC.
3. Accession specimen → barcode appears.
4. Move status to `IN_TESTING`.
5. Enter result (or run instrument ingest).
6. Show status `RESULTED` / `REPORTED` and audit entries.
7. Search by barcode and open the same specimen.

---

## 9. Resume wording (after you build it)

Safe examples:

- Built a **Specimen Management System** (Spring Boot, PostgreSQL, Next.js) covering order entry, accessioning, status tracking, and result recording.
- Implemented **status lifecycle rules** and **audit logging** for specimen workflow.
- Added a **simulated lab instrument interface** that posts analyzer results to specimens by barcode. *(only after Phase 4)*

Avoid:

- “Administered STARLIMS / Outreach” unless that was real job experience.
- Claiming HL7/ASTM mastery unless you actually implement and can explain the message format you used.

---

## 10. Out of scope for MVP

- Full commercial LIMS (billing, QC charts, outreach portals, multi-lab routing)
- Real analyzer hardware / vendor drivers
- PHI from real patients — **fictional seed data only**
- AI/RAG (not needed for this domain story)

---

## 11. First actions when you open this repo in Cursor

1. Keep this file as the source of truth; update checkboxes as you finish phases.
2. Follow `docs/NEON_SETUP.md`, then create empty `backend/` + `frontend/` skeletons.
3. Start Phase 0, then Phase 1 — do not jump to instrument interface first.
4. Log decisions in `docs/LEARNING_LOG.md` (one short note per phase).

**Suggested first commit message after bootstrap:** `chore: bootstrap specimen management system skeleton`
