-- ========================================================
-- Migration 00005: Universal Taxonomy & "Хорошая цена" Engine
-- 1. Full universal categories tree for EVERYTHING
-- 2. Hot Deals RPC calculating >= 25% discount against category/unit median
-- ========================================================

-- 1. UNIVERSAL CATEGORIES TREE (EXPANDED TO ALL NICHES)
INSERT INTO public.categories (id, name, slug, description, attributes_schema) VALUES
-- Электроника
('c0000000-0000-0000-0000-000000000004', 'Электроника', 'electronics', 'Смартфоны, ноутбуки, телевизоры, гаджеты', '{"brand": "string", "model": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000010', 'Смартфоны и телефоны', 'smartphones', 'iPhone, Samsung, Xiaomi и аксессуары', '{"brand": "string", "memory_gb": "number"}'::jsonb),
('c0000000-0000-0000-0000-000000000011', 'Ноутбуки и компьютеры', 'laptops-pc', 'Ноутбуки, ПК, мониторы, комплектующие', '{"ram_gb": "number", "cpu": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000012', 'Бытовая техника', 'appliances', 'Холодильники, стиральные машины, кондиционеры', '{"type": "string"}'::jsonb),

-- Недвижимость
('c0000000-0000-0000-0000-000000000002', 'Аренда квартир', 'apartment-rent', 'Долгосрочная и посуточная аренда квартир в Одессе', '{"rooms": "string", "area_sqm": "number", "floor": "number"}'::jsonb),
('c0000000-0000-0000-0000-000000000020', 'Продажа квартир', 'apartment-sale', 'Вторичка и новостройки Одессы', '{"rooms": "string", "area_sqm": "number"}'::jsonb),
('c0000000-0000-0000-0000-000000000021', 'Дома и участки', 'houses-plots', 'Дома, дачи, таунхаусы в Одессе и пригороде', '{"area_sqm": "number"}'::jsonb),

-- Силовая техника & Автономность
('c0000000-0000-0000-0000-000000000001', 'Генераторы и энергопитание', 'generators', 'Бензиновые, дизельные, инверторные генераторы, EcoFlow', '{"power_kw": "number", "fuel_type": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000030', 'Зарядные станции и аккумуляторы', 'power-stations', 'EcoFlow, Bluetti, инверторы, LiFePO4 аккумуляторы', '{"capacity_wh": "number"}'::jsonb),

-- Услуги и мастера
('c0000000-0000-0000-0000-000000000003', 'Услуги и ремонт', 'services', 'Электрики, сантехники, ремонт под ключ, мастера', '{"service_type": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000040', 'Грузоперевозки и грузчики', 'cargo-moving', 'Переезды, газели, грузчики по Одессе', '{"vehicle_type": "string"}'::jsonb),

-- Транспорт и автотовары
('c0000000-0000-0000-0000-000000000050', 'Транспорт и авто', 'transport-auto', 'Легковые автомобили, мотоциклы, запчасти', '{"year": "number", "brand": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000051', 'Шины, диски и автозапчасти', 'auto-parts', 'Резина, диски, аккумуляторы, запчасти', '{"radius": "string"}'::jsonb),

-- Дом, мебель и интерьер
('c0000000-0000-0000-0000-000000000060', 'Дом, мебель и сад', 'home-furniture', 'Диваны, шкафы, столы, стройматериалы, растения', '{"material": "string"}'::jsonb),

-- Детский мир
('c0000000-0000-0000-0000-000000000070', 'Детский мир', 'kids-baby', 'Коляски, автокресла, детская одежда, игрушки', '{"age_group": "string"}'::jsonb),

-- Спорт и хобби
('c0000000-0000-0000-0000-000000000080', 'Спорт, отдых и хобби', 'sports-hobby', 'Велосипеды, самокаты, тренажеры, рыбалка', '{"type": "string"}'::jsonb),

-- Одежда и обувь
('c0000000-0000-0000-0000-000000000090', 'Мода и стиль', 'fashion-clothing', 'Одежда, обувь, аксессуары', '{"size": "string", "brand": "string"}'::jsonb),

-- Животные
('c0000000-0000-0000-0000-000000000100', 'Животные и зоотовары', 'animals-pets', 'Собаки, кошки, корма, аксессуары', '{"breed": "string"}'::jsonb),

-- Работа в Одессе
('c0000000-0000-0000-0000-000000000110', 'Работа и вакансии', 'jobs-odessa', 'Работа в Одессе (водители, продавцы, повара, удаленка)', '{"salary_type": "string"}'::jsonb)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    attributes_schema = EXCLUDED.attributes_schema;


-- 2. "ХОРОШАЯ ЦЕНА" ENGINE (RPC: GET_HOT_DEALS)
-- Automatically calculates median price (or relative unit price: per sqm, per kw)
-- and flags listings that are >= 25% cheaper than the median.

DROP FUNCTION IF EXISTS public.get_hot_deals(UUID, DOUBLE PRECISION, DOUBLE PRECISION, NUMERIC);

