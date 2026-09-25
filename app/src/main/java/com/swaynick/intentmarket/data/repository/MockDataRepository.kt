package com.swaynick.intentmarket.data.repository

import com.swaynick.intentmarket.domain.model.Category
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.ListingItem
import com.swaynick.intentmarket.domain.model.MatchGrade
import kotlin.math.*

object MockDataRepository {

    val ODESA_ADMIN_AREAS = listOf(
        com.swaynick.intentmarket.domain.model.AdministrativeArea(
            id = "kyivskyi",
            name = "Киевский район",
            subdistricts = listOf(
                District("tairova", "Таирова", 46.3980, 30.7120, "Киевский район"),
                District("vuzivskyi", "Вузовский", 46.4150, 30.7200, "Киевский район"),
                District("chubaivka", "Чубаевка", 46.4250, 30.7200, "Киевский район"),
                District("chernomorka", "Черноморка", 46.3400, 30.7100, "Киевский район")
            )
        ),
        com.swaynick.intentmarket.domain.model.AdministrativeArea(
            id = "prymorskyi",
            name = "Приморский район",
            subdistricts = listOf(
                District("arcadia", "Аркадия", 46.4350, 30.7600, "Приморский район"),
                District("center", "Центр", 46.4825, 30.7233, "Приморский район"),
                District("fontan", "Большой Фонтан", 46.4420, 30.7480, "Приморский район"),
                District("frantsuzskyi", "Французский бульвар", 46.4550, 30.7550, "Приморский район"),
                District("moldavanka", "Молдаванка", 46.4700, 30.7100, "Приморский район")
            )
        ),
        com.swaynick.intentmarket.domain.model.AdministrativeArea(
            id = "khadzhybeyskyi",
            name = "Хаджибейский район",
            subdistricts = listOf(
                District("cheremushki", "Черёмушки", 46.4370, 30.7020, "Хаджибейский район"),
                District("zastava", "Застава", 46.4650, 30.6800, "Хаджибейский район"),
                District("slobodka", "Слободка", 46.4950, 30.7050, "Хаджибейский район"),
                District("lenposelok", "Ленпосёлок", 46.4750, 30.6400, "Хаджибейский район")
            )
        ),
        com.swaynick.intentmarket.domain.model.AdministrativeArea(
            id = "peresypskyi",
            name = "Пересыпский район",
            subdistricts = listOf(
                District("kotovskogo", "Пос. Котовского", 46.5750, 30.7950, "Пересыпский район"),
                District("luzanivka", "Лузановка", 46.5450, 30.7550, "Пересыпский район"),
                District("peresyp", "Пересыпь", 46.5050, 30.7250, "Пересыпский район")
            )
        )
    )

    val ODESA_DISTRICTS: List<District> = ODESA_ADMIN_AREAS.flatMap { it.subdistricts }

    fun findClosestDistrict(lat: Double, lon: Double): District {
        return ODESA_DISTRICTS.minByOrNull { calculateDistance(lat, lon, it.lat, it.lon) } ?: ODESA_DISTRICTS[0]
    }

