# THREATS.md - LedgerLine threat model

| # | Threat | Mitigation in this codebase | Residual risk / where verified |
|---|---|---|---|
| **Financial integrity** |||
| F1 | Money created/lost in a transfer | One DB transaction: debit + credit + tx row + ledger rows + audit row. Debit amount == credit amount (single `amount`). | Test 3, 15. Deposits/withdrawals/adjustments are single-sided by design (see Accepted risks) |
| F2 | Partial transfer after a crash | `@Transactional` on `TransactionProcessor.process`; any RuntimeException rolls back everything | Test 14 (injected failure on 2nd ledger write) |
| F3 | Float rounding | `BigDecimal` in Java, `DECIMAL(19,4)` in MySQL, amounts sent as strings, max 2 decimals enforced | - |
| F4 | Balance driven negative | Check on locked row + DB `CHECK (balance >= 0)` as last line of defence | Test 4, 9 |
| **Double spending / concurrency** |||
| C1 | Two withdrawals read the same balance | `SELECT ... FOR UPDATE` via `refresh(PESSIMISTIC_WRITE)`; validate and update in the same DB tx | Test 9 |
| C2 | Stale entity from the persistence context defeats the lock | Locked reload overwrites state (`refresh`), not a plain query | Test 9; review `TransactionProcessor` |
| C3 | Deadlock from opposite transfers (A->B and B->A) | Locks always taken in ascending account-id order; bounded retry (3x) on lock failures | Test 10 |
| C4 | Lock waits pile up | InnoDB lock-wait timeout -> retry -> clean error. Hikari pool bounded | Accepted: tune under real load |
| **Duplicate requests** |||
| D1 | Client retry after timeout double-pays | Mandatory `Idempotency-Key`; unique `(initiated_by, key)`; replay returns original tx | Test 7 |
| D2 | Two identical requests at the same instant | Unique index arbitrates; loser catches the violation and returns the winner's result | `simultaneousDuplicateRequestsProcessOnce` |
| D3 | Key reused for a different payment | SHA-256 request fingerprint compare -> 409 | Test 8 |
| D4 | Key guessing across users | Key scope is per user (`initiated_by`) | Keys are UUIDs from `crypto.randomUUID()` |
| **Authorization** |||
| A1 | Moving another user's money | Identity from JWT only; owner-or-permission check per account in service | Test 11 |
| A2 | Privilege escalation at registration | Public register always yields ACCOUNT_HOLDER; role in body ignored | `registrationCannotChooseRole` |
| A3 | Auditor/admin moving money | Service refuses non-holder roles; adjustments ADMIN-only with reason | Test 12 |
| A4 | Stolen/old token of disabled user | Filter reloads the user from DB each request; disabled => 401 | Accepted: token valid until expiry for enabled users |
| A5 | Account enumeration at login | One generic 401 message | - |
| A6 | Frontend-only checks bypassed | All rules server-side; React guards are UX only | - |
| **Ledger tampering** |||
| L1 | App bug/attacker updates or deletes ledger rows | No update/delete repository methods; `@Immutable`; MySQL triggers reject UPDATE/DELETE | Test 13 |
| L2 | Tamper by someone with DB admin rights | Not prevented (they can drop triggers) | Accepted. Next step: hash-chain entries / off-site log |
| L3 | Silent loss of failure evidence | Refused attempts persisted in separate `REQUIRES_NEW` tx; table also append-only | Test 4, 11, 14 |

## Accepted risks
1. Single-sided ledger lines for deposits, withdrawals and adjustments (no clearing/"cash" account). `sum(credits - debits)` over all accounts equals net external inflow, not zero.
2. A privileged DB operator can drop triggers or edit data; there is no cryptographic chaining.
3. JWT is stored in `localStorage` (XSS would expose it); no refresh token / revocation list beyond the per-request enabled check.
4. No rate limiting or brute-force lockout on login.
5. A failed request does not consume its Idempotency-Key, so a retry re-executes (desired: a later retry may legitimately succeed).
6. Tests and the lock-ordering argument cover InnoDB REPEATABLE READ locking reads; behaviour on other engines/isolation levels is unverified.
7. Unverified code: none of this has been executed yet (see README).
