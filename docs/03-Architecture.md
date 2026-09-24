# Architecture — IntentMarket («Мой OLX»)

## 1. High-Level Architecture Diagram

```
┌────────────────────────────────────────────────────────┐
│                   Client Layer                         │
│                                                        │
│  [Android App (Native Kotlin + Compose)]              │
│    ├── Clean Architecture: Presentation, Domain, Data  │
│    ├── Material 3 (Light & OLED Dark themes)           │
│    ├── Google AdMob SDK (Native & Rewarded)            │
│    └── Supabase Kotlin SDK                             │
│                                                        │
│  [Web PWA / GitHub Pages Preview]                      │
│    └── Vanilla JS + Supabase JS Client                 │
└──────────────────────────┬─────────────────────────────┘
                           │ (HTTPS / WebSockets)
                           ▼
┌────────────────────────────────────────────────────────┐
│               Supabase Cloud Platform                  │
│                                                        │
│  [PostgreSQL 15+ Core]                                 │
│    ├── PostGIS (Geography Point 4326, ST_DWithin)     │
│    ├── pgvector (Embeddings 768, Cosine Similarity)    │
│    ├── Row Level Security (RLS) Policies               │
│    └── Hybrid Matching Engine (RPC SQL Stored Proc)    │
│                                                        │
│  [Edge Functions / Deno]                               │
│    └── parse-intent (Bilingual AI LLM normalization)   │
│                                                        │
│  [Storage & Auth]                                      │
│    ├── Supabase Storage (Photo attachments)            │
│    └── Supabase Auth (Email / Phone OTP)               │
└──────────────────────────▲─────────────────────────────┘
                           │ (Service Role Secret)
┌──────────────────────────┴─────────────────────────────┐
│          Background Ingestion & Adapters               │
│  [Python Ingestion Workers] (Cron: 1-3 days)           │
│    ├── OLX Adapter, Prom Adapter, DOM.ria Adapter      │
│    ├── Validation, Deduplication, Provenance           │
│    └── Freshness Status Checker                        │
└────────────────────────────────────────────────────────┘
```

---

## 2. Data Model Design (PostgreSQL)

### Core Object: `Intent` vs `Listing`
* **`intents`:** Спрос (`DEMAND`) или предложение (`SUPPLY`), выраженное на естественном языке.
* **`listings`:** Структурированное проверенное объявление пользователя.
* **`external_listings`:** Агрегированные внешние предложения, строго изолированные от внутренней базы пользователей.

### Атрибуты: Баланс между B-Tree и JSONB
* **Нормализованные SQL-колонки:** `price`, `currency`, `category_id`, `status`, `location (geography)` — используются для быстрого отсечения (Hard Filters) с B-Tree и GIST индексами.
* **`attributes JSONB`:** Специфичные характеристики (`power_kw`, `rooms`, `fuel_type`, `floor`) индексируются через `GIN`.

---

## 3. 4-Stage Hybrid Matching Engine

1. **Этап 1: Hard Constraints (SQL)**  
   `category_id = p_category_id AND status = 'ACTIVE' AND price <= max_price * 1.15`
2. **Этап 2: PostGIS Proximity (Геолокация)**  
   `ST_DWithin(location, user_geom, radius_km * 1000)`  
   Расчет физического расстояния в км (`ST_Distance / 1000.0`).
3. **Этап 3: Vector Similarity (pgvector)**  
   Косинусное расстояние: `1.0 - (embedding <=> p_embedding)`.
4. **Этап 4: Scoring & Grading**  
   Итоговый балл: $Score = W_{geo} \cdot Geo + W_{price} \cdot Price + W_{vec} \cdot Vector$.  
   Присвоение бейджей: `EXCELLENT`, `GOOD`, `PARTIAL`.
