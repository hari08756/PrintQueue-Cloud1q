# Phase 14 — Inventory & Expenses (local only)

Owner dashboard at http://127.0.0.1:5173/ contains an **Inventory & expenses** section. Login as owner.

- Add paper stock with sheet count, reorder threshold, and cost per sheet.
- Restock or record used sheets manually; low stock displays an alert.
- Record dated expenses: ink/toner, paper purchase, electricity, maintenance, other. Delete mistakes.
- Choose the India-local report date and review paid revenue, dated expenses, estimated paper cost and estimated profit.

**Estimate limitations:** Paid revenue is the sum of non-cancelled PAID orders created that day, not confirmed bank receipts. Estimated paper consumption assumes one physical A4-equivalent sheet per printed page per copy, ignores duplex, paper sizes and color-specific costs, and uses the *first paper stock row* as the reference cost. Recorded purchases may double-count against estimated paper use if both treated as expenses. Profit is a planning estimate, not an accounting profit/loss statement. Stock is NOT automatically decremented on order status changes (owner explicitly uses the Use sheets button).

Endpoints require OWNER Basic Authentication, under `/api/owner/operations/`:
`GET/POST /stocks`, `PUT /stocks/{id}`, `POST /stocks/{id}/adjust`, `POST /expenses`, `DELETE /expenses/{id}`, `GET /summary?date=YYYY-MM-DD`.

Uses new tables `paper_stocks` and `shop_expenses` with `spring.jpa.hibernate.ddl-auto=update`; existing order tables are not changed.

## Test
1. Start Docker PostgreSQL, `START_BACKEND.cmd`, `START_DASHBOARD.cmd`.
2. Login as owner, add 100 A4 sheets with low stock threshold 20 and ₹0.50 unit cost.
3. Click Use sheets, enter 85; expect remaining 15 and a low stock warning.
4. Record an INK_TONER expense of ₹100 dated today.
5. Mark a test order as PAID and refresh the report. Check the estimate and amount.
6. Verify the customer portal continues to work.

Entire app remains localhost-only. Avoid running `docker compose down -v` unless you intend to erase the database.
