# PrintQueue Phase 14: Render Free + Supabase Free (testing only)

See ChatGPT step-by-step reply for the full setup and security notes.

1. Create a free Supabase project. Under Storage make a **private** bucket `print-documents`.
2. Under Supabase Connect select **Session Pooler** IPv4 port 5432. Note host, user, password. Avoid direct IPv6 host; Render doesn't support outbound IPv6.
3. Create private GitHub repository containing this extracted project. Do not add .env/secrets.
4. Render > New > Web Service > Git repo > Language Docker > Dockerfile Path `Dockerfile.render` > Instance Free.
5. Add Render environment keys from `RENDER_ENV_EXAMPLE.txt` (real values, never upload secrets in Git).
6. Deploy. `https://YOUR-RENDER-NAME.onrender.com/api/health` -> {"status":"ok"}.
7. Owner website `https://YOUR-RENDER-NAME.onrender.com/`; customer `https://YOUR-RENDER-NAME.onrender.com/customer.html`.
8. Submit a tiny PDF. Confirm object in Supabase Storage. Test print/download and deletion.

## Storage semantics
DocumentStore uses local disk if Supabase environment variables are absent. If both SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are configured, it writes/reads/deletes **private** Supabase objects using server-side key only. Customer deletion removes storage bytes. Owner deletion only hides file in owner dashboard. Do not expose the secret key to browser/Android.

## Caveats
- On free Render, 512MB RAM may be insufficient for large PDFs and Word conversion. This image omits LibreOffice; DOC/DOCX print-preview will return 422. Use PDF/JPG/PNG first.
- Supabase Free Storage 1GB, database 500MB. No production durability guarantees; free project can pause.
- The app uses Basic auth and lacks production abuse controls. Use only synthetic test files/accounts.
- This project has not been full Docker built or tested against live services in this environment.
