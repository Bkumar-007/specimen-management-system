# Learning log

## Phase 0: Bootstrap

- Chose Neon free Postgres instead of local DB install; connection via root `.env` + `dotenv-java`.
- Spring Boot 3 + Security JWT stub; seed users `admin` / `tech`.
- Next.js App Router login shell calling `POST /api/auth/login`.

## Phase 1: Orders, patients, clients

- Table is named `orders` but the entity is `LabOrder`, and `Specimen` points at it through `labOrder`, because `order` is a reserved SQL word.
- Patients are found by MRN, so re-ordering for the same MRN reuses the record instead of duplicating it.
- Catalog, clients and users are seeded idempotently on startup (`DataSeeder`).
- Order status is derived from its specimens rather than stored, so it can never drift out of sync.

## Phase 2: Specimens and status engine

- Transition rules live in `StatusTransitionPolicy`, a pure class with no persistence, which made them cheap to unit test exhaustively.
- Decision: `REPORTED` needs at least one result; an admin can override with a reason from RECEIVED/IN_TESTING. `REPORTED` is treated as final alongside CANCELLED/REJECTED.
- Accessioning marks a specimen RECEIVED by default (`markReceived=false` leaves it COLLECTED), so the demo is shorter.
- Barcodes are `SPC` + date + random digits, checked for uniqueness.
- Search uses JPA Specifications so optional filters do not produce typed-null SQL issues on Postgres.

## Phase 3: Results and audit

- Results are posted as a batch (a CBC has several analytes); posting moves the specimen to RESULTED, which is why a second post is refused.
- Every create and status change writes an `audit_log` row in the same transaction as the change.
- Method-level `@PreAuthorize` plus service-level checks for admin-only status changes, because one PATCH endpoint serves both roles.
- Consistent JSON errors (`ApiExceptionHandler`): 400 validation with field errors, 404, 409 for rule violations.

## Phase 4: Instrument interface

- `POST /api/instruments/ingest` accepts a JWT or an `X-Instrument-Key`, so a script can act as an analyzer without a user login.
- The optional inbox watcher reuses the exact same `InstrumentService`, so HTTP and file paths cannot diverge.
- Results are attributed to `instrument:<id>` in both the result row and the audit log.
- The format is custom JSON on purpose; claiming HL7 would need real segment parsing and ACKs.

## Phase 5: Polish

- Integration tests run on H2 in PostgreSQL mode so they never touch Neon.
- A column named `value` is reserved in some databases, so the result column is `result_value`.
- Left out on purpose: refresh tokens, QC and billing.

## Portfolio Phase 4: Work-queue UI

- Home is a **work queue** (`needsAction=true`) instead of a generic dashboard dump.
- Global barcode **Scan** in the top bar (`/` to focus) for scan → open → act demos.
- Specimen search is paginated with date filters; empty/loading/error states tightened on main pages.
- Optional accession **label print preview** (browser print; visual bars only — not a certified symbology).
- Frontend stays JavaScript (no TypeScript migration).

## Architecture alignment (HR-style)

- Backend `application` split by domain: `order`, `specimen`, `audit`, `instrument`, `referencedata`, `common`.
- Frontend: `utils/apiRoutes` (API paths), `store/{domain}` (actions + types), `src/component/{common,layout}`.
