# PrintQueue owner Print / Download troubleshooting (local)

## Why old files show 403
The Spring Security filter previously denied Spring Boot error-dispatch requests, masking an underlying file-not-found error as HTTP 403. This release permits only the ERROR dispatcher and `/error` route, while preserving owner-only access to `/api/owner/**`.

The owner orders API now returns `available: true/false` on each file. Missing files are labeled in the dashboard and Print/Download buttons are disabled for them. File routes return 404 with a meaningful message instead of being disguised as 403.

## Test with a fresh uploaded file
1. Start your existing PostgreSQL Docker container.
2. Stop the old backend and Vite dashboard completely before launching this folder.
3. Run `START_BACKEND.cmd` and `START_DASHBOARD.cmd`.
4. Browse to `http://127.0.0.1:5173/customer.html`; log in and upload a NEW PDF.
5. Browse to `http://127.0.0.1:5173/`; sign in as owner. The new file should have `available: true` and working Download/Print buttons.
6. On Print, Chrome opens its PDF viewer; click the viewer print icon or press Ctrl+P. For a Word file, install LibreOffice and set `LIBREOFFICE_PATH` to its `soffice.exe` path.

## Existing uploads
Current start script stores new uploads in `%LOCALAPPDATA%\PrintQueue\uploads`. Legacy files may be located in an older `backend\private-uploads`. Run `RESTORE_OLD_UPLOADS.cmd` only if you wish to retain old uploads. You said old test data does not need preservation; instead upload a new document for this test. No database records are deleted automatically.

## Verification
Check `http://127.0.0.1:5173/api/health`. If a new file returns 403, check Network > Headers > Request URL and Response, and confirm no old backend occupies port 8080. Avoid sharing Authorization headers.
