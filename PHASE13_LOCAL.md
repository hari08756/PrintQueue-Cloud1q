# PrintQueue Phase 13 — Local Sales & Payments

## New features
- Owner can mark any order PAID or UNPAID (confirmation required). This is **manual accounting**, NOT a payment gateway verification.
- Owner can filter queue by payment status, printing status, or local calendar date.
- Owner can choose a date and generate/print a daily sales report in Asia/Kolkata time.
- Daily report shows paid, unpaid, cancelled, order counts, collected, outstanding and booked totals, plus itemized orders.
- CANCELLED orders are excluded from revenue totals even if marked PAID. Legacy orders with missing totals are counted but treated as ₹0; review them separately.
- Customer portal, Android app, owner branding, pricing, printing, file deletion and token controls remain present.

## Local setup
1. Extract ZIP; keep your existing PostgreSQL Docker container running.
2. Stop the previous backend/dashboard processes so ports 8080 and 5173 are free.
3. Run START_BACKEND.cmd and START_DASHBOARD.cmd.
4. Open http://127.0.0.1:5173 and sign in as owner.
5. Mark a test order as PAID; generate today's report; filter for PAID; use Print daily report.
6. Customer portal: http://127.0.0.1:5173/customer.html

## Notes
No database reset or migration is needed; Phase 13 reuses print_orders.payment_status and total_amount.
Dates are interpreted as Asia/Kolkata. Reports use the order creation date, not date paid: marking an older order paid updates its original order-date report.
Collected figures mean **orders marked PAID**, not externally verified money receipts.
This is LOCAL TESTING ONLY. Browser printing may require allowing popups.
