# PrintQueue Phase 9 — Secure Document Deletion (LOCAL ONLY)

## Behavior

- **Customer -> Delete permanently:** Authenticated customer may delete only files attached to their own orders. The document bytes are removed from the configured local upload directory and the document disappears from both customer and owner order views. If all documents are removed, the order disappears from both queues. This action cannot be undone.
- **Owner -> Delete (owner only):** Removes the document from the owner dashboard and prevents owner Print/Download. Customer order history remains unchanged. Physical file bytes are retained, so the customer can still see that order and can later delete the document. If all documents are hidden, the order disappears from the owner queue only.
- Print prices and order records are kept for historical purposes. Hiding or deleting a file does **not** automatically refund the customer or recalculate order totals.
- Old documents which already say FILE MISSING are not restored by this update. Both delete actions can clear such records from the relevant view.

## Secure endpoints

`DELETE /api/customer/orders/{orderId}/files/{fileId}` — requires CUSTOMER Basic authentication and verifies order ownership server-side. Returns 204 No Content.

`DELETE /api/owner/orders/{orderId}/files/{fileId}` — requires OWNER Basic authentication. Returns 204 No Content.

Owner file download and print-preview endpoints refuse owner-hidden/customer-deleted documents (404). No public delete URLs are provided.

## Windows test

1. Extract this ZIP into a fresh directory and keep your existing PostgreSQL container running.
2. Stop old backend and dashboard processes, then run `START_BACKEND.cmd` and `START_DASHBOARD.cmd`.
3. Open `http://127.0.0.1:5173/customer.html` and upload two fresh PDFs in one order.
4. Open `http://127.0.0.1:5173/` and sign in as owner.
5. Click **Delete (owner only)** for the first PDF. The owner should no longer see it. Refresh the customer page: customer **still sees** that PDF.
6. Click **Delete permanently** for the second PDF from the customer portal. Refresh the owner queue: it is **gone**. The stored file has been erased.
7. Finally customer-delete the first PDF. The entire empty order disappears from the customer and owner lists.
8. Repeat on the Android app if desired. Customer-side document deletion is available there too.

You said you do not need previous test data preserved, but the scripts do **not** wipe the database or previously uploaded documents. This upgrade adds boolean columns automatically via `spring.jpa.hibernate.ddl-auto=update`.

## Limitations / caution

- These are application-level local deletes. Physical deletion via the OS is permanent from the active storage path, but copies, existing printouts, backups, exported/downloaded files, disk recovery artifacts, and browser caches are beyond the application's control.
- The existing Basic Authentication design is only intended for local testing. Use HTTPS, CSRF protections (if session authentication is introduced), audit trails, quotas and retention rules before an internet release.
- A full Maven/Android/React build was not available in this packaging environment. Validate locally before using with important documents.
