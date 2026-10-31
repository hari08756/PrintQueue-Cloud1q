# PrintQueue Phase 11 — Shop Branding (local)

Owner dashboard → **Shop branding**. Set shop name and optionally upload PNG/JPG (max 300 KB).

The branding is persisted in PostgreSQL (`shop_branding` table) and retrieved by authenticated customers and owner. It appears on owner/customer headers and printed customer token receipts. Default name: `My Print Shop`.

**Test**: start backend and dashboard; log in as owner; save a shop name/logo; refresh the customer page; log in and print a token receipt.

The receipt uses the current branding at print time; old receipts already printed cannot change. Logo is loaded into the page as a data URL; no separate public uploads directory is used.

Android loads the shop name/logo from the same authenticated endpoint when users sign in or refresh pricing.
