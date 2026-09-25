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
        val stopWords = setOf("ищу", "нужен", "нужна", "нужно", "куплю", "до", "грн", "uah", "бу", "б/у", "в", "на", "одесса", "одессе")
        val tokens = queryLower
            .replace(Regex("""[.,\/#!$%\^&\*;:{}=\-_`~()"?«»]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 3 && it !in stopWords && !it.all { c -> c.isDigit() } }

        // Strict compound matching: requires all tokens to match
        val exactMatches = LISTINGS_POOL.filter { listing ->
            val text = (listing.title + " " + listing.description).lowercase()
            tokens.isEmpty() || tokens.all { token ->
                val stem = if (token.length > 4) token.substring(0, token.length - 1) else token
                text.contains(token) || text.contains(stem)
            }
        }

        val pool = if (exactMatches.isNotEmpty()) {
            exactMatches
        } else if (tokens.isNotEmpty()) {
            // No item matched all tokens -> demand is captured, no false-positive matches
            emptyList()
        } else {
            LISTINGS_POOL.filter { category == Category.OTHER || it.category == category }
        }

        return pool
            .map { listing ->
                val distance = calculateDistance(
                    userDistrict.lat, userDistrict.lon,
                    listing.district.lat, listing.district.lon
                )
                // Determine relevance grade based on distance and price
                val isWithinBudget = (maxPrice == null || listing.price <= maxPrice)
                val grade = when {
                    isWithinBudget && distance <= 3.0 -> MatchGrade.EXCELLENT
                    isWithinBudget -> MatchGrade.EXCELLENT
                    maxPrice != null && listing.price <= maxPrice * 1.5 -> MatchGrade.GOOD
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
        val stopWords = setOf("ищу", "нужен", "нужна", "нужно", "куплю", "до", "грн", "uah", "бу", "б/у", "в", "на", "одесса", "одессе")
        val tokens = queryLower
            .replace(Regex("""[.,\/#!$%\^&\*;:{}=\-_`~()"?«»]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 3 && it !in stopWords && !it.all { c -> c.isDigit() } }

        return LISTINGS_POOL
            .filter { listing ->
                val hotDealMatch = listing.isHotDeal
                val categoryMatch = (category == null || category == Category.OTHER || listing.category == category)
                val text = (listing.title + " " + listing.description).lowercase()
                val keywordMatch = tokens.isEmpty() || tokens.all { token ->
                    val stem = if (token.length > 4) token.substring(0, token.length - 1) else token
                    text.contains(token) || text.contains(stem)
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
