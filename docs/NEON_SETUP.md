# Neon free Postgres — step by step

Use Neon free-tier Postgres for local development.

## 1. Create account

1. Go to [https://neon.tech](https://neon.tech)
2. Sign up with GitHub or email
3. Confirm email if asked

## 2. Create a project

1. Click **Create project** (or **New project**)
2. Settings that work well for this app:
   - **Project name:** `specimen-management` (or any name)
   - **Postgres version:** default (15+)
   - **Region:** closest to you (e.g. Singapore / US East)
3. Click **Create project**

Neon creates a default database (often named `neondb`) and a role for you.

## 3. Copy the connection string

1. Open your project in the Neon console
2. Go to **Dashboard** → **Connection details** (or **Connect**)
3. Choose:
   - **Connection string**
   - Driver / format: **JDBC** if shown, otherwise use the Postgres URI
4. Toggle **Show password** so the real password is visible
5. Copy the string — it looks like one of these:

**Postgres URI (common):**
```text
postgresql://USER:PASSWORD@ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
```

**JDBC (what Spring Boot needs):**
```text
jdbc:postgresql://ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
```

If Neon only shows the `postgresql://...` URI, convert it:

| Piece | From URI | For Spring |
|-------|----------|------------|
| Host | `ep-xxxx....neon.tech` | same |
| Database | `/neondb` | same |
| User | before `:` | `spring.datasource.username` |
| Password | between `:` and `@` | `spring.datasource.password` |
| URL | — | `jdbc:postgresql://HOST/neondb?sslmode=require` |

Keep **`sslmode=require`** — Neon needs SSL.

## 4. Put secrets in local env (do not commit)

In the repo root (or `backend/`), create a file named `.env` (gitignored):

```env
DB_URL=jdbc:postgresql://ep-xxxx.region.aws.neon.tech/neondb?sslmode=require
DB_USERNAME=your_neon_user
DB_PASSWORD=your_neon_password
JWT_SECRET=change-me-to-a-long-random-string
```

`docs/NEON_SETUP.md` and `.env.example` stay public; real `.env` never goes to git.

## 5. Wire Spring Boot (Phase 0)

In `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

Run the backend with those env vars set (IDE run config, or a tool that loads `.env`).

## 6. Smoke-test the DB

1. In Neon console → **SQL Editor**
2. Run:

```sql
SELECT version();
```

3. After Spring Boot starts once with `ddl-auto: update`, refresh **Tables** in Neon — you should see app tables appear.

## Tips

- Free tier may **suspend** after idle time; first request can take a few seconds (cold start).
- Prefer Neon’s **pooled** connection string if the UI offers both “direct” and “pooled” — good default for a small Spring app.
- Rotate the password in Neon if you ever paste it into chat or commit it by mistake.
- Do **not** store real patient PHI — fictional demo data only.

## Done checklist

- [ ] Neon project created
- [ ] JDBC URL + user + password copied
- [ ] Local `.env` filled in
- [ ] `application.yml` reads `${DB_URL}` / `${DB_USERNAME}` / `${DB_PASSWORD}`
- [ ] `SELECT version();` works in Neon SQL Editor
