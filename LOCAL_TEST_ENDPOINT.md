# PrintQueue Phase 5 local API test

This archive contains the complete Phase 5 project and one extra harmless public GET endpoint: `/api/test`.
The existing `/api/health` endpoint remains available. No deployment or customer data is required.

## Windows test
1. Keep PostgreSQL Docker running, and stop the previous backend (Ctrl+C).
2. Run START_BACKEND.cmd in the extracted project root (requires the existing owner configuration).
3. On the same laptop, open http://localhost:8080/api/test
   Expected JSON includes `status: ok` and `message: Local test endpoint working`.
4. If the React dashboard is running, http://127.0.0.1:5173/api/test should also work via Vite proxy.
5. To test on another device on the same Wi-Fi, use the laptop's LAN IPv4 address instead of localhost, provided the backend is listening on all interfaces and the firewall permits TCP 8080.

This is NOT a publicly hosted HTTPS endpoint. It is for local testing only. Do not forward port 8080 to the Internet.
