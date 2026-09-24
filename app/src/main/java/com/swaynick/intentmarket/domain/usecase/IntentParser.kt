package com.swaynick.intentmarket.domain.usecase

import com.swaynick.intentmarket.data.repository.MockDataRepository
import com.swaynick.intentmarket.domain.model.Category
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.IntentType
import com.swaynick.intentmarket.domain.model.ParsedIntent

object IntentParser {

    fun parse(text: String, defaultType: IntentType = IntentType.DEMAND): ParsedIntent {
        val lower = text.lowercase()
        val attributes = mutableMapOf<String, String>()

        // 1. Detect Category
        val category = when {
            lower.contains("генератор") || lower.contains("генератори") || lower.contains("квт") || lower.contains("kw") -> {
                Category.POWER_GENERATORS
            }
            lower.contains("квартир") || lower.contains("аренд") || lower.contains("оренд") || lower.contains("сдам") || lower.contains("сниму") -> {
                Category.APARTMENT_RENT
            }
            lower.contains("электрик") || lower.contains("електрик") || lower.contains("ремонт") || lower.contains("услуг") || lower.contains("послуг") -> {
                Category.SERVICES
            }
            lower.contains("телефон") || lower.contains("ноутбук") || lower.contains("iphone") || lower.contains("айфон") -> {
                Category.ELECTRONICS
            }
            else -> Category.OTHER
        }

        // 2. Extract Category specific attributes
        if (category == Category.POWER_GENERATORS) {
            // Power extraction (e.g. 5 кВт, 5.5 kw)
            val powerRegex = Regex("""(\d+[.,]?\d*)\s*(?:квт|kw|киловатт|кіловат)""")
            val powerMatch = powerRegex.find(lower)
            if (powerMatch != null) {
                attributes["power_kw"] = powerMatch.groupValues[1].replace(',', '.')
            }

            // Fuel extraction
            when {
                lower.contains("бензин") || lower.contains("бензо") -> attributes["fuel"] = "petrol"
                lower.contains("дизел") -> attributes["fuel"] = "diesel"
                lower.contains("газ") -> attributes["fuel"] = "gas"
                lower.contains("инвертор") || lower.contains("інвертор") -> attributes["type"] = "inverter"
            }
        }

        if (category == Category.APARTMENT_RENT) {
            // Rooms extraction (1к, 2к, 1-комн)
            val roomsRegex = Regex("""(\d)\s*(?:к|комн|кімн)""")
            val roomsMatch = roomsRegex.find(lower)
            if (roomsMatch != null) {
                attributes["rooms"] = roomsMatch.groupValues[1]
            } else if (lower.contains("студи") || lower.contains("студі")) {
                attributes["rooms"] = "1 (студия)"
            }
        }

        // 3. Extract Price (до 40 тысяч, 40000, 15к)
        var priceMax: Double? = null
        val priceThousandRegex = Regex("""(?:до\s*)?(\d+)\s*(?:тысяч|тис|тис\.|к|k)""")
        val pricePlainRegex = Regex("""(?:до\s*)?(\d{4,7})\s*(?:грн|uah)?""")

        val thousandMatch = priceThousandRegex.find(lower)
        if (thousandMatch != null) {
            val num = thousandMatch.groupValues[1].toDoubleOrNull()
            if (num != null) priceMax = num * 1000
        } else {
            val plainMatch = pricePlainRegex.find(lower)
            if (plainMatch != null) {
                priceMax = plainMatch.groupValues[1].toDoubleOrNull()
            }
        }

        // 4. Extract District in Odesa
        var targetDistrict: District? = null
        for (district in MockDataRepository.ODESA_DISTRICTS) {
            val districtStem = when (district.id) {
                "tairova" -> "таиров"
                "arcadia" -> "аркади"
                "center" -> "центр"
                "cheremushki" -> "черёмуш"
                "fontan" -> "фонтан"
                "kotovskogo" -> "котовск"
                else -> district.name.lowercase()
            }
            if (lower.contains(districtStem)) {
                targetDistrict = district
                break
            }
        }

        return ParsedIntent(
            intentType = defaultType,
            rawText = text,
            category = category,
            attributes = attributes,
            priceMax = priceMax,
            targetDistrict = targetDistrict ?: MockDataRepository.ODESA_DISTRICTS[0] // Default to Tairova
        )
    }
}
