-- ========================================================
-- IntentMarket (Мой OLX) — Consolidated Supabase Setup
-- PostGIS, pgvector, Schema, RLS, Hybrid Match Engine, Seed Data
-- ========================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 2. TABLES

-- Profiles
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

-- Categories
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

-- Intents (DEMAND & SUPPLY)
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

-- Native Listings (Supply)
CREATE TABLE IF NOT EXISTS public.listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
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

-- External Sources
CREATE TABLE IF NOT EXISTS public.external_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    base_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    terms_policy JSONB DEFAULT '{}'::jsonb,
    rate_limit_per_min INTEGER DEFAULT 30,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- External Listings
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

-- Matches
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

-- Notifications
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

-- Reports (Google Play UGC)
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

-- Analytics Events
CREATE TABLE IF NOT EXISTS public.analytics_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    event_name TEXT NOT NULL,
    properties JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. INDEXES
CREATE INDEX IF NOT EXISTS idx_intents_location ON public.intents USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_listings_location ON public.listings USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_external_listings_location ON public.external_listings USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_intents_attributes ON public.intents USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_listings_attributes ON public.listings USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_external_listings_attributes ON public.external_listings USING GIN (attributes);
CREATE INDEX IF NOT EXISTS idx_listings_cat_status_price ON public.listings (category_id, status, price);
CREATE INDEX IF NOT EXISTS idx_external_cat_status_price ON public.external_listings (category_normalized, availability_status, price);

-- 4. ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.intents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.external_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.external_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.analytics_events ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public profiles are viewable" ON public.profiles FOR SELECT USING (status = 'ACTIVE');
CREATE POLICY "Users can insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Categories are readable by everyone" ON public.categories FOR SELECT USING (is_active = TRUE);
CREATE POLICY "Users manage own intents" ON public.intents FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Active listings are public" ON public.listings FOR SELECT USING (status = 'ACTIVE');
CREATE POLICY "Users insert own listings" ON public.listings FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own listings" ON public.listings FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users delete own listings" ON public.listings FOR DELETE USING (auth.uid() = user_id);

CREATE POLICY "External sources readable" ON public.external_sources FOR SELECT USING (is_active = TRUE);
CREATE POLICY "Fresh external listings are public" ON public.external_listings FOR SELECT USING (availability_status IN ('FRESH', 'STALE'));

CREATE POLICY "Users view own matches" ON public.matches FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.intents WHERE public.intents.id = public.matches.demand_intent_id AND public.intents.user_id = auth.uid())
);
CREATE POLICY "Users view own notifications" ON public.notifications FOR ALL USING (auth.uid() = user_id);
CREATE POLICY "Users submit reports" ON public.reports FOR INSERT WITH CHECK (TRUE);
CREATE POLICY "Allow public events" ON public.analytics_events FOR INSERT WITH CHECK (TRUE);

-- 5. MATCHING ENGINE (STORED PROCEDURE RPC)
DROP TYPE IF EXISTS public.matched_offer_result CASCADE;
CREATE TYPE public.matched_offer_result AS (
    id UUID,
    title TEXT,
    description TEXT,
    price NUMERIC,
    currency TEXT,
    district_name TEXT,
    distance_km NUMERIC,
    is_external BOOLEAN,
    source_name TEXT,
    source_url TEXT,
    phone TEXT,
    images TEXT[],
    attributes JSONB,
    score NUMERIC,
    grade TEXT
);

