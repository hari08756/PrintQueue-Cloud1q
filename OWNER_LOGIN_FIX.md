# PrintQueue Owner Login Fix (Phase 4.1)

Your database, uploads and customer accounts are preserved. The previous backend created owner only once; changing OWNER_PASSWORD did not update the stored password. This update adds a one-time, explicit password reset switch.

## Windows CMD: reset existing owner password

1. Stop the existing backend process using Ctrl+C (leave PostgreSQL running).
2. Open a NEW CMD window in this updated project backend folder.
3. Run (replace the example password with your own strong password):

```cmd
set "OWNER_USERNAME=owner"
set "OWNER_PASSWORD=CHOOSE_A_NEW_LONG_RANDOM_PASSWORD"
set "OWNER_RESET_PASSWORD=true"
mvn spring-boot:run
```

4. When startup succeeds, open your React dashboard and sign in using username `owner` and the same password from step 3.
5. Once login works, stop the backend (Ctrl+C). For normal starts, remove the reset switch and start normally:

```cmd
set "OWNER_RESET_PASSWORD=false"
mvn spring-boot:run
```

The username is normalized to lowercase. A reset never changes a CUSTOMER account into an OWNER account. The stored password uses BCrypt. Never commit secrets or publish the local app.

## If login still fails

- Confirm `http://localhost:8080/api/health` responds.
- Ensure only one backend is running on port 8080 and the updated backend was started.
- Check the browser network request to `/api/owner/orders`: HTTP 401 means credentials, HTTP 403 means wrong role, Failed to fetch means network/CORS/backend.
- The old dashboard and new backend should both use `http://localhost:8080/api`.
- Keep your Docker PostgreSQL volume; do not use `docker compose down -v`.
