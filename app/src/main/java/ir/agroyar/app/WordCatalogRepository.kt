package ir.agroyar.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WordCatalogRepository {
    fun load(context: Context): List<Pesticide> = runCatching {
        val text = context.assets.open("pesticides.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val array = JSONArray(text)
        buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toPesticide(index))
            }
        }
    }.getOrElse { emptyList() }

    private fun JSONObject.toPesticide(index: Int): Pesticide = Pesticide(
        id = text("id").ifBlank { "word-record-$index" },
        scientificName = text("scientificName"),
        tradeNames = stringList("tradeNames"),
        activeIngredient = text("activeIngredient"),
        concentration = text("concentration"),
        formulation = text("formulation"),
        category = text("category"),
        target = text("target"),
        modeOfAction = text("modeOfAction"),
        registeredCrops = text("registeredCrops"),
        doseGuidance = text("doseGuidance"),
        restrictions = text("restrictions"),
        weatherCautions = text("weatherCautions"),
        waterStressCautions = text("waterStressCautions"),
        phi = text("phi"),
        sourceStatus = text("sourceStatus").ifBlank { "Imported from project Word source" }
    )

    private fun JSONObject.text(key: String): String = optString(key, "").trim()

    private fun JSONObject.stringList(key: String): List<String> {
        val value = opt(key)
        return when (value) {
            is JSONArray -> buildList {
                for (index in 0 until value.length()) {
                    value.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
            is String -> value.split(';', '؛', ',', '،')
                .map(String::trim)
                .filter(String::isNotBlank)
            else -> emptyList()
        }
    }
}