    private val LISTINGS_POOL = listOf(
        // POWER GENERATORS & ENERGY
        ListingItem(
            id = "gen-1",
            title = "Бензиновый генератор Hyundai 5.5 кВт HHY7050FE",
            description = "Отличное состояние, медная обмотка, электростартер. Работал 15 моточасов. Самовывоз Таирова.",
            category = Category.POWER_GENERATORS,
            price = 21000.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.2,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380671234567",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("power_kw" to "5.5", "fuel" to "petrol", "starter" to "electric"),
            isHotDeal = true,
            discountPct = 27,
            unitMetricComparison = "3 818 грн/кВт (медиана 5 250 грн/кВт)"
        ),
        ListingItem(
            id = "gen-2",
            title = "Генератор Daewoo 5.0 кВт бензин GDA 6500E",
            description = "Новый в коробке, гарантия 1 год. 2 розетки 220V, AVR стабилизатор.",
            category = Category.POWER_GENERATORS,
            price = 38500.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 4.5,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/generator-daewoo-5kw-ID123.html",
            imageUrl = "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("power_kw" to "5.0", "fuel" to "petrol")
        ),
        ListingItem(
            id = "gen-3",
            title = "Зарядная станция EcoFlow RIVER 2 Pro 768Wh",
            description = "Быстрая зарядка за 70 мин, LiFePO4 батарея на 3000 циклов. Официальная гарантия.",
            category = Category.POWER_GENERATORS,
            price = 23900.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 6.2,
            isExternal = true,
            sourceName = "Prom",
            sourceUrl = "https://prom.ua/p12345-ecoflow.html",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("capacity_wh" to "768")
        ),

        // SMARTPHONES & TABLETS
        ListingItem(
            id = "phone-1",
            title = "Apple iPhone 15 Pro 128GB Natural Titanium",
            description = "Идеальное состояние, 100% батарея, Neverlock. Комплект с коробкой и чехлом.",
            category = Category.SMARTPHONES,
            price = 28500.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.1,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/iphone-15-pro-ID888.html",
            imageUrl = "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Apple", "memory_gb" to "128"),
            isHotDeal = true,
            discountPct = 28,
            unitMetricComparison = "28 500 грн (медиана по Одессе 39 500 грн)"
        ),
        ListingItem(
            id = "phone-2",
            title = "Samsung Galaxy S24 Ultra 12/256GB Titanium Gray",
            description = "Официал, Snapdragon 8 Gen 3, гарантия до конца года. В защитной пленке.",
            category = Category.SMARTPHONES,
            price = 37000.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.4,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380671112233",
            imageUrl = "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("brand" to "Samsung", "memory_gb" to "256")
        ),
        ListingItem(
            id = "phone-3",
            title = "Apple iPhone 11 64GB Black (Neverlock)",
            description = "Отличное состояние, Face ID и True Tone работают, батарея 85%. Комплект: телефон, провод, чехол.",
            category = Category.SMARTPHONES,
            price = 8400.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 2.1,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/iphone-11-black-ID901.html",
            imageUrl = "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Apple", "memory_gb" to "64"),
            isHotDeal = true,
            discountPct = 26,
            unitMetricComparison = "8 400 грн (медиана по Одессе 11 500 грн)"
        ),
        ListingItem(
            id = "phone-4",
            title = "Samsung Galaxy A54 5G 8/128GB Awesome Graphite",
            description = "Экран 120Hz Super AMOLED, стереозвук, батарея 5000 mAh. Полный магазинный комплект с гарантией.",
            category = Category.SMARTPHONES,
            price = 7900.0,
            district = ODESA_DISTRICTS[3], // Черемушки
            distanceKm = 3.5,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380679998877",
            imageUrl = "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Samsung", "memory_gb" to "128"),
            isHotDeal = true,
            discountPct = 30,
            unitMetricComparison = "7 900 грн (медиана по Одессе 11 200 грн)"
        ),
        ListingItem(
            id = "phone-5",
            title = "Xiaomi Redmi Note 12 Pro 8/256GB Midnight Black",
            description = "Камера 50MP Sony IMX766 с OIS, быстрая зарядка 67W Turbo Charge. Идеальное состояние.",
            category = Category.SMARTPHONES,
            price = 6700.0,
            district = ODESA_DISTRICTS[5], // Поселок Котовского
            distanceKm = 8.2,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/redmi-note-12-pro-ID902.html",
            imageUrl = "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Xiaomi", "memory_gb" to "256")
        ),
        ListingItem(
            id = "phone-6",
            title = "Google Pixel 6a 6/128GB Charcoal (Идеальное состояние)",
            description = "Чистый Android, шикарная камера Google Pixel, процессор Google Tensor. Без дефектов.",
            category = Category.SMARTPHONES,
            price = 7500.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 4.8,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380504443322",
            imageUrl = "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Google", "memory_gb" to "128")
        ),
        ListingItem(
            id = "phone-7",
            title = "Apple iPhone XR 64GB Coral (Neverlock)",
            description = "Все функции исправны, аккумулятор 88%, бережная эксплуатация девушкой. Одесса Приморский.",
            category = Category.SMARTPHONES,
            price = 6200.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.9,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/iphone-xr-coral-ID903.html",
            imageUrl = "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Apple", "memory_gb" to "64")
        ),

        // LAPTOPS & COMPUTERS
        ListingItem(
            id = "lap-1",
            title = "Apple MacBook Air 13\" M2 16GB / 256GB Midnight",
            description = "Состояние нового, 38 циклов зарядки, кастомная версия на 16 ГБ RAM. Одесса Центр.",
            category = Category.LAPTOPS_PC,
            price = 32000.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 5.0,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380503334455",
            imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Apple", "ram_gb" to "16"),
            isHotDeal = true,
            discountPct = 26,
            unitMetricComparison = "32 000 грн (медиана по Одессе 43 000 грн)"
        ),

        // APPLIANCES
        ListingItem(
            id = "app-1",
            title = "Стиральная машина Bosch Serie 6 EcoSilence Drive 8 кг",
            description = "Инверторный мотор, бесшумная работа, пар, 1400 об/мин. Продажа в связи с переездом.",
            category = Category.APPLIANCES,
            price = 11500.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 4.2,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/bosch-washing-machine-ID555.html",
            imageUrl = "https://images.unsplash.com/photo-1626806787461-102c1bfaaea1?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Bosch", "capacity_kg" to "8"),
            isHotDeal = true,
            discountPct = 26,
            unitMetricComparison = "11 500 грн (медиана по Одессе 15 500 грн)"
        ),
        ListingItem(
            id = "app-kettle-1",
            title = "Чайник электрический Scarlett SC-EK21S25 б/у рабочий",
            description = "Дисковый нагревательный элемент, автоотключение. Рабочий. Таирова (рынок Южный).",
            category = Category.APPLIANCES,
            price = 90.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 0.9,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/chainik-scarlett-tairova.html",
            imageUrl = "https://images.unsplash.com/photo-1594213114663-d94db9b17125?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Scarlett", "condition" to "used"),
            isHotDeal = true,
            discountPct = 64,
            unitMetricComparison = "90 грн (медиана по Одессе 250 грн)"
        ),
        ListingItem(
            id = "app-kettle-2",
            title = "Чайник со свистком из нержавеющей стали 2.5 л б/у",
            description = "Для газовых и индукционных плит, громкий свисток, бакелитовая ручка. Черёмушки.",
            category = Category.APPLIANCES,
            price = 100.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 2.8,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380671113355",
            imageUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("type" to "kettle", "condition" to "used"),
            isHotDeal = true,
            discountPct = 50,
            unitMetricComparison = "100 грн (медиана по Одессе 200 грн)"
        ),
        ListingItem(
            id = "app-kettle-3",
            title = "Электрочайник Bosch TWK7808 металл 1.7 л б/у в идеале",
            description = "Корпус из нержавеющей стали, скрытая спираль, светодиодный индикатор. Большой Фонтан.",
            category = Category.APPLIANCES,
            price = 180.0,
            district = ODESA_DISTRICTS[4], // Фонтан
            distanceKm = 2.4,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/bosch-twk-fontan.html",
            imageUrl = "https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("brand" to "Bosch", "condition" to "used")
        ),

        // REAL ESTATE (RENT & SALE)
        ListingItem(
            id = "rent-1",
            title = "Аренда 2к квартиры 54м² в Аркадии, ЖК 36 Жемчужина",
            description = "Евроремонт, вся техника, посудомойка, генератор на лифты и воду в доме! 14 этаж.",
            category = Category.APARTMENT_RENT,
            price = 12000.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.2,
            isExternal = true,
            sourceName = "DOM.ria",
            sourceUrl = "https://dom.ria.com/realty_rent-123.html",
            imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("rooms" to "2", "area_sqm" to "54", "floor" to "14", "generator_in_building" to "true"),
            isHotDeal = true,
            discountPct = 28,
            unitMetricComparison = "222 грн/м² (медиана по Аркадии 310 грн/м²)"
        ),
        ListingItem(
            id = "rent-2",
            title = "1-комнатная студия 32м² в Аркадии возле моря",
            description = "Стильная студия, балкон, автономное отопление, скоростной Wi-Fi.",
            category = Category.APARTMENT_RENT,
            price = 11000.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.5,
            isExternal = true,
            sourceName = "DOM.ria",
            sourceUrl = "https://dom.ria.com/realty_rent-456.html",
            imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("rooms" to "1", "area_sqm" to "32", "sea_view" to "true")
        ),
        ListingItem(
            id = "rent-3",
            title = "Уютная 2к квартира 48м² на Таирова, Ак. Королёва",
            description = "Раздельные комнаты, чистая, теплая, рядом рынок «Южный» и школы. 5/9 этаж.",
            category = Category.APARTMENT_RENT,
            price = 9500.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.0,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380974445566",
            imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("rooms" to "2", "area_sqm" to "48", "floor" to "5")
        ),
        ListingItem(
            id = "rent-center-2k-1",
            title = "Аренда 2-комнатной квартиры 65м² Центр (ул. Дерибасовская / Горсад)",
            description = "Просторная двухкомнатная квартира в самом центре Одессы. Автономное отопление, вся техника, тихий одесский дворик.",
            category = Category.APARTMENT_RENT,
            price = 13500.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 0.5,
            isExternal = true,
            sourceName = "DOM.ria",
            sourceUrl = "https://dom.ria.com/realty_rent-center-2k.html",
            imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("rooms" to "2", "area_sqm" to "65", "type" to "rent"),
            isHotDeal = true,
            discountPct = 27,
            unitMetricComparison = "13 500 грн (медиана центра 18 500 грн • -27%)"
        ),
        ListingItem(
            id = "rent-center-2k-2",
            title = "2к квартира-лофт в Центре (ул. Греческая / Дерибасовская, 58м²)",
            description = "Долгосрочная аренда двухкомнатной квартиры с ремонтом. Тихий центр, оптоволоконный интернет при отключениях света.",
            category = Category.APARTMENT_RENT,
            price = 12000.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 0.6,
            isExternal = true,
            sourceName = "DOM.ria",
            sourceUrl = "https://dom.ria.com/realty_rent-center-loft.html",
            imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("rooms" to "2", "area_sqm" to "58", "type" to "rent"),
            isHotDeal = true,
            discountPct = 35,
            unitMetricComparison = "12 000 грн (медиана центра 18 500 грн • -35%)"
        ),

        // FURNITURE & HOME
        ListingItem(
            id = "furn-1",
            title = "Большой угловой диван с ортопедическим матрасом",
            description = "Ткань антикоготь, короб для белья, спальное место 200х160. Состояние нового.",
            category = Category.HOME_FURNITURE,
            price = 7500.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.1,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380677778899",
            imageUrl = "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("type" to "sofa"),
            isHotDeal = true,
            discountPct = 31,
            unitMetricComparison = "7 500 грн (медиана по Одессе 10 800 грн)"
        ),

        // KIDS & BABY
        ListingItem(
            id = "kids-1",
            title = "Универсальная детская коляска Anex e/type 2 в 1",
            description = "Экокожа, отличная амортизация, полный комплект: дождевик, москитка, рюкзак.",
            category = Category.KIDS,
            price = 8500.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.8,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/anex-etype-ID777.html",
            imageUrl = "https://images.unsplash.com/photo-1591088398332-8a7791972843?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("type" to "stroller"),
            isHotDeal = true,
            discountPct = 32,
            unitMetricComparison = "8 500 грн (медиана по Одессе 12 500 грн)"
        ),

        // SPORTS & HOBBY
        ListingItem(
            id = "sport-1",
            title = "Горный велосипед Pride Marvel 29\" гидравлика Shimano",
            description = "Алюминиевая рама L (19\"), дисковая гидравлика Shimano MT200, вилка с локаутом.",
            category = Category.SPORTS,
            price = 9200.0,
            district = ODESA_DISTRICTS[1], // Аркадия (Трасса Здоровья)
            distanceKm = 3.6,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380931234599",
            imageUrl = "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("wheel_size" to "29", "brakes" to "hydraulic"),
            isHotDeal = true,
            discountPct = 29,
            unitMetricComparison = "9 200 грн (медиана по Одессе 13 000 грн)"
        ),

        // TRANSPORT & AUTO
        ListingItem(
            id = "auto-1",
            title = "Комплект зимних шин Michelin Alpin 6 205/55 R16 (4 шт.)",
            description = "Остаток протектора 7 мм, без шишек и порезов, производство Германия. Цена за комплект.",
            category = Category.TRANSPORT_AUTO,
            price = 6000.0,
            currency = "грн",
            district = ODESA_DISTRICTS[3], // Застава / Хаджибейский
            distanceKm = 5.2,
            isExternal = true,
            sourceName = "AUTO.ria",
            sourceUrl = "https://auto.ria.com/tires/michelin-16-123.html",
            imageUrl = "https://images.unsplash.com/photo-1578844251758-2f71da64c96f?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("radius" to "R16", "season" to "winter"),
            isHotDeal = true,
            discountPct = 29,
            unitMetricComparison = "1 500 грн/шт (медиана 2 100 грн/шт)"
        ),
        ListingItem(
            id = "auto-2",
            title = "Nissan Leaf 30 kWh Acenta 2016",
            description = "Батарея 10 из 12 делений (SOH 82%), запас хода 160-180 км. Порты CHAdeMO и Type 1. Камера, климат-контроль. Одесса Центр.",
            category = Category.TRANSPORT_AUTO,
            price = 8900.0,
            currency = "USD",
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 2.1,
            isExternal = true,
            sourceName = "AUTO.ria",
            sourceUrl = "https://auto.ria.com/auto_nissan_leaf_ID101.html",
            imageUrl = "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Nissan", "year" to "2016", "fuel" to "electric"),
            isHotDeal = true,
            discountPct = 26,
            unitMetricComparison = "8 900 $ (медиана по Одессе 12 000 $)"
        ),
        ListingItem(
            id = "auto-3",
            title = "Renault Megane 1.5 dCi Bose Edition 2015 Универсал",
            description = "Экономичный надежный дизель K9K (расход 4.8 л), панорамная крыша, акустика Bose, бесключевой доступ. Таирова.",
            category = Category.TRANSPORT_AUTO,
            price = 7800.0,
            currency = "USD",
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.1,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/renault-megane-2015-ID102.html",
            imageUrl = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Renault", "year" to "2015", "fuel" to "diesel"),
            isHotDeal = true,
            discountPct = 28,
            unitMetricComparison = "7 800 $ (медиана по Одессе 10 800 $)"
        ),
        ListingItem(
            id = "auto-4",
            title = "Ford Focus 2.0 AT Titanium 2016",
            description = "Свежепригнан, чистый 2016 год, 2.0 бензин на надежном классическом автомате. Кожа, люк, SYNC 3, климат. Аркадия.",
            category = Category.TRANSPORT_AUTO,
            price = 8200.0,
            currency = "USD",
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 2.8,
            isExternal = true,
            sourceName = "AUTO.ria",
            sourceUrl = "https://auto.ria.com/auto_ford_focus_ID103.html",
            imageUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Ford", "year" to "2016", "transmission" to "automatic"),
            isHotDeal = true,
            discountPct = 25,
            unitMetricComparison = "8 200 $ (медиана по Одессе 11 000 $)"
        ),
        ListingItem(
            id = "auto-5",
            title = "Skoda Octavia A5 FL 1.6 MPI Газ/Бензин 2012",
            description = "Простой надежный атмосферный двигатель 1.6 MPI, установлен газ Евро-4, обслужена ходовая, кондиционер. Черёмушки.",
            category = Category.TRANSPORT_AUTO,
            price = 6900.0,
            currency = "USD",
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 3.2,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380673332211",
            imageUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Skoda", "year" to "2012", "fuel" to "gas/petrol")
        ),
        ListingItem(
            id = "auto-6",
            title = "Volkswagen Golf 7 1.6 TDI 2014",
            description = "Отличное состояние, родной пробег 185 тыс км, 2-зонный климат, адаптивный круиз, мультируль, чистый салон. Центр.",
            category = Category.TRANSPORT_AUTO,
            price = 9200.0,
            currency = "USD",
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 3.8,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua/d/obyavlenie/vw-golf-7-ID104.html",
            imageUrl = "https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Volkswagen", "year" to "2014")
        ),
        ListingItem(
            id = "auto-7",
            title = "Chevrolet Cruze 1.4 Turbo LTZ 2015",
            description = "Максимальная комплектация LTZ, кожаный салон, кнопка Start/Stop, люк, литые диски R17. Большой Фонтан.",
            category = Category.TRANSPORT_AUTO,
            price = 6500.0,
            currency = "USD",
            district = ODESA_DISTRICTS[4], // Большой Фонтан
            distanceKm = 2.4,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380509998877",
            imageUrl = "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("brand" to "Chevrolet", "year" to "2015"),
            isHotDeal = true,
            discountPct = 31,
            unitMetricComparison = "6 500 $ (медиана по Одессе 9 400 $)"
        ),
        ListingItem(
            id = "auto-8",
            title = "Volkswagen Passat B8 2.0 TDI 2017 Official",
            description = "Официальный седан бизнес-класса, сервисная книжка, без ДТП. Кожаный салон, 3-зонный климат, LED фары. Таирова.",
            category = Category.TRANSPORT_AUTO,
            price = 15800.0,
            currency = "USD",
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.4,
            isExternal = true,
            sourceName = "AUTO.ria",
            sourceUrl = "https://auto.ria.com/auto_passat_b8_ID105.html",
            imageUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("brand" to "Volkswagen", "year" to "2017")
        ),

        // SERVICES
        ListingItem(
            id = "srv-1",
            title = "Услуги электрика, подключение генераторов и инверторов",
            description = "Срочный выезд по Таирова и Черёмушкам. Установка АВР, переключателей ввода, проводка.",
            category = Category.SERVICES,
            price = 800.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.5,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380678889900",
            imageUrl = "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("service_type" to "electrician")
        ),
        ListingItem(
            id = "srv-2",
            title = "Сантехник Одесса: установка бойлеров, насосов, смесителей",
            description = "Срочный выезд мастера по Центру и Фонтану со своим инструментом. Гарантия.",
            category = Category.SERVICES,
            price = 500.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 2.1,
            isExternal = true,
            sourceName = "Работники UA",
            sourceUrl = "https://vserabotniki.com.ua/odessa/plumbing/",
            imageUrl = "https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("service_type" to "plumber"),
            isHotDeal = true,
            discountPct = 35,
            unitMetricComparison = "500 грн (медиана по Одессе 770 грн)"
        ),
        ListingItem(
            id = "srv-3",
            title = "Мастер по ремонту бытовой техники и стиральных машин",
            description = "Ремонт стиралок, бойлеров, электроплит на дому в день обращения. Таирова, Черёмушки.",
            category = Category.SERVICES,
            price = 350.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.2,
            isExternal = true,
            sourceName = "Работники UA",
            sourceUrl = "https://vserabotniki.com.ua/odessa/remont/",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("service_type" to "appliance_repair")
        ),
        ListingItem(
            id = "srv-4",
            title = "Грузчики и грузоперевозки по Одессе (Газель, Бус)",
            description = "Квартирные переезды, подъем стройматериалов и мебели на этаж. Черёмушки, Таирова.",
            category = Category.SERVICES,
            price = 400.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 3.5,
            isExternal = true,
            sourceName = "Работники UA",
            sourceUrl = "https://vserabotniki.com.ua/odessa/movers/",
            imageUrl = "https://images.unsplash.com/photo-1600518464441-9154a4dea21b?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("service_type" to "movers")
        ),
        // BICYCLE & SPORTS
        ListingItem(
            id = "velo-pump-1",
            title = "Насос велосипедный ручной со шлангом и манометром Giyo",
            description = "Универсальный ручной велосипедный насос. Автониппель (Schrader) и Presta. Давление до 8 bar. Таирова.",
            category = Category.SPORTS,
            price = 160.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 0.9,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua",
            imageUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            isHotDeal = true,
            discountPct = 36,
            unitMetricComparison = "160 грн (медиана 250 грн • -36%)"
        ),
        ListingItem(
            id = "velo-pump-2",
            title = "Велосипедный насос ножной универсальный с манометром",
            description = "Надёжный ножной насос для велосипеда, мячей и шин. Металлический корпус, переходники. Черёмушки.",
            category = Category.SPORTS,
            price = 220.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 2.8,
            isExternal = true,
            sourceName = "Prom",
            sourceUrl = "https://prom.ua",
            imageUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT
        ),
        ListingItem(
            id = "velo-pump-3",
            title = "Компактный мини-насос на раму велосипеда алюминиевый",
            description = "Легкий портативный велосипедный насос с креплением на раму. Центр Одессы.",
            category = Category.SPORTS,
            price = 195.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 2.5,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua",
            imageUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            isHotDeal = true,
            discountPct = 25,
            unitMetricComparison = "195 грн (медиана 260 грн • -25%)"
        ),
        ListingItem(
            id = "velo-bike-1",
            title = "Горный велосипед Trek Marlin 7 29 (Рама L, Deore)",
            description = "Гидравлические тормоза Shimano, блокировка вилки, накат по Трассе Здоровья. Большой Фонтан.",
            category = Category.SPORTS,
            price = 15200.0,
            district = ODESA_DISTRICTS[4], // Большой Фонтан
            distanceKm = 2.8,
            isExternal = false,
            sourceName = "На нашей площадке",
            imageUrl = "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            isHotDeal = true,
            discountPct = 32,
            unitMetricComparison = "15 200 грн (медиана 22 500 грн)"
        ),
        ListingItem(
            id = "app-kettle-1",
            title = "Чайник электрический Scarlett SC-EK21S25 б/у рабочий",
            description = "Дисковый нагревательный элемент, автоотключение. Полностью рабочий. Таирова.",
            category = Category.APPLIANCES,
            price = 90.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 0.9,
            isExternal = true,
            sourceName = "OLX",
            sourceUrl = "https://olx.ua",
            imageUrl = "https://images.unsplash.com/photo-1594213114663-d94db9b17125?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            isHotDeal = true,
            discountPct = 64,
            unitMetricComparison = "90 грн (медиана 250 грн • -64%)"
        )
    )

    // Calculate distance using Haversine formula
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of the earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }

    // Filter and rank listings based on intent and location
    fun findMatches(
        category: Category,
        userDistrict: District,
        maxPrice: Double?,
        keywords: String
    ): List<ListingItem> {
        val queryLower = keywords.lowercase()
        val stopWords = setOf(
            "ищу", "нужен", "нужна", "нужно", "куплю", "купить", "купит", "купят", "купим", "купите", "приобрести", "покупка", "продам", "продажа",
            "хороший", "хорошую", "хорошие", "хорошее", "отличный", "нормальный", "недорого",
            "до", "грн", "uah", "гривен", "гривны", "дол", "долл", "доллар", "доллара", "долларов", "usd", "сша", "бакс", "баксов", "евро", "eur",
            "бу", "б/у", "в", "на", "одесса", "одессе", "тыс", "тис", "тысяч"
        )
        val tokens = queryLower
            .replace(Regex("""[.,\/#!$%\^&\*;:{}=\-_`~()"?«»]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 3 && it !in stopWords && !it.all { c -> c.isDigit() } }

        val synonymGroups = listOf(
            setOf("2к", "2-к", "2-комн", "2 комн", "двухкомнатн", "2-комнатн", "двухкомнатная", "двухкомнатной", "двухкомнатную"),
            setOf("1к", "1-к", "1-комн", "1 комн", "однокомнатн", "1-комнатн", "однокомнатная", "однокомнатной", "студи", "студия"),
            setOf("3к", "3-к", "3-комн", "3 комн", "трехкомнатн", "трёхкомнатн", "3-комнатн"),
            setOf("центр", "дерибасовск", "горсад", "греческ", "ришельевск", "пушкинск"),
            setOf("аренд", "аренда", "снять", "сниму", "сдам", "сдается", "долгосрочн"),
            setOf("телефон", "телефона", "телефоны", "смартфон", "смартфона", "смартфоны", "айфон", "iphone", "samsung", "самсунг", "xiaomi", "сяоми", "редми", "redmi", "pixel", "пиксель", "motorola", "моторола", "oneplus"),
            setOf("машина", "машину", "машины", "авто", "автомобиль", "автомобиля", "легковой", "легковая", "иномарка", "nissan", "ниссан", "volkswagen", "фольксваген", "passat", "пассат", "golf", "гольф", "renault", "рено", "megane", "меган", "ford", "форд", "focus", "фокус", "skoda", "шкода", "octavia", "октавия", "toyota", "тойота", "corolla", "hyundai", "хюндай", "kia", "киа", "электромобиль", "leaf", "лиф", "chevrolet", "cruze", "шевроле")
        )

        // Strict compound matching: requires all tokens or their synonyms to match
        val exactMatches = LISTINGS_POOL.filter { listing ->
            val categoryMatch = (category == Category.OTHER || listing.category == category)
            val text = (listing.title + " " + listing.description).lowercase()
            val tokenMatch = tokens.isEmpty() || tokens.all { token ->
                val stem = if (token.length > 4) token.substring(0, token.length - 1) else token
                if (text.contains(token) || text.contains(stem)) {
                    true
                } else {
                    val group = synonymGroups.firstOrNull { syns -> syns.any { s -> token.contains(s) || s.contains(token) || (stem.length >= 3 && s.contains(stem)) } }
                    group != null && group.any { syn -> text.contains(syn) }
                }
            }
            categoryMatch && tokenMatch
        }

        val pool = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else if (tokens.isNotEmpty()) {
            // No item matched all tokens -> demand is captured, no false-positive matches
            emptyList()
        } else {
            LISTINGS_POOL.filter { category == Category.OTHER || it.category == category }
        }

        val isUsdQuery = Regex("""(?:дол|долл|usd|\$|сша|баксов)""").containsMatchIn(queryLower)

        return pool
            .map { listing ->
                val distance = calculateDistance(
                    userDistrict.lat, userDistrict.lon,
                    listing.district.lat, listing.district.lon
                )
                // Normalize price based on currency comparison
                val normalizedPrice = when {
                    isUsdQuery && listing.currency != "USD" -> listing.price / 41.5
                    !isUsdQuery && listing.currency == "USD" -> listing.price * 41.5
                    else -> listing.price
                }
                val isWithinBudget = (maxPrice == null || normalizedPrice <= maxPrice)
                val grade = when {
                    isWithinBudget && distance <= 3.0 -> MatchGrade.EXCELLENT
                    isWithinBudget -> MatchGrade.EXCELLENT
                    maxPrice != null && normalizedPrice <= maxPrice * 1.5 -> MatchGrade.GOOD
                    else -> MatchGrade.PARTIAL
                }
                listing.copy(distanceKm = distance, matchGrade = grade)
            }
            .sortedWith(
                compareBy(
                    { it.matchGrade != MatchGrade.EXCELLENT }, // Excellents first
                    { if (maxPrice != null) it.price > maxPrice else false }, // Within budget first
                    { it.distanceKm },                         // Closest first (Location-First)
                    { it.price }                               // Best price
                )
            )
    }

    // Get hot deals (>=25% discount relative to median) matching user query and category
    fun getHotDeals(
        userDistrict: District,
        category: Category? = null,
        keywords: String = ""
    ): List<ListingItem> {
        val queryLower = keywords.lowercase().trim()
        val stopWords = setOf(
            "ищу", "нужен", "нужна", "нужно", "куплю", "купить", "купит", "купят", "купим", "купите", "приобрести", "покупка", "продам", "продажа",
            "хороший", "хорошую", "хорошие", "хорошее", "отличный", "нормальный", "недорого",
            "до", "грн", "uah", "гривен", "гривны", "дол", "долл", "доллар", "доллара", "долларов", "usd", "сша", "бакс", "баксов", "евро", "eur",
            "бу", "б/у", "в", "на", "одесса", "одессе", "тыс", "тис", "тысяч"
        )
        val tokens = queryLower
            .replace(Regex("""[.,\/#!$%\^&\*;:{}=\-_`~()"?«»]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 3 && it !in stopWords && !it.all { c -> c.isDigit() } }

        val synonymGroups = listOf(
            setOf("2к", "2-к", "2-комн", "2 комн", "двухкомнатн", "2-комнатн", "двухкомнатная", "двухкомнатной", "двухкомнатную"),
            setOf("1к", "1-к", "1-комн", "1 комн", "однокомнатн", "1-комнатн", "однокомнатная", "однокомнатной", "студи", "студия"),
            setOf("3к", "3-к", "3-комн", "3 комн", "трехкомнатн", "трёхкомнатн", "3-комнатн"),
            setOf("центр", "дерибасовск", "горсад", "греческ", "ришельевск", "пушкинск"),
            setOf("аренд", "аренда", "снять", "сниму", "сдам", "сдается", "долгосрочн"),
            setOf("телефон", "телефона", "телефоны", "смартфон", "смартфона", "смартфоны", "айфон", "iphone", "samsung", "самсунг", "xiaomi", "сяоми", "редми", "redmi", "pixel", "пиксель", "motorola", "моторола", "oneplus"),
            setOf("машина", "машину", "машины", "авто", "автомобиль", "автомобиля", "легковой", "легковая", "иномарка", "nissan", "ниссан", "volkswagen", "фольксваген", "passat", "пассат", "golf", "гольф", "renault", "рено", "megane", "меган", "ford", "форд", "focus", "фокус", "skoda", "шкода", "octavia", "октавия", "toyota", "тойота", "corolla", "hyundai", "хюндай", "kia", "киа", "электромобиль", "leaf", "лиф", "chevrolet", "cruze", "шевроле")
        )

        return LISTINGS_POOL
            .filter { listing ->
                val hotDealMatch = listing.isHotDeal
                val categoryMatch = (category == null || category == Category.OTHER || listing.category == category)
                val text = (listing.title + " " + listing.description).lowercase()
                val keywordMatch = tokens.isEmpty() || tokens.all { token ->
                    val stem = if (token.length > 4) token.substring(0, token.length - 1) else token
                    if (text.contains(token) || text.contains(stem)) {
                        true
                    } else {
                        val group = synonymGroups.firstOrNull { syns -> syns.any { s -> token.contains(s) || s.contains(token) || (stem.length >= 3 && s.contains(stem)) } }
                        group != null && group.any { syn -> text.contains(syn) }
                    }
                }
                hotDealMatch && categoryMatch && keywordMatch
            }
            .map { listing ->
                val distance = calculateDistance(
                    userDistrict.lat, userDistrict.lon,
                    listing.district.lat, listing.district.lon
                )
                listing.copy(distanceKm = distance)
            }
            .sortedWith(
                compareByDescending<ListingItem> { it.discountPct ?: 0 }
                    .thenBy { it.distanceKm }
                    .thenBy { it.price }
            )
    }

    fun getAllListings(): List<ListingItem> = LISTINGS_POOL
}