CREATE OR REPLACE FUNCTION public.get_hot_deals(
    p_category_id UUID DEFAULT NULL,
    p_user_lat DOUBLE PRECISION DEFAULT 46.4825,
    p_user_lon DOUBLE PRECISION DEFAULT 30.7233,
    p_min_discount_pct NUMERIC DEFAULT 25.0
)
RETURNS TABLE (
    id UUID,
    title TEXT,
    description TEXT,
    price NUMERIC,
    currency TEXT,
    district_name TEXT,
    distance_km NUMERIC,
    source_name TEXT,
    source_url TEXT,
    images TEXT[],
    category_id UUID,
    category_name TEXT,
    median_price NUMERIC,
    unit_metric TEXT,
    discount_pct NUMERIC,
    is_hot_deal BOOLEAN
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_geom GEOGRAPHY;
BEGIN
    v_user_geom := ST_SetSRID(ST_MakePoint(p_user_lon, p_user_lat), 4326)::GEOGRAPHY;

    RETURN QUERY
    WITH all_pool AS (
        -- Combine listings and external_listings
        SELECT
            l.id, l.title, l.description, l.price, l.currency, l.district_name,
            ROUND((ST_Distance(l.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            'На нашей площадке'::TEXT AS src_name, NULL::TEXT AS src_url,
            l.images, l.category_id,
            -- Normalized unit price calculation:
            -- 1. If apartment: price per sqm
            -- 2. If generator: price per kW
            -- 3. Default: raw price
            CASE
                WHEN (l.attributes->>'area_sqm') IS NOT NULL AND (l.attributes->>'area_sqm')::NUMERIC > 0 
                    THEN (l.price / (l.attributes->>'area_sqm')::NUMERIC)
                WHEN (l.attributes->>'power_kw') IS NOT NULL AND (l.attributes->>'power_kw')::NUMERIC > 0 
                    THEN (l.price / (l.attributes->>'power_kw')::NUMERIC)
                ELSE l.price
            END AS normalized_unit_price,
            CASE
                WHEN (l.attributes->>'area_sqm') IS NOT NULL THEN 'грн/м²'
                WHEN (l.attributes->>'power_kw') IS NOT NULL THEN 'грн/кВт'
                ELSE 'грн/шт'
            END AS unit_metric_name
        FROM public.listings l
        WHERE l.status = 'ACTIVE' AND l.price > 0
          AND (p_category_id IS NULL OR l.category_id = p_category_id)

        UNION ALL

        SELECT
            el.id, el.title, el.description, el.price, el.currency, el.district_name,
            ROUND((ST_Distance(el.location, v_user_geom) / 1000.0)::NUMERIC, 1) AS dist_km,
            s.name AS src_name, el.external_url AS src_url,
            el.images, el.category_normalized AS category_id,
            CASE
                WHEN (el.attributes->>'area_sqm') IS NOT NULL AND (el.attributes->>'area_sqm')::NUMERIC > 0 
                    THEN (el.price / (el.attributes->>'area_sqm')::NUMERIC)
                WHEN (el.attributes->>'power_kw') IS NOT NULL AND (el.attributes->>'power_kw')::NUMERIC > 0 
                    THEN (el.price / (el.attributes->>'power_kw')::NUMERIC)
                ELSE el.price
            END AS normalized_unit_price,
            CASE
                WHEN (el.attributes->>'area_sqm') IS NOT NULL THEN 'грн/м²'
                WHEN (el.attributes->>'power_kw') IS NOT NULL THEN 'грн/кВт'
                ELSE 'грн/шт'
            END AS unit_metric_name
        FROM public.external_listings el
        JOIN public.external_sources s ON s.id = el.source_id
        WHERE el.availability_status = 'FRESH' AND el.price > 0
          AND (p_category_id IS NULL OR el.category_normalized = p_category_id)
    ),
    category_medians AS (
        -- Calculate median price per category and unit metric
        SELECT
            a.category_id,
            a.unit_metric_name,
            PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY a.normalized_unit_price)::NUMERIC(12, 2) AS cat_median
        FROM all_pool a
        GROUP BY a.category_id, a.unit_metric_name
    )
    SELECT
        p.id,
        p.title,
        p.description,
        p.price,
        p.currency,
        p.district_name,
        p.dist_km,
        p.src_name,
        p.src_url,
        p.images,
        p.category_id,
        c.name AS category_name,
        m.cat_median AS median_price,
        p.unit_metric_name AS unit_metric,
        ROUND(((m.cat_median - p.normalized_unit_price) / m.cat_median * 100.0)::NUMERIC, 0) AS discount_pct,
        (ROUND(((m.cat_median - p.normalized_unit_price) / m.cat_median * 100.0)::NUMERIC, 0) >= p_min_discount_pct) AS is_hot_deal
    FROM all_pool p
    JOIN category_medians m ON m.category_id = p.category_id AND m.unit_metric_name = p.unit_metric_name
    JOIN public.categories c ON c.id = p.category_id
    WHERE m.cat_median > 0
      AND ((m.cat_median - p.normalized_unit_price) / m.cat_median * 100.0) >= p_min_discount_pct
    ORDER BY discount_pct DESC, p.dist_km ASC, p.price ASC;
END;
$$;
