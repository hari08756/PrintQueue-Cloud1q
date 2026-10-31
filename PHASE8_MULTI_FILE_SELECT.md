# Phase 8 — Multi-file selection (local only)

## What changed
Customer Portal (`/customer.html`) now has **Select multiple documents at once**. In the Windows file picker select several documents with Ctrl+click or Shift+click and press Open. The selected documents are added as individual cards, each with its own B&W/color, pages and copies controls. PDFs have their pages detected automatically through `/api/customer/inspect`. Customers can add files in batches up to **10 files**, max **15 MB per file**, and submit one order with one token. The original one-at-a-time selector remains available.

## Local run
1. Unzip into a NEW directory; do not replace your working old project.
2. Keep existing PostgreSQL/Docker running. Stop old backend/frontend processes on 8080/5173.
3. Run `START_BACKEND.cmd` and then `START_DASHBOARD.cmd`.
4. Open `http://127.0.0.1:5173/customer.html` and sign in. Select multiple PDFs and images at once, adjust separate print settings, place order.
5. Open `http://127.0.0.1:5173/` as owner and verify all chosen files appear under the same token.

No schema changes or data resets required. Offline, localhost-only.
