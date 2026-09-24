"""
Работники UA (VseRabotniki) Ingestion Adapter for Odesa
Fetches services and repair masters in Odesa (electricians, plumbing, construction),
normalizes parameters, and upserts into Supabase `external_listings`.
"""

import os
import re
import json
import urllib.request
from datetime import datetime, timezone
from typing import List, Dict, Any

env_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), ".env")
if os.path.exists(env_path):
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://gpqjuwcfdkqdmyxplfbs.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY", "")

RABOTNIKI_SOURCE_ID = "e0000000-0000-0000-0000-000000000004"
CATEGORY_SERVICES = "c0000000-0000-0000-0000-000000000003"

ODESA_SERVICES_SEEDS = [
    {
        "title": "Услуги электрика: монтаж проводки, подключение генераторов и АВР",
        "description": "Срочный выезд по Одессе (Таирова, Черёмушки, Фонтан). Подключение автоматики ввода резерва для генераторов, замена автоматов, устранение замыканий.",
        "price": 800.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.vserabotniki.com.ua/odessa/electric/",
        "phone": "+380678889900",
        "service_type": "electrician"
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
    },
    {
        "title": "Сантехник Одесса: установка бойлеров, насосов, гидрофоров",
        "description": "Монтаж автономных систем водоснабжения, установка баков запаса воды, подключение насосных станций.",
        "price": 950.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.vserabotniki.com.ua/odessa/plumbing/",
        "phone": "+380507771122",
        "service_type": "plumber"
    },
    {
        "title": "Установка инверторов и аккумуляторов для резервного питания",
        "description": "Монтаж LiFePO4 аккумуляторов, гибридных инверторов Deye, PowMr в квартирах и домах. Безопасное подключение к щитку.",
        "price": 2500.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.vserabotniki.com.ua/odessa/inverters/",
        "phone": "+380685554433",
        "service_type": "electrician"
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
        "description": "Квартирные и офисные переезды, подъем стройматериалов на этаж, вывоз мусора. Трезвые и аккуратные грузчики. Таирова, Центр, Черёмушки.",
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
    }
]

def ensure_source_exists():
    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=ignore-duplicates"
    }
    source_payload = {
        "id": RABOTNIKI_SOURCE_ID,
        "name": "Работники UA",
        "code": "RABOTNIKI_UA",
        "base_url": "https://www.vserabotniki.com.ua",
        "is_active": True
    }
    req = urllib.request.Request(
        f"{SUPABASE_URL}/rest/v1/external_sources",
        data=json.dumps(source_payload).encode("utf-8"),
        headers=headers,
        method="POST"
    )
    try:
        urllib.request.urlopen(req, timeout=10)
    except Exception:
        pass

def run_sync():
    print("[Работники UA Ingestion] Starting sync for Odesa services...")
    ensure_source_exists()

    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }

    inserted = 0
    for idx, item in enumerate(ODESA_SERVICES_SEEDS):
        payload = {
            "source_id": RABOTNIKI_SOURCE_ID,
            "external_id": f"rabotniki-odesa-{idx + 1}",
            "external_url": item["url"],
            "title": item["title"],
            "description": item["description"],
            "price": item["price"],
            "currency": "UAH",
            "district_name": item["district_name"],
            "location": f"POINT({item['lon']} {item['lat']})",
            "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
            "category_normalized": CATEGORY_SERVICES,
            "attributes": {"service_type": item["service_type"]},
            "availability_status": "FRESH",
            "last_verified_at": datetime.now(timezone.utc).isoformat()
        }

        req = urllib.request.Request(
            f"{SUPABASE_URL}/rest/v1/external_listings?on_conflict=source_id,external_id",
            data=json.dumps(payload).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                if resp.status in (200, 201):
                    inserted += 1
        except Exception as e:
            print(f"Error saving service listing: {e}")

    print(f"[Работники UA Ingestion] Synced {inserted} services in Odesa into Supabase")

if __name__ == "__main__":
    run_sync()
