# PrintQueue Phase 8 — local-only upgrade

## New functions
- PDF page counts are detected from PDF content in Spring Boot using Apache PDFBox, not trusted from the browser.
- Customer browser previews PDF page count before submitting through authenticated `POST /api/customer/inspect`.
- PDF count is read-only in the customer page, and server rechecks it while placing the order.
- DOC/DOCX/JPG/PNG continue to use customer-entered page counts.
- Owner can export currently visible orders as a CSV file.

## Windows start
1. Extract this ZIP into a **new** folder.
2. Start Docker Desktop; run `docker compose up -d` in project root if database isn't running.
3. Stop old backend and dashboard processes, then run `START_BACKEND.cmd` followed by `START_DASHBOARD.cmd`.
4. Customer: http://127.0.0.1:5173/customer.html
5. Owner: http://127.0.0.1:5173/
6. Backend health: http://127.0.0.1:8080/api/health

## Smoke test
- Register a customer, select a 3-page PDF and confirm `3` appears automatically.
- Submit the PDF as 2 B&W copies; at ₹2/page, total should be ₹12.
- Check the token in owner queue; download the PDF and change status to READY.
- Check customer order history and CSV export.
- Try a malformed or password-protected PDF; backend should reject it.
- Word/image page counts still require manual entry.

## Cautions
- This does not auto-print customer uploads or verify payments.
- All services remain local; no cloud endpoints are configured.
- Database reset is **not automatic**, even though preserving old testing data is not required.
- Do not expose Basic auth / HTTP services to the public internet.
