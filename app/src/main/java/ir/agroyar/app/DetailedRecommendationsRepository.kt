package ir.agroyar.app

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

data class DetailedRecommendation(
    val id: String,
    val crop: String,
    val target: String,
    val recommendedPesticides: String,
    val formulation: String,
    val dose: String,
    val timing: String,
    val sourceNotes: String,
    val pdfPage: String
)

object DetailedRecommendationsRepository {
    const val PREVIEW_RECORDS = 25
    const val FULL_SOURCE_RECORDS = 389
    private const val ASSET = "recommendations.preview.b64"

    fun load(context: Context): List<DetailedRecommendation> = runCatching {
        val array = JSONArray(loadCompressedJson(context))
        buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toRecommendation(index))
            }
        }
    }.getOrElse { emptyList() }

    private fun loadCompressedJson(context: Context): String {
        val encoded = context.assets.open(ASSET)
            .bufferedReader(Charsets.US_ASCII)
            .use { it.readText().filterNot(Char::isWhitespace) }
        val compressed = Base64.decode(encoded, Base64.DEFAULT)
        return GZIPInputStream(ByteArrayInputStream(compressed))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun JSONObject.toRecommendation(index: Int) = DetailedRecommendation(
        id = text("id").ifBlank { "recommendation-$index" },
        crop = text("crop"),
        target = text("target"),
        recommendedPesticides = text("recommendedPesticides"),
        formulation = text("formulation"),
        dose = text("dose"),
        timing = text("timing"),
        sourceNotes = text("sourceNotes"),
        pdfPage = text("pdfPage")
    )

    private fun JSONObject.text(key: String): String = optString(key, "").trim()
}
