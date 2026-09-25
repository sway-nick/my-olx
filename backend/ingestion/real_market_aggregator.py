"""
Real Market Aggregator & Crawler Orchestrator (Odesa)
Coordinates live scrapers across DOM.ria, AUTO.ria, Prom.ua, and Rabotniki.ua.
Upserts authentic, compressed listings directly into Supabase external_listings table.
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
import domria_real_scraper
import autoria_real_scraper
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

def run_full_scraping_cycle(domria_pages: int = 5, autoria_pages: int = 5) -> Dict[str, Any]:
    start_time = time.time()
    print("=" * 75)
    print("  🌐 СМАРТ Маркет • Автоматический Реальный Парсер (Одесса)")
    print(f"  Старт цикла сбора: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    print("=" * 75)

    # 1. Purge stale records older than 14 days
    purged = smart_cache_worker.purge_stale_listings(max_days=14)

    all_harvested: List[Dict[str, Any]] = []

    # 2. DOM.ria (Real Estate: Rent & Sale)
    try:
        domria_items = domria_real_scraper.scrape_domria_catalog(max_pages_per_type=domria_pages)
        print(f"  ✅ [DOM.ria] Собрано {len(domria_items)} реальных квартир (аренда + продажа)")
        all_harvested.extend(domria_items)
    except Exception as e:
        print(f"  ❌ [DOM.ria Error] {e}")

    # 3. AUTO.ria (Cars in Odesa)
    try:
        autoria_items = autoria_real_scraper.scrape_autoria_catalog(max_pages=autoria_pages)
        print(f"  ✅ [AUTO.ria] Собрано {len(autoria_items)} реальных автомобилей в Одессе")
        all_harvested.extend(autoria_items)
    except Exception as e:
        print(f"  ❌ [AUTO.ria Error] {e}")

    # 4. Prom.ua (Generators, Inverters, EcoFlow, Pumps, Appliances, Electronics)
    try:
        prom_items = prom_real_scraper.scrape_prom_catalog(max_niches=12)
        print(f"  ✅ [Prom.ua] Собрано {len(prom_items)} реальных товаров и техники")
        all_harvested.extend(prom_items)
    except Exception as e:
        print(f"  ❌ [Prom.ua Error] {e}")

    # 5. Rabotniki.ua (Masters, Electricians, Plumbers, Builders)
    try:
        rabotniki_items = rabotniki_real_scraper.scrape_rabotniki_catalog()
        print(f"  ✅ [Работники UA] Собрано {len(rabotniki_items)} контактов проверенных мастеров")
        all_harvested.extend(rabotniki_items)
    except Exception as e:
        print(f"  ❌ [Работники UA Error] {e}")

    print("-" * 75)
    print(f"  📊 Всего собрано с реальных сайтов за этот цикл: {len(all_harvested)} позиций")

    # 6. Upsert into Supabase with automatic deduplication
    synced = smart_cache_worker.upsert_compressed_batch(all_harvested)
    print(f"  💾 Успешно загружено/обновлено в Supabase: {synced} позиций")

    # 7. Check database metrics
    metrics = smart_cache_worker.get_db_metrics()
    duration = round(time.time() - start_time, 1)

    print("-" * 75)
    print(f"  📈 Состояние базы Supabase:")
    print(f"     • Всего записей в базе: {metrics['total_listings']}")
    print(f"     • Занято памяти: ~{metrics['storage_mb']} МБ из 500 МБ ({metrics['used_percentage']}%)")
    print(f"     • Доступно свободного места: ~{round(500.0 - metrics['storage_mb'], 1)} МБ")
    print(f"     • Длительность сбора: {duration} сек")
    print("=" * 75)

    # 8. Update State
    state = load_crawler_state()
    state["total_cycles"] = state.get("total_cycles", 0) + 1
    state["last_run_utc"] = datetime.now(timezone.utc).isoformat()
    state["last_harvest_count"] = len(all_harvested)
    state["last_synced_count"] = synced
    state["total_listings"] = metrics["total_listings"]
    state["storage_mb"] = metrics["storage_mb"]
    save_crawler_state(state)

    return {
        "harvested": len(all_harvested),
        "synced": synced,
        "metrics": metrics,
        "duration_seconds": duration
    }

if __name__ == "__main__":
    run_full_scraping_cycle(domria_pages=3, autoria_pages=3)
