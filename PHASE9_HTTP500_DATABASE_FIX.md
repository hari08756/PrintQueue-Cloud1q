# PrintQueue Phase 9 - HTTP 500 fix (local PostgreSQL)

## Symptom
Owner login shows HTTP 500 and customer `/api/customer/orders` returns 500 after upgrading from Phase 8 to Phase 9.

## Likely root cause
Phase 9 introduced `customer_deleted`, `owner_hidden` in `order_files`, and `legacy_customer_deleted`, `legacy_owner_hidden` in `print_orders`. Previously populated PostgreSQL tables could not accept newly-added NOT NULL columns without defaults. Hibernate `ddl-auto=update` does not necessarily repair this automatically.

## Fix in this package
`backend/src/main/resources/schema.sql` runs at application startup *before* Hibernate's entity checks. It creates missing deletion columns with `DEFAULT false`, fills NULLs in existing rows, and makes them NOT NULL. It is repeatable and does not drop or truncate any tables.

## Run
1. Keep the Docker PostgreSQL container running.
2. Stop previous backend on port 8080.
3. Start `START_BACKEND.cmd` from this extracted package. Watch the backend console for errors.
4. Check `http://localhost:8080/api/health` for `{"status":"ok"}`.
5. Restart `START_DASHBOARD.cmd` and sign in, then check the customer portal.
6. Test delete semantics with **newly uploaded** files.

If HTTP 500 persists, copy the **first exception and first `Caused by`** from the backend CMD window (remove credentials), which will identify the real database/runtime error. The health endpoint may still return OK even when database queries fail.

No database reset is required; avoid `docker compose down -v` unless you explicitly want to destroy test data.
