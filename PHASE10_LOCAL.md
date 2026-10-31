# Phase 10 — Local owner controls

Start PostgreSQL and run START_BACKEND.cmd, then START_DASHBOARD.cmd. Open http://127.0.0.1:5173 for owner and /customer.html for customer.

- Owner sets max files per order (1–30) and max file size (1–50 MB), stored in PostgreSQL and checked by the backend. Browser reads the same limits.
- Owner **Remove entire document set** hides all files in that order from the owner view only. Customer still sees them and the bytes remain stored.
- Customer **Delete entire document set** permanently removes documents from disk and both views.
- Owner **Reset next token to 0001** creates a new non-colliding reset series for the current day: e.g. `20261030-R2-0001`. Existing records and their tokens remain unchanged. Repeat reset increments the R generation.
- Do not run two backend instances against the same upload data while testing. Existing local database is reused.
- File upload HTTP cap is 50 MB per file and request cap 1500 MB; enforce actual owner limits in the API.
- There is no automatic printer configuration or public network deployment.

Test: change limits to 2 files / 3 MB; confirm 3-file requests or >3MB files are rejected. Upload two files, owner-hide one set (customer still sees it); upload another set, customer-delete all (both views lose it). Reset token and place one order; verify token ends R2-0001 (or later generation).
