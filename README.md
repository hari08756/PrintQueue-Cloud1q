# PrintQueue Phase 4 — Multi-file orders and role-isolated APIs

This is an **upgrade of the Phase 3 project**, retaining the original PostgreSQL tables, daily-counter token generator, Docker Compose definition and React dashboard. The backend now requires authentication for every order endpoint; all old public `/api/orders...` endpoints have been removed.

## New functionality

- Customer registration + login (username/password; BCrypt hashes at rest)
- Android multi-select for up to 10 PDF, DOC, DOCX, JPEG or PNG documents per order
- Per-file choice of B&W/color and copies (1–100)
- One server-generated daily sequence token per multi-file order
- Customer access to **only their own** order summaries via `/api/customer/orders`; no customer file-download route exists
- Separate `/api/owner/orders` and `/api/owner/orders/{orderId}/files/{fileId}` routes, enforced with Spring Security ROLE_OWNER
- Private owner dashboard login and authenticated downloads
- Historical Phase 3 orders remain in the database and show in the owner's queue; they are not automatically assigned to any customer

## Before starting: keep your existing data

**Do NOT run `docker compose down -v`**, delete the Docker volume, or drop your `printqueue` database. The existing PostgreSQL database is reused; Hibernate updates the schema and creates `app_users`, `order_files` and the `customer_id` column on `print_orders`.

If moving to a new extracted folder, the Docker Compose project name may change. To avoid connecting to a fresh/empty volume, **continue using the old Docker project** for PostgreSQL. Leave your existing database running, or run `docker compose up -d` from your *original* Phase 3 project directory. Do not start a second Compose stack from the new folder on the same 5432 port.

The existing Phase 3 `backend/private-uploads` directory holds previously uploaded documents. **Copy this folder** from the old backend into the new `backend` directory before launching, or set `UPLOAD_DIR` to its original absolute path. New files are also stored there. Files are outside the web root; do not expose this directory using a static web server.

## Start the server on Windows CMD

From the **new** `backend` directory, using JDK 21 and Maven 3.10:

```cmd
set "JAVA_HOME=C:\Users\hp\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Tools\apache-maven-3.10.0\bin;%PATH%"
set "OWNER_USERNAME=owner"
set "OWNER_PASSWORD=CHOOSE_A_STRONG_UNIQUE_PASSWORD"
mvn spring-boot:run
```

Change the placeholder to your own strong password. An owner account is created on first run. If that account already exists, changing `OWNER_PASSWORD` does **not** reset its password in the database.

The PostgreSQL database still uses `localhost:5432`, database/user `printqueue`, and development password `localdev` from the existing Compose file. The backend listens on `http://localhost:8080`.

## Owner dashboard

From the new `dashboard` directory:

```cmd
npm install
npm run dev
```

Go to `http://localhost:5173`, sign in as `owner` using the password from `OWNER_PASSWORD`. Downloads are fetched with owner authentication rather than open browser links.

## Android

Open the updated `android` directory in Android Studio; sync Gradle and run the emulator. Default API URL is `http://10.0.2.2:8080`, the Android emulator loopback route to the Windows host. For a physical phone, use `http://YOUR_PC_LAN_IP:8080` on the same trusted Wi-Fi, and allow the Windows firewall port only on a trusted private network. Register two different customer accounts to test isolation.

## API contract

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/api/health` | Public | Health probe |
| POST | `/api/auth/register` | Public | Create customer only |
| GET | `/api/customer/orders` | Customer | Own order summaries |
| GET | `/api/customer/orders/{id}` | Customer | Own order only (404 for other users) |
| POST | `/api/customer/orders` | Customer | Multipart: `files` repeated, `modes` repeated, `copies` repeated, `paymentMethod` |
| GET | `/api/owner/orders` | Owner | All orders and file metadata |
| PATCH | `/api/owner/orders/{id}/status` | Owner | Status update |
| GET | `/api/owner/orders/{orderId}/files/{fileId}` | Owner | Authorized file download |

Authentication currently uses **HTTP Basic for local development only**. Customers cannot obtain other customers' orders by guessing token or order ID. A token is an identifier, not an access credential. File downloads are owner-only. For each file, the multipart fields `modes` and `copies` follow the same order as its repeated `files` field.

## Verify authorization (suggested tests)

1. Register `alice` and `bob` in separate Android sessions, each with a password of 8+ characters.
2. As Alice, submit 2 files, e.g. one PDF and one JPG, with different mode/copies.
3. Alice's `My orders` list shows the token and both file summaries; Bob's does not.
4. Owner dashboard shows both, and download buttons work after owner login.
5. Send a GET request to `/api/owner/orders` with Alice's credentials: should be **403**.
6. Request Alice's `/api/customer/orders/{id}` using Bob's credentials: should be **404**.
7. Request any `/api/owner/orders/{id}/files/{fileId}` using Bob's credentials: should be **403**.
8. Anonymous request to previous `/api/orders` route should not return data.

## Important limitations

This is a **local development build**, not yet suitable for real customers or internet deployment. HTTP Basic over plain HTTP is not safe over untrusted networks: use HTTPS + a properly scoped session/token mechanism. There is no payment gateway or charge verification; `ONLINE` is only a preference, and `paymentStatus` remains `UNPAID`. Files are checked by extension and size, not by magic bytes or malware scanning. Add file-signature checking, malware scanning, content sanitization, upload quotas/rate limits, HTTPS, audit logs, retention/deletion policy, stronger login/session controls, and automated integration tests before production. Existing demo database credentials must be changed before deployment.

**Build verification:** ZIP created and statically checked; no full Gradle/Maven integration build was run here. If you see a build error, send the exact console output.


## Phase 4.2: Windows quickstart

Open `PHASE4_2_WINDOWS_SETUP.md` then run `START_BACKEND.cmd` and `START_DASHBOARD.cmd` from the extracted project.
