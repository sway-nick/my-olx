from fastapi import FastAPI, Query
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import re
import math

app = FastAPI(
    title="IntentMarket API",
    description="Backend API for AI Marketplace Aggregator & Intent Matching",
    version="1.0.0"
)

# Odesa Districts
ODESA_DISTRICTS = [
    {"id": "tairova", "name": "Таирова", "lat": 46.3980, "lon": 30.7120},
    {"id": "arcadia", "name": "Аркадия", "lat": 46.4350, "lon": 30.7600},
    {"id": "center", "name": "Центр", "lat": 46.4825, "lon": 30.7233},
    {"id": "cheremushki", "name": "Черёмушки", "lat": 46.4370, "lon": 30.7020},
    {"id": "fontan", "name": "Большой Фонтан", "lat": 46.4420, "lon": 30.7480},
    {"id": "kotovskogo", "name": "Пос. Котовского", "lat": 46.5750, "lon": 30.7950}
]

# Curated Listings Pool in Odesa
LISTINGS_POOL = [
    {
        "id": "gen-1",
        "title": "Бензиновый генератор Honda 5.5 кВт",
        "description": "Отличное состояние, медная обмотка, электростартер. Работал 15 моточасов. Самовывоз Таирова.",
        "category": "POWER_GENERATORS",
        "price": 35000.0,
        "currency": "грн",
        "district": ODESA_DISTRICTS[0],
        "is_external": False,
        "source_name": "На нашей площадке",
        "phone": "+380671234567",
        "image_url": "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500",
        "attributes": {"power_kw": "5.5", "fuel": "petrol"}
    },
    {
        "id": "gen-2",
        "title": "Генератор Daewoo 5.0 кВт бензин",
        "description": "Новый в коробке, гарантия 1 год. 2 розетки 220V, AVR стабилизатор.",
        "category": "POWER_GENERATORS",
        "price": 38500.0,
        "currency": "грн",
        "district": ODESA_DISTRICTS[3],
        "is_external": True,
        "source_name": "OLX",
        "source_url": "https://olx.ua/d/obyavlenie/generator-daewoo-5kw-ID123.html",
        "image_url": "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=500",
        "attributes": {"power_kw": "5.0", "fuel": "petrol"}
    },
    {
        "id": "rent-1",
        "title": "Аренда 2к квартиры в Аркадии с видом на море",
        "description": "ЖК 36 Жемчужина. Евроремонт, вся техника, посудомойка, генератор в доме! 14 этаж.",
        "category": "APARTMENT_RENT",
        "price": 15000.0,
        "currency": "грн",
        "district": ODESA_DISTRICTS[1],
        "is_external": False,
        "source_name": "На нашей площадке",
        "phone": "+380631112233",
        "image_url": "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=500",
        "attributes": {"rooms": "2", "floor": "14"}
    }
]

def calculate_haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2 +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) *
         math.sin(d_lon / 2) ** 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return round(r * c, 1)

class ParseRequest(BaseModel):
    text: str
    intent_type: str = "DEMAND"

class MatchRequest(BaseModel):
    category: str
    user_district_id: str
    max_price: Optional[float] = None
    keywords: Optional[str] = None

@app.get("/")
def read_root():
    return {"status": "ok", "service": "IntentMarket API", "version": "1.0.0"}

@app.get("/api/v1/districts")
def get_districts():
    return ODESA_DISTRICTS

@app.post("/api/v1/intents/parse")
def parse_intent(req: ParseRequest):
    text_lower = req.text.lower()
    attributes = {}

    # Category Detection
    if any(k in text_lower for k in ["генератор", "квт", "kw", "бензогенератор"]):
        category = "POWER_GENERATORS"
        power_match = re.search(r"(\d+[.,]?\d*)\s*(?:квт|kw|киловатт)", text_lower)
        if power_match:
            attributes["power_kw"] = power_match.group(1).replace(",", ".")
        if "дизел" in text_lower:
            attributes["fuel"] = "diesel"
        elif "газ" in text_lower:
            attributes["fuel"] = "gas"
        else:
            attributes["fuel"] = "petrol"
    elif any(k in text_lower for k in ["квартир", "аренд", "оренд", "сдам", "сниму"]):
        category = "APARTMENT_RENT"
        rooms_match = re.search(r"(\d)\s*(?:к|комн|кімн)", text_lower)
        if rooms_match:
            attributes["rooms"] = rooms_match.group(1)
        elif "студи" in text_lower:
            attributes["rooms"] = "1 (студия)"
    else:
        category = "OTHER"

    # Price Detection
    price_max = None
    price_k_match = re.search(r"(?:до\s*)?(\d+)\s*(?:тысяч|тис|к|k)", text_lower)
    if price_k_match:
        price_max = float(price_k_match.group(1)) * 1000
    else:
        price_plain_match = re.search(r"(?:до\s*)?(\d{4,7})", text_lower)
        if price_plain_match:
            price_max = float(price_plain_match.group(1))

    # District Detection
    district_found = ODESA_DISTRICTS[0]
    for d in ODESA_DISTRICTS:
        stem = d["name"][:5].lower()
        if stem in text_lower:
            district_found = d
            break

    return {
        "intent_type": req.intent_type,
        "category": category,
        "attributes": attributes,
        "price_max": price_max,
        "target_district": district_found
    }

@app.post("/api/v1/matches")
def find_matches(req: MatchRequest):
    user_district = next((d for d in ODESA_DISTRICTS if d["id"] == req.user_district_id), ODESA_DISTRICTS[0])
    
    results = []
    for item in LISTINGS_POOL:
        if item["category"] != req.category:
            continue
        dist_km = calculate_haversine_distance(
            user_district["lat"], user_district["lon"],
            item["district"]["lat"], item["district"]["lon"]
        )
        
        # Scoring & Grading
        is_price_ok = (req.max_price is None) or (item["price"] <= req.max_price)
        if is_price_ok and dist_km <= 3.0:
            grade = "EXCELLENT"
        elif dist_km <= 7.0:
            grade = "GOOD"
        else:
            grade = "PARTIAL"

        entry = dict(item)
        entry["distance_km"] = dist_km
        entry["match_grade"] = grade
        results.append(entry)

    # Location-First Sorting
    results.sort(key=lambda x: (x["match_grade"] != "EXCELLENT", x["distance_km"], x["price"]))
    return {"matches": results, "count": len(results), "user_district": user_district}
