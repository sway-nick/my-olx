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

        // 1. Detect Category across universal niches
        val category = when {
            lower.contains("генератор") || lower.contains("генератори") || lower.contains("квт") || lower.contains("kw") || lower.contains("ecoflow") -> {
                Category.POWER_GENERATORS
            }
            lower.contains("iphone") || lower.contains("айфон") || lower.contains("samsung") || lower.contains("смартфон") || lower.contains("телефон") -> {
                Category.SMARTPHONES
            }
            lower.contains("ноутбук") || lower.contains("macbook") || lower.contains("макбук") || lower.contains("компьютер") -> {
                Category.LAPTOPS_PC
            }
            lower.contains("стиральн") || lower.contains("холодильник") || lower.contains("кондиционер") || lower.contains("пылесос") ||
            lower.contains("чайник") || lower.contains("чайники") || lower.contains("утюг") || lower.contains("микроволновк") || lower.contains("блендер") || lower.contains("кофеварк") || lower.contains("тостер") -> {
                Category.APPLIANCES
            }
            lower.contains("квартир") || lower.contains("аренд") || lower.contains("оренд") || lower.contains("сдам") || lower.contains("сниму") -> {
                Category.APARTMENT_RENT
            }
            lower.contains("купить квартиру") || lower.contains("продажа квартир") || lower.contains("продам квартиру") -> {
                Category.APARTMENT_SALE
            }
            lower.contains("диван") || lower.contains("мебел") || lower.contains("шкаф") || lower.contains("кровать") || lower.contains("стол") -> {
                Category.HOME_FURNITURE
            }
            lower.contains("коляск") || lower.contains("детск") || lower.contains("автокресл") || lower.contains("игрушк") -> {
                Category.KIDS
            }
            lower.contains("велосипед") || lower.contains("самокат") || lower.contains("тренажер") || lower.contains("рыбалк") -> {
                Category.SPORTS
            }
            lower.contains("авто") || lower.contains("машин") || lower.contains("шин") || lower.contains("резин") || lower.contains("диск") -> {
                Category.TRANSPORT_AUTO
            }
            lower.contains("электрик") || lower.contains("електрик") || lower.contains("ремонт") || lower.contains("услуг") || lower.contains("послуг") || lower.contains("сантехник") ||
            lower.contains("мастер") || lower.contains("грузчик") || lower.contains("переезд") || lower.contains("муж на час") -> {
                Category.SERVICES
            }
            lower.contains("одежд") || lower.contains("обув") || lower.contains("куртк") || lower.contains("кроссовк") -> {
                Category.FASHION
            }
            lower.contains("кот") || lower.contains("собак") || lower.contains("щенок") || lower.contains("корм") -> {
                Category.ANIMALS
            }
            lower.contains("работ") || lower.contains("ваканси") || lower.contains("водитель") || lower.contains("курьер") -> {
                Category.JOBS
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

        // 3. Extract Price (до 40 тысяч, 10 тыс дол США, 40000, 15к, до 100 грн)
        val isUsd = Regex("""(?:дол|долл|usd|\$|сша|баксов)""").containsMatchIn(lower)
        val currency = if (isUsd) "USD" else "UAH"

        var priceMax: Double? = null
        val priceThousandRegex = Regex("""(?:до\s*|бюджет\s*|<=|<)?\s*(\d+[.,]?\d*)\s*(?:тысяч[а-я]*|тыс[а-я.]*|тис[а-я.]*|к|k)(?:[\s,.]|$)""")
        val pricePlainRegex = Regex("""(?:до\s*|бюджет\s*|<=|<)\s*(\d{1,7})\s*(?:грн|uah|usd|\$|дол)?""")
        val priceFallbackRegex = Regex("""(\d{1,7})\s*(?:грн|uah|usd|\$|дол)""")

        val thousandMatch = priceThousandRegex.find(lower)
        if (thousandMatch != null) {
            val num = thousandMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
            if (num != null) priceMax = num * 1000
        } else {
            val plainMatch = pricePlainRegex.find(lower) ?: priceFallbackRegex.find(lower)
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
            currency = currency,
            targetDistrict = targetDistrict ?: MockDataRepository.ODESA_DISTRICTS[0] // Default to Tairova
        )
    }
}
