# Phase 12 — Shop contact details and custom receipt message (local only)

## Features
- Owner Dashboard > Shop branding: optional phone, shop address, receipt message, alongside name and logo.
- Customer Portal header shows shop address and phone when provided.
- Printed token receipts include shop address, phone, and message. User-supplied text is HTML escaped in receipt generation.
- These details persist in PostgreSQL as columns on the existing `shop_branding` table; Hibernate `ddl-auto=update` adds columns automatically. No database reset needed.
- Existing Android app keeps its shop name and logo support; these extra contact fields are displayed in the web customer portal and token receipt, not yet in the Android UI.

## Local run
1. Keep PostgreSQL Docker running. Stop previous backend and dashboard.
2. Run `START_BACKEND.cmd`, then `START_DASHBOARD.cmd`.
3. Open http://127.0.0.1:5173/ and sign in as owner.
4. Save phone, address, and receipt message in Shop branding.
5. Visit http://127.0.0.1:5173/customer.html and print a token receipt.

If the backend fails at startup, inspect the first `Caused by:` message in backend console. Do not share passwords or full credential logs.
