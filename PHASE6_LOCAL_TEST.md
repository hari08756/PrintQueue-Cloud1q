# PrintQueue Phase 6 — Local-only enhancements

## No hosting or public deployment

Run `START_BACKEND.cmd` and `START_DASHBOARD.cmd` from this folder while your existing Docker PostgreSQL database is running. Keep your old project as a backup. Do NOT run `docker compose down -v`.

- Customer portal: http://127.0.0.1:5173/customer.html
- Owner dashboard: http://127.0.0.1:5173/
- Health API through Vite: http://127.0.0.1:5173/api/health

## What changed

- Customer: file-size/type and integer settings checks before uploading, order search/status filter, progress indicator, auto-refresh visibility, printable test receipt. Pay-at-shop only in browser.
- Owner: queue search, status filter, matched order count, success feedback, local customer portal shortcut.
- Existing database schema and Spring Boot routes preserved; no migration required. Android source and original upload workflow retained.

## Manual end-to-end test

1. Ensure backend `/api/health` returns `{"status":"ok"}`.
2. Register a new customer, sign in from `/customer.html`.
3. Upload a sample PDF with pages/copies; verify estimated total.
4. Submit; note token and print a test receipt.
5. Sign in to the owner dashboard on `/` with owner credentials; search for token.
6. Download the PDF and check the file opens.
7. Change order status `PENDING -> PRINTING -> READY`.
8. Refresh customer page and verify READY.
9. Change owner prices; submit a new order and verify latest prices are used, older totals unchanged.
10. Verify customer A cannot see orders of customer B.

## Notes

- Receipt is a testing/order record, not payment confirmation.
- Page counts must be entered manually.
- Basic auth and localhost HTTP remain development-only.
- Browser test portal does not require changes to the Android app.
- A machine with Node.js, Java 21, Maven, and Docker is needed to run the full system.
