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

    private val CATEGORY_UUID_MAP = mapOf(
        Category.POWER_GENERATORS to "c0000000-0000-0000-0000-000000000001",
        Category.APARTMENT_RENT to "c0000000-0000-0000-0000-000000000002",
        Category.SERVICES to "c0000000-0000-0000-0000-000000000003",
        Category.ELECTRONICS to "c0000000-0000-0000-0000-000000000004",
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
}
