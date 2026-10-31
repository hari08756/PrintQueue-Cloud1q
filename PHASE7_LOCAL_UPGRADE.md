# PrintQueue Phase 7 — Local workflow upgrade

This update keeps the same local Spring Boot API, PostgreSQL schema, Android app, customer browser portal, and owner dashboard.

## New features
- Customer can preview a selected PDF/JPG/PNG **before submitting**. Preview uses a temporary local browser object URL; it is not uploaded by opening the preview. DOC/DOCX files remain uploadable but are not previewed.
- Selected upload rows display their filename and size.
- Owner can check several orders at once and update their status in a batch; the same protected `PATCH /api/owner/orders/{id}/status` is used for each selected order. No database schema changes.
- Bulk updates run sequentially; if an update fails midway, earlier changes will have been saved. An error displays how many were updated.

## Run on Windows
1. Keep the original PostgreSQL Docker database/volume. **Never run `docker compose down -v`.**
2. Back up your currently working project before changing to this folder.
3. Stop the earlier backend and dashboard CMD windows.
4. Keep previously uploaded files available: the ZIP intentionally omits private customer files. Copy your old `backend/private-uploads` directory into the new project or configure `UPLOAD_DIR` to its existing absolute path.
5. Start `START_BACKEND.cmd` (Java 21 and Maven) then `START_DASHBOARD.cmd` (Node/npm).
6. Owner dashboard: http://127.0.0.1:5173/
7. Customer portal: http://127.0.0.1:5173/customer.html
8. API health: http://127.0.0.1:5173/api/health

## Local test checklist
1. Customer: sign in, choose a local PDF or JPG, select **Preview file**, close preview, and submit.
2. Owner: sign in, verify the new token and download the file.
3. Create two test orders. Select both checkboxes in the owner queue, choose READY, and click Apply to selected.
4. Refresh customer order history to check new status.
5. Search/filter owner orders and confirm checkboxes select visible orders only.

## Verification and limitations
JSX files were checked with Babel's parser. A complete Vite build couldn't be executed in the build container because the uploaded node_modules contains Windows-native packages; run `npm install` and `npm run build` on Windows for full verification. Backend APIs, payment behaviour and database schema have not been altered. These remain localhost-only and aren't ready for public internet use.
