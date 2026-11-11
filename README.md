# LedgerLine - Money Movement with a Ledger You Can Prove

Secure financial transaction management: accounts, deposits, withdrawals, transfers, an append-only double-entry ledger,
idempotent requests, pessimistic row locking, refused-attempt tracking and role-based access (ADMIN / ACCOUNT_HOLDER / AUDITOR).

## Verification status (read this first)
This code was written in a sandbox with **no Maven, no network and no MySQL**. So:

| Item | Status |
|---|---|
| Java source: syntax parses (javac front-end) | Checked; all remaining compiler errors were missing-dependency errors only |
| Java source: full compile, Spring wiring, JPA mappings | **Not verified. Run `mvn test` first.** |
| 30 automated tests (JUnit 5 / Spring Boot Test / Mockito, real MySQL) | **Written, never executed.** Expect to fix some first-run issues |
| React app: builds and runs | **Not verified** (only a bracket-balance check) |
| Deployment / public URL | **Not done** (no network access). Steps in `docs/DEPLOYMENT.md` |

Nothing below should be presented as "tested" until you have run it. Fix compile errors first, then tests.

## Architecture
`React (Vite, Axios)  ->  Controller -> Service -> Repository -> MySQL`

* Controllers are thin; DTOs only (entities never leave the service layer; `DtoMapper`).
* Identity always comes from the verified JWT (`AppUserPrincipal`), never from a request body.
* Money path: `TransactionService` (orchestrator, no tx) -> `TransactionProcessor` (one `@Transactional` unit) and
  `AttemptRecorder` (`REQUIRES_NEW`).
* Schema is owned by `database/schema.sql` (Hibernate `ddl-auto: none`).

### Financial integrity, in plain language
1. **Money conservation / double entry:** a transfer writes one DEBIT and one CREDIT ledger entry, same transaction, same amount.
   Deposits/withdrawals are single-sided by nature (money crosses the system boundary); admin adjustments likewise, with a mandatory reason.
2. **Atomicity:** transaction row, both balance updates, ledger entries and audit row commit or roll back together.
3. **Concurrency:** `SELECT ... FOR UPDATE` row locks, taken in ascending account-id order (no lock-order deadlocks). The balance
   check happens on the locked, freshly re-read row inside the same DB transaction. A retry handles rare InnoDB deadlock/lock-timeout errors.
   Hibernate would otherwise reuse a stale already-loaded balance, so the code uses `EntityManager.refresh(..., PESSIMISTIC_WRITE)`.
4. **Idempotency:** `Idempotency-Key` header is mandatory. Unique index `(initiated_by, idempotency_key)`. Duplicate -> original result
   (HTTP 200 + `Idempotent-Replayed: true`). Same key + different payload (SHA-256 fingerprint) -> 409. Simultaneous duplicates: the unique
   index lets one win; the others catch the violation and return the winner's result.
5. **Append-only ledger:** no update/delete method exists in `LedgerRepository`; entities are `@Immutable`; MySQL triggers reject UPDATE/DELETE
   on `ledger_entries`, `transaction_attempts` and `audit_logs`.
6. **Failure recording:** a refused request is written to `transaction_attempts` in a separate `REQUIRES_NEW` transaction *after* the money
   transaction rolled back. It cannot commit partial money movement. Failed requests do not consume their idempotency key.
7. **Authorization:** role check + per-account permission check (owner, or explicit `account_permissions` row) in the service layer.
   Auditors/admins cannot deposit, withdraw or transfer.

## Requirements
Java 23, Maven 3.9+, Node 20+, MySQL 8.4 LTS (or the version you have; run `mysql --version` - the brief says "26.7", which I could not
confirm exists; the schema uses only MySQL 8.0.16+ features), Docker optional.

## Setup (Windows PowerShell)
```powershell
# 1. Database (creates ledgerline_db AND ledgerline_test, with triggers)
mysql -u root -p < database\schema.sql
mysql -u root -p < database\schema-test.sql
#    or: docker compose up -d   (applies both automatically)

# 2. Backend
cd backend
$env:DB_USER="root"; $env:DB_PASSWORD="your_password"
$env:JWT_SECRET=[Convert]::ToBase64String((1..48 | % {Get-Random -Max 256}))
$env:SEED_ENABLED="true"; $env:SEED_PASSWORD="ChangeMe#2026"
mvn test                 # run the 30 tests against ledgerline_test (needs DB_USER/DB_PASSWORD)
mvn spring-boot:run      # http://localhost:8080

# 3. Frontend
cd ..\frontend
npm install
npm run dev              # http://localhost:5173
```

## Seed accounts (created when SEED_ENABLED=true; password = your SEED_PASSWORD)
| Role | Email |
|---|---|
| ADMIN | admin@ledgerline.local |
| AUDITOR | auditor@ledgerline.local |
| ACCOUNT_HOLDER | holder@ledgerline.local (has a savings account with 10,000.00) |
| ACCOUNT_HOLDER | holder2@ledgerline.local (no account yet) |

Disable seeding (`SEED_ENABLED=false`) or change the password in any shared deployment.

## API
See `docs/API.md`. Money endpoints require `Authorization: Bearer <jwt>` and `Idempotency-Key: <uuid>`.

## Tests
`backend/src/test/java/com/ledgerline`:
| # | Required scenario | Test |
|---|---|---|
| 1 | deposit | `TransactionServiceTest.successfulDeposit` |
| 2 | withdrawal | `successfulWithdrawal` |
| 3 | transfer (+ double entry) | `successfulTransfer` |
| 4 | insufficient balance (+ recorded) | `insufficientBalanceIsRefusedAndRecorded` |
| 5 | negative amount | `negativeAmountRejected` |
| 6 | zero amount | `zeroAmountRejected` |
| 7 | same key twice | `sameIdempotencyKeyTwiceProcessesOnce` |
| 8 | same key, different payload | `sameKeyDifferentPayloadIsRejected` |
| 9 | two concurrent withdrawals | `ConcurrencyTest.twoConcurrentWithdrawalsCannotOverdraw` |
| 10 | concurrent transfers | `concurrentOppositeTransfersConserveMoneyAndDoNotDeadlock` |
| 11 | unauthorized access | `strangerCannotWithdrawFromSomeoneElsesAccount`, `ApiSecurityTest.holderCannotReadAnotherHoldersAccount` |
| 12 | auditor cannot modify | `ApiSecurityTest.auditorCannotModifyFinancialData` |
| 13 | ledger immutable | `LedgerImmutabilityTest` |
| 14 | rollback after injected failure | `TransactionRollbackTest` (Mockito spy throws on the 2nd ledger write) |
| 15 | conservation over many transfers | `moneyIsConservedAcrossManyTransfers` |

Concurrency tests use real threads released together by a latch, each on its own pooled MySQL connection. Tests never delete rows
(they cannot; triggers forbid it); each test creates its own users/accounts, so the test database simply grows.

## Deployment
See `docs/DEPLOYMENT.md`. Secrets only via environment variables; nothing production-specific is in the repo.
