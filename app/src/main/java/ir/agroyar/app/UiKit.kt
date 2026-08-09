package ir.agroyar.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal data class Palette(
    val background: Int,
    val surface: Int,
    val surfaceAlt: Int,
    val text: Int,
    val secondaryText: Int,
    val primary: Int,
    val onPrimary: Int,
    val stroke: Int
)

internal fun palette(dark: Boolean): Palette = if (dark) {
    Palette(
        background = Color.rgb(18, 24, 18),
        surface = Color.rgb(29, 38, 29),
        surfaceAlt = Color.rgb(38, 50, 37),
        text = Color.rgb(240, 244, 236),
        secondaryText = Color.rgb(190, 203, 185),
        primary = Color.rgb(139, 203, 109),
        onPrimary = Color.rgb(10, 45, 22),
        stroke = Color.rgb(72, 91, 69)
    )
} else {
    Palette(
        background = Color.rgb(248, 250, 243),
        surface = Color.WHITE,
        surfaceAlt = Color.rgb(238, 243, 229),
        text = Color.rgb(25, 35, 25),
        secondaryText = Color.rgb(83, 96, 79),
        primary = Color.rgb(46, 125, 50),
        onPrimary = Color.WHITE,
        stroke = Color.rgb(210, 221, 202)
    )
}

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

internal fun rounded(color: Int, radiusDp: Int, context: Context, stroke: Int? = null): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = context.dp(radiusDp).toFloat()
        stroke?.let { setStroke(context.dp(1), it) }
    }

internal fun vertical(context: Context, paddingDp: Int = 0, spacingDp: Int = 0): LinearLayout =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        if (paddingDp > 0) setPadding(context.dp(paddingDp), context.dp(paddingDp), context.dp(paddingDp), context.dp(paddingDp))
        if (spacingDp > 0 && android.os.Build.VERSION.SDK_INT >= 21) dividerPadding = context.dp(spacingDp)
    }

internal fun titleText(context: Context, text: String, colors: Palette, sizeSp: Float = 22f): TextView =
    TextView(context).apply {
        this.text = text
        setTextColor(colors.text)
        textSize = sizeSp
        setTypeface(typeface, Typeface.BOLD)
    }

internal fun bodyText(context: Context, text: String, colors: Palette, sizeSp: Float = 15f): TextView =
    TextView(context).apply {
        this.text = text
        setTextColor(colors.text)
        textSize = sizeSp
        setLineSpacing(0f, 1.15f)
    }

internal fun mutedText(context: Context, text: String, colors: Palette, sizeSp: Float = 13f): TextView =
    TextView(context).apply {
        this.text = text
        setTextColor(colors.secondaryText)
        textSize = sizeSp
    }

internal fun actionButton(context: Context, text: String, colors: Palette): Button =
    Button(context).apply {
        this.text = text
        setTextColor(colors.onPrimary)
        textSize = 15f
        isAllCaps = false
        background = rounded(colors.primary, 14, context)
        minHeight = context.dp(48)
    }

internal fun sectionCard(
    context: Context,
    colors: Palette,
    title: String,
    body: View,
    topMarginDp: Int = 10
): LinearLayout = vertical(context, 14).apply {
    background = rounded(colors.surface, 18, context, colors.stroke)
    addView(titleText(context, title, colors, 17f))
    addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = context.dp(8)
    })
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = context.dp(topMarginDp)
    }
}

internal fun spacer(context: Context, heightDp: Int): View = View(context).apply {
    layoutParams = LinearLayout.LayoutParams(1, context.dp(heightDp))
}

internal fun centeredMessage(context: Context, text: String, colors: Palette): TextView =
    bodyText(context, text, colors).apply {
        gravity = Gravity.CENTER
        setPadding(context.dp(16), context.dp(30), context.dp(16), context.dp(30))
    }
