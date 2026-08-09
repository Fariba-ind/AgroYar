package ir.agroyar.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var store: SettingsStore
    private lateinit var t: Strings
    private lateinit var colors: Palette
    private lateinit var content: FrameLayout
    private lateinit var backButton: Button
    private lateinit var settingsButton: Button
    private lateinit var titleView: TextView
    private var language = AppLanguage.FA
    private var themeMode = ThemeMode.SYSTEM
    private var pesticides: List<Pesticide> = emptyList()
    private var recommendations: List<DetailedRecommendation> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            store = SettingsStore(this)
            language = store.language()
            themeMode = store.theme()
            t = Strings(language)
            colors = palette(isDarkMode(this, themeMode))
            configureWindow()
            loadDataSafely()
            buildShell()
            openHome()
        } catch (error: Throwable) {
            showSafeMode(error)
        }
    }

    private fun configureWindow() {
        window.statusBarColor = colors.background
        window.navigationBarColor = colors.background
        if (Build.VERSION.SDK_INT >= 23 && !isDarkMode(this, themeMode)) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun loadDataSafely() {
        pesticides = runCatching { WordCatalogRepository.load(applicationContext) }.getOrDefault(emptyList())
        recommendations = runCatching { DetailedRecommendationsRepository.load(applicationContext) }.getOrDefault(emptyList())
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.background)
            layoutDirection = if (language == AppLanguage.FA) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(colors.surface)
        }
        backButton = Button(this).apply {
            text = if (language == AppLanguage.FA) "← ${t.back}" else "← ${t.back}"
            isAllCaps = false
            visibility = View.GONE
            setTextColor(colors.primary)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = t.back
            setOnClickListener { openHome() }
        }
        titleView = titleText(this, t.appName, colors, 21f).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        settingsButton = Button(this).apply {
            text = "⚙"
            textSize = 22f
            setTextColor(colors.primary)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = t.settings
            setOnClickListener { openSettings() }
        }
        top.addView(backButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)))
        top.addView(titleView, LinearLayout.LayoutParams(0, dp(48), 1f))
        top.addView(settingsButton, LinearLayout.LayoutParams(dp(56), dp(48)))

        content = FrameLayout(this).apply { setBackgroundColor(colors.background) }
        root.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun setTopBar(title: String, canGoBack: Boolean, showSettings: Boolean = false) {
        titleView.text = title
        backButton.visibility = if (canGoBack) View.VISIBLE else View.GONE
        settingsButton.visibility = if (showSettings) View.VISIBLE else View.GONE
    }

    fun openHome() {
        runScreen(t.appName, canGoBack = false, showSettings = true) {
            CompatScreens.showHome(this, content, t, colors, pesticides, recommendations)
        }
    }

    fun openPesticideSearch(initialQuery: String = "") {
        runScreen(t.pesticideSearch, true) {
            CompatScreens.showPesticideSearch(this, content, t, colors, pesticides, initialQuery)
        }
    }

    fun openCropSearch() {
        runScreen(t.cropSearch, true) {
            CompatScreens.showRecommendationSearch(this, content, t, colors, recommendations, byCrop = true)
        }
    }

    fun openTargetSearch() {
        runScreen(t.targetSearch, true) {
            CompatScreens.showRecommendationSearch(this, content, t, colors, recommendations, byCrop = false)
        }
    }

    fun openPesticideDetail(item: Pesticide) {
        runScreen(t.pesticideSearch, true) {
            CompatScreens.showPesticideDetail(this, content, t, colors, item)
        }
    }

    fun openRecommendationDetail(item: DetailedRecommendation) {
        runScreen(t.recommendations, true) {
            CompatScreens.showRecommendationDetail(this, content, t, colors, item)
        }
    }

    private fun runScreen(title: String, canGoBack: Boolean, showSettings: Boolean = false, block: () -> Unit) {
        try {
            setTopBar(title, canGoBack, showSettings)
            block()
        } catch (error: Throwable) {
            showInlineError(error)
        }
    }

    private fun openSettings() {
        runScreen(t.settings, true) {
            content.removeAllViews()
            val scroll = ScrollView(this)
            val column = vertical(this, 16)
            column.setBackgroundColor(colors.background)

            val languageGroup = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL
                addView(choice("فارسی", language == AppLanguage.FA) {
                    store.saveLanguage(AppLanguage.FA)
                    recreate()
                })
                addView(choice("English", language == AppLanguage.EN) {
                    store.saveLanguage(AppLanguage.EN)
                    recreate()
                })
            }
            column.addView(sectionCard(this, colors, t.languageTitle, languageGroup, 0))

            val themeGroup = RadioGroup(this).apply {
                orientation = RadioGroup.VERTICAL
                addView(choice(t.systemTheme, themeMode == ThemeMode.SYSTEM) {
                    store.saveTheme(ThemeMode.SYSTEM)
                    recreate()
                })
                addView(choice(t.lightTheme, themeMode == ThemeMode.LIGHT) {
                    store.saveTheme(ThemeMode.LIGHT)
                    recreate()
                })
                addView(choice(t.darkTheme, themeMode == ThemeMode.DARK) {
                    store.saveTheme(ThemeMode.DARK)
                    recreate()
                })
            }
            column.addView(sectionCard(this, colors, t.themeTitle, themeGroup))

            val sourceBody = bodyText(this, t.sourceText, colors)
            column.addView(sectionCard(this, colors, t.source, sourceBody))

            val developerBody = vertical(this).apply {
                addView(titleText(this@MainActivity, t.developerName, colors, 17f))
                addView(bodyText(this@MainActivity, t.developerRole, colors, 14f), matchWrap(4))
                val repository = actionButton(this@MainActivity, "GitHub: Fariba-ind/AgroYar", colors).apply {
                    setOnClickListener { openWeb("https://github.com/Fariba-ind/AgroYar") }
                }
                addView(repository, matchWrap(8))
            }
            column.addView(sectionCard(this, colors, t.developer, developerBody))

            val versionBody = vertical(this).apply {
                addView(bodyText(this@MainActivity, "${t.version}: ${BuildConfig.VERSION_NAME}", colors))
                val check = actionButton(this@MainActivity, if (language == AppLanguage.FA) "بررسی نسخه جدید" else "Check for updates", colors)
                val result = mutedText(this@MainActivity, "", colors)
                check.setOnClickListener {
                    check.isEnabled = false
                    result.text = if (language == AppLanguage.FA) "در حال بررسی..." else "Checking..."
                    checkForUpdateAsync(check, result)
                }
                addView(check, matchWrap(8))
                addView(result, matchWrap(8))
            }
            column.addView(sectionCard(this, colors, t.version, versionBody))
            column.addView(bodyText(this, t.dataNotice, colors, 12f), matchWrap(14))
            column.addView(spacer(this, 20))

            scroll.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            content.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private fun choice(label: String, checked: Boolean, action: () -> Unit): RadioButton = RadioButton(this).apply {
        text = label
        isChecked = checked
        setTextColor(colors.text)
        textSize = 15f
        setPadding(dp(4), dp(4), dp(4), dp(4))
        setOnClickListener { action() }
    }

    private fun checkForUpdateAsync(button: Button, result: TextView) {
        Thread {
            val message = runCatching {
                val connection = URL("https://api.github.com/repos/Fariba-ind/AgroYar/releases/latest").openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "AgroYar-Android")
                val code = connection.responseCode
                if (code == 404) {
                    connection.disconnect()
                    return@runCatching if (language == AppLanguage.FA) "هنوز نسخه Release منتشر نشده است." else "No release is published yet."
                }
                if (code !in 200..299) {
                    connection.disconnect()
                    return@runCatching if (language == AppLanguage.FA) "بررسی نسخه انجام نشد." else "Update check failed."
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val latest = JSONObject(body).optString("tag_name").removePrefix("v")
                if (latest.isBlank()) {
                    if (language == AppLanguage.FA) "اطلاعات نسخه معتبر نبود." else "Invalid release information."
                } else if (compareVersions(latest, BuildConfig.VERSION_NAME) > 0) {
                    if (language == AppLanguage.FA) "نسخه $latest موجود است." else "Version $latest is available."
                } else {
                    if (language == AppLanguage.FA) "برنامه به‌روز است." else "The app is up to date."
                }
            }.getOrElse {
                if (language == AppLanguage.FA) "اتصال به سرور نسخه انجام نشد." else "Could not reach the update server."
            }
            runOnUiThread {
                if (!isFinishing) {
                    result.text = message
                    button.isEnabled = true
                }
            }
        }.apply { name = "AgroYar-UpdateCheck" }.start()
    }

    private fun compareVersions(a: String, b: String): Int {
        val left = a.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val right = b.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(left.size, right.size)
        for (i in 0 until size) {
            val x = left.getOrElse(i) { 0 }
            val y = right.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun openWeb(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Throwable) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }

    private fun showInlineError(error: Throwable) {
        content.removeAllViews()
        val message = TextView(this).apply {
            text = "${t.fatalBody}\n\n${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            setTextColor(colors.text)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        content.addView(message, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun showSafeMode(error: Throwable) {
        val dark = false
        val safeColors = palette(dark)
        window.statusBarColor = safeColors.background
        window.navigationBarColor = safeColors.background
        val root = vertical(this, 24).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(safeColors.background)
        }
        val safeLanguage = runCatching { SettingsStore(this).language() }.getOrDefault(AppLanguage.FA)
        val safeStrings = Strings(safeLanguage)
        root.addView(titleText(this, safeStrings.fatalTitle, safeColors, 22f))
        root.addView(bodyText(this, safeStrings.fatalBody, safeColors, 15f), matchWrap(14))
        root.addView(mutedText(this, "${error.javaClass.name}\n${error.message.orEmpty()}", safeColors, 12f), matchWrap(14))
        setContentView(root)
    }

    private fun matchWrap(topMargin: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        this.topMargin = dp(topMargin)
    }
}
