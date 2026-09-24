-- ========================================================
-- 00001_initial_schema.sql: Core Marketplace Schema
-- PostgreSQL + PostGIS + pgvector on Supabase
-- ========================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 2. Profiles (Users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    phone TEXT,
    display_name TEXT,
    avatar_url TEXT,
    city TEXT DEFAULT 'Одесса',
    location GEOGRAPHY(Point, 4326),
    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BLOCKED', 'SUSPENDED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Categories (Hierarchical with JSONB schema)
CREATE TABLE IF NOT EXISTS public.categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    attributes_schema JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Intents (Primary System Object: DEMAND & SUPPLY)
CREATE TABLE IF NOT EXISTS public.intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    intent_type TEXT NOT NULL CHECK (intent_type IN ('DEMAND', 'SUPPLY')),
    raw_text TEXT NOT NULL,
    category_id UUID REFERENCES public.categories(id) ON DELETE RESTRICT,
    product_type TEXT,
    title TEXT,
    description TEXT,
    price_min NUMERIC(12, 2),
    price_max NUMERIC(12, 2),
    currency TEXT DEFAULT 'UAH',
    condition TEXT DEFAULT 'ANY',
    quantity INTEGER DEFAULT 1,
    location GEOGRAPHY(Point, 4326),
    district_name TEXT,
    radius_km NUMERIC(5, 2) DEFAULT 10.0,
    availability_from TIMESTAMPTZ,
    availability_to TIMESTAMPTZ,
    urgency TEXT DEFAULT 'NORMAL',
    attributes JSONB DEFAULT '{}'::jsonb,
    embedding VECTOR(768),
    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    confidence NUMERIC(4, 3),
    parser_version TEXT DEFAULT 'v1.0',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '30 days')
);

-- 5. Native Listings (User-submitted Supply)
CREATE TABLE IF NOT EXISTS public.listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    intent_id UUID REFERENCES public.intents(id) ON DELETE SET NULL,
    category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE RESTRICT,
    title TEXT NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'UAH',
    condition TEXT DEFAULT 'GOOD',
    quantity INTEGER DEFAULT 1,
    location GEOGRAPHY(Point, 4326),
    district_name TEXT,
    phone TEXT,
    images TEXT[] DEFAULT '{}',
    attributes JSONB DEFAULT '{}'::jsonb,
    embedding VECTOR(768),
    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SOLD', 'PAUSED', 'DELETED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '30 days')
);

-- 6. External Sources Configuration
CREATE TABLE IF NOT EXISTS public.external_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE, -- 'OLX', 'PROM', 'DOM_RIA', 'PARTNER_FEED'
    base_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    terms_policy JSONB DEFAULT '{}'::jsonb,
    rate_limit_per_min INTEGER DEFAULT 30,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. External Listings (Aggregated Supply Layer - Strictly Separated)
CREATE TABLE IF NOT EXISTS public.external_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES public.external_sources(id) ON DELETE RESTRICT,
    external_id TEXT NOT NULL,
    external_url TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    price NUMERIC(12, 2),
    currency TEXT DEFAULT 'UAH',
    location GEOGRAPHY(Point, 4326),
    district_name TEXT,
    images TEXT[] DEFAULT '{}',
    category_raw TEXT,
    category_normalized UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    attributes JSONB DEFAULT '{}'::jsonb,
    embedding VECTOR(768),
    published_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    availability_status TEXT DEFAULT 'FRESH' CHECK (availability_status IN ('FRESH', 'STALE', 'EXPIRED', 'REMOVED', 'UNKNOWN')),
    content_hash TEXT,
    parser_version TEXT DEFAULT 'v1.0',
    retrieved_at TIMESTAMPTZ DEFAULT NOW(),
    last_verified_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_source_external_id UNIQUE (source_id, external_id)
);

-- 8. Matches (Bidirectional Match Engine Results)
CREATE TABLE IF NOT EXISTS public.matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demand_intent_id UUID NOT NULL REFERENCES public.intents(id) ON DELETE CASCADE,
    listing_id UUID REFERENCES public.listings(id) ON DELETE CASCADE,
    external_listing_id UUID REFERENCES public.external_listings(id) ON DELETE CASCADE,
    score NUMERIC(5, 4) NOT NULL,
    grade TEXT NOT NULL CHECK (grade IN ('EXCELLENT', 'GOOD', 'PARTIAL')),
    distance_km NUMERIC(6, 2),
    is_viewed BOOLEAN DEFAULT FALSE,
    is_contacted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT check_single_listing_type CHECK (
        (listing_id IS NOT NULL AND external_listing_id IS NULL) OR
        (listing_id IS NULL AND external_listing_id IS NOT NULL)
    )
);

-- 9. Notifications (Push / In-App alerts)
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('NEW_MATCH', 'PRICE_DROP', 'DEMAND_MATCH', 'SYSTEM')),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Reports (Google Play UGC Compliance: In-app flagging & reporting)
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    listing_id UUID REFERENCES public.listings(id) ON DELETE CASCADE,
    external_listing_id UUID REFERENCES public.external_listings(id) ON DELETE CASCADE,
    reason TEXT NOT NULL CHECK (reason IN ('SPAM', 'SCAM', 'PROHIBITED', 'OFFENSIVE', 'STALE', 'OTHER')),
    details TEXT,
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'RESOLVED', 'DISMISSED')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. Analytics Events (KPI tracking from Day 1)
CREATE TABLE IF NOT EXISTS public.analytics_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    event_name TEXT NOT NULL,
    properties JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================
-- INDEXES FOR PERFORMANCE & FAST HYBRID SEARCH
-- ========================================================

-- Spatial Indexes (PostGIS GIST)
CREATE INDEX IF NOT EXISTS idx_intents_location ON public.intents USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_listings_location ON public.listings USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_external_listings_location ON public.external_listings USING GIST (location);

-- JSONB Indexes (GIN)
CREATE INDEX IF NOT EXISTS idx_intents_attributes ON public.intents USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_listings_attributes ON public.listings USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_external_listings_attributes ON public.external_listings USING GIN (attributes);

-- Filter & Status Indexes
CREATE INDEX IF NOT EXISTS idx_listings_cat_status_price ON public.listings (category_id, status, price);
CREATE INDEX IF NOT EXISTS idx_external_cat_status_price ON public.external_listings (category_normalized, availability_status, price);
CREATE INDEX IF NOT EXISTS idx_intents_user_status ON public.intents (user_id, status);
CREATE INDEX IF NOT EXISTS idx_matches_demand_grade ON public.matches (demand_intent_id, grade);
