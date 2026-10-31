# Phase 9 backend startup fix

Fixed the `Unterminated dollar quote` error from Spring's schema.sql parser.

## Run
Stop the previous backend. Keep Docker PostgreSQL running. Extract this ZIP and run `START_BACKEND.cmd`, then `START_DASHBOARD.cmd`.

Check `http://localhost:8080/api/health` before logging in.

The migration consists of ordinary PostgreSQL ALTER TABLE statements and does not delete existing data.

**If you previously posted your real owner password in a log, change it before sharing the app or logs.**
