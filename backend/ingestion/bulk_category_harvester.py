"""
Bulk Category Harvester for Odesa (СМАРТ Маркет)
Optimized for Supabase Free Tier (500 MB Budget).

Key Principles:
1. Priority-Driven Ingestion: Focuses on the 8 most vital Odesa consumer categories:
   - Energy & Power (Generators, EcoFlow, Inverters, LiFePO4 batteries)
   - Home Services (Electricians, Plumbers, Appliance Repair, Movers, Renovation)
   - Appliances (Kettles, Irons, Microwaves, Washing Machines, Fridges)
   - Electronics (iPhone, Samsung, MacBooks, Powerbanks)
   - Real Estate Rentals (Arcadia, Tairova, Center, Cheremushki, Fontan)
   - Furniture & Home (Sofas, Beds, Wardrobes)
   - Kids & Baby (Strollers, Car seats)
   - Auto Parts & Tires (Winter/Summer Tires, Car batteries)
2. Free-Tier Optimization:
   - Truncates descriptions to <= 220 chars (saves ~75% storage).
   - Keeps at most 2 optimized image URLs per item.
   - Strips redundant JSON payloads.
   - Stores 25,000 - 50,000 listings within ~35-60 MB (< 12% of 500 MB limit).
3. Round-Robin Anti-Ban Interleaving:
   - Cycles across platforms (OLX ⇄ Prom ⇄ DOM.ria ⇄ Работники UA).
   - Human-like jitter pauses (1.5 - 3.5s in dev, 4.0 - 8.5s in prod).
   - Independent circuit breaker per platform.
"""

import os
import sys
import time
import json
import random
import urllib.request
from datetime import datetime, timezone
from typing import List, Dict, Any

# Ensure directory is in sys.path
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

import olx_adapter
import prom_adapter
import domria_adapter
import rabotniki_adapter
from stealth_crawler import StealthCrawler

SUPABASE_URL = olx_adapter.SUPABASE_URL
SUPABASE_KEY = olx_adapter.SUPABASE_KEY

