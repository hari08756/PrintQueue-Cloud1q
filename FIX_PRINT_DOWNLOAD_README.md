# PrintQueue Phase 8 — Print + Download repair (local Windows)

## Root cause and fix
The database keeps orders even when you extract a new project directory. Earlier releases saved uploaded document bytes under their own `backend/private-uploads` folder. When you start a different extracted project, the owner still sees old order records but both Print and Download can return **404** because the actual stored file is missing in the new folder.

This build sets `UPLOAD_DIR=%LOCALAPPDATA%\PrintQueue\uploads` in `START_BACKEND.cmd`, so **new uploads stay at a stable location across subsequent ZIP versions**. It also gives the owner useful HTTP error text rather than a silent failure.

## To restore documents from older orders
1. Find the earlier project directory where those customer orders were uploaded. Its `backend/private-uploads` folder should contain UUID-named files (not the original filenames).
2. Run `RESTORE_OLD_UPLOADS.cmd` from the new project's root. Paste or drag the old `backend/private-uploads` folder path when prompted. If you used several versions to upload documents, run it for each folder.
3. Start **only one** backend with `START_BACKEND.cmd` and one dashboard with `START_DASHBOARD.cmd`.
4. Upload **a fresh new PDF** using `http://127.0.0.1:5173/customer.html` and test Download + Print in `http://127.0.0.1:5173/`.
5. For PDFs, Print opens Chrome's native PDF viewer. Press Ctrl+P there. For DOC/DOCX, install LibreOffice and configure `LIBREOFFICE_PATH` as described in `PHASE8_DIRECT_PRINT.md`.

If you do not need previous test orders, just upload a new document to test. Previous DB entries with missing files will still not work until those files are restored.

## If it still fails
In Chrome open F12 -> Network and click Download. Note the HTTP code for `/api/owner/orders/{id}/files/{fileId}`: **404** means the stored file is missing; **401/403** means credentials; **500** means a server error. Also check `http://127.0.0.1:5173/api/health`.

All features remain local. No data is automatically deleted.
