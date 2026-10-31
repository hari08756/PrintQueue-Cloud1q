# PrintQueue Phase 14 — single VPS HTTPS deployment

This package is configured to run the Phase 14 customer portal, owner dashboard, Spring Boot API, PostgreSQL and private uploaded files on an Ubuntu VPS. **This is an internet-facing TEST deployment, not yet a security-audited production release.** The backend uses Basic Authentication (over HTTPS), and public registration currently lacks comprehensive brute-force, bot, and abuse mitigation. Do not invite real customers with sensitive documents until those are addressed, tested and backed up.

## 1. Get a VPS and domain
- Ubuntu 24.04 LTS VPS, recommend **4 GB RAM or more** (LibreOffice conversions are memory-heavy). Reserve adequate persistent disk space.
- Buy or use an existing domain. Create DNS `A` record: `print.yourdomain.com` -> your VPS PUBLIC IP.
- VPS firewall: allow inbound TCP 22 (SSH; restrict source if possible), 80, 443 and UDP 443. **Do not expose 5432, 8080 or 5173.**
- Configure VPS SSH public-key access; use the provider's documented hardening.

## 2. Install Docker on Ubuntu
Follow https://docs.docker.com/engine/install/ubuntu/ to install Docker Engine and the Docker Compose plugin. Check `docker compose version`. Use an up-to-date Ubuntu host.

## 3. Upload folder from Windows
In PowerShell, from the parent of the extracted folder:

```
scp -r .\PrintQueue_Phase14_Cloud_Deployment youruser@YOUR_VPS_IP:~/
```

Or use WinSCP. Connect by SSH and:

```
cd ~/PrintQueue_Phase14_Cloud_Deployment
cp .env.example .env
nano .env
```

Set SITE_DOMAIN to your real DNS hostname; set different, long passwords for DB_PASSWORD and OWNER_PASSWORD. Store .env securely; do not upload it to GitHub. Existing owner rows can have an older password in a reused DB; these instructions use a *fresh* DB.

## 4. Deploy

```
chmod 600 .env
docker compose --env-file .env -f docker-compose.cloud.yml config -q
docker compose --env-file .env -f docker-compose.cloud.yml up -d --build
docker compose --env-file .env -f docker-compose.cloud.yml ps
docker compose --env-file .env -f docker-compose.cloud.yml logs --tail=100 backend web
```

First build downloads Maven/npm dependencies and LibreOffice, so it may take time. Caddy requests public HTTPS certificates after DNS and ports 80/443 are working.

## 5. Verify
- `https://print.yourdomain.com/api/health` -> `{ "status": "ok" }`
- `https://print.yourdomain.com/customer.html` -> customer register, log in, upload PDF, get token, track order.
- `https://print.yourdomain.com/` -> owner login, download/print, shop branding, inventory/expense tracking.
- Open customer portal on your phone's MOBILE DATA to confirm it works away from home Wi-Fi.
- Verify refreshing the site and rebooting the server doesn't lose orders or uploaded files.

## 6. Android APK
Edit `android/app/src/main/java/com/printqueue/app/MainActivity.kt` and replace `https://print.example.com` with `https://YOUR_ACTUAL_HOSTNAME`. The Android manifest in this package denies unencrypted HTTP. Build and sign the APK in Android Studio. Do not leave example.com in the released APK.

## Backups (critical for customer files)
PostgreSQL and `/srv/uploads` are stored in named Docker volumes. They survive reboots and container recreation, but **not** `docker compose down -v`, accidental volume deletion, or VPS loss. Schedule encrypted, **offsite** DB backups and file-volume backups; perform a test restore before collecting real customer documents.

Simple database export, on the VPS (for testing only; copy offsite securely):

```
docker compose --env-file .env -f docker-compose.cloud.yml exec -T db pg_dump -U printqueue -d printqueue -Fc > printqueue-db-backup.dump
```

For file backups, archive the `customer_uploads` named volume to protected backup storage; test restoration. Keep all backups access-restricted and comply with retention/deletion obligations.

## Production readiness checklist before real customers
- Rate-limit login/registration/upload endpoints; add abuse protections, per-account quotas and monitoring.
- Migrate away from browser-stored Basic Authentication to short-lived secure sessions/tokens; introduce user-friendly password recovery.
- Validate upload MIME/signatures, scan files for malware, and isolate LibreOffice conversion with resource/time limits.
- Define document retention, expiry and deletion rules. Check customer deletion also removes backups within the stated retention window.
- Secure VPS with firewall, SSH keys, automatic OS security patches; monitor CPU/disk/uptime and failed sign-ins.
- Set HTTP upload size and request concurrency limits consistent with owner settings.
- Verify privacy policy, consent and legal/payment rules; PAY_AT_SHOP is safer while gateway verification is not integrated.
- Review CORS, CSRF/session policy and other headers, especially before adding authentication cookies.
- Build end-to-end tests for direct print, deleting documents and inventory data.

**Known cloud limitation:** A browser on the owner's computer shows its own printer dialog (Ctrl+P); a VPS cannot silently print to a shop-local printer without a separate local trusted print agent.
