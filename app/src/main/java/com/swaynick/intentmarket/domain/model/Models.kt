package com.swaynick.intentmarket.domain.model

enum class IntentType {
    DEMAND, // «Ищу / Куплю / Нужен»
    SUPPLY  // «Предлагаю / Продам / Сдам»
}

enum class Category(val displayNameRu: String, val displayNameUa: String) {
    POWER_GENERATORS("Генераторы", "Генератори"),
    APARTMENT_RENT("Аренда квартир", "Оренда квартир"),
    ELECTRONICS("Электроника", "Електроніка"),
    SERVICES("Услуги и ремонт", "Послуги та ремонт"),
    OTHER("Другое", "Інше")
}

data class District(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double
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
    val attributes: Map<String, String> = emptyMap()
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
