# PrintQueue Phase 14 — Local Inventory and Expense Management

This is the complete project based on your working Phase 13 ZIP.

Start the PostgreSQL container, then run `START_BACKEND.cmd` and `START_DASHBOARD.cmd` from this Phase 14 folder. Owner dashboard: http://127.0.0.1:5173/; Customer portal: http://127.0.0.1:5173/customer.html

Under the Owner Dashboard you'll find **Inventory & expenses**. Add a paper stock, set a low stock warning threshold and a sheet cost; record used sheets or restock. Log ink/toner, paper purchases, electricity, maintenance and other expenses. View a dated profit estimate.

The new database entities create `paper_stocks` and `shop_expenses` tables using Hibernate `ddl-auto=update`. Existing tables and old customer files are not deleted or reset. Everything remains local, without cloud hosting or public HTTPS.

**Important limitations**: Profit is only a rough estimate: paid, non-cancelled order totals dated by order creation in India, minus entered dated expenses, minus estimated pages×copies×cost per sheet from the first paper stock type. Paper sizes, double-sided printing and actual ink use are not modeled. Expenses for purchased paper may be double-counted alongside estimated paper usage. Stock counts change only through owner Restock / Use sheets controls. Do not use for audited accounting.

The archive structure and React JSX syntax were checked here; a complete Maven/Android runtime test was not possible. See PHASE14_INVENTORY_EXPENSES.md for a manual test checklist.
