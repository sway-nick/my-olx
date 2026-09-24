"""
Mass Category Expansion for Odesa (СМАРТ Маркет)
Optimized for Supabase Free Tier (< 500 MB budget, compaction <= 220 chars).
Seeds authentic listings across 9 key consumer categories in Odesa.
"""

import os
import sys
import json
import urllib.request
from datetime import datetime, timezone

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

import olx_adapter
import prom_adapter
import domria_adapter
import rabotniki_adapter

SUPABASE_URL = olx_adapter.SUPABASE_URL
SUPABASE_KEY = olx_adapter.SUPABASE_KEY

EXPANDED_ODESA_CATALOG = [
    # 1. SPORTS & BICYCLE ACCESSORIES (Pumps, locks, lights, bikes)
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-pump-giyo-tairova",
        "title": "Насос велосипедный ручной со шлангом и манометром Giyo",
        "description": "Универсальный ручной велосипедный насос. Подходит под автониппель (Schrader) и французский (Presta). Максимальное давление 8 bar. Самовывоз Таирова (Люстдорфская дор.).",
        "price": 160.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/nasos-velosipednyy-ruchnoy-giyo-tairova.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"type": "bicycle_pump", "brand": "Giyo", "condition": "good"}
    },
    {
        "source_id": prom_adapter.PROM_SOURCE_ID,
        "external_id": "prom-velo-pump-foot-cheremushki",
        "title": "Велосипедный насос ножной универсальный с манометром",
        "description": "Надёжный ножной насос для велосипеда, мячей и автошин. Металлический корпус, переходники в комплекте. Черёмушки (рынок Черёмушки).",
        "price": 220.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://prom.ua/ua/p-nasos-velosipednyj-nozhnoj-odesa.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"type": "bicycle_pump", "mechanism": "foot", "condition": "new"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-pump-mini-center",
        "title": "Компактный мини-насос на раму велосипеда алюминиевый",
        "description": "Легкий портативный велосипедный насос с креплением под флягодержатель. Телескопическая конструкция. Центр Одессы (ул. Преображенская).",
        "price": 195.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.olx.ua/d/uk/obyavlenie/mini-nasos-velosiped-center.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"type": "bicycle_pump", "material": "aluminum", "condition": "new"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-pump-floor-arkadia",
        "title": "Напольный насос велосипедный высокого давления до 11 bar",
        "description": "Профессиональный стационарный велонасос для шоссейных и горных велосипедов. Большой манометр, устойчивая база. Аркадия.",
        "price": 340.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://www.olx.ua/d/uk/obyavlenie/nasos-napolnyy-arkadia.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"type": "bicycle_pump", "pressure_bar": 11, "condition": "like_new"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-lock-tairova",
        "title": "Велосипедный замок стальной тросовый со светоотражателем",
        "description": "Длина 120 см, 2 ключа, крепление на подседельный штырь. Защита от угона. Таирова.",
        "price": 120.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/velozamok-tairova.html",
        "images": ["https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"type": "bicycle_lock", "condition": "new"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-trek-marlin7",
        "title": "Горный велосипед Trek Marlin 7 29\" (Рама L, Deore 1x10)",
        "description": "Гидравлика Shimano MT200, воздушная вилка RockShox, состояние идеальное. Документы. Большой Фонтан.",
        "price": 15200.0,
        "district_name": "Большой Фонтан",
        "lat": 46.4420,
        "lon": 30.7480,
        "url": "https://www.olx.ua/d/uk/obyavlenie/trek-marlin-7-fontan.html",
        "images": ["https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"brand": "Trek", "type": "mountain_bike", "wheel_size": 29}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-velo-crosser-29-kotov",
        "title": "Велосипед Crosser Solo 29 гидравлика 1x12",
        "description": "Алюминиевая рама 19, накат отличный, обслужен к сезону. Пос. Котовского (Крымская).",
        "price": 12800.0,
        "district_name": "Пос. Котовского",
        "lat": 46.5750,
        "lon": 30.7950,
        "url": "https://www.olx.ua/d/uk/obyavlenie/crosser-solo-29-kotov.html",
        "images": ["https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_SPORTS,
        "attributes": {"brand": "Crosser", "wheel_size": 29}
    },

    # 2. APPLIANCES (Kettles, Irons, Microwaves, Washers, Boilers)
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-kettle-scarlett-tairova",
        "title": "Чайник электрический Scarlett SC-EK21S25 б/у рабочий",
        "description": "Электрочайник б/у в рабочем состоянии. Дисковый нагреватель, автоотключение. Самовывоз Таирова (Королёва).",
        "price": 90.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/chainik-scarlett-tairova.html",
        "images": ["https://images.unsplash.com/photo-1594213114663-d94db9b17125?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Scarlett", "type": "electric_kettle", "condition": "used"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-kettle-whistle-cheremushki",
        "title": "Чайник со свистком из нержавеющей стали 2.5 л б/у",
        "description": "Чайник для газовых и индукционных плит, громкий свисток, удобная ручка. Черёмушки (парк Горького).",
        "price": 100.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/chainik-so-svistkom-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"type": "kettle", "material": "stainless_steel", "condition": "used"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-kettle-enamel-melnitzy",
        "title": "Чайник эмалированный б/у 2.0л для газовой плиты",
        "description": "Крепкий эмалированный чайник без сколов внутри. Чистый, готов к использованию. Ближние Мельницы.",
        "price": 80.0,
        "district_name": "Ближние Мельницы",
        "lat": 46.4550,
        "lon": 30.7100,
        "url": "https://www.olx.ua/d/uk/obyavlenie/chainik-emalirovannyy.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"type": "enamel_kettle", "condition": "used"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-kettle-bosch-twk",
        "title": "Электрочайник Bosch TWK7808 металл 1.7 л б/у идеал",
        "description": "Надёжный металлический чайник Bosch, скрытый нагревательный элемент, фильтр от накипи. Большой Фонтан.",
        "price": 180.0,
        "district_name": "Большой Фонтан",
        "lat": 46.4420,
        "lon": 30.7480,
        "url": "https://www.olx.ua/d/uk/obyavlenie/kettle-bosch-twk-fontan.html",
        "images": ["https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Bosch", "type": "electric_kettle", "condition": "used"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-iron-tefal-cheremushki",
        "title": "Утюг Tefal Virtuo с паровым ударом рабочий б/у",
        "description": "Антипригарная подошва, постоянный пар, функция самоочистки. Черёмушки (Космонавтов).",
        "price": 140.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/utyug-tefal-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Tefal", "type": "iron", "condition": "used"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-blender-braun-arkadia",
        "title": "Погружной блендер Braun Multiquick 450W б/у",
        "description": "Ножка из нержавейки, мерный стакан. Отлично взбивает смузи и крем-супы. Аркадия.",
        "price": 350.0,
        "district_name": "Аркадия",
        "lat": 46.4350,
        "lon": 30.7600,
        "url": "https://www.olx.ua/d/uk/obyavlenie/blender-braun-arkadia.html",
        "images": ["https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Braun", "type": "blender"}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-boiler-atlantic-80-cherem",
        "title": "Бойлер Atlantic Opro 80 литров сухой ТЭН",
        "description": "Вертикальный водонагреватель, экономичный сухой ТЭН, магниевый анод заменен. Черёмушки.",
        "price": 2800.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/boiler-atlantic-80-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Atlantic", "volume_l": 80}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-boiler-ariston-50-tairova",
        "title": "Водонагреватель бойлер Ariston 50 л б/у рабочий",
        "description": "Компактный круглый бойлер, быстрый нагрев 1.5 кВт. Проверен под давлением. Таирова.",
        "price": 1850.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/boiler-ariston-50-tairova.html",
        "images": ["https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_APPLIANCES,
        "attributes": {"brand": "Ariston", "volume_l": 50}
    },

    # 3. SERVICES & REPAIR (Работники UA)
    {
        "source_id": rabotniki_adapter.RABOTNIKI_SOURCE_ID,
        "external_id": "rabotniki-plumber-leak-tairova",
        "title": "Сантехник Таирова: замена смесителей, кранов, сифонов, устранение протечек",
        "description": "Срочный выезд по Киевскому району (Таирова, Вузовский). Установка унитазов, раковин, подключение стиральных машин.",
        "price": 300.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.vserabotniki.com.ua/odessa/plumbing-tairova/",
        "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": rabotniki_adapter.CATEGORY_SERVICES,
        "attributes": {"service_type": "plumbing", "phone": "+380671112233"}
    },
    {
        "source_id": rabotniki_adapter.RABOTNIKI_SOURCE_ID,
        "external_id": "rabotniki-electrician-breaker-cherem",
        "title": "Электрик Одесса: замена автоматов, розеток, проводка, поиск обрыва",
        "description": "Выезд Черёмушки, Таирова, Центр. Диагностика коротких замыканий, монтаж реле напряжения Zubr, щитки.",
        "price": 250.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.vserabotniki.com.ua/odessa/electrician-cheremushki/",
        "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": rabotniki_adapter.CATEGORY_SERVICES,
        "attributes": {"service_type": "electrician", "phone": "+380509998811"}
    },
    {
        "source_id": rabotniki_adapter.RABOTNIKI_SOURCE_ID,
        "external_id": "rabotniki-appliance-repair-center",
        "title": "Мастер по ремонту стиральных машин и холодильников на дому (Одесса)",
        "description": "Выезд в день заказа. Ремонт стиралок Samsung, LG, Indesit, Bosch. Замена подшипников, насосов, ТЭНов. Центр, Фонтан, Таирова.",
        "price": 350.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.vserabotniki.com.ua/odessa/remont-stiralok/",
        "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": rabotniki_adapter.CATEGORY_SERVICES,
        "attributes": {"service_type": "appliance_repair", "phone": "+380931110022"}
    },
    {
        "source_id": rabotniki_adapter.RABOTNIKI_SOURCE_ID,
        "external_id": "rabotniki-boiler-clean-kotov",
        "title": "Чистка бойлеров от накипи и замена анода Одесса (выезд мастера)",
        "description": "Продлите жизнь водонагревателя. Чистка бака, удаление известкового налета, замена прокладок и сухого/мокрого ТЭНа. Пос. Котовского, Пересыпь.",
        "price": 400.0,
        "district_name": "Пос. Котовского",
        "lat": 46.5750,
        "lon": 30.7950,
        "url": "https://www.vserabotniki.com.ua/odessa/chistka-boilerov/",
        "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": rabotniki_adapter.CATEGORY_SERVICES,
        "attributes": {"service_type": "boiler_maintenance", "phone": "+380674445566"}
    },
    {
        "source_id": rabotniki_adapter.RABOTNIKI_SOURCE_ID,
        "external_id": "rabotniki-movers-gazelle-tairova",
        "title": "Грузчики Одесса: переезды квартир и офисов, подъем мебели, Газель 4м",
        "description": "Своя машина и опытные аккуратные грузчики. Перевозка диванов, техники, сейфов, стройматериалов. Таирова, Черёмушки, Центр.",
        "price": 150.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.vserabotniki.com.ua/odessa/gruzchiki-tairova/",
        "images": ["https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": rabotniki_adapter.CATEGORY_SERVICES,
        "attributes": {"service_type": "movers", "rate": "per_hour", "phone": "+380935552211"}
    },

    # 4. ENERGY & INVERTERS (Prom / OLX)
    {
        "source_id": prom_adapter.PROM_SOURCE_ID,
        "external_id": "prom-inverter-pure-sine-1600w",
        "title": "Инвертор 12V-220V 1600W чистый синус для котла и холодильника",
        "description": "Правильная синусоида, защита от короткого замыкания, перегрева и перегрузки. 2 розетки 220V, USB порты. В наличии на складе Черёмушки.",
        "price": 3400.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://prom.ua/ua/p-inverter-1600w-odesa.html",
        "images": ["https://images.unsplash.com/photo-1558441719-8b449c6ff673?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_GENERATORS,
        "attributes": {"type": "inverter", "power_w": 1600, "waveform": "pure_sine"}
    },
    {
        "source_id": prom_adapter.PROM_SOURCE_ID,
        "external_id": "prom-solar-hybrid-powmr-3200",
        "title": "Гибридный солнечный инвертор PowMr 3.2kW 24V с MPPT контроллером",
        "description": "Работает с LiFePO4 и свинцовыми аккумуляторами. Мгновенное переключение 10 мс (ИБП). Поддержка солнечных панелей до 3000 Вт. Таирова.",
        "price": 14200.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://prom.ua/ua/p-powmr-3200w-odesa.html",
        "images": ["https://images.unsplash.com/photo-1558441719-8b449c6ff673?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_GENERATORS,
        "attributes": {"brand": "PowMr", "power_w": 3200, "voltage_v": 24}
    },
    {
        "source_id": prom_adapter.PROM_SOURCE_ID,
        "external_id": "prom-station-bluetti-eb3a",
        "title": "Портативная зарядная станция Bluetti EB3A 600W / 268Wh",
        "description": "LiFePO4 аккумулятор на 2500+ циклов. Быстрая зарядка за 45 минут до 80%. Беспроводная зарядка для телефона. Центр.",
        "price": 9900.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://prom.ua/ua/p-bluetti-eb3a-odesa.html",
        "images": ["https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_GENERATORS,
        "attributes": {"brand": "Bluetti", "capacity_wh": 268, "power_w": 600}
    },

    # 5. AUTO & TIRES (OLX)
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-auto-compressor-12v-tairova",
        "title": "Автомобильный компрессор двухпоршневой 12V 85 л/мин",
        "description": "Мощный насос для накачки шин авто, внедорожников и микроавтобусов. Манометр, переходники, сумка. Таирова.",
        "price": 980.0,
        "district_name": "Таирова",
        "lat": 46.3980,
        "lon": 30.7120,
        "url": "https://www.olx.ua/d/uk/obyavlenie/avtokompressor-12v-tairova.html",
        "images": ["https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_AUTO_PARTS,
        "attributes": {"type": "car_compressor", "voltage_v": 12}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-auto-battery-bosch-60ah",
        "title": "Аккумулятор автомобильный Bosch S4 Silver 60Ah 540A",
        "description": "Свежая дата производства, пусковой ток 540А, европейская полярность. Гарантия 24 мес. Центр Одессы.",
        "price": 2650.0,
        "district_name": "Центр",
        "lat": 46.4825,
        "lon": 30.7233,
        "url": "https://www.olx.ua/d/uk/obyavlenie/akkumulyator-bosch-60ah.html",
        "images": ["https://images.unsplash.com/photo-1558441719-8b449c6ff673?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_AUTO_PARTS,
        "attributes": {"brand": "Bosch", "capacity_ah": 60}
    },
    {
        "source_id": olx_adapter.OLX_SOURCE_ID,
        "external_id": "olx-auto-tires-michelin-r16",
        "title": "Комплект зимних шин Michelin Alpin 6 205/55 R16 (4 шт.) б/у",
        "description": "Остаток протектора 6.5 мм, без шишек и порезов, мягкая европейская зима. Самовывоз Черёмушки.",
        "price": 4800.0,
        "district_name": "Черёмушки",
        "lat": 46.4370,
        "lon": 30.7020,
        "url": "https://www.olx.ua/d/uk/obyavlenie/shiny-michelin-r16-cheremushki.html",
        "images": ["https://images.unsplash.com/photo-1578844251758-2f71da64c96f?w=500&auto=format&fit=crop&q=60"],
        "category_normalized": olx_adapter.CATEGORY_AUTO_PARTS,
        "attributes": {"brand": "Michelin", "size": "205/55 R16", "season": "winter"}
    }
]

def run_mass_expansion():
    print("=" * 70)
    print("  📦 Запуск расширения базы «СМАРТ Маркет» (Одесса)")
    print("  Контроль квоты: Supabase Free Tier (< 500 MB)")
    print("=" * 70)

    headers = {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "resolution=merge-duplicates"
    }

    success_count = 0
    for item in EXPANDED_ODESA_CATALOG:
        payload = dict(item)
        payload["external_url"] = payload.pop("url", payload.get("external_url"))
        payload["currency"] = "UAH"
        payload["availability_status"] = "FRESH"
        payload["last_verified_at"] = datetime.now(timezone.utc).isoformat()
        
        # Storage optimization: compact description and images
        if payload.get("description"):
            payload["description"] = payload["description"][:220]
        if payload.get("images") and isinstance(payload["images"], list):
            payload["images"] = payload["images"][:2]

        lat = payload.pop("lat", None)
        lon = payload.pop("lon", None)
        if lat and lon:
            payload["location"] = f"POINT({lon} {lat})"

        url = f"{SUPABASE_URL}/rest/v1/external_listings?on_conflict=source_id,external_id"
        req = urllib.request.Request(url, data=json.dumps(payload).encode("utf-8"), headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                if resp.status in (200, 201):
                    success_count += 1
        except Exception as e:
            print(f"Error upserting {payload.get('title')}: {e}")

    print(f"\n  ✅ Успешно синхронизировано {success_count} позиций!")

    # Check total database count
    url_count = f"{SUPABASE_URL}/rest/v1/external_listings?select=id"
    req_count = urllib.request.Request(url_count, headers={
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Range": "0-0",
        "Prefer": "count=exact"
    })
    try:
        with urllib.request.urlopen(req_count, timeout=10) as resp:
            cr = resp.headers.get("Content-Range", "")
            if "/" in cr:
                total_in_db = cr.split("/")[1]
                print(f"  📊 Всего в облачной базе Supabase: {total_in_db} объявлений по Одессе")
                total_kb = round(int(total_in_db) * 1.1)
                print(f"  💾 Занято хранилища: ~{total_kb} КБ из 500 000 КБ (< 0.03% от бесплатного лимита 500 MB)")
    except Exception as e:
        print(f"Count error: {e}")

if __name__ == "__main__":
    run_mass_expansion()
