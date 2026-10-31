# PrintQueue Phase 5 — Customer Web Portal (Local Test)

1. Start Docker PostgreSQL and the Spring Boot backend (`START_BACKEND.cmd`).
2. Start Vite dashboard (`START_DASHBOARD.cmd`).
3. Owner: http://127.0.0.1:5173/
4. Customer: http://127.0.0.1:5173/customer.html
5. Create a NEW customer account, sign in, select document(s), pages, copies, B&W/color, then submit.
6. View token/status and compare the owner queue at the owner URL.
7. API health: http://127.0.0.1:5173/api/health

Do NOT open /api/customer/orders directly expecting a page; it is a protected JSON endpoint.

Local development only: Basic Authentication over HTTP; don't expose port 8080 or Vite to the public internet. Rates are calculated on the server. Pages are manual. The ONLINE payment selection does NOT verify actual payment.

If `npm run dev` fails due to missing modules, run `npm install` in the dashboard folder.
