"""
DOM.ria Real Scraper for Odesa (Rent & Sale)
Extracts real listings from DOM.ria HTML realty-item sections.
"""

import re
import sys
import json
import urllib.request
from typing import List, Dict, Any

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept-Language': 'uk-UA,uk;q=0.9,ru;q=0.8'
}

DOMRIA_SOURCE_ID = "e0000000-0000-0000-0000-000000000003"
CAT_APARTMENT_RENT = "c0000000-0000-0000-0000-000000000002"
CAT_APARTMENT_SALE = "c0000000-0000-0000-0000-000000000020"

ODESA_DISTRICTS = [
    {"name": "Приморский", "lat": 46.4825, "lon": 30.7233},
    {"name": "Аркадия", "lat": 46.4350, "lon": 30.7600},
    {"name": "Центр", "lat": 46.4825, "lon": 30.7233},
    {"name": "Большой Фонтан", "lat": 46.4420, "lon": 30.7480},
    {"name": "Таирова", "lat": 46.3980, "lon": 30.7120},
    {"name": "Черёмушки", "lat": 46.4370, "lon": 30.7020},
    {"name": "пос. Котовского", "lat": 46.5750, "lon": 30.7950},
    {"name": "Молдаванка", "lat": 46.4700, "lon": 30.7100}
]

def parse_domria_html(html: str, deal_type: str = "rent") -> List[Dict[str, Any]]:
    items = re.findall(r'<section[^>]*class="[^"]*realty-item[^"]*"[^>]*>(.*?)</section>', html, re.DOTALL)
    cat_id = CAT_APARTMENT_SALE if deal_type == "sale" else CAT_APARTMENT_RENT
    extracted = []

    for it in items:
        link_m = re.search(r'href="(/uk/realty-[^"]+\.html)"', it)
        if not link_m:
            continue
        url = f"https://dom.ria.com{link_m.group(1)}"

        title_m = re.search(r'title="([^"]+)"\s+class="[^"]*size22', it) or re.search(r'class="tit".*?>\s*([^<]+)<', it, re.DOTALL)
        raw_title = title_m.group(1).strip() if title_m else ("Оренда квартири в Одесі" if deal_type == "rent" else "Продаж квартири в Одесі")
        # Format title cleanly
        title = f"{'Аренда' if deal_type == 'rent' else 'Продажа'}: {raw_title}"[:100].strip()

        price_m = re.search(r'<b class="size22">([\d\s]+)\s*(грн|\$|USD)', it)
        price_val = 0.0
        curr = "UAH"
        if price_m:
            try:
                price_val = float(price_m.group(1).replace(" ", ""))
                curr = "USD" if ("$" in price_m.group(2) or "USD" in price_m.group(2)) else "UAH"
            except ValueError:
                price_val = 0.0

        desc_m = re.search(r'class="mt-15[^"]*desc-hidden[^"]*">\s*([^<]+)', it)
        desc = desc_m.group(1).strip()[:200] if desc_m else ""

        img_m = re.search(r'<img[^>]*src="([^"]+riastatic\.com[^"]+)"', it)
        img = img_m.group(1) if img_m else "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=500&auto=format&fit=crop&q=60"

        id_m = re.search(r'id="realty-(\d+)"', it) or re.search(r'(\d+)\.html', url)
        ext_id = f"domria-{id_m.group(1)}" if id_m else f"domria-{abs(hash(url)) % 10000000}"

        # Match district
        text_for_dist = f"{title} {desc}".lower()
        matched_dist = ODESA_DISTRICTS[0]
        for d in ODESA_DISTRICTS:
            if d["name"].lower() in text_for_dist:
                matched_dist = d
                break
            if "фонтан" in text_for_dist:
                matched_dist = next(x for x in ODESA_DISTRICTS if x["name"] == "Большой Фонтан")
                break
            if "аркад" in text_for_dist:
                matched_dist = next(x for x in ODESA_DISTRICTS if x["name"] == "Аркадия")
                break
            if "таир" in text_for_dist:
                matched_dist = next(x for x in ODESA_DISTRICTS if x["name"] == "Таирова")
                break
            if "черёмуш" in text_for_dist or "черемуш" in text_for_dist:
                matched_dist = next(x for x in ODESA_DISTRICTS if x["name"] == "Черёмушки")
                break
            if "котовск" in text_for_dist:
                matched_dist = next(x for x in ODESA_DISTRICTS if x["name"] == "пос. Котовского")
                break

        extracted.append({
            "source_id": DOMRIA_SOURCE_ID,
            "external_id": ext_id,
            "external_url": url,
            "title": title,
            "description": desc,
            "price": price_val,
            "currency": curr,
            "district_name": matched_dist["name"],
            "lat": matched_dist["lat"],
            "lon": matched_dist["lon"],
            "images": [img],
            "category_normalized": cat_id,
            "attributes": {
                "deal": deal_type,
                "source": "DOM.ria Real Crawler"
            }
        })

    return extracted

def scrape_domria_catalog(max_pages_per_type: int = 5) -> List[Dict[str, Any]]:
    all_items = []
    print(f"[DOM.ria] Starting live scrape for Odesa ({max_pages_per_type} pages rent, {max_pages_per_type} pages sale)...")
    
    # 1. Rent
    for page in range(1, max_pages_per_type + 1):
        url = "https://dom.ria.com/uk/arenda-kvartir/odessa/" if page == 1 else f"https://dom.ria.com/uk/arenda-kvartir/odessa/?page={page}"
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=12) as resp:
                html = resp.read().decode('utf-8', errors='ignore')
            items = parse_domria_html(html, deal_type="rent")
            print(f"  🏢 Rent Page {page}: scraped {len(items)} live listings")
            all_items.extend(items)
        except Exception as e:
            print(f"  ⚠️ Rent Page {page} error: {e}")

    # 2. Sale
    for page in range(1, max_pages_per_type + 1):
        url = "https://dom.ria.com/uk/prodazha-kvartir/odessa/" if page == 1 else f"https://dom.ria.com/uk/prodazha-kvartir/odessa/?page={page}"
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=12) as resp:
                html = resp.read().decode('utf-8', errors='ignore')
            items = parse_domria_html(html, deal_type="sale")
            print(f"  🔑 Sale Page {page}: scraped {len(items)} live listings")
            all_items.extend(items)
        except Exception as e:
            print(f"  ⚠️ Sale Page {page} error: {e}")

    return all_items

if __name__ == "__main__":
    results = scrape_domria_catalog(max_pages_per_type=2)
    print(f"Total scraped: {len(results)}")
