# PROMPTS.md - AI interaction log

> Rule from the challenge: do not fabricate. Fill the commit links yourself after committing, and add **at least five** real,
> dated entries from your own sessions. Only entry 1 below was written for you because it reflects something that really happened
> in the session that produced this code. Entries 2-5 must come from your own work.

## Entry 1 - 2026-11-06 - Initial build with Claude
**Original prompt:** the full "MASTER PROMPT: BUILD LEDGERLINE" (see the challenge brief).
**AI output summary:** Day 1 (auth, JWT, schema) then the whole stack: entities, services, controllers, 20 tests, React app, docs.
**Specific mistakes identified:**
1. Day 1 set `spring.jpa.hibernate.ddl-auto: validate`. With MySQL `ENUM` columns, Hibernate 6 expects a native enum type and can fail validation. Noticed while writing the Day 2 entities.
2. Day 1 `GlobalExceptionHandler` mapped *any* `DataIntegrityViolationException` to "email already exists", which would mislabel unrelated constraint errors (e.g. duplicate idempotency key).
3. Day 1 used `UserDetails` without the user id, so controllers could not get the caller's id from the token context without another lookup.
**Corrections implemented:** `ddl-auto: none`; email conflict now a typed `BusinessException`, integrity violations get a generic 409; added `AppUserPrincipal` carrying id and role.
**Commit link:** `<paste your commit URL here after you commit>`
**Note:** nothing in this entry was compiled or run by the AI. Test results are yours to add.

## Entry 2 - <date> - <tool>
**Original prompt:** 
**AI output summary:** 
**Specific mistakes identified:** 
**Corrections implemented:** 
**Commit link:** 

## Entry 3 ... Entry 5 (same template)