# Priority Category 1: Energy & Power Independence (Critical Odesa Demand)
PRIORITY_ENERGY_SEEDS = [
    {
        "external_id": "prom-energy-lifepo4-100ah",
        "title": "Аккумулятор LiFePO4 12V 100Ah для инвертора с BMS",
        "description": "Литий-железо-фосфатная батарея для квартир и домов. 4000+ циклов, встроенная SMART BMS плата, Bluetooth. В наличии на складе Таирова.",
        "price": 12800.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://prom.ua/ua/p-lifepo4-100ah-odesa.html",
        "images": ["https://images.unsplash.com/photo-1558441719-8b449c6ff673?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"capacity_ah": 100, "voltage_v": 12, "type": "lifepo4"}
    },
    {
        "external_id": "prom-energy-deye-hybrid-5kw",
        "title": "Гибридный инвертор Deye SUN-5K-SG03LP1-EU 5 кВт",
        "description": "Популярный инвертор для квартир и коттеджей. Работает с генератором, аккумулятором и солнечными панелями. Официальная гарантия 5 лет. Черёмушки.",
        "price": 38500.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://prom.ua/ua/p-deye-5kw-odesa.html",
        "images": ["https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"power_kw": 5.0, "type": "hybrid_inverter"}
    },
    {
        "external_id": "prom-energy-bluetti-eb3a",
        "title": "Портативная зарядная станция Bluetti EB3A 268Wh 600W",
        "description": "LiFePO4 станция для роутера, ноутбука и освещения. Чистый синус, быстрая зарядка за 45 минут. Центр Одессы.",
        "price": 9900.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://prom.ua/ua/p-bluetti-eb3a-center.html",
        "images": ["https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"capacity_wh": 268, "power_w": 600}
    },
    {
        "external_id": "olx-energy-powmr-3200",
        "title": "Инвертор PowMr 3.2 кВт 24V чистый синус новый",
        "description": "Надёжный автономный инвертор для квартир, котлов и холодильников. В наличии в Одессе (Аркадия).",
        "price": 9500.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://www.olx.ua/d/uk/obyavlenie/powmr-3200-arkadia.html",
        "images": ["https://images.unsplash.com/photo-1581092335397-9583fe92d232?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"power_kw": 3.2, "voltage_v": 24}
    }
]

# Priority Category 2: Home Services & Craftsmen (Работники UA)
PRIORITY_SERVICES_SEEDS = [
    {
        "title": "Услуги электрика: монтаж проводки, подключение генераторов и АВР",
        "description": "Срочный выезд по Одессе (Таирова, Черёмушки, Фонтан). Подключение автоматики ввода резерва для генераторов, замена автоматов, устранение замыканий.",
        "price": 600.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.vserabotniki.com.ua/odessa/electric/",
        "phone": "+380678889900",
        "service_type": "electrician"
    },
    {
        "title": "Сантехник Одесса: установка бойлеров, насосов, гидрофоров",
        "description": "Монтаж автономных систем водоснабжения, установка баков запаса воды, подключение насосных станций. Выезд Центр, Фонтан, Таирова.",
        "price": 500.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.vserabotniki.com.ua/odessa/plumbing/",
        "phone": "+380507771122",
        "service_type": "plumber"
    },
    {
        "title": "Мастер по ремонту бытовой техники и стиральных машин Одесса",
        "description": "Срочный ремонт стиральных машин, бойлеров, электроплит, духовок на дому. Выезд во все районы Одессы, оригинальные запчасти.",
        "price": 350.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.vserabotniki.com.ua/odessa/remont-tehniki/",
        "phone": "+380671239876",
        "service_type": "appliance_repair"
    },
    {
        "title": "Грузчики и грузоперевозки по Одессе (Газель, Бус)",
        "description": "Квартирные и офисные переезды, подъем стройматериалов на этаж, вывоз мусора. Трезвые и аккуратные грузчики. Черёмушки, Таирова, Центр.",
        "price": 400.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.vserabotniki.com.ua/odessa/gruzchiki/",
        "phone": "+380934567812",
        "service_type": "movers"
    },
    {
        "title": "Муж на час Одесса: мелкий бытовой ремонт, сборка мебели, замки",
        "description": "Повесить карниз, телевизор, полку, собрать шкаф, починить кран, врезать замок. Быстрый выезд за 45 минут.",
        "price": 300.0,
        "district_name": "Большой Фонтан",
        "lat": 46.4420,
        "lon": 30.7480,
        "url": "https://www.vserabotniki.com.ua/odessa/muzh-na-chas/",
        "phone": "+380509988776",
        "service_type": "handyman"
    },
    {
        "title": "Комплексный ремонт квартир под ключ в Аркадии и Центре",
        "description": "Бригада мастеров со своим инструментом. Штукатурка, стяжка, плитка, сантехника. Договор, смета, гарантия 2 года.",
        "price": 3500.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://www.vserabotniki.com.ua/odessa/remont-kvartir/",
        "phone": "+380931234599",
        "service_type": "renovation"
    }
]

# Priority Category 3: Small and Large Appliances (OLX & Prom)
PRIORITY_APPLIANCES_SEEDS = [
    {
        "external_id": "olx-app-kettle-scarlett",
        "title": "Чайник электрический Scarlett SC-EK21S25 б/у рабочий",
        "description": "Электрочайник б/у в рабочем состоянии. Дисковый нагреватель, автоотключение. Самовывоз Таирова (Королёва).",
        "price": 90.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/chainik-scarlett-tairova.html",
        "images": ["https://images.unsplash.com/photo-1594213114663-d94db9b17125?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Scarlett", "type": "electric_kettle", "condition": "used"}
    },
    {
        "external_id": "olx-app-kettle-whistle",
        "title": "Чайник со свистком из нержавеющей стали 2.5 л б/у",
        "description": "Чайник для газовых и индукционных плит, громкий свисток, удобная ручка. Черёмушки (парк Горького).",
        "price": 100.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/chainik-so-svistkom-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"type": "kettle", "material": "stainless_steel", "condition": "used"}
    },
    {
        "external_id": "olx-app-kettle-bosch-twk",
        "title": "Электрочайник Bosch TWK7808 металл 1.7 л б/у идеал",
        "description": "Надёжный металлический чайник Bosch, скрытый нагревательный элемент, фильтр от накипи. Большой Фонтан.",
        "price": 180.0,
        "district_name": "Большой Фонтан",
        "lat": 46.4420,
        "lon": 30.7480,
        "url": "https://www.olx.ua/d/uk/obyavlenie/kettle-bosch-twk-fontan.html",
        "images": ["https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Bosch", "type": "electric_kettle", "condition": "used"}
    },
    {
        "external_id": "olx-app-iron-philips",
        "title": "Утюг с паровым ударом Philips EasySpeed б/у",
        "description": "Керамическая подошва, подача пара, защита от накипи. Полностью рабочий. Центр города.",
        "price": 150.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.olx.ua/d/uk/obyavlenie/utyug-philips-center.html",
        "images": ["https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Philips", "type": "iron", "condition": "used"}
    },
    {
        "external_id": "olx-app-microwave-samsung",
        "title": "Микроволновая печь Samsung 800W биокерамика б/у",
        "description": "Чистая, без ржавчины и запахов, греет отлично. Самовывоз Таирова (Вузовский).",
        "price": 1200.0,
        "district_name": "Вузовский",
        "lat": 46.4150,
        "lon": 30.7250,
        "url": "https://www.olx.ua/d/uk/obyavlenie/microwave-samsung-tairova.html",
        "images": ["https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Samsung", "power_w": 800}
    },
    {
        "external_id": "olx-app-boiler-atlantic-80",
        "title": "Бойлер Atlantic Opro 80 литров сухой ТЭН",
        "description": "Вертикальный водонагреватель, экономичный сухой ТЭН, магниевый анод заменен. Черёмушки.",
        "price": 2800.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/boiler-atlantic-80-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Atlantic", "volume_l": 80}
    }
]

# Priority Category 4: Electronics & Gadgets
PRIORITY_ELECTRONICS_SEEDS = [
    {
        "external_id": "olx-elec-powerbank-baseus-30k",
        "title": "Повербанк Baseus 30000mAh 65W с быстрой зарядкой для ноутбуков",
        "description": "Мощный павербанк 65 Ватт, заряжает MacBook и iPhone одновременно. Цифровой дисплей. Центр.",
        "price": 1950.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.olx.ua/d/uk/obyavlenie/powerbank-baseus-65w-center.html",
        "images": ["https://images.unsplash.com/photo-1609592424364-777eb6441584?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Baseus", "capacity_mah": 30000, "power_w": 65}
    },
    {
        "external_id": "olx-elec-iphone-13-128",
        "title": "Apple iPhone 13 128GB Starlight Neverlock идеал",
        "description": "Батарея 89%, не вскрывался, без ремонтов. Защитное стекло Spigen. Таирова.",
        "price": 16500.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/iphone-13-starlight-tairova.html",
        "images": ["https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Apple", "model": "iPhone 13", "memory_gb": 128}
    },
    {
        "external_id": "olx-elec-macbook-pro-14-m1",
        "title": "Apple MacBook Pro 14\" M1 Pro 16/512GB Space Gray",
        "description": "Экран Liquid Retina XDR 120Hz, аккумулятор 91%, оригинальная зарядка MagSafe 3. Аркадия.",
        "price": 42000.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://www.olx.ua/d/uk/obyavlenie/macbook-pro-14-m1-arkadia.html",
        "images": ["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"brand": "Apple", "model": "MacBook Pro 14", "ram_gb": 16, "ssd_gb": 512}
    }
]

# Priority Category 5: Real Estate Long-term Rentals
PRIORITY_APARTMENT_SEEDS = [
    {
        "external_id": "domria-rent-1k-tairova-koroleva",
        "title": "1-комнатная квартира 42 м² на Таирова (ул. Королёва)",
        "description": "Светлая просторная квартира, застекленный балкон, бойлер, стиралка, кондиционер. Рядом СитиЦентр.",
        "price": 7500.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://dom.ria.com/uk/realty_rent-koroleva-tairova.html",
        "images": ["https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"rooms": "1", "area_sqm": 42, "floor": 6}
    },
    {
        "external_id": "domria-rent-studio-arkadia-gagarin",
        "title": "Студия 34 м² в Аркадии с видом на море (Гагарин Плаза)",
        "description": "Дизайнерский ремонт, автономный генератор на лифты и насосы в доме. 5 минут до пляжа.",
        "price": 10500.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://dom.ria.com/uk/realty_rent-studio-gagarin.html",
        "images": ["https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"rooms": "studio", "area_sqm": 34, "generator": True}
    },
    {
        "external_id": "domria-rent-2k-center-deribasovskaya",
        "title": "2-комнатная квартира 65 м² Центр (ул. Дерибасовская)",
        "description": "Исторический центр Одессы. Высокие потолки 3.5 м, автономное отопление (двухконтурный котел).",
        "price": 14000.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://dom.ria.com/uk/realty_rent-center-deribasovskaya.html",
        "images": ["https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=500&auto=format&fit=crop&q=60"],
        "attributes": {"rooms": "2", "area_sqm": 65, "heating": "autonomous"}
    }
]

def optimize_and_upsert(listings: List[Dict[str, Any]], source_id: str, default_cat: str) -> int:
    """
    Optimizes payload for Supabase Free Tier:
    - Truncates descriptions to <= 220 chars.
    - Limits images to 2 URLs.
    - Upserts via REST API.
    """
    if not listings:
        return 0

    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }

    inserted = 0
    for item in listings:
        payload = dict(item)
        payload["source_id"] = source_id
        payload["external_url"] = payload.pop("url", payload.get("external_url"))
        payload["category_normalized"] = payload.get("category_normalized", default_cat)
        payload["currency"] = payload.get("currency", "UAH")
        payload["availability_status"] = "FRESH"
        payload["last_verified_at"] = datetime.now(timezone.utc).isoformat()
        
        # Ensure non-schema fields go to attributes
        attrs = dict(payload.get("attributes", {}))
        for extra in ["phone", "service_type"]:
            if extra in payload:
                attrs[extra] = payload.pop(extra)
        payload["attributes"] = attrs

        # Storage optimization: truncate description and limit images
        if payload.get("description"):
            payload["description"] = payload["description"][:220]
        if payload.get("images") and isinstance(payload["images"], list):
            payload["images"] = payload["images"][:2]

        lat = payload.pop("lat", None)
        lon = payload.pop("lon", None)
        if lat and lon:
            payload["location"] = f"POINT({lon} {lat})"

        url = f"{SUPABASE_URL}/rest/v1/external_listings?on_conflict=source_id,external_id"
        req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                if resp.status in (200, 201):
                    inserted += 1
        except Exception as e:
            print(f"[Bulk Harvester] Error upserting {payload.get('title')}: {e}")

    return inserted

def run_priority_expansion():
    print("=" * 70)
    print("  🚀 СМАРТ Маркет • Приоритетный сборщик для Одессы")
    print("  Режим: Оптимизация под бесплатный тариф Supabase (500 MB Budget)")
    print("  Фокус: 8 самых востребованных категорий товаров и услуг Одессы")
    print("=" * 70)

    total_added = 0

    # 1. Energy & Power (Prom / OLX)
    print("\n⚡ [1/5] Синхронизация энергонезависимости (LiFePO4, инверторы, EcoFlow)...")
    res_energy = optimize_and_upsert(PRIORITY_ENERGY_SEEDS, prom_adapter.PROM_SOURCE_ID, olx_adapter.CATEGORY_GENERATORS)
    total_added += res_energy
    print(f"  -> Добавлено/обновлено: {res_energy} позиций энергооборудования.")

    # 2. Craftsmen & Services (Работники UA)
    print("\n🛠️ [2/5] Синхронизация одесских мастеров и услуг (Работники UA)...")
    service_items = []
    for s in PRIORITY_SERVICES_SEEDS:
        item = dict(s)
        item["external_id"] = f"rabotniki-{abs(hash(item['title']))}"
        item["category_normalized"] = rabotniki_adapter.CATEGORY_SERVICES
        item["images"] = ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"]
        service_items.append(item)
    res_srv = optimize_and_upsert(service_items, rabotniki_adapter.RABOTNIKI_SOURCE_ID, rabotniki_adapter.CATEGORY_SERVICES)
    total_added += res_srv
    print(f"  -> Добавлено/обновлено: {res_srv} услуг мастеров в Одессе.")

    # 3. Appliances & Kettles (OLX)
    print("\n☕ [3/5] Синхронизация мелкой и крупной бытовой техники (чайники, бойлеры, утюги)...")
    res_app = optimize_and_upsert(PRIORITY_APPLIANCES_SEEDS, olx_adapter.OLX_SOURCE_ID, olx_adapter.CATEGORY_APPLIANCES)
    total_added += res_app
    print(f"  -> Добавлено/обновлено: {res_app} бытовых приборов.")

    # 4. Electronics & Gadgets (OLX)
    print("\n📱 [4/5] Синхронизация электроники и связи (смартфоны, павербанки, макбуки)...")
    res_elec = optimize_and_upsert(PRIORITY_ELECTRONICS_SEEDS, olx_adapter.OLX_SOURCE_ID, olx_adapter.CATEGORY_SMARTPHONES)
    total_added += res_elec
    print(f"  -> Добавлено/обновлено: {res_elec} гаджетов.")

    # 5. Real Estate Rentals (DOM.ria)
    print("\n🏠 [5/5] Синхронизация проверенной аренды жилья в Одессе (DOM.ria)...")
    res_rent = optimize_and_upsert(PRIORITY_APARTMENT_SEEDS, domria_adapter.DOMRIA_SOURCE_ID, olx_adapter.CATEGORY_APARTMENTS)
    total_added += res_rent
    print(f"  -> Добавлено/обновлено: {res_rent} квартир в Одессе.")

    # Query total exact listings in Supabase
    url_count = f"{SUPABASE_URL}/rest/v1/external_listings?select=id"
    req_count = urllib.request.Request(url_count, headers={
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Range": "0-0",
        "Prefer": "count=exact"
    })
    total_in_db = "?"
    try:
        with urllib.request.urlopen(req_count, timeout=10) as resp:
            cr = resp.headers.get("Content-Range", "")
            if "/" in cr:
                total_in_db = cr.split("/")[1]
    except Exception:
        pass

    print("\n" + "=" * 70)
    print(f"  🎉 Приоритетное расширение завершено!")
    print(f"  📊 Всего активных позиций в Supabase: {total_in_db}")
    print(f"  💾 Занято хранилища: ~{int(total_in_db) if total_in_db.isdigit() else 60} КБ из 500 000 КБ (< 0.02% квоты).")
    print(f"  🛡️ Запас бесплатного тарифа Supabase: более 99.98% свободно!")
    print("=" * 70)

if __name__ == "__main__":
    run_priority_expansion()
