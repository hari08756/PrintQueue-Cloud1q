# Direct printing from the owner dashboard (local-only)

The owner can open **Print** beside each file. Browser printing needs user confirmation: a print dialog is opened; the server cannot silently control your physical printer. PDF/JPG/PNG display securely in a temporary browser tab without triggering a download. Browser PDF print depends on Chrome/Edge internal PDF viewer; use its toolbar Print icon if the page Print button is blocked.

For Word DOC/DOCX, install LibreOffice on the Windows laptop and ensure its `soffice.exe` executable is on PATH, or set `LIBREOFFICE_PATH` to its full path before launching `START_BACKEND.cmd`, e.g. `set "LIBREOFFICE_PATH=C:\Program Files\LibreOffice\program\soffice.exe"`. Word documents are converted to temporary PDFs, and temporary files are removed after response generation. If LibreOffice is missing, Word print preview returns HTTP 422 with guidance.

1. Start existing PostgreSQL Docker container.
2. Stop old backend/dashboard and start the new `START_BACKEND.cmd` and `START_DASHBOARD.cmd`.
3. Open http://127.0.0.1:5173 and log in as owner.
4. Upload a test PDF and image from http://127.0.0.1:5173/customer.html.
5. Click **Print** next to a file in the owner queue. Allow popups for the dashboard if needed. Print from the new browser tab.

Security: the `/api/owner/orders/{orderId}/files/{fileId}/print-preview` endpoint is owner-only, returns `inline` content and `no-store`, and requires Basic authentication. Downloads remain available separately. The preview is loaded into memory in the browser and no file is explicitly downloaded, but browser temporary caches/spool files are outside application control. The owner must select the right printer, paper settings, B&W/color, and copies manually; `mode` and `copies` are shown as a reminder, not automatically forced.
