package com.swaynick.intentmarket.domain.model

enum class IntentType {
    DEMAND,     // «Ищу / Куплю / Нужен»
    SUPPLY,     // «Предлагаю / Продам / Сдам»
    HOT_DEALS   // «🔥 Хорошая цена (скидка от 25%)»
}

enum class Category(val displayNameRu: String, val displayNameUa: String) {
    SMARTPHONES("Смартфоны", "Смартфони"),
    LAPTOPS_PC("Ноутбуки и ПК", "Ноутбуки та ПК"),
    APPLIANCES("Бытовая техника", "Побутова техніка"),
    ELECTRONICS("Электроника", "Електроніка"),
    APARTMENT_RENT("Аренда квартир", "Оренда квартир"),
    APARTMENT_SALE("Продажа квартир", "Продаж квартир"),
    POWER_GENERATORS("Генераторы", "Генератори"),
    SERVICES("Услуги и ремонт", "Послуги та ремонт"),
    TRANSPORT_AUTO("Транспорт и авто", "Транспорт та авто"),
    HOME_FURNITURE("Дом и мебель", "Дім та меблі"),
    KIDS("Детский мир", "Дитячий світ"),
    SPORTS("Спорт и хобби", "Спорт та хобі"),
    FASHION("Мода и одежда", "Мода та одяг"),
    ANIMALS("Животные", "Тварини"),
    JOBS("Работа", "Робота"),
    OTHER("Другое", "Інше")
}

data class District(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val parentArea: String? = null
)

data class AdministrativeArea(
    val id: String,
    val name: String,
    val subdistricts: List<District>
)

enum class MatchGrade(val title: String) {
    EXCELLENT("Отлично подходит"),
    GOOD("Подходит"),
    PARTIAL("Частично подходит")
}

data class ListingItem(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val price: Double,
    val currency: String = "грн",
    val district: District,
    val distanceKm: Double,
    val isExternal: Boolean,
    val sourceName: String, // "OLX", "Prom", "На нашей площадке"
    val sourceUrl: String? = null,
    val phone: String? = null,
    val imageUrl: String? = null,
    val matchGrade: MatchGrade = MatchGrade.GOOD,
    val attributes: Map<String, String> = emptyMap(),
    val isHotDeal: Boolean = false,
    val discountPct: Int? = null,
    val unitMetricComparison: String? = null
)

data class ParsedIntent(
    val intentType: IntentType,
    val rawText: String,
    val category: Category,
    val attributes: Map<String, String>,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val currency: String = "UAH",
    val targetDistrict: District? = null,
    val photos: List<String> = emptyList()
)
