# Deploy live (Render + Vercel + Neon)

This guide deploys:

| Piece | Host | Cost |
|-------|------|------|
| Database | Neon (already set up) | Free |
| Backend API | [Render](https://render.com) | Free tier |
| Frontend UI | [Vercel](https://vercel.com) | Free tier |

**Before you start:** push the latest code (including `backend/Dockerfile` and `PORT` support) to GitHub.

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

## Step 2 — Deploy frontend on Vercel

1. Sign up / log in at [https://vercel.com](https://vercel.com) (GitHub login).
2. Click **Add New…** → **Project**.
3. Import **`Bkumar-007/specimen-management-system`**.
4. Configure:

| Field | Value |
|-------|--------|
| Framework Preset | Next.js |
| Root Directory | **`frontend`** (click Edit) |
| Build Command | `npm run build` (default) |
| Output | default |

5. **Environment Variables** → add:

| Key | Value |
|-----|--------|
| `NEXT_PUBLIC_API_URL` | `https://specimen-sms-api.onrender.com` *(your Render URL, no trailing slash)* |

6. Click **Deploy**. Wait ~1–2 minutes.
7. Copy the frontend URL, e.g.:

```text
https://specimen-management-system-xxxx.vercel.app
```

---

## Step 3 — Fix CORS (required)

1. Go back to Render → your web service → **Environment**.
2. Change:

```text
CORS_ALLOWED_ORIGINS=https://specimen-management-system-xxxx.vercel.app
```

Use your real Vercel URL (exact match, `https`, no trailing slash).  
If you have a custom domain later, put that instead (or comma-separate both).

3. **Save** → Render redeploys automatically.
4. Wait until deploy is **Live**.

---

## Step 4 — Smoke test

1. Open the Vercel URL.
2. Sign in:
   - Tech: `tech` / `tech123`
   - Admin: `admin` / `admin123`
3. Create an order → Accession → move to testing → enter results → Report.
4. If login fails with a network/CORS error:
   - Confirm `NEXT_PUBLIC_API_URL` is correct and you **redeployed** Vercel after setting it.
   - Confirm `CORS_ALLOWED_ORIGINS` matches the Vercel origin exactly.
5. If the first load is slow, wait ~1 minute (Render cold start) and retry.

---

## Step 5 — Put the live link on GitHub

In `README.md` (top of the file), add:

```markdown
## Live demo

- App: https://YOUR-APP.vercel.app
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
| Vercel UI loads but API calls fail | Rebuild after setting `NEXT_PUBLIC_API_URL` |
| CORS error in browser console | `CORS_ALLOWED_ORIGINS` must equal the Vercel URL exactly |
| Login 401 with seed users | Ensure `SEED_DEMO_USERS=true` on first boot; check Logs for seeder |

---

## What this repo already supports

- `server.port: ${PORT:8080}` — works on Render
- `backend/Dockerfile` — build + run without Maven wrapper
- Frontend reads `NEXT_PUBLIC_API_URL` in `frontend/lib/api.js`
