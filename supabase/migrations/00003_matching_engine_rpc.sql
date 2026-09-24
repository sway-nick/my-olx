-- ========================================================
-- 00003_matching_engine_rpc.sql: Hybrid Matching RPC
-- Combines Hard Filters + PostGIS Distance + Vector Similarity
-- ========================================================

-- Return type for matching results
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

-- RPC: Match a Buyer's Demand against Native and External Listings
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
        -- 1. Native Listings
        SELECT
            l.id,
            l.title,
            l.description,
            l.price,
            l.currency,
            l.district_name,
            ROUND((ST_Distance(l.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            FALSE AS is_ext,
            'На нашей площадке'::TEXT AS src_name,
            NULL::TEXT AS src_url,
            l.phone,
            l.images,
            l.attributes,
            l.embedding
        FROM public.listings l
        WHERE l.category_id = p_category_id
          AND l.status = 'ACTIVE'
          AND (p_max_price IS NULL OR l.price <= p_max_price * 1.15)
          AND (l.location IS NULL OR ST_DWithin(l.location, v_user_geom, p_radius_km * 1000.0))

        UNION ALL

        -- 2. Permitted External Listings
        SELECT
            el.id,
            el.title,
            el.description,
            el.price,
            el.currency,
            el.district_name,
            ROUND((ST_Distance(el.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            TRUE AS is_ext,
            es.name AS src_name,
            el.external_url AS src_url,
            NULL::TEXT AS phone,
            el.images,
            el.attributes,
            el.embedding
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
            -- Location-First & Price Scoring formula
            ROUND((
                -- Geo proximity factor (up to 40%)
                (CASE 
                    WHEN c.dist_km <= 3.0 THEN 0.40
                    WHEN c.dist_km <= 7.0 THEN 0.30
                    WHEN c.dist_km <= 15.0 THEN 0.20
                    ELSE 0.10
                 END) +
                -- Price constraint factor (up to 40%)
                (CASE 
                    WHEN p_max_price IS NULL OR c.price <= p_max_price THEN 0.40
                    WHEN c.price <= p_max_price * 1.10 THEN 0.25
                    ELSE 0.10
                 END) +
                -- Vector semantic factor (up to 20%)
                (CASE 
                    WHEN p_embedding IS NOT NULL AND c.embedding IS NOT NULL 
                    THEN GREATEST(0.0, (1.0 - (c.embedding <=> p_embedding))) * 0.20
                    ELSE 0.15
                 END)
            )::NUMERIC, 2) AS calculated_score
        FROM candidate_offers c
    )
    SELECT
        s.id,
        s.title,
        s.description,
        s.price,
        s.currency,
        s.district_name,
        s.dist_km,
        s.is_ext,
        s.src_name,
        s.src_url,
        s.phone,
        s.images,
        s.attributes,
        s.calculated_score,
        CASE
            WHEN s.calculated_score >= 0.75 AND s.dist_km <= 5.0 THEN 'EXCELLENT'
            WHEN s.calculated_score >= 0.55 THEN 'GOOD'
            ELSE 'PARTIAL'
        END AS grade
    FROM scored_offers s
    ORDER BY 
        (grade = 'EXCELLENT') DESC,
        s.dist_km ASC,
        s.price ASC;
END;
$$;

-- RPC: Reverse Matching (Seller creates Supply -> find existing Buyer Demands)
CREATE OR REPLACE FUNCTION public.count_matching_demands(
    p_category_id UUID,
    p_price NUMERIC,
    p_lat DOUBLE PRECISION,
    p_lon DOUBLE PRECISION
)
RETURNS TABLE (
    matching_demands_count BIGINT,
    sample_demands JSONB
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_geom GEOGRAPHY;
BEGIN
    v_user_geom := ST_SetSRID(ST_MakePoint(p_lon, p_lat), 4326)::GEOGRAPHY;

    RETURN QUERY
    WITH active_demands AS (
        SELECT 
            i.id,
            i.raw_text,
            i.price_max,
            i.district_name,
            ROUND((ST_Distance(i.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km
        FROM public.intents i
        WHERE i.category_id = p_category_id
          AND i.intent_type = 'DEMAND'
          AND i.status = 'ACTIVE'
          AND (i.price_max IS NULL OR p_price <= i.price_max * 1.10)
          AND (i.location IS NULL OR ST_DWithin(i.location, v_user_geom, 25000.0))
    )
    SELECT
        COUNT(*)::BIGINT,
        COALESCE(
            JSONB_AGG(
                JSONB_BUILD_OBJECT(
                    'text', a.raw_text,
                    'price_max', a.price_max,
                    'district', a.district_name,
                    'distance_km', a.dist_km
                )
            ) FILTER (WHERE a.id IS NOT NULL), 
            '[]'::jsonb
        )
    FROM active_demands a;
END;
$$;
