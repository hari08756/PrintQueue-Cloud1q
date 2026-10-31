# Phase 4.2 — Windows startup and owner login repair

This is an updated **full copy** of the existing Phase 4.1 source (Android, backend and dashboard). Do not overwrite your original folder until you have tested this version.

## Start in this order

1. Keep the original PostgreSQL Docker volume. Open Docker Desktop and run `docker compose up -d` from your **original, previously working** project folder. Do not run `docker compose down -v`.
2. If you have earlier uploads, copy `backend\private-uploads` from the previous project into the new project's `backend` folder. You can alternatively use an absolute `UPLOAD_DIR` path.
3. Stop any existing Spring Boot server using `Ctrl+C`, so port 8080 is free.
4. Double-click **START_BACKEND.cmd** from this folder. It checks Java 21, Maven, PostgreSQL and port 8080; then prompts for your owner's password and runs `mvn spring-boot:run` from the correct backend folder. The password is entered visibly into CMD, so use this only on your own trusted computer and close the window when finished.
5. Check `http://localhost:8080/api/health` for `{"status":"ok"}`.
6. Double-click **START_DASHBOARD.cmd**; open `http://localhost:5173` and log in as `owner` with the same password.
7. To diagnose connectivity, double-click **CHECK_CONNECTION.cmd**.

### Important

`START_BACKEND.cmd` explicitly enables `OWNER_RESET_PASSWORD=true` each time for **local development only**. That updates the existing owner's hashed password in the database, without wiping orders. Before deploying, replace this demo credential workflow with a secure secret-management and account-recovery process, and disable automatic password resets.

The dashboard will report the backend URL when the API isn't reachable, rather than a generic `Failed to fetch`. It still uses HTTP Basic auth over plain localhost HTTP and is **not production-ready**. Online payment is a *preference only*, not a processed charge. Customer-only order endpoints and owner-only download authorization are preserved from Phase 4.

### If the backend does not start

- `Connection refused` / PostgreSQL error: verify Docker Desktop engine and the original database container (`docker compose ps`).
- `Port 8080 already occupied`: terminate the older backend. `netstat -ano | findstr :8080` can identify the process.
- `Java/Maven error`: edit the two installation paths at the top of `START_BACKEND.cmd`.
- If `Application failed to start`, copy the *first* `Caused by:` section from the Maven terminal, not the React screenshot.

**Build verification:** archive creation and Python syntax-level/file integrity checks only; no full Maven or Android Gradle build performed in this container. This package does not claim to cure an unknown backend compilation or migration error; the scripts expose it for diagnosis.
