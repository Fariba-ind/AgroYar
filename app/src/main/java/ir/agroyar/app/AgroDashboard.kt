package ir.agroyar.app

import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView

internal object CompatScreens {
    fun showHome(
        activity: MainActivity,
        container: FrameLayout,
        t: Strings,
        colors: Palette,
        pesticides: List<Pesticide>,
        recommendations: List<DetailedRecommendation>
    ) {
        container.removeAllViews()
        val scroll = ScrollView(activity)
        val column = vertical(activity, 16)
        column.setBackgroundColor(colors.background)

        val hero = vertical(activity, 18).apply {
            background = rounded(colors.surfaceAlt, 24, activity, colors.stroke)
            addView(titleText(activity, "AgroYar", colors, 28f))
            addView(bodyText(activity, t.smartCompanion, colors, 16f))
            addView(spacer(activity, 8))
            addView(mutedText(activity, "${t.pesticideCatalog}: ${pesticides.size}    •    ${t.recommendations}: ${recommendations.size}", colors, 14f))
        }
        column.addView(hero, matchWrap(activity))

        val pesticideButton = actionButton(activity, t.pesticideSearch, colors).apply {
            contentDescription = t.pesticideSearch
            setOnClickListener { activity.openPesticideSearch() }
        }
        column.addView(pesticideButton, matchWrap(activity, 12))

        val cropButton = actionButton(activity, t.cropSearch, colors).apply {
            contentDescription = t.cropSearch
            setOnClickListener { activity.openCropSearch() }
        }
        column.addView(cropButton, matchWrap(activity, 10))

        val targetButton = actionButton(activity, t.targetSearch, colors).apply {
            contentDescription = t.targetSearch
            setOnClickListener { activity.openTargetSearch() }
        }
        column.addView(targetButton, matchWrap(activity, 10))

        val notice = bodyText(activity, t.dataNotice, colors, 13f)
        column.addView(sectionCard(activity, colors, t.source, notice, 14))
        column.addView(spacer(activity, 20))
        scroll.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(scroll, matchMatch())
    }

