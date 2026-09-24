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
        // POWER GENERATORS
        ListingItem(
            id = "gen-1",
            title = "Бензиновый генератор Honda 5.5 кВт",
            description = "Отличное состояние, медная обмотка, электростартер. Работал 15 моточасов. Самовывоз Таирова.",
            category = Category.POWER_GENERATORS,
            price = 35000.0,
            district = ODESA_DISTRICTS[0], // Таирова
            distanceKm = 1.2,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380671234567",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("power_kw" to "5.5", "fuel" to "petrol", "starter" to "electric")
        ),
        ListingItem(
            id = "gen-2",
            title = "Генератор Daewoo 5.0 кВт бензин",
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
            title = "Дизельный генератор Hyundai 6.0 кВт",
            description = "Экономный расход 1.4 л/ч, профессиональная серия. Черёмушки.",
            category = Category.POWER_GENERATORS,
            price = 45000.0,
            district = ODESA_DISTRICTS[3], // Черёмушки
            distanceKm = 4.8,
            isExternal = true,
            sourceName = "Prom",
            sourceUrl = "https://prom.ua/p12345-generator-hyundai.html",
            imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("power_kw" to "6.0", "fuel" to "diesel")
        ),
        ListingItem(
            id = "gen-4",
            title = "Инверторный генератор 2.5 кВт",
            description = "Компактный, тихий, подходит для котлов и чувствительной электроники.",
            category = Category.POWER_GENERATORS,
            price = 22000.0,
            district = ODESA_DISTRICTS[2], // Центр
            distanceKm = 8.1,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380509876543",
            imageUrl = "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.PARTIAL,
            attributes = mapOf("power_kw" to "2.5", "fuel" to "petrol")
        ),

        // APARTMENT RENT
        ListingItem(
            id = "rent-1",
            title = "Аренда 2к квартиры в Аркадии с видом на море",
            description = "ЖК 36 Жемчужина. Евроремонт, вся техника, посудомойка, генератор в доме! 14 этаж.",
            category = Category.APARTMENT_RENT,
            price = 15000.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.2,
            isExternal = false,
            sourceName = "На нашей площадке",
            phone = "+380631112233",
            imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.EXCELLENT,
            attributes = mapOf("rooms" to "2", "floor" to "14", "generator_in_building" to "true")
        ),
        ListingItem(
            id = "rent-2",
            title = "1-комнатная студия в Аркадии посуточно / долгосрочно",
            description = "Стильная студия возле моря. Полная комплектация, балкон, автономное отопление.",
            category = Category.APARTMENT_RENT,
            price = 11000.0,
            district = ODESA_DISTRICTS[1], // Аркадия
            distanceKm = 3.5,
            isExternal = true,
            sourceName = "DOM.ria",
            sourceUrl = "https://dom.ria.com/realty_rent-123.html",
            imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=500&auto=format&fit=crop&q=60",
            matchGrade = MatchGrade.GOOD,
            attributes = mapOf("rooms" to "1", "sea_view" to "true")
        ),
        ListingItem(
            id = "rent-3",
            title = "Уютная 2к квартира на Таирова, Ак. Королёва",
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
            attributes = mapOf("rooms" to "2", "floor" to "5")
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
        return LISTINGS_POOL
            .filter { it.category == category }
            .map { listing ->
                val distance = calculateDistance(
                    userDistrict.lat, userDistrict.lon,
                    listing.district.lat, listing.district.lon
                )
                // Determine relevance grade based on distance and price
                val grade = when {
                    (maxPrice != null && listing.price <= maxPrice) && distance <= 3.0 -> MatchGrade.EXCELLENT
                    (maxPrice != null && listing.price <= maxPrice * 1.15) && distance <= 7.0 -> MatchGrade.GOOD
                    else -> MatchGrade.PARTIAL
                }
                listing.copy(distanceKm = distance, matchGrade = grade)
            }
            .sortedWith(
                compareBy(
                    { it.matchGrade != MatchGrade.EXCELLENT }, // Excellents first
                    { it.distanceKm },                         // Closest first (Location-First)
                    { it.price }                               // Best price
                )
            )
    }

    fun getAllListings(): List<ListingItem> = LISTINGS_POOL
}
