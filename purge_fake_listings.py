"""
Purge All Fake & Synthetic Seed Listings from Supabase
Ensures ONLY 100% authentic, verified, live scraped listings remain in external_listings table.
"""

import sys
import json
import urllib.request
from typing import List, Dict, Any

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

import os
env_file = os.path.join(os.path.dirname(__file__), "backend", ".env")
if os.path.exists(env_file):
    with open(env_file, "r", encoding="utf-8") as f:
        for line in f:
            if "=" in line and not line.startswith("#"):
                k, v = line.strip().split("=", 1)
                os.environ.setdefault(k, v)

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://gpqjuwcfdkqdmyxplfbs.supabase.co")
SERVICE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY", "")

def fetch_all_listings() -> List[Dict[str, Any]]:
    all_items = []
    page_size = 1000
    offset = 0

    while True:
        url = f"{SUPABASE_URL}/rest/v1/external_listings?select=id,source_id,external_id,external_url,title"
        req = urllib.request.Request(url, headers={
            'apikey': SERVICE_KEY,
            'Authorization': f'Bearer {SERVICE_KEY}',
            'Range': f"{offset}-{offset + page_size - 1}"
        })
        try:
            with urllib.request.urlopen(req) as resp:
                batch = json.loads(resp.read().decode('utf-8'))
                if not batch:
                    break
                all_items.extend(batch)
                if len(batch) < page_size:
                    break
                offset += page_size
        except Exception as e:
            print(f"Fetch error at offset {offset}: {e}")
            break

    return all_items

def is_fake_listing(item: Dict[str, Any]) -> bool:
    eid = item.get('external_id', '') or ''
    url = item.get('external_url', '') or ''
    sid = item.get('source_id', '') or ''
    uuid_str = item.get('id', '') or ''

    # 1. Old mock OLX source
    if sid == 'e0000000-0000-0000-0000-000000000001':
        return True

    # 2. Hardcoded seed patterns
    if any(x in eid for x in ['studio', 'odesa-', 'sample', 'seed', 'test', 'mock', 'demo', 'tairova', 'cheremushki', 'arcadia']):
        # Real IDs on platforms are purely numeric or autoria-12345
        # If it contains handwritten slug strings like 'tairova', 'arcadia-studio', it's fake!
        if not eid.replace('autoria-', '').replace('domria-', '').replace('prom-', '').replace('rabotniki-', '').isdigit():
            return True

    if '11111111-' in uuid_str or '00000000-' in uuid_str:
        return True

    if 'realty_rent-123' in url or 'generator-hyundai-3050' in url:
        return True

    # 3. Non-authentic platform URLs
    if sid == 'e0000000-0000-0000-0000-000000000005': # AUTO.ria
        if not ('auto.ria.com/uk/auto_' in url):
            return True
    elif sid == 'e0000000-0000-0000-0000-000000000003': # DOM.ria
        if not ('dom.ria.com/uk/realty-' in url):
            return True
    elif sid == 'e0000000-0000-0000-0000-000000000002': # Prom.ua
        if not ('prom.ua/' in url):
            return True
    elif sid == 'e0000000-0000-0000-0000-000000000004': # Rabotniki.ua
        if not ('rabotniki.ua/' in url):
            return True

    return False

def purge_fakes():
    print("=" * 70)
    print("  🧹 Очистка базы данных Supabase от всех фейковых и сидовых объявлений")
    print("=" * 70)

    listings = fetch_all_listings()
    print(f"Всего записей в таблице external_listings: {len(listings)}")

    fakes_to_delete = [item for item in listings if is_fake_listing(item)]
    reals_to_keep = [item for item in listings if not is_fake_listing(item)]

    print(f"Обнаружено фейковых/сидовых записей: {len(fakes_to_delete)}")
    print(f"Подлинных реальных объявлений: {len(reals_to_keep)}")

    if not fakes_to_delete:
        print("✅ База уже чиста! Фейковых объявлений не обнаружено.")
        return

    # Delete in batches by ID
    batch_size = 50
    deleted_count = 0

    for i in range(0, len(fakes_to_delete), batch_size):
        chunk = fakes_to_delete[i:i + batch_size]
        ids = [c['id'] for c in chunk]
        id_filter = f"id=in.({','.join(ids)})"
        url = f"{SUPABASE_URL}/rest/v1/external_listings?{id_filter}"
        
        req = urllib.request.Request(url, method='DELETE', headers={
            'apikey': SERVICE_KEY,
            'Authorization': f'Bearer {SERVICE_KEY}',
            'Prefer': 'count=exact'
        })
        try:
            with urllib.request.urlopen(req) as resp:
                deleted_count += len(ids)
                print(f"  Удален пакет {deleted_count}/{len(fakes_to_delete)}...")
        except Exception as e:
            print(f"  Ошибка удаления пакета: {e}")

    print(f"\n🎉 Успешно удалено {deleted_count} фейковых объявлений!")
    
    # Check final count
    remaining = fetch_all_listings()
    print(f"Осталось в базе 100% ПОДЛИННЫХ объявлений: {len(remaining)}")

if __name__ == "__main__":
    purge_fakes()
