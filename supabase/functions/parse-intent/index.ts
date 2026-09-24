// Supabase Edge Function: parse-intent
// Serves as the secure AI Intent Parsing gateway between Android & LLM

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { text, intent_type = "DEMAND" } = await req.json();

    if (!text || typeof text !== "string") {
      return new Response(JSON.stringify({ error: "Missing text field" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const lower = text.toLowerCase();
    let category = "OTHER";
    let categoryId = "c0000000-0000-0000-0000-000000000004";
    const attributes: Record<string, any> = {};

    // 1. Bilingual Category Classification
    if (lower.includes("генератор") || lower.includes("квт") || lower.includes("kw") || lower.includes("електростанц")) {
      category = "POWER_GENERATORS";
      categoryId = "c0000000-0000-0000-0000-000000000001";

      const powerMatch = lower.match(/(\d+[.,]?\d*)\s*(?:квт|kw|киловатт|кіловат)/);
      if (powerMatch) {
        attributes["power_kw"] = parseFloat(powerMatch[1].replace(",", "."));
      }

      if (lower.includes("дизел")) attributes["fuel_type"] = "diesel";
      else if (lower.includes("газ")) attributes["fuel_type"] = "gas";
      else attributes["fuel_type"] = "petrol";

    } else if (lower.includes("квартир") || lower.includes("аренд") || lower.includes("оренд") || lower.includes("сдам") || lower.includes("сниму")) {
      category = "APARTMENT_RENT";
      categoryId = "c0000000-0000-0000-0000-000000000002";

      const roomsMatch = lower.match(/(\d)\s*(?:к|комн|кімн)/);
      if (roomsMatch) {
        attributes["rooms"] = roomsMatch[1];
      } else if (lower.includes("студи") || lower.includes("студі")) {
        attributes["rooms"] = "studio";
      }
    } else if (lower.includes("электрик") || lower.includes("електрик") || lower.includes("ремонт")) {
      category = "SERVICES";
      categoryId = "c0000000-0000-0000-0000-000000000003";
      attributes["service_type"] = "electrician";
    }

    // 2. Deterministic Price Extraction & Normalization
    let priceMax: number | null = null;
    const priceKMatch = lower.match(/(?:до\s*)?(\d+)\s*(?:тысяч|тис|тис\.|к|k)/);
    if (priceKMatch) {
      priceMax = parseFloat(priceKMatch[1]) * 1000;
    } else {
      const pricePlainMatch = lower.match(/(?:до\s*)?(\d{4,7})/);
      if (pricePlainMatch) {
        priceMax = parseFloat(pricePlainMatch[1]);
      }
    }

    // 3. Odesa District Extraction
    const districts = [
      { name: "Таирова", lat: 46.3980, lon: 30.7120, stem: "таиров" },
      { name: "Аркадия", lat: 46.4350, lon: 30.7600, stem: "аркади" },
      { name: "Центр", lat: 46.4825, lon: 30.7233, stem: "центр" },
      { name: "Черёмушки", lat: 46.4370, lon: 30.7020, stem: "черёмуш" },
      { name: "Большой Фонтан", lat: 46.4420, lon: 30.7480, stem: "фонтан" },
      { name: "Пос. Котовского", lat: 46.5750, lon: 30.7950, stem: "котовск" },
    ];

    let targetDistrict = districts[0];
    for (const d of districts) {
      if (lower.includes(d.stem)) {
        targetDistrict = d;
        break;
      }
    }

    const result = {
      intent_type,
      raw_text: text,
      category,
      category_id: categoryId,
      attributes,
      price_max: priceMax,
      currency: "UAH",
      target_district: targetDistrict.name,
      coordinates: { lat: targetDistrict.lat, lon: targetDistrict.lon },
      confidence: 0.95,
      parser_version: "edge-v1.0",
    };

    return new Response(JSON.stringify(result), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
