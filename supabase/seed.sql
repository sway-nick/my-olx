-- ========================================================
-- seed.sql: Realistic Seed Data for Odesa
-- Categories, External Sources, Sample Listings & Demands
-- ========================================================

-- 1. Categories
INSERT INTO public.categories (id, name, slug, description, attributes_schema) VALUES
('c0000000-0000-0000-0000-000000000001', 'Генераторы', 'generators', 'Бензиновые, дизельные и инверторные генераторы', '{"power_kw": "number", "fuel_type": "string", "phase": "number"}'::jsonb),
('c0000000-0000-0000-0000-000000000002', 'Аренда квартир', 'apartment-rent', 'Долгосрочная и посуточная аренда квартир в Одессе', '{"rooms": "string", "floor": "number", "sea_view": "boolean"}'::jsonb),
('c0000000-0000-0000-0000-000000000003', 'Услуги и ремонт', 'services', 'Электрики, сантехники, монтажники', '{"service_type": "string", "urgency": "string"}'::jsonb),
('c0000000-0000-0000-0000-000000000004', 'Электроника', 'electronics', 'Смартфоны, ноутбуки, повербанки', '{"brand": "string", "model": "string"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

-- 2. External Sources
INSERT INTO public.external_sources (id, name, code, base_url, is_active) VALUES
('e0000000-0000-0000-0000-000000000001', 'OLX', 'OLX', 'https://www.olx.ua', true),
('e0000000-0000-0000-0000-000000000002', 'Prom', 'PROM', 'https://prom.ua', true),
('e0000000-0000-0000-0000-000000000003', 'DOM.ria', 'DOM_RIA', 'https://dom.ria.com', true)
ON CONFLICT (id) DO NOTHING;

-- 3. External Listings (Aggregated layer)
-- Point format: ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography
-- Tairova: lon 30.7120, lat 46.3980
-- Cheremushki: lon 30.7020, lat 46.4370
-- Arcadia: lon 30.7600, lat 46.4350

INSERT INTO public.external_listings (
    id, source_id, external_id, external_url, title, description, price, currency,
    location, district_name, category_normalized, attributes, availability_status
) VALUES
(
    '11111111-0000-0000-0000-000000000001',
    'e0000000-0000-0000-0000-000000000001', -- OLX
    'olx-gen-5kw-daewoo',
    'https://www.olx.ua/d/obyavlenie/generator-daewoo-5kw-ID123.html',
    'Генератор Daewoo 5.0 кВт бензин',
    'Новый в коробке, медная обмотка, электростартер, 1 год гарантии. Черёмушки.',
    38500.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7020, 46.4370), 4326)::geography, -- Черёмушки
    'Черёмушки',
    'c0000000-0000-0000-0000-000000000001',
    '{"power_kw": 5.0, "fuel_type": "petrol"}'::jsonb,
    'FRESH'
),
(
    '11111111-0000-0000-0000-000000000002',
    'e0000000-0000-0000-0000-000000000002', -- Prom
    'prom-gen-6kw-hyundai',
    'https://prom.ua/p12345-generator-hyundai.html',
    'Дизельный генератор Hyundai 6.0 кВт',
    'Профессиональный дизель-генератор, экономный расход, AVR стабилизатор.',
    45000.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7233, 46.4825), 4326)::geography, -- Центр
    'Центр',
    'c0000000-0000-0000-0000-000000000001',
    '{"power_kw": 6.0, "fuel_type": "diesel"}'::jsonb,
    'FRESH'
),
(
    '11111111-0000-0000-0000-000000000003',
    'e0000000-0000-0000-0000-000000000003', -- DOM.ria
    'domria-arcadia-studio',
    'https://dom.ria.com/realty_rent-123.html',
    '1-комнатная квартира-студия в Аркадии',
    'Евроремонт, вся техника, вид на море, закрытая охраняемая территория.',
    11000.00,
    'UAH',
    ST_SetSRID(ST_MakePoint(30.7600, 46.4350), 4326)::geography, -- Аркадия
    'Аркадия',
    'c0000000-0000-0000-0000-000000000002',
    '{"rooms": "1", "sea_view": true}'::jsonb,
    'FRESH'
)
ON CONFLICT (id) DO NOTHING;
