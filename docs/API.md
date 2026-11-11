# LedgerLine API

Base URL `http://localhost:8080`. JSON. Errors: `{ timestamp, status, error: <CODE>, message, fieldErrors? }`.
Auth: `Authorization: Bearer <jwt>` on everything except register/login.

## Auth
| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/register | `{fullName,email,password(8-72)}` -> 201 `{token,tokenType,expiresInSeconds,user}`. Always ACCOUNT_HOLDER. 409 if email exists |
| POST | /api/auth/login | `{email,password}` -> 200 same shape; 401 generic on any failure |
| GET | /api/auth/me | current user |

## Users (ADMIN; GET /{id} also self)
GET /api/users · POST /api/users `{fullName,email,password,role}` · GET /api/users/{id} · PUT /api/users/{id} `{fullName?,role?,enabled?}` ·
DELETE /api/users/{id} (disables; history is preserved). An admin cannot demote/disable themselves.

## Accounts
| Method | Path | Who |
|---|---|---|
| POST | /api/accounts `{accountType: SAVINGS|CURRENT, holderId?}` | holder (own) / admin (holderId required) |
| GET | /api/accounts | holder: accessible; admin/auditor: all |
| GET | /api/accounts/my-accounts | owned + explicitly permitted |
| GET | /api/accounts/{id} · /balance · /statement | anyone with view access (403 otherwise) |
| PUT | /api/accounts/{id}/status `{status}` | ADMIN |
| GET/POST/DELETE | /api/accounts/{id}/permissions[/{permId}] `{userId,permissionType: VIEW|DEPOSIT|WITHDRAW|TRANSFER}` | ADMIN (GET also AUDITOR) |

## Transactions (header `Idempotency-Key` required on POST, max 100 chars)
| Method | Path | Body |
|---|---|---|
| POST | /api/transactions/deposit | `{accountId, amount}` |
| POST | /api/transactions/withdraw | `{accountId, amount}` |
| POST | /api/transactions/transfer | `{sourceAccountId, destinationAccountNumber, amount}` |
| POST | /api/transactions/adjustment | ADMIN: `{accountId, amount, direction: CREDIT|DEBIT, reason}` |
| GET | /api/transactions?page&size | holder: own accounts'; admin/auditor: all |
| GET | /api/transactions/{id} · /account/{accountId} | participant / staff |

Amount: > 0, max 2 decimals, <= 999,999,999,999.99. Success: **201**; replay of same key+payload: **200** + `Idempotent-Replayed: true`.
Error codes: `AMOUNT_NOT_POSITIVE`, `AMOUNT_PRECISION`, `INSUFFICIENT_FUNDS` (422), `FORBIDDEN` (403), `IDEMPOTENCY_KEY_REQUIRED` (400),
`IDEMPOTENCY_KEY_REUSED` (409), `ACCOUNT_NOT_FOUND` (404), `ACCOUNT_NOT_ACTIVE` (422), `SAME_ACCOUNT` (400).

## Ledger (read-only) 
GET /api/ledger (ADMIN/AUDITOR) · /api/ledger/account/{id} · /api/ledger/transaction/{id}. No write endpoints exist.

## Audit (ADMIN/AUDITOR, read-only)
GET /api/audit/logs · /logs/{id} · /attempts (refused attempts) · /stats

## Postman quick test
1. Login as holder -> copy `token`. 2. `GET /api/accounts/my-accounts` -> note `id`.
3. POST deposit with header `Idempotency-Key: abc-123`, body `{"accountId":1,"amount":"50.00"}` -> 201.
4. Send the identical request again -> 200 + `Idempotent-Replayed: true`, balance unchanged.
5. Same key, amount `51.00` -> 409 `IDEMPOTENCY_KEY_REUSED`.