    fun showPesticideSearch(
        activity: MainActivity,
        container: FrameLayout,
        t: Strings,
        colors: Palette,
        catalog: List<Pesticide>,
        initialQuery: String = ""
    ) {
        container.removeAllViews()
        val root = vertical(activity, 12)
        root.setBackgroundColor(colors.background)
        val search = searchField(activity, t.searchHintPesticide, colors, initialQuery)
        val count = mutedText(activity, "", colors)
        val list = ListView(activity).apply {
            dividerHeight = activity.dp(1)
            setBackgroundColor(colors.background)
        }
        root.addView(search, matchWrap(activity))
        root.addView(count, matchWrap(activity, 8))
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = activity.dp(6) })
        container.addView(root, matchMatch())

        var visible = catalog
        fun refresh(query: String) {
            val q = query.trim().lowercase()
            visible = if (q.isBlank()) catalog else catalog.filter { p ->
                listOf(
                    p.scientificName,
                    p.tradeNames.joinToString(" "),
                    p.activeIngredient,
                    p.formulation,
                    p.category,
                    p.target,
                    p.registeredCrops,
                    p.modeOfAction
                ).any { it.lowercase().contains(q) }
            }
            count.text = "${visible.size}"
            list.adapter = SimpleTextAdapter(activity, visible.map { pesticideRow(it, t) }, colors)
        }
        list.setOnItemClickListener { _, _, position, _ -> visible.getOrNull(position)?.let(activity::openPesticideDetail) }
        search.addTextChangedListener(afterTextChanged { refresh(it) })
        refresh(initialQuery)
    }

    fun showRecommendationSearch(
        activity: MainActivity,
        container: FrameLayout,
        t: Strings,
        colors: Palette,
        recommendations: List<DetailedRecommendation>,
        byCrop: Boolean
    ) {
        container.removeAllViews()
        val root = vertical(activity, 12)
        root.setBackgroundColor(colors.background)
        val search = searchField(activity, if (byCrop) t.searchHintCrop else t.searchHintTarget, colors)
        val count = mutedText(activity, "", colors)
        val list = ListView(activity).apply {
            dividerHeight = activity.dp(1)
            setBackgroundColor(colors.background)
        }
        root.addView(search, matchWrap(activity))
        root.addView(count, matchWrap(activity, 8))
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = activity.dp(6) })
        container.addView(root, matchMatch())

        var visible = recommendations
        fun refresh(query: String) {
            val q = query.trim().lowercase()
            visible = if (q.isBlank()) recommendations else recommendations.filter { r ->
                val fields = if (byCrop) {
                    listOf(r.crop, r.target, r.recommendedPesticides, r.formulation)
                } else {
                    listOf(r.target, r.crop, r.recommendedPesticides, r.formulation)
                }
                fields.any { it.lowercase().contains(q) }
            }
            count.text = "${visible.size}"
            list.adapter = SimpleTextAdapter(activity, visible.map { recommendationRow(it, t) }, colors)
        }
        list.setOnItemClickListener { _, _, position, _ -> visible.getOrNull(position)?.let(activity::openRecommendationDetail) }
        search.addTextChangedListener(afterTextChanged { refresh(it) })
        refresh("")
    }

    fun showPesticideDetail(
        activity: MainActivity,
        container: FrameLayout,
        t: Strings,
        colors: Palette,
        item: Pesticide
    ) {
        val fields = listOf(
            t.tradeName to item.tradeNames.joinToString("، "),
            t.activeIngredient to item.activeIngredient,
            t.concentration to item.concentration,
            t.formulation to item.formulation,
            t.category to item.category,
            t.target to item.target,
            t.modeOfAction to item.modeOfAction,
            t.crops to item.registeredCrops,
            t.dose to item.doseGuidance,
            t.restrictions to item.restrictions,
            t.weather to item.weatherCautions,
            t.waterStress to item.waterStressCautions,
            t.phi to item.phi
        )
        showDetail(activity, container, colors, item.scientificName.ifBlank { item.tradeNames.firstOrNull().orEmpty() }, item.sourceStatus, fields)
    }

    fun showRecommendationDetail(
        activity: MainActivity,
        container: FrameLayout,
        t: Strings,
        colors: Palette,
        item: DetailedRecommendation
    ) {
        val fields = listOf(
            t.crop to item.crop,
            t.target to item.target,
            t.recommendedPesticide to item.recommendedPesticides,
            t.formulation to item.formulation,
            t.dose to item.dose,
            t.timing to item.timing,
            t.sourceNotes to item.sourceNotes,
            t.sourcePage to item.pdfPage
        )
        showDetail(activity, container, colors, t.recommendations, "", fields)
    }

    private fun showDetail(
        activity: MainActivity,
        container: FrameLayout,
        colors: Palette,
        heading: String,
        subtitle: String,
        fields: List<Pair<String, String>>
    ) {
        container.removeAllViews()
        val scroll = ScrollView(activity)
        val column = vertical(activity, 14)
        column.setBackgroundColor(colors.background)
        column.addView(titleText(activity, heading, colors, 23f))
        if (subtitle.isNotBlank()) column.addView(mutedText(activity, subtitle, colors, 12f), matchWrap(activity, 6))
        fields.filter { it.second.isNotBlank() }.forEach { (label, value) ->
            val body = bodyText(activity, value, colors, 14f)
            column.addView(sectionCard(activity, colors, label, body))
        }
        column.addView(spacer(activity, 20))
        scroll.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(scroll, matchMatch())
    }

    private fun pesticideRow(p: Pesticide, t: Strings): String {
        val first = p.scientificName.ifBlank { p.tradeNames.firstOrNull().orEmpty() }
        val second = p.tradeNames.joinToString("، ")
        val third = listOf(p.activeIngredient, p.formulation).filter { it.isNotBlank() }.joinToString(" • ")
        return listOf(first, second, third).filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun recommendationRow(r: DetailedRecommendation, t: Strings): String {
        val top = listOf(r.crop, r.target).filter { it.isNotBlank() }.joinToString(" — ")
        val second = r.recommendedPesticides
        val third = listOf(r.formulation, r.dose).filter { it.isNotBlank() }.joinToString(" • ")
        return listOf(top, second, third).filter { it.isNotBlank() }.joinToString("\n")
    }

    private fun searchField(activity: MainActivity, hint: String, colors: Palette, value: String = ""): EditText =
        EditText(activity).apply {
            setText(value)
            this.hint = hint
            setTextColor(colors.text)
            setHintTextColor(colors.secondaryText)
            setSingleLine(true)
            textSize = 16f
            setPadding(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10))
            background = rounded(colors.surface, 16, activity, colors.stroke)
        }

    private fun afterTextChanged(block: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = block(s?.toString().orEmpty())
    }

    private fun matchMatch() = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    private fun matchWrap(activity: MainActivity, topMargin: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        this.topMargin = activity.dp(topMargin)
    }
}

private class SimpleTextAdapter(
    private val activity: MainActivity,
    private val rows: List<String>,
    private val colors: Palette
) : BaseAdapter() {
    override fun getCount() = rows.size
    override fun getItem(position: Int) = rows[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val text = (convertView as? TextView) ?: TextView(activity).apply {
            setPadding(activity.dp(14), activity.dp(12), activity.dp(14), activity.dp(12))
            textSize = 15f
            setLineSpacing(0f, 1.15f)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        text.text = rows[position]
        text.setTextColor(colors.text)
        text.setTypeface(null, Typeface.NORMAL)
        text.background = rounded(colors.surface, 12, activity, colors.stroke)
        return text
    }
}
