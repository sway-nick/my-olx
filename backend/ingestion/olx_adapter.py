"""
OLX Ingestion Adapter for Odesa (Generators & Apartments)
Fetches public listing feeds from OLX for Odesa, normalizes parameters,
and upserts records into Supabase `external_listings`.
"""

import os
import re
import json
import urllib.request
import urllib.parse
from datetime import datetime, timezone
from typing import List, Dict, Any

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://gpqjuwcfdkqdmyxplfbs.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY", "")

OLX_SOURCE_ID = "e0000000-0000-0000-0000-000000000001"
CATEGORY_GENERATORS = "c0000000-0000-0000-0000-000000000001"
CATEGORY_APARTMENTS = "c0000000-0000-0000-0000-000000000002"

ODESA_DISTRICTS_COORDS = {
    "таирова": (46.3980, 30.7120),
    "киевский": (46.4150, 30.7250),
    "аркадия": (46.4350, 30.7600),
    "приморский": (46.4825, 30.7233),
    "центр": (46.4825, 30.7233),
    "черёмушки": (46.4370, 30.7020),
    "малиновский": (46.4550, 30.7100),
    "хаджибейский": (46.4550, 30.7100),
    "котовского": (46.5750, 30.7950),
    "пересыпский": (46.5750, 30.7950),
    "суворовский": (46.5750, 30.7950),
    "фонтан": (46.4420, 30.7480)
}

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36"
]

def fetch_olx_html(url: str) -> str:
    headers = {
        "User-Agent": USER_AGENTS[0],
        "Accept-Language": "uk-UA,uk;q=0.9,ru;q=0.8,en;q=0.7",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.read().decode("utf-8", errors="ignore")
    except Exception as e:
        print(f"[OLX Adapter] Error fetching {url}: {e}")
        return ""

def parse_olx_data(html: str, target_category_id: str) -> List[Dict[str, Any]]:
    listings = []

    # 1. Try extracting structured __NEXT_DATA__ JSON from OLX page
    next_data_match = re.search(r'<script id="__NEXT_DATA__" type="application/json">({.*?})</script>', html)
    if next_data_match:
        try:
            data = json.loads(next_data_match.group(1))
            ads = (
                data.get("props", {})
                    .get("pageProps", {})
                    .get("data", {})
                    .get("listing", {})
                    .get("listing", {})
                    .get("ads", [])
            )
            for ad in ads:
                title = ad.get("title", "")
                external_id = str(ad.get("id", ""))
                url = ad.get("url", "")
                if url and not url.startswith("http"):
                    url = f"https://www.olx.ua{url}"

                # Price extraction
                price_info = ad.get("price", {})
                price_val = None
                for param in ad.get("params", []):
                    if param.get("key") == "price":
                        price_val = param.get("value", {}).get("value")
                if not price_val and "value" in price_info:
                    price_val = price_info.get("value")

                # Location & District
                location_data = ad.get("location", {})
                district_name = location_data.get("districtName") or location_data.get("cityName") or "Одесса"

                # Coordinates
                lat = location_data.get("latitude")
                lon = location_data.get("longitude")
                if not lat or not lon:
                    # Fallback to Odesa district coordinates
                    d_lower = district_name.lower()
                    for k, (dlat, dlon) in ODESA_DISTRICTS_COORDS.items():
                        if k in d_lower:
                            lat, lon = dlat, dlon
                            break
                    if not lat:
                        lat, lon = 46.4825, 30.7233

                # Photos
                photos = [photo.get("link", "").replace("{width}", "600").replace("{height}", "450")
                          for photo in ad.get("photos", []) if photo.get("link")]

                # Attributes (Power, fuel, etc.)
                attrs = {}
                title_lower = title.lower()
                power_match = re.search(r"(\d+[.,]?\d*)\s*(?:квт|kw|киловатт)", title_lower)
                if power_match:
                    attrs["power_kw"] = float(power_match.group(1).replace(",", "."))
                if "дизел" in title_lower:
                    attrs["fuel_type"] = "diesel"
                elif "газ" in title_lower:
                    attrs["fuel_type"] = "gas"
                elif "бензин" in title_lower:
                    attrs["fuel_type"] = "petrol"

                if external_id and title:
                    listings.append({
                        "source_id": OLX_SOURCE_ID,
                        "external_id": f"olx-{external_id}",
                        "external_url": url,
                        "title": title,
                        "description": ad.get("description") or title,
                        "price": float(price_val) if price_val else None,
                        "currency": "UAH",
                        "district_name": district_name,
                        "lat": float(lat),
                        "lon": float(lon),
                        "images": photos[:5],
                        "category_normalized": target_category_id,
                        "attributes": attrs,
                        "availability_status": "FRESH"
                    })
            if listings:
                print(f"[OLX Adapter] Extracted {len(listings)} listings via __NEXT_DATA__")
                return listings
        except Exception as e:
            print(f"[OLX Adapter] Error parsing __NEXT_DATA__: {e}")

    # 2. Fallback heuristic HTML parser if __NEXT_DATA__ not present
    card_pattern = re.compile(r'<div data-cy="l-card".*?<a href="([^"]+)".*?<h4[^>]*>([^<]+)</h4>.*?<p data-testid="ad-price"[^>]*>([^<]+)</p>', re.DOTALL)
    for match in card_pattern.finditer(html):
        url, title, price_str = match.groups()
        price_num = re.sub(r"[^\d]", "", price_str)
        ext_id = re.search(r"-ID([a-zA-Z0-9]+)\.html", url)
        external_id = ext_id.group(1) if ext_id else str(hash(url))

        full_url = f"https://www.olx.ua{url}" if not url.startswith("http") else url

        listings.append({
            "source_id": OLX_SOURCE_ID,
            "external_id": f"olx-{external_id}",
            "external_url": full_url,
            "title": title.strip(),
            "description": title.strip(),
            "price": float(price_num) if price_num else None,
            "currency": "UAH",
            "district_name": "Таирова",
            "lat": 46.3980,
            "lon": 30.7120,
            "images": [],
            "category_normalized": target_category_id,
            "attributes": {},
            "availability_status": "FRESH"
        })

    return listings

def upsert_to_supabase(listings: List[Dict[str, Any]]) -> int:
    if not listings:
        return 0

    inserted_count = 0
    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }

    for item in listings:
        # Build PostGIS point
        lat = item.pop("lat")
        lon = item.pop("lon")
        
        # Format payload
        payload = dict(item)
        # Add timestamp
        payload["last_verified_at"] = datetime.now(timezone.utc).isoformat()
        
        url = f"{SUPABASE_URL}/rest/v1/external_listings"
        req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                if resp.status in (200, 201):
                    inserted_count += 1
        except Exception as e:
            # Silently continue on duplicate/error
            pass

    return inserted_count

