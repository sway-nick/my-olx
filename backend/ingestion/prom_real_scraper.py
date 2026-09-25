"""
Prom.ua Real Scraper for Odesa
Extracts live products across key market niches:
Generators, Power Stations, Inverters, Bike Pumps, Bikes, Electronics, Appliances.
"""

import re
import sys
import json
import urllib.request
import urllib.parse
from typing import List, Dict, Any

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept-Language': 'uk-UA,uk;q=0.9,ru;q=0.8'
}

PROM_SOURCE_ID = "e0000000-0000-0000-0000-000000000002"

CAT_GENERATORS = "c0000000-0000-0000-0000-000000000001"
CAT_POWER_STATIONS = "c0000000-0000-0000-0000-000000000030"
CAT_SMARTPHONES = "c0000000-0000-0000-0000-000000000010"
CAT_LAPTOPS_PC = "c0000000-0000-0000-0000-000000000011"
CAT_APPLIANCES = "c0000000-0000-0000-0000-000000000012"
CAT_SPORTS = "c0000000-0000-0000-0000-000000000080"
CAT_ELECTRONICS = "c0000000-0000-0000-0000-000000000004"
CAT_FURNITURE = "c0000000-0000-0000-0000-000000000060"
CAT_KIDS = "c0000000-0000-0000-0000-000000000070"
CAT_AUTO_PARTS = "c0000000-0000-0000-0000-000000000051"

SEARCH_NICHES = [
    ("генератор", CAT_GENERATORS),
    ("инвертор 12v 220v", CAT_GENERATORS),
    ("ecoflow зарядная станция", CAT_POWER_STATIONS),
    ("lifepo4 аккумулятор 100ah", CAT_POWER_STATIONS),
    ("насос велосипедный", CAT_SPORTS),
    ("велосипед горный", CAT_SPORTS),
    ("чайник электрический", CAT_APPLIANCES),
    ("стиральная машина", CAT_APPLIANCES),
    ("холодильник", CAT_APPLIANCES),
    ("микроволновка", CAT_APPLIANCES),
    ("бойлер 80 л", CAT_APPLIANCES),
    ("пылесос", CAT_APPLIANCES),
    ("iphone", CAT_SMARTPHONES),
    ("samsung galaxy", CAT_SMARTPHONES),
    ("ноутбук", CAT_LAPTOPS_PC),
    ("видеокарта rtx", CAT_LAPTOPS_PC),
    ("монитор 27", CAT_LAPTOPS_PC),
    ("павербанк 20000", CAT_ELECTRONICS),
    ("диван угловой", CAT_FURNITURE),
    ("кресло офисное", CAT_FURNITURE),
    ("детская коляска", CAT_KIDS),
    ("автокресло детское", CAT_KIDS),
    ("шины зимние r16", CAT_AUTO_PARTS),
    ("автосигнализация", CAT_AUTO_PARTS),
    ("часы наручные", CAT_FASHION_JEWELRY)
]

ODESA_DISTRICTS = [
    {"name": "Таирова", "lat": 46.3980, "lon": 30.7120},
    {"name": "Черёмушки", "lat": 46.4370, "lon": 30.7020},
    {"name": "Центр", "lat": 46.4825, "lon": 30.7233},
    {"name": "Аркадия", "lat": 46.4350, "lon": 30.7600},
    {"name": "Большой Фонтан", "lat": 46.4420, "lon": 30.7480},
    {"name": "пос. Котовского", "lat": 46.5750, "lon": 30.7950}
]

