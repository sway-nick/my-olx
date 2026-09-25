"""
Deep Mass Harvester for Odesa (СМАРТ Маркет)
Coordinates deep multi-platform harvesting across:
- AUTO.ria (Odesa cars, strictly state[0]=12 / city/odessa/)
- DOM.ria (Odesa rent and sale apartments)
- Prom.ua (25 major consumer and tech niches)
- Rabotniki.ua (Verified local masters and tradesmen)

Safely compresses and streams batches into Supabase external_listings table.
Enforces Supabase Free Tier quotas (500 MB budget, $0/month).
"""

import os
import sys
import json
import time
from datetime import datetime, timezone
from typing import List, Dict, Any

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

import smart_cache_worker
import autoria_real_scraper
import domria_real_scraper
import prom_real_scraper
import rabotniki_real_scraper

STATE_FILE = os.path.join(current_dir, "crawler_state.json")

def load_crawler_state() -> Dict[str, Any]:
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {"total_cycles": 0, "last_run_utc": None, "total_live_items": 0}

def save_crawler_state(state: Dict[str, Any]):
    try:
        with open(STATE_FILE, "w", encoding="utf-8") as f:
            json.dump(state, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"[State Save Error] {e}")

def run_deep_mass_harvest(
    autoria_pages: int = 10,
    domria_pages: int = 10,
    prom_niches: int = 25
) -> Dict[str, Any]:
    start_time = time.time()
    print("=" * 80)
    print("  🚀 СМАРТ Маркет • Глубокий Масштабный Сборщик Данных (Одесса)")
    print(f"  Старт сбора: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    print("=" * 80)

    total_harvested = 0
    total_upserted = 0

    # 1. AUTO.ria (Real Odesa Cars)
    print("\n🚗 [1/4] Сбор реальных автомобилей в Одессе (AUTO.ria)...")
    try:
        cars = autoria_real_scraper.scrape_autoria_catalog(max_pages=autoria_pages)
        total_harvested += len(cars)
        if cars:
            synced_cars = smart_cache_worker.upsert_compressed_batch(cars)
            total_upserted += synced_cars
            print(f"  ✅ [AUTO.ria] Собрано: {len(cars)}, успешно синхронизировано в Supabase: {synced_cars}")
    except Exception as e:
        print(f"  ❌ [AUTO.ria Error] {e}")

    # 2. DOM.ria (Real Estate: Rent & Sale in Odesa)
    print("\n🏢 [2/4] Сбор недвижимости в Одессе (DOM.ria: Аренда + Продажа)...")
    try:
        realty = domria_real_scraper.scrape_domria_catalog(max_pages_per_type=domria_pages)
        total_harvested += len(realty)
        if realty:
            synced_realty = smart_cache_worker.upsert_compressed_batch(realty)
            total_upserted += synced_realty
            print(f"  ✅ [DOM.ria] Собрано: {len(realty)}, успешно синхронизировано в Supabase: {synced_realty}")
    except Exception as e:
        print(f"  ❌ [DOM.ria Error] {e}")

    # 3. Prom.ua (25 Broad Consumer Niches with Schema.org JSON-LD)
    print(f"\n🛍️ [3/4] Сбор потребительских товаров и техники (Prom.ua, {prom_niches} ниш)...")
    try:
        prom_items = prom_real_scraper.scrape_prom_catalog(max_niches=prom_niches)
        total_harvested += len(prom_items)
        if prom_items:
            synced_prom = smart_cache_worker.upsert_compressed_batch(prom_items)
            total_upserted += synced_prom
            print(f"  ✅ [Prom.ua] Собрано: {len(prom_items)}, успешно синхронизировано в Supabase: {synced_prom}")
    except Exception as e:
        print(f"  ❌ [Prom.ua Error] {e}")

    # 4. Rabotniki.ua (Services & Masters in Odesa)
    print("\n🔨 [4/4] Сбор контактов проверенных мастеров (Работники UA)...")
    try:
        services = rabotniki_real_scraper.scrape_rabotniki_catalog()
        total_harvested += len(services)
        if services:
            synced_services = smart_cache_worker.upsert_compressed_batch(services)
            total_upserted += synced_services
            print(f"  ✅ [Работники UA] Собрано: {len(services)}, успешно синхронизировано в Supabase: {synced_services}")
    except Exception as e:
        print(f"  ❌ [Работники UA Error] {e}")

    # Metrics & Final Status
    duration = round(time.time() - start_time, 1)
    db_metrics = smart_cache_worker.get_db_metrics()

    print("\n" + "=" * 80)
    print("  🎉 Глубокий сбор успешно завершен!")
    print(f"     • Собрано с площадок за проход: {total_harvested} поз.")
    print(f"     • Успешно обновлено/загружено в Supabase: {total_upserted} поз.")
    print(f"     • ИТОГО актуальных предложений в базе Одессы: {db_metrics['total_listings']} поз.")
    print(f"     • Занято памяти: ~{db_metrics['storage_mb']} МБ из 500 МБ ({db_metrics['used_percentage']}%)")
    print(f"     • Свободно в рамках бесплатного лимита: ~{round(500.0 - db_metrics['storage_mb'], 1)} МБ")
    print(f"     • Время выполнения: {duration} сек")
    print("=" * 80)

    # Update crawler state
    state = load_crawler_state()
    state["total_cycles"] = state.get("total_cycles", 0) + 1
    state["last_run_utc"] = datetime.now(timezone.utc).isoformat()
    state["last_harvest_count"] = total_harvested
    state["last_synced_count"] = total_upserted
    state["total_listings"] = db_metrics["total_listings"]
    state["storage_mb"] = db_metrics["storage_mb"]
    save_crawler_state(state)

    return {
        "harvested": total_harvested,
        "upserted": total_upserted,
        "metrics": db_metrics,
        "duration": duration
    }

if __name__ == "__main__":
    run_deep_mass_harvest()
