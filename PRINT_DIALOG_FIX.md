# Print dialog fix (local)

The previous owner Print action attempted `iframe.contentWindow.print()` for PDFs. Chrome's internal PDF viewer may ignore that call, so no dialog appears.

## Updated behavior
- PDF, including DOC/DOCX converted to PDF by the backend: **Print** opens the file in Chrome's native PDF viewer. Click its printer icon at top-right, or press **Ctrl+P** in that PDF tab. Chrome doesn't reliably permit a webpage to force the native PDF dialog programmatically. This is not a download-and-save workflow.
- JPG/PNG: the preview opens in a new tab and attempts `window.print()` once loaded. If blocked, use the **Print (Ctrl+P)** button in that tab.
- Both print paths use the existing owner-authenticated `/api/owner/orders/{orderId}/files/{fileId}/print-preview` endpoint.
- Original Download button remains unchanged.

## If Print shows nothing
1. Use the updated project's `START_BACKEND.cmd` and `START_DASHBOARD.cmd`; stop old processes first.
2. In Chrome, open `http://127.0.0.1:5173` and sign in as owner.
3. Allow pop-ups/redirects for `127.0.0.1:5173` using the blocked pop-up icon in the address bar.
4. Test a PDF. A *new tab* should display its pages. Use Ctrl+P from **that tab**, not the owner dashboard tab.
5. Test a PNG/JPG. The print dialog should be requested after the image loads.
6. If backend responds with 422 for DOC/DOCX, install LibreOffice and configure `LIBREOFFICE_PATH` as explained in `PHASE8_DIRECT_PRINT.md`.
7. If no new tab and there is an error, inspect Chrome F12 Console / Network for `print-preview` status and response.

Printing may create temporary browser/Windows printer spool files; the app avoids requiring a manual download of the source document.
