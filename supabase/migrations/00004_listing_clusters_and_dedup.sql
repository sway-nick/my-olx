-- ========================================================
-- Migration 00004: Listing Clusters & Deduplication
-- Cross-platform entity resolution for apartments, electronics, etc.
-- ========================================================

-- 1. Table for Canonical Clusters (Master Entities)
CREATE TABLE IF NOT EXISTS public.listing_clusters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_title TEXT NOT NULL,
    canonical_description TEXT,
    category_id UUID REFERENCES public.categories(id) ON DELETE SET NULL,
    district_name TEXT NOT NULL,
    complex_name TEXT, -- ЖК (напр. "ЖК Элегия Парк", "ЖК 51 Жемчужина")
    location GEOGRAPHY(Point, 4326),
    rooms INTEGER,
    floor INTEGER,
    total_floors INTEGER,
    area_sqm NUMERIC(6, 1),
    min_price NUMERIC(12, 2) NOT NULL,
    max_price NUMERIC(12, 2) NOT NULL,
    currency TEXT DEFAULT 'UAH',
    canonical_images TEXT[] DEFAULT '{}',
    image_phash TEXT, -- Перцептивный хеш главного фото (64-bit dHash/pHash)
    listings_count INTEGER DEFAULT 1,
    sources TEXT[] DEFAULT '{}', -- Список источников: ['OLX', 'DOM_RIA', 'LUN']
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for spatial entity resolution (finding candidates within 50m)
CREATE INDEX IF NOT EXISTS idx_clusters_location ON public.listing_clusters USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_clusters_complex_name ON public.listing_clusters USING gin(complex_name gin_trgm_ops);

-- 2. Link external_listings to clusters
ALTER TABLE public.external_listings
ADD COLUMN IF NOT EXISTS cluster_id UUID REFERENCES public.listing_clusters(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS image_phash TEXT;

-- 3. RLS for listing_clusters
ALTER TABLE public.listing_clusters ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read listing clusters"
    ON public.listing_clusters FOR SELECT
    USING (true);
