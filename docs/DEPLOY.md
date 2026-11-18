# Deploy live (Render + Netlify + Neon)

This guide deploys:

| Piece | Host | Cost |
|-------|------|------|
| Database | Neon (already set up) | Free |
| Backend API | [Render](https://render.com) | Free tier |
| Frontend UI | [Netlify](https://netlify.com) | Free tier |

**Before you start:** push the latest code (including `backend/Dockerfile`, Flyway migrations, and `PORT` support) to GitHub.

Schema is applied by **Flyway** on API startup (`ddl-auto: validate`). Existing Neon databases that already have tables are baselined automatically so `V1` is not re-applied.

```powershell
cd c:\Users\bk733\Downloads\BKDeveloper\specimen-management-system
git add backend/src/main/resources/application.yml backend/Dockerfile docs/DEPLOY.md
git commit -m "chore: prepare backend for Render deploy"
git push origin main
```

---

## Step 0 — Neon values (copy these first)

1. Open [https://console.neon.tech](https://console.neon.tech)
2. Open your project → **Dashboard** → **Connection details**
3. Choose **Pooled connection**
4. Copy into a notepad (you will paste into Render):

```text
DB_URL=jdbc:postgresql://ep-XXXX-pooler.REGION.aws.neon.tech/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=********
```

If Neon gives a `postgresql://...` URI, convert it:

```text
postgresql://USER:PASSWORD@HOST/DB?sslmode=require
→
jdbc:postgresql://HOST/DB?sslmode=require
```

Use `USER` / `PASSWORD` as `DB_USERNAME` / `DB_PASSWORD`.

Also invent two secrets (do not reuse local `.env` values for a public demo):

```text
JWT_SECRET=uOf6tJ4HeEzCLDAlj2wyQmgBGdP30NKc89UvpiX1xsqVh75I
INSTRUMENT_API_KEY=sms-instr-change-me-to-random
```

---

## Step 1 — Deploy backend on Render

1. Sign up / log in at [https://dashboard.render.com](https://dashboard.render.com) (GitHub login is easiest).
2. Click **New +** → **Web Service**.
3. Connect GitHub if asked, then select repo **`Bkumar-007/specimen-management-system`**.
4. Fill the form:

| Field | Value |
|-------|--------|
| Name | `specimen-sms-api` |
| Region | closest to you |
| Root Directory | `backend` |
| Runtime | **Docker** |
| Branch | `main` |
| Instance type | **Free** |

5. Open **Environment** → **Add Environment Variable** and add:

| Key | Value |
|-----|--------|
| `DB_URL` | Neon JDBC URL from Step 0 |
| `DB_USERNAME` | Neon username |
| `DB_PASSWORD` | Neon password |
| `JWT_SECRET` | long random string (32+ chars) |
| `INSTRUMENT_API_KEY` | random key |
| `SEED_DEMO_USERS` | `true` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` *(temporary — update in Step 3)* |

6. Click **Create Web Service**. Wait for the first build (often 5–10 minutes).
7. When live, copy the service URL, e.g.:

```text
https://specimen-sms-api.onrender.com
```

8. Test in the browser:

```text
https://specimen-sms-api.onrender.com/api/health
```

You should see a JSON health response.  
If it fails: open **Logs** on Render. Common issues = bad `DB_URL`, wrong Root Directory, or build error.

> **Free tier note:** the API sleeps after idle time. The first request after sleep can take 30–60 seconds.

---

## Step 2 — Deploy frontend on Netlify

1. Sign up / log in at [https://app.netlify.com](https://app.netlify.com) (GitHub login).
2. Click **Add new site** → **Import an existing project** → **GitHub**.
3. Select **`Bkumar-007/specimen-management-system`**.
4. Build settings (repo has `netlify.toml`, so these should auto-fill):

| Field | Value |
|-------|--------|
| Base directory | `frontend` |
| Build command | `npm run build` |
| Publish directory | `.next` |
| Plugin | `@netlify/plugin-nextjs` (from `netlify.toml`) |

5. **Environment variables** → **Add a variable** → **Add a single variable**:

| Key | Value |
|-----|--------|
| `NEXT_PUBLIC_API_URL` | `https://specimen-sms-api.onrender.com` *(your Render URL, no trailing slash)* |

6. Click **Deploy site**. Wait ~2–4 minutes.
7. Optional — show Netlify status on GitHub **commits**: **Project configuration** → **Notifications** → **Deploy notifications** → add **GitHub commit status** for deploy succeeded/failed. Push to `main` (or open a PR) and the status appears under Checks on that commit.
8. Copy the site URL, e.g.:

```text
https://random-name-123456.netlify.app
```

9. Optional: **Site configuration** → **Domain management** → **Options** → **Edit site name**  
   Example: `specimen-sms` → `https://specimen-sms.netlify.app`  
   Later you can attach a custom domain (e.g. `sms.yourname.dev`) so it does not end with `.netlify.app`.

---

## Step 3 — Fix CORS (required)

1. Go back to Render → your web service → **Environment**.
2. Change:

```text
CORS_ALLOWED_ORIGINS=https://specimen-sms.netlify.app
```

Use your real Netlify URL (exact match, `https`, no trailing slash).  
If you add a custom domain later, set that instead (or comma-separate both).

3. **Save** → Render redeploys automatically.
4. Wait until deploy is **Live**.

---

## Step 4 — Smoke test

1. Open the Netlify URL.
2. Sign in:
   - Tech: `tech` / `tech123`
   - Admin: `admin` / `admin123`
3. Create an order → Accession → move to testing → enter results → Report.
4. If login fails with a network/CORS error:
   - Confirm `NEXT_PUBLIC_API_URL` is correct and you **redeployed** Netlify after setting it.
   - Confirm `CORS_ALLOWED_ORIGINS` matches the Netlify origin exactly.
5. If the first load is slow, wait ~1 minute (Render cold start) and retry.

---

## Step 5 — Put the live link on GitHub

In `README.md` (top of the file), add:

```markdown
## Live demo

- App: https://YOUR-APP.netlify.app
- API health: https://YOUR-API.onrender.com/api/health

Demo users: `tech` / `tech123`, `admin` / `admin123` (demo only).
```

Then commit and push.

---

## Optional hardening after it works

- Set `SEED_DEMO_USERS=false` on Render **after** users exist (or keep `true` only for a private portfolio demo).
- Rotate `JWT_SECRET` and `INSTRUMENT_API_KEY` if the repo/demo is public.
- Neon free DB also sleeps; first API call after a pause can be slow.
- File-drop instrument inbox (`INSTRUMENT_INBOX_ENABLED`) is not useful on ephemeral Render disks — use HTTP ingest only in the cloud.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Render build fails | Root Directory must be `backend`; Runtime = Docker |
| `/api/health` 502 / spin | Wait for cold start; check Logs for DB connection errors |
| Neon connection refused | Use **pooled** host (`-pooler`) and `sslmode=require` |
| Netlify UI loads but API calls fail | Rebuild after setting `NEXT_PUBLIC_API_URL` |
| CORS error in browser console | `CORS_ALLOWED_ORIGINS` must equal the Netlify URL exactly |
| Netlify Next build fails | Ensure `netlify.toml` is on `main`; build uses `next build` (no turbopack) |
| Login 401 with seed users | Ensure `SEED_DEMO_USERS=true` on first boot; check Logs for seeder |

---

## What this repo already supports

- `server.port: ${PORT:8080}` — works on Render
- `backend/Dockerfile` — build + run without Maven wrapper
- Frontend reads `NEXT_PUBLIC_API_URL` in `frontend/lib/api.js`