CREATE OR REPLACE FUNCTION public.match_demand_to_listings(
    p_category_id UUID,
    p_user_lat DOUBLE PRECISION,
    p_user_lon DOUBLE PRECISION,
    p_max_price NUMERIC DEFAULT NULL,
    p_radius_km NUMERIC DEFAULT 25.0,
    p_embedding VECTOR(768) DEFAULT NULL
)
RETURNS SETOF public.matched_offer_result
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_geom GEOGRAPHY;
BEGIN
    v_user_geom := ST_SetSRID(ST_MakePoint(p_user_lon, p_user_lat), 4326)::GEOGRAPHY;

    RETURN QUERY
    WITH candidate_offers AS (
        SELECT
            l.id, l.title, l.description, l.price, l.currency, l.district_name,
            ROUND((ST_Distance(l.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            FALSE AS is_ext, 'На нашей площадке'::TEXT AS src_name, NULL::TEXT AS src_url,
            l.phone, l.images, l.attributes, l.embedding
        FROM public.listings l
        WHERE l.category_id = p_category_id AND l.status = 'ACTIVE'
          AND (p_max_price IS NULL OR l.price <= p_max_price * 1.15)
          AND (l.location IS NULL OR ST_DWithin(l.location, v_user_geom, p_radius_km * 1000.0))

        UNION ALL

        SELECT
            el.id, el.title, el.description, el.price, el.currency, el.district_name,
            ROUND((ST_Distance(el.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            TRUE AS is_ext, es.name AS src_name, el.external_url AS src_url,
            NULL::TEXT AS phone, el.images, el.attributes, el.embedding
        FROM public.external_listings el
        JOIN public.external_sources es ON es.id = el.source_id
        WHERE el.category_normalized = p_category_id
          AND el.availability_status IN ('FRESH', 'STALE')
          AND (p_max_price IS NULL OR el.price <= p_max_price * 1.15)
          AND (el.location IS NULL OR ST_DWithin(el.location, v_user_geom, p_radius_km * 1000.0))
    ),
    scored_offers AS (
        SELECT
            c.*,
            ROUND((
                (CASE 
                    WHEN c.dist_km <= 3.0 THEN 0.40
                    WHEN c.dist_km <= 7.0 THEN 0.30
                    WHEN c.dist_km <= 15.0 THEN 0.20
                    ELSE 0.10
                 END) +
                (CASE 
                    WHEN p_max_price IS NULL OR c.price <= p_max_price THEN 0.40
                    WHEN c.price <= p_max_price * 1.10 THEN 0.25
                    ELSE 0.10
                 END) +
                (CASE 
                    WHEN p_embedding IS NOT NULL AND c.embedding IS NOT NULL 
                    THEN GREATEST(0.0, (1.0 - (c.embedding <=> p_embedding))) * 0.20
                    ELSE 0.15
                 END)
            )::NUMERIC, 2) AS calculated_score
        FROM candidate_offers c
    )
    SELECT
        s.id, s.title, s.description, s.price, s.currency, s.district_name,
        s.dist_km, s.is_ext, s.src_name, s.src_url, s.phone, s.images,
        s.attributes, s.calculated_score,
        CASE
            WHEN s.calculated_score >= 0.75 AND s.dist_km <= 5.0 THEN 'EXCELLENT'
            WHEN s.calculated_score >= 0.55 THEN 'GOOD'
            ELSE 'PARTIAL'
        END AS grade
    FROM scored_offers s
    ORDER BY s.calculated_score DESC, s.dist_km ASC, s.price ASC;
END;
$$;

-- 6. SEED DATA (CATEGORIES & REALISTIC LISTINGS FOR ODESA)
INSERT INTO public.categories (id, name, slug, description, attributes_schema) VALUES
('c0000000-0000-0000-0000-000000000001', 'Генераторы', 'generators', 'Бензиновые, дизельные и инверторные генераторы', '{"power_kw": "number", "fuel_type": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000002', 'Аренда квартир', 'apartment-rent', 'Долгосрочная и посуточная аренда квартир в Одессе', '{"rooms": "string", "floor": "number"}'::jsonb),
('c0000000-0000-0000-0000-000000000003', 'Услуги и ремонт', 'services', 'Электрики, сантехники, монтажники', '{"service_type": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000004', 'Электроника', 'electronics', 'Смартфоны, ноутбуки, техника', '{"brand": "string"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.external_sources (id, name, code, base_url, is_active) VALUES
('e0000000-0000-0000-0000-000000000001', 'OLX', 'OLX', 'https://www.olx.ua', true),
('e0000000-0000-0000-0000-000000000002', 'Prom', 'PROM', 'https://prom.ua', true),
('e0000000-0000-0000-0000-000000000003', 'DOM.ria', 'DOM_RIA', 'https://dom.ria.com', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.external_listings (
    id, source_id, external_id, external_url, title, description, price, currency,
    location, district_name, category_normalized, attributes, availability_status
) VALUES
(
    '11111111-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001',
    'olx-gen-5kw-daewoo',
    'https://www.olx.ua/d/obyavlenie/generator-daewoo-5kw-ID123.html',
    'Генератор Daewoo 5.0 кВт бензин',
    'Новый в коробке, медная обмотка, электростартер, 1 год гарантии. Черёмушки.',
    38500.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7020, 46.4370), 4326)::geography,
    'Черёмушки',
    'c0000000-0000-0000-0000-000000000001',
    '{"power_kw": 5.0, "fuel_type": "petrol"}'::jsonb,
    'FRESH'
),
(
    '11111111-0000-0000-0000-000000000002',
    'e0000000-0000-0000-0000-000000000002',
    'prom-gen-6kw-hyundai',
    'https://prom.ua/p12345-generator-hyundai.html',
    'Дизельный генератор Hyundai 6.0 кВт',
    'Профессиональный дизель-генератор, экономный расход, AVR стабилизатор.',
    45000.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7233, 46.4825), 4326)::geography,
    'Центр',
    'c0000000-0000-0000-0000-000000000001',
    '{"power_kw": 6.0, "fuel_type": "diesel"}'::jsonb,
    'FRESH'
),
(
    '11111111-0000-0000-0000-000000000003',
    'e0000000-0000-0000-0000-000000000003',
    'domria-arcadia-studio',
    'https://dom.ria.com/realty_rent-123.html',
    '1-комнатная квартира-студия в Аркадии',
    'Евроремонт, вся техника, вид на море, закрытая охраняемая территория.',
    11000.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7600, 46.4350), 4326)::geography,
    'Аркадия',
    'c0000000-0000-0000-0000-000000000002',
    '{"rooms": "1", "sea_view": true}'::jsonb,
    'FRESH'
)
ON CONFLICT (id) DO NOTHING;

-- 7. CLUSTERS & DEDUPLICATION (CROSS-PLATFORM MASTER ENTITIES)
CREATE TABLE IF NOT EXISTS public.listing_clusters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_title TEXT NOT NULL,
    canonical_description TEXT,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    district_name TEXT NOT NULL,
    complex_name TEXT,
    location GEOGRAPHY(Point, 4326),
    rooms INTEGER,
    floor INTEGER,
    total_floors INTEGER,
    area_sqm NUMERIC(6, 1),
    min_price NUMERIC(12, 2) NOT NULL,
    max_price NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'UAH',
    canonical_images TEXT[] DEFAULT '{}',
    image_phash TEXT,
    listings_count INTEGER DEFAULT 1,
    sources TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clusters_location ON public.listing_clusters USING GIST(location);

ALTER TABLE public.external_listings
ADD COLUMN IF NOT EXISTS cluster_id UUID REFERENCES public.listing_clusters(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS image_phash TEXT;

ALTER TABLE public.listing_clusters ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'listing_clusters' AND policyname = 'Public read listing clusters'
    ) THEN
        CREATE POLICY "Public read listing clusters" ON public.listing_clusters FOR SELECT USING (true);
    END IF;
END $$;
