-- Phase 9 database migration (compatible with Spring Boot SQL script parsing).
-- Each statement is separate; no PostgreSQL DO blocks.
-- IF EXISTS means a first-time empty database can still start.
-- Existing records receive FALSE when the new columns are added.
ALTER TABLE IF EXISTS public.order_files ADD COLUMN IF NOT EXISTS customer_deleted boolean NOT NULL DEFAULT false;
ALTER TABLE IF EXISTS public.order_files ADD COLUMN IF NOT EXISTS owner_hidden boolean NOT NULL DEFAULT false;
ALTER TABLE IF EXISTS public.print_orders ADD COLUMN IF NOT EXISTS legacy_customer_deleted boolean NOT NULL DEFAULT false;
ALTER TABLE IF EXISTS public.print_orders ADD COLUMN IF NOT EXISTS legacy_owner_hidden boolean NOT NULL DEFAULT false;

-- Phase 10 owner-controlled upload limits and collision-free token series.
ALTER TABLE IF EXISTS public.daily_counter ADD COLUMN IF NOT EXISTS generation integer NOT NULL DEFAULT 1;
