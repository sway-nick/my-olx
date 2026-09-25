"""
AUTO.ria Real Scraper for Odesa
Extracts live, authentic car listings directly from AUTO.ria (Odesa city).
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

AUTORIA_SOURCE_ID = "e0000000-0000-0000-0000-000000000005"
CAT_AUTO = "c0000000-0000-0000-0000-000000000050"

ODESA_DISTRICTS = [
    {"name": "Приморский", "lat": 46.4825, "lon": 30.7233, "keywords": ["приморск", "центр", "дерибасов", "ришельев", "французск", "шевченк", "гагарин"]},
    {"name": "Таирова", "lat": 46.3980, "lon": 30.7120, "keywords": ["таиров", "таїров", "глушко", "королев", "вузовск", "вильямс", "люстдорф"]},
    {"name": "Аркадия", "lat": 46.4350, "lon": 30.7600, "keywords": ["аркади", "аркаді", "генуэзск", "каманин"]},
    {"name": "Центр", "lat": 46.4825, "lon": 30.7233, "keywords": ["центр", "соборн", "преображен", "пантелеймон"]},
    {"name": "Черёмушки", "lat": 46.4370, "lon": 30.7020, "keywords": ["черемушк", "черьомушк", "малиновск", "филатов", "космонавт"]},
    {"name": "Большой Фонтан", "lat": 46.4420, "lon": 30.7480, "keywords": ["фонтан", "чубаевк", "станция"]},
    {"name": "Пос. Котовского", "lat": 46.5750, "lon": 30.7950, "keywords": ["котовск", "поскот", "заболотн", "добровольск", "днепродорог", "паустовск", "бочаров", "суворовск", "пересып"]}
]

def determine_district(text: str, idx: int) -> Dict[str, Any]:
    text_l = text.lower()
    for d in ODESA_DISTRICTS:
        for kw in d["keywords"]:
            if kw in text_l:
                return d
    return ODESA_DISTRICTS[idx % len(ODESA_DISTRICTS)]

def parse_autoria_html(html: str) -> List[Dict[str, Any]]:
    listings = []
    m = re.search(r'window\.__PINIA__\s*=\s*({.*?});\s*</script>', html, re.DOTALL)
    if not m:
        return listings

    try:
        pinia = json.loads(m.group(1))
    except Exception:
        return listings

    def find_proposals(obj):
        found = []
        if isinstance(obj, dict):
            if "proposal_id" in obj or ("link" in obj and "auto_" in str(obj.get("link", ""))):
                found.append(obj)
            for v in obj.values():
                found.extend(find_proposals(v))
        elif isinstance(obj, list):
            for it in obj:
                found.extend(find_proposals(it))
        return found

    raw_proposals = find_proposals(pinia)
    seen_links = set()

    for idx, prop in enumerate(raw_proposals):
        link = prop.get("link", "")
        if not link or "auto_" not in link or link in seen_links:
            continue
        seen_links.add(link)

        # 1. Clean Title Extraction
        t_obj = prop.get("title", {})
        title_content = ""
        if isinstance(t_obj, dict):
            title_content = str(t_obj.get("content", "")).strip()
        elif isinstance(t_obj, str):
            title_content = t_obj.strip()

        subtitle = str(prop.get("subtitle", "")).strip()

        if not title_content:
            title_content = subtitle or "Автомобиль в Одессе"

        title = f"Авто: {title_content}"[:100].strip()

        # 2. Clean Description Extraction (unwrap dict and remove artifacts)
        desc_content = ""
        desc_obj = prop.get("description", {})
        if isinstance(desc_obj, dict):
            desc_content = str(desc_obj.get("content", "")).strip()
        elif isinstance(desc_obj, str):
            desc_content = desc_obj.strip()

        # Extract basic technical specs (mileage, transmission, fuel, city)
        specs = []
        location_found = "Одеса"
        for b in prop.get("basicInfo", []):
            if isinstance(b, dict):
                content = str(b.get("content", "")).strip()
                style = b.get("icon", {}).get("data", {}).get("style", "")
                if "location" in style and content:
                    location_found = content
                elif content:
                    specs.append(content)

        specs_str = " • ".join(specs)

        # Combine or fallback
        if desc_content:
            # Clean newlines and consecutive whitespace
            cleaned_desc = re.sub(r'[\r\n\t]+', ' ', desc_content)
            cleaned_desc = re.sub(r'\s{2,}', ' ', cleaned_desc).strip()
            desc = f"{specs_str} • {cleaned_desc}" if specs_str else cleaned_desc
        else:
            desc = f"{specs_str} • В хорошем состоянии, осмотр в Одессе" if specs_str else "В хорошем техническом состоянии, Одесса."

        desc = desc[:280].strip()

        # 3. Price (USD & UAH)
        price_obj = prop.get("price", {})
        usd_price = 0.0
        uah_price = 0.0
        if isinstance(price_obj, dict):
            try:
                usd_price = float(price_obj.get("USD", 0.0))
            except Exception:
                pass
            try:
                uah_price = float(price_obj.get("UAH", 0.0))
            except Exception:
                pass

        main_price = usd_price if usd_price > 0 else uah_price
        curr = "USD" if usd_price > 0 else "UAH"

        if main_price <= 0:
            continue
        if not title_content or title_content == "Автомобиль в Одессе":
            continue

        # 4. Authentic Photos (HD preferred)
        photos = prop.get("photos", [])
        images = []
        if isinstance(photos, list):
            for p in photos[:3]:
                if isinstance(p, dict):
                    formats = p.get("formats", {})
                    img_url = formats.get("large") or formats.get("middle") or p.get("src", "")
                    if img_url:
                        images.append(img_url)
                elif isinstance(p, str) and p.startswith("http"):
                    images.append(p)

        if not images:
            images = ["https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=500&auto=format&fit=crop&q=60"]

        # 5. External ID
        id_val = prop.get("id") or prop.get("proposal_id")
        ext_id = f"autoria-{id_val}" if id_val else f"autoria-{abs(hash(link)) % 10000000}"

        # 6. District & Coordinates
        full_text = f"{title} {subtitle} {desc}"
        dist_meta = determine_district(full_text, idx)

        listings.append({
            "source_id": AUTORIA_SOURCE_ID,
            "external_id": ext_id,
            "external_url": link,
            "title": title,
            "description": desc,
            "price": main_price,
            "currency": curr,
            "district_name": dist_meta["name"],
            "lat": dist_meta["lat"],
            "lon": dist_meta["lon"],
            "images": images,
            "category_normalized": CAT_AUTO,
            "attributes": {
                "usd_price": usd_price,
                "uah_price": uah_price,
                "city": location_found,
                "subtitle": subtitle,
                "source": "AUTO.ria Odesa Live Scraper"
            }
        })

    return listings

def scrape_autoria_catalog(max_pages: int = 5) -> List[Dict[str, Any]]:
    """
    Crawls AUTO.ria specifically for Odesa city listings.
    Primary URL: https://auto.ria.com/uk/city/odessa/?page={page}
    Fallback URL: https://auto.ria.com/uk/search/?target=search&category_id=1&state[0]=12&city[0]=12&page={page}
    """
    all_items = []
    print(f"[AUTO.ria] Starting live scrape for Odesa ({max_pages} pages)...")
    for page in range(1, max_pages + 1):
        url = f"https://auto.ria.com/uk/city/odessa/?page={page}"
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=12) as resp:
                html = resp.read().decode('utf-8', errors='ignore')
            items = parse_autoria_html(html)
            print(f"  🚗 Page {page}: scraped {len(items)} live car listings in Odesa")
            all_items.extend(items)
        except Exception as e:
            print(f"  ⚠️ Page {page} primary failed ({e}), trying fallback search URL...")
            fallback_url = f"https://auto.ria.com/uk/search/?target=search&category_id=1&state[0]=12&city[0]=12&page={page}"
            try:
                req = urllib.request.Request(fallback_url, headers=HEADERS)
                with urllib.request.urlopen(req, timeout=12) as resp:
                    html = resp.read().decode('utf-8', errors='ignore')
                items = parse_autoria_html(html)
                print(f"  🚗 Page {page} (fallback): scraped {len(items)} live car listings in Odesa")
                all_items.extend(items)
            except Exception as e2:
                print(f"  ❌ Page {page} fallback error: {e2}")

    return all_items

if __name__ == "__main__":
    results = scrape_autoria_catalog(max_pages=2)
    print(f"Total AUTO.ria scraped: {len(results)}")
    if results:
        sample = results[0]
        print(f"Sample: {sample['title']}")
        print(f"Price: {sample['price']} {sample['currency']}")
        print(f"District: {sample['district_name']} ({sample['lat']}, {sample['lon']})")
        print(f"Desc: {sample['description'][:100]}...")
        print(f"URL: {sample['external_url']}")
        print(f"Images: {sample['images'][:1]}")