def run_sync():
    print("[OLX Ingestion] Starting scheduled sync for Odesa...")
    
    # 1. Fetch Generators in Odesa
    gen_url = "https://www.olx.ua/uk/dom-i-sad/stroitelstvo-remont/elektroinstrument/odessa/?q=%D0%B3%D0%B5%D0%BD%D0%B5%D1%80%D0%B0%D1%82%D0%BE%D1%80"
    html_gen = fetch_olx_html(gen_url)
    if html_gen:
        gen_listings = parse_olx_data(html_gen, CATEGORY_GENERATORS)
        print(f"[OLX Ingestion] Found {len(gen_listings)} generators in Odesa")
        res = upsert_to_supabase(gen_listings)
        print(f"[OLX Ingestion] Saved {res} generators to Supabase")

    # 2. Fetch Apartment Rentals in Odesa
    rent_url = "https://www.olx.ua/uk/nedvizhimost/kvartiry/dolgosrochnaya-arenda-kvartir/odessa/"
    html_rent = fetch_olx_html(rent_url)
    if html_rent:
        rent_listings = parse_olx_data(html_rent, CATEGORY_APARTMENTS)
        print(f"[OLX Ingestion] Found {len(rent_listings)} apartment rentals in Odesa")
        res = upsert_to_supabase(rent_listings)
        print(f"[OLX Ingestion] Saved {res} apartments to Supabase")

if __name__ == "__main__":
    run_sync()
