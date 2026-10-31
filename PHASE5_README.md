# PrintQueue Phase 5 – Owner-managed printing prices

## What's new
- Shop owner adjusts black-and-white and color rates in Dashboard (initial defaults ₹2 and ₹10 per page).
- Backend stores rates in PostgreSQL and calculates **fixed order totals at submission**, storing file-by-file breakdown.
- Android app permits manual page count per file and shows an estimate; backend remains authoritative.
- Older orders remain accessible; their totals show as Legacy if not recorded.

## Start
1. Keep the existing PostgreSQL Docker container/database and uploads. **Do not run `docker compose down -v`.**
2. Stop the previous backend, then use `START_BACKEND.cmd` to start this new backend with the same database credentials and owner password. Hibernate `ddl-auto=update` adds columns/table.
3. Run `START_DASHBOARD.cmd` (or `cd dashboard && npm install && npm run dev`). Browse http://127.0.0.1:5173.
4. Set rates as owner; save. Build/install Android app in Android Studio from `android/`. The default emulator backend remains http://10.0.2.2:8080.
5. In Android customer app sign in, refresh prices, add documents, set **actual pages** and copies; preview the estimated price, submit, then track token and billed amount.

## Important limitations
- **Page counts are manual estimates**, not automatically read from PDFs, DOC/DOCX or images. Verify page counts before printing. An A4 photo/image is assumed one page unless customer changes it.
- The customer quote may be stale if rates change between fetching rates and submission; **the server price is authoritative**. A price-lock/quote-confirmation workflow is not yet available.
- Existing Basic auth and the ONLINE payment option are for local tests only; ONLINE does not process payment. Do not expose this system publicly without HTTPS, hardened authentication and payment verification.
- Prices are currency amounts in INR; no GST, duplex, paper size, binding, or special options.
