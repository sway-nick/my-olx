"""
Master Ingestion Orchestrator for Odesa (IntentMarket / Мой OLX)
Executes all registered platform adapters, syncs listings into Supabase,
and outputs a unified status report.
"""

import sys
import os
import json
import urllib.request
from datetime import datetime, timezone

# Ensure ingestion directory is in sys.path
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

# Fix Windows console encoding
if hasattr(sys.stdout, "buffer"):
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

import olx_adapter
import prom_adapter
import domria_adapter
import rabotniki_adapter

def print_banner():
    print("=" * 65)
    print("  🚀 IntentMarket • Master Ingestion Pipeline (Одесса)")
    print("  Aggregating: OLX, Prom, DOM.ria, Работники UA")
    print(f"  Timestamp: {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')}")
    print("=" * 65)

def run_all():
    print_banner()

    results = {}

    print("\n--- [1/4] Running OLX.ua Adapter (Generators & Apartments) ---")
    try:
        olx_adapter.run_sync()
        results["OLX"] = "SUCCESS"
    except Exception as e:
        print(f"OLX adapter error: {e}")
        results["OLX"] = f"ERROR: {e}"

    print("\n--- [2/4] Running Prom.ua Adapter (Power Warehouses & Stations) ---")
    try:
        prom_res = prom_adapter.run_sync()
        results["Prom.ua"] = f"SUCCESS ({prom_res} synced)"
    except Exception as e:
        print(f"Prom adapter error: {e}")
        results["Prom.ua"] = f"ERROR: {e}"

    print("\n--- [3/4] Running DOM.ria Adapter (Verified Apartments) ---")
    try:
        domria_res = domria_adapter.run_sync()
        results["DOM.ria"] = f"SUCCESS ({domria_res} synced)"
    except Exception as e:
        print(f"DOM.ria adapter error: {e}")
        results["DOM.ria"] = f"ERROR: {e}"

    print("\n--- [4/4] Running Работники UA Adapter (Electricians & Masters) ---")
    try:
        rabotniki_adapter.run_sync()
        results["Работники UA"] = "SUCCESS"
    except Exception as e:
        print(f"Работники UA adapter error: {e}")
        results["Работники UA"] = f"ERROR: {e}"

    # Fetch total summary from Supabase Cloud
    print("\n" + "=" * 65)
    print("  📊 ИТОГОВАЯ СВОДКА БАЗЫ SUPABASE ПО ОДЕССЕ:")
    print("=" * 65)

    try:
        supabase_url = olx_adapter.SUPABASE_URL
        supabase_key = olx_adapter.SUPABASE_KEY
        if supabase_key:
            req = urllib.request.Request(
                f"{supabase_url}/rest/v1/external_listings?select=source_id,district_name,title,price,category_normalized",
                headers={"apikey": supabase_key, "Authorization": f"Bearer {supabase_key}"}
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.loads(resp.read().decode())
                print(f"  Всего внешних объявлений в базе: {len(data)}")

                # Count by district
                districts = {}
                for item in data:
                    d = item.get("district_name") or "Не указан"
                    districts[d] = districts.get(d, 0) + 1
                
                print("\n  📍 Распределение по районам Одессы:")
                for d, count in sorted(districts.items(), key=lambda x: x[1], reverse=True):
                    print(f"     • {d}: {count} объявл.")
    except Exception as e:
        print(f"  Could not fetch live Supabase summary: {e}")

    print("\n" + "=" * 65)
    print("  ✅ Синхронизация успешно завершена!")
    print("=" * 65)

if __name__ == "__main__":
    run_all()
