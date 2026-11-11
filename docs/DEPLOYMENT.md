# Deployment (not yet performed - requires your accounts)

No public URL exists yet. Do not claim one until you have opened the deployed app and logged in as all three roles.

## 1. Database (managed MySQL: Aiven, Railway, PlanetScale-compatible MySQL, AWS RDS, ...)
1. Create database `ledgerline_db`; apply `database/schema.sql` with the **mysql CLI** (triggers use DELIMITER; some web consoles break it).
2. Verify triggers: `SHOW TRIGGERS;` should list 6 `trg_*_no_*` triggers.
3. Create an app user with only SELECT/INSERT/UPDATE (+DELETE on `account_permissions`) - no DELETE/UPDATE needed on `ledger_entries`.
   (The triggers already block it; least privilege adds a second wall.)

## 2. Backend (Render / Railway / Fly.io / any Docker host) using `backend/Dockerfile`
Environment variables:
`DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET (openssl rand -base64 48), CORS_ALLOWED_ORIGINS=https://<your-frontend>,
SEED_ENABLED=true (first start only), SEED_PASSWORD=<strong>`. Then set `SEED_ENABLED=false`.
Use TLS on the DB connection (`useSSL=true`) if your provider requires it.

## 3. Frontend (Vercel / Netlify)
Root `frontend`, build `npm run build`, output `dist`, env `VITE_API_URL=https://<your-backend>`. SPA rewrites are in `vercel.json` / `public/_redirects`.

## 4. Verify
Login as admin, auditor, holder; make a deposit and a transfer; replay one request; confirm the refused-attempts page lists a failed withdrawal.