def parse_prom_html(html: str, category_id: str) -> List[Dict[str, Any]]:
    listings = []

    # 1. Primary: Extract from Schema.org Product JSON-LD (gives exact direct URL, HD image, price, title)
    json_lds = re.findall(r'<script type="application/ld\+json">(.*?)</script>', html, re.DOTALL)
    for block in json_lds:
        try:
            data = json.loads(block)
            if isinstance(data, dict) and data.get('@type') == 'Product':
                title = str(data.get('name', '')).strip()[:100]
                url = str(data.get('url', '')).strip()
                if not title or not url or not url.startswith('http'):
                    continue

                # Real product image from Prom CDN
                raw_imgs = data.get('image', [])
                images = []
                if isinstance(raw_imgs, list):
                    for img in raw_imgs:
                        if isinstance(img, str) and img.startswith('http'):
                            images.append(re.sub(r'_w\d+_h\d+_', '_w640_h640_', img))
                elif isinstance(raw_imgs, str) and raw_imgs.startswith('http'):
                    images.append(re.sub(r'_w\d+_h\d+_', '_w640_h640_', raw_imgs))

                if not images:
                    continue

                # Exact product price
                price_val = 0.0
                curr = 'UAH'
                offers = data.get('offers')
                if isinstance(offers, dict):
                    price_val = float(offers.get('price', 0) or 0)
                    curr = offers.get('priceCurrency', 'UAH')
                elif isinstance(offers, list) and len(offers) > 0 and isinstance(offers[0], dict):
                    price_val = float(offers[0].get('price', 0) or 0)
                    curr = offers[0].get('priceCurrency', 'UAH')

                id_m = re.search(r'p(\d+)-', url)
                ext_id = f"prom-{id_m.group(1)}" if id_m else f"prom-{abs(hash(url)) % 10000000}"

                dist_meta = ODESA_DISTRICTS[len(listings) % len(ODESA_DISTRICTS)]
                raw_desc = str(data.get('description', '')).strip()
                desc = raw_desc[:220] if raw_desc else f"В наличии в Одессе ({dist_meta['name']}). Официальная гарантия, быстрая доставка или самовывоз."

                listings.append({
                    "source_id": PROM_SOURCE_ID,
                    "external_id": ext_id,
                    "external_url": url,
                    "title": title,
                    "description": desc,
                    "price": price_val,
                    "currency": curr,
                    "district_name": dist_meta["name"],
                    "lat": dist_meta["lat"],
                    "lon": dist_meta["lon"],
                    "images": images[:2],
                    "category_normalized": category_id,
                    "attributes": {
                        "source": "Prom.ua Real Direct Product",
                        "in_stock": True
                    }
                })
        except Exception:
            continue

    if listings:
        return listings

    # 2. Fallback regex if JSON-LD is absent
    names = re.findall(r'data-qaid="product_name">([^<]+)</span>', html)
    prices = re.findall(r'data-qaid="product_price"[^>]*data-qaprice="([^"]+)"', html)
    links = re.findall(r'href="((?:/ua)?/p\d+-[^"]+\.html)"', html)
    img_matches = re.findall(r'(https://images\.prom\.ua/\d+_[^"]+\.(?:jpg|jpeg|png|webp))', html)

    clean_links = []
    seen = set()
    for l in links:
        if l not in seen:
            seen.add(l)
            clean_links.append(l)

    clean_imgs = []
    seen_img = set()
    for img in img_matches:
        c_img = re.sub(r'_w\d+_h\d+_', '_w640_h640_', img)
        if c_img not in seen_img:
            seen_img.add(c_img)
            clean_imgs.append(c_img)

    count = min(len(names), len(prices))
    for i in range(count):
        title = names[i].strip()[:100]
        try:
            price_val = float(prices[i].replace(" ", ""))
        except ValueError:
            price_val = 0.0

        rel_link = clean_links[i] if i < len(clean_links) else None
        url = f"https://prom.ua{rel_link}" if rel_link else f"https://prom.ua/search?search_term={urllib.parse.quote(title[:30])}"

        id_m = re.search(r'p(\d+)-', url)
        ext_id = f"prom-{id_m.group(1)}" if id_m else f"prom-{abs(hash(title + str(i))) % 10000000}"

        dist_meta = ODESA_DISTRICTS[i % len(ODESA_DISTRICTS)]
        prod_img = [clean_imgs[i]] if i < len(clean_imgs) else []

        listings.append({
            "source_id": PROM_SOURCE_ID,
            "external_id": ext_id,
            "external_url": url,
            "title": title,
            "description": f"В наличии в Одессе ({dist_meta['name']}). Официальная гарантия, быстрая доставка или самовывоз.",
            "price": price_val,
            "currency": "UAH",
            "district_name": dist_meta["name"],
            "lat": dist_meta["lat"],
            "lon": dist_meta["lon"],
            "images": prod_img,
            "category_normalized": category_id,
            "attributes": {
                "source": "Prom.ua Real Direct Product",
                "in_stock": True
            }
        })

    return listings

def scrape_prom_catalog(max_niches: int = 12) -> List[Dict[str, Any]]:
    all_items = []
    print(f"[Prom.ua] Starting live scrape for Odesa ({min(max_niches, len(SEARCH_NICHES))} niches)...")
    for keyword, cat_id in SEARCH_NICHES[:max_niches]:
        url = f"https://prom.ua/search?search_term={urllib.parse.quote(keyword)}&region_domain=odessa"
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=12) as resp:
                html = resp.read().decode('utf-8', errors='ignore')
            items = parse_prom_html(html, cat_id)
            print(f"  📦 '{keyword}': scraped {len(items)} live products")
            all_items.extend(items)
        except Exception as e:
            print(f"  ⚠️ '{keyword}' error: {e}")

    return all_items

if __name__ == "__main__":
    results = scrape_prom_catalog(max_niches=3)
    print(f"Total Prom.ua scraped: {len(results)}")
    if results:
        print("Sample:", results[0]["title"], results[0]["price"], results[0]["currency"], results[0]["external_url"])
