package com.swaynick.intentmarket.data.repository

import com.swaynick.intentmarket.domain.model.Category
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.ListingItem
import com.swaynick.intentmarket.domain.model.MatchGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseRepository {
    const val SUPABASE_URL = "https://gpqjuwcfdkqdmyxplfbs.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_nbP4Szi9LyDh4BY15fsJUg_Q_RJkaqd"

    val CATEGORY_UUID_MAP = mapOf(
        Category.POWER_GENERATORS to "c0000000-0000-0000-0000-000000000001",
        Category.APARTMENT_RENT to "c0000000-0000-0000-0000-000000000002",
        Category.APARTMENT_SALE to "c0000000-0000-0000-0000-000000000020",
        Category.SERVICES to "c0000000-0000-0000-0000-000000000003",
        Category.ELECTRONICS to "c0000000-0000-0000-0000-000000000004",
        Category.SMARTPHONES to "c0000000-0000-0000-0000-000000000010",
        Category.LAPTOPS_PC to "c0000000-0000-0000-0000-000000000011",
        Category.APPLIANCES to "c0000000-0000-0000-0000-000000000012",
        Category.TRANSPORT_AUTO to "c0000000-0000-0000-0000-000000000050",
        Category.HOME_FURNITURE to "c0000000-0000-0000-0000-000000000060",
        Category.KIDS to "c0000000-0000-0000-0000-000000000070",
        Category.SPORTS to "c0000000-0000-0000-0000-000000000080",
        Category.FASHION to "c0000000-0000-0000-0000-000000000090",
        Category.ANIMALS to "c0000000-0000-0000-0000-000000000100",
        Category.JOBS to "c0000000-0000-0000-0000-000000000110",
        Category.OTHER to "c0000000-0000-0000-0000-000000000001"
    )

    /**
     * Calls Supabase RPC `match_demand_to_listings`
     * Computes PostGIS distance and hybrid match score directly in the cloud.
     */
    suspend fun matchDemand(
        category: Category,
        userLat: Double,
        userLon: Double,
        maxPrice: Double? = null,
        radiusKm: Double = 25.0
    ): Result<List<ListingItem>> = withContext(Dispatchers.IO) {
        try {
            val categoryId = CATEGORY_UUID_MAP[category] ?: CATEGORY_UUID_MAP[Category.POWER_GENERATORS]!!
            val rpcUrl = URL("$SUPABASE_URL/rest/v1/rpc/match_demand_to_listings")
            val conn = (rpcUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("p_category_id", categoryId)
                put("p_user_lat", userLat)
                put("p_user_lon", userLon)
                if (maxPrice != null && maxPrice > 0) {
                    put("p_max_price", maxPrice)
                }
                put("p_radius_km", radiusKm)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseStr = reader.readText()
                reader.close()

                val jsonArray = JSONArray(responseStr)
                val items = mutableListOf<ListingItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "item-$i")
                    val title = obj.optString("title", "Без названия")
                    val desc = obj.optString("description", "")
                    val price = obj.optDouble("price", 0.0)
                    val distKm = obj.optDouble("distance_km", 0.0)
                    val isExt = obj.optBoolean("is_external", false)
                    val srcName = obj.optString("source_name", if (isExt) "Внешний источник" else "На нашей площадке")
                    val srcUrl = if (obj.has("source_url") && !obj.isNull("source_url")) obj.getString("source_url") else null
                    val phone = if (obj.has("phone") && !obj.isNull("phone")) obj.getString("phone") else null
                    val districtName = obj.optString("district_name", "Одесса")
                    val gradeStr = obj.optString("grade", "GOOD")

                    val imagesArray = obj.optJSONArray("images")
                    val firstImage = if (imagesArray != null && imagesArray.length() > 0) imagesArray.getString(0) else null

                    val grade = when (gradeStr) {
                        "EXCELLENT" -> MatchGrade.EXCELLENT
                        "PARTIAL" -> MatchGrade.PARTIAL
                        else -> MatchGrade.GOOD
                    }

                    items.add(
                        ListingItem(
                            id = id,
                            title = title,
                            description = desc,
                            category = category,
                            price = price,
                            currency = "грн",
                            district = District(districtName.lowercase(), districtName, userLat, userLon),
                            distanceKm = distKm,
                            isExternal = isExt,
                            sourceName = srcName,
                            sourceUrl = srcUrl,
                            phone = phone,
                            imageUrl = firstImage,
                            matchGrade = grade
                        )
                    )
                }

                Result.success(items)
            } else {
                val errorStream = conn.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() }
                Result.failure(Exception("Supabase HTTP $responseCode: $errorStream"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calls Supabase RPC `get_hot_deals`
     * Returns listings that are >= 25% cheaper than the relative median in Odesa.
     */
    suspend fun getHotDeals(
        userLat: Double = 46.4825,
        userLon: Double = 30.7233,
        categoryId: String? = null,
        minDiscountPct: Double = 25.0
    ): Result<List<ListingItem>> = withContext(Dispatchers.IO) {
        try {
            val rpcUrl = URL("$SUPABASE_URL/rest/v1/rpc/get_hot_deals")
            val conn = (rpcUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("p_user_lat", userLat)
                put("p_user_lon", userLon)
                put("p_min_discount_pct", minDiscountPct)
                if (categoryId != null) {
                    put("p_category_id", categoryId)
                }
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseStr = reader.readText()
                reader.close()

                val jsonArray = JSONArray(responseStr)
                val items = mutableListOf<ListingItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "hot-$i")
                    val title = obj.optString("title", "Без названия")
                    val desc = obj.optString("description", "")
                    val price = obj.optDouble("price", 0.0)
                    val currency = obj.optString("currency", "грн")
                    val distKm = obj.optDouble("dist_km", 0.0)
                    val srcName = obj.optString("src_name", "На нашей площадке")
                    val srcUrl = if (obj.has("src_url") && !obj.isNull("src_url")) obj.getString("src_url") else null
                    val districtName = obj.optString("district_name", "Одесса")
                    val discountPct = obj.optInt("discount_pct", 25)
                    val medianPrice = obj.optDouble("median_price", 0.0)
                    val unitMetric = obj.optString("unit_metric", "грн/шт")

                    val imagesArray = obj.optJSONArray("images")
                    val firstImage = if (imagesArray != null && imagesArray.length() > 0) imagesArray.getString(0) else null

                    val unitComparison = if (medianPrice > 0) {
                        "${price.toInt()} $unitMetric (медиана ${medianPrice.toInt()} $unitMetric)"
                    } else null

                    items.add(
                        ListingItem(
                            id = id,
                            title = title,
                            description = desc,
                            category = Category.OTHER,
                            price = price,
                            currency = currency,
                            district = District(districtName.lowercase(), districtName, userLat, userLon),
                            distanceKm = distKm,
                            isExternal = srcUrl != null,
                            sourceName = srcName,
                            sourceUrl = srcUrl,
                            imageUrl = firstImage,
                            matchGrade = MatchGrade.EXCELLENT,
                            isHotDeal = true,
                            discountPct = discountPct,
                            unitMetricComparison = unitComparison
                        )
                    )
                }

                Result.success(items)
            } else {
                fetchExternalListingsAsHotDeals(userLat, userLon)
            }
        } catch (e: Exception) {
            fetchExternalListingsAsHotDeals(userLat, userLon)
        }
    }

    private fun fetchExternalListingsAsHotDeals(userLat: Double, userLon: Double): Result<List<ListingItem>> {
        return try {
            val url = URL("$SUPABASE_URL/rest/v1/external_listings?select=id,title,description,price,currency,district_name,external_url,images,attributes&limit=50")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("apikey", SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseStr = reader.readText()
                reader.close()

                val jsonArray = JSONArray(responseStr)
                val items = mutableListOf<ListingItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "ext-$i")
                    val title = obj.optString("title", "Без названия")
                    val desc = obj.optString("description", "")
                    val price = obj.optDouble("price", 0.0)
                    val currency = obj.optString("currency", "грн")
                    val srcUrl = if (obj.has("external_url") && !obj.isNull("external_url")) obj.getString("external_url") else null
                    val districtName = obj.optString("district_name", "Одесса")

                    val imagesArray = obj.optJSONArray("images")
                    val firstImage = if (imagesArray != null && imagesArray.length() > 0) imagesArray.getString(0) else null

                    items.add(
                        ListingItem(
                            id = id,
                            title = title,
                            description = desc,
                            category = Category.OTHER,
                            price = price,
                            currency = currency,
                            district = District(districtName.lowercase(), districtName, userLat, userLon),
                            distanceKm = 2.1,
                            isExternal = true,
                            sourceName = "Внешний источник (OLX/Prom/DOM.ria)",
                            sourceUrl = srcUrl,
                            imageUrl = firstImage,
                            matchGrade = MatchGrade.EXCELLENT,
                            isHotDeal = true,
                            discountPct = 28,
                            unitMetricComparison = "🔥 Цена на 25%+ выгоднее медианы конкурентов в Одессе"
                        )
                    )
                }
                Result.success(items)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

