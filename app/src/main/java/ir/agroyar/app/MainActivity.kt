package ir.agroyar.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgroYarRoot() }
    }
}

enum class AppLanguage { FA, EN }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Pesticide(
    val id: String,
    val scientificName: String,
    val tradeNames: List<String>,
    val activeIngredient: String,
    val concentration: String,
    val formulation: String,
    val category: String,
    val target: String,
    val modeOfAction: String,
    val registeredCrops: String,
    val doseGuidance: String,
    val restrictions: String,
    val weatherCautions: String,
    val waterStressCautions: String,
    val phi: String,
    val sourceStatus: String
)

private class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("agroyar_settings", Context.MODE_PRIVATE)

    fun language(): AppLanguage = runCatching {
        AppLanguage.valueOf(prefs.getString("language", AppLanguage.FA.name) ?: AppLanguage.FA.name)
    }.getOrDefault(AppLanguage.FA)

    fun theme(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM)

    fun saveLanguage(value: AppLanguage) = prefs.edit().putString("language", value.name).apply()
    fun saveTheme(value: ThemeMode) = prefs.edit().putString("theme", value.name).apply()
}

@Composable
private fun AgroYarRoot() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var language by remember { mutableStateOf(store.language()) }
    var themeMode by remember { mutableStateOf(store.theme()) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val direction = if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AgroYarApp(
                    language = language,
                    themeMode = themeMode,
                    onLanguageChange = {
                        language = it
                        store.saveLanguage(it)
                    },
                    onThemeChange = {
                        themeMode = it
                        store.saveTheme(it)
                    }
                )
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Detail(val pesticide: Pesticide) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgroYarApp(
    language: AppLanguage,
    themeMode: ThemeMode,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val t = Strings(language)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t.appName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (screen !is Screen.Home) {
                        IconButton(onClick = { screen = Screen.Home }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = t.back)
                        }
                    }
                },
                actions = {
                    if (screen is Screen.Home) {
                        IconButton(onClick = { screen = Screen.Settings }) {
                            Icon(Icons.Default.Settings, contentDescription = t.settings)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (val current = screen) {
                Screen.Home -> HomeScreen(t) { screen = Screen.Detail(it) }
                Screen.Settings -> SettingsScreen(
                    t = t,
                    language = language,
                    themeMode = themeMode,
                    onLanguageChange = onLanguageChange,
                    onThemeChange = onThemeChange
                )
                is Screen.Detail -> DetailScreen(current.pesticide, t)
            }
        }
    }
}

@Composable
private fun HomeScreen(t: Strings, onOpen: (Pesticide) -> Unit) {
    val context = LocalContext.current
    val catalog = remember { WordCatalogRepository.load(context) }
    var query by remember { mutableStateOf("") }
    val normalized = query.trim().lowercase()
    val results = remember(normalized, catalog) {
        if (normalized.isBlank()) {
            catalog
        } else {
            catalog.filter { item ->
                listOf(
                    item.scientificName,
                    item.tradeNames.joinToString(" "),
                    item.activeIngredient,
                    item.formulation,
                    item.category,
                    item.target,
                    item.registeredCrops
                ).any { it.lowercase().contains(normalized) }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(t.searchIntro, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(t.searchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Text(if (catalog.isEmpty()) t.wordSourcePending else t.wordSourceActive)
            }
        }

        Text(t.results(results.size), fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { pesticide ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(pesticide) }
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            pesticide.scientificName.ifBlank { pesticide.tradeNames.firstOrNull().orEmpty() },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (pesticide.tradeNames.isNotEmpty()) {
                            Text("${t.tradeName}: ${pesticide.tradeNames.joinToString()}")
                        }
                        if (pesticide.activeIngredient.isNotBlank()) {
                            Text("${t.activeIngredient}: ${pesticide.activeIngredient}")
                        }
                        if (pesticide.formulation.isNotBlank()) {
                            Text("${t.formulation}: ${pesticide.formulation}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(item: Pesticide, t: Strings) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                item.scientificName.ifBlank { item.tradeNames.firstOrNull().orEmpty() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (item.sourceStatus.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.sourceStatus, style = MaterialTheme.typography.bodySmall)
            }
        }
        detailItem(t.tradeName, item.tradeNames.joinToString())
        detailItem(t.activeIngredient, item.activeIngredient)
        detailItem(t.concentration, item.concentration)
        detailItem(t.formulation, item.formulation)
        detailItem(t.category, item.category)
        detailItem(t.target, item.target)
        detailItem(t.modeOfAction, item.modeOfAction)
        detailItem(t.registeredCrops, item.registeredCrops)
        detailItem(t.dose, item.doseGuidance)
        detailItem(t.restrictions, item.restrictions)
        detailItem(t.weather, item.weatherCautions)
        detailItem(t.waterStress, item.waterStressCautions)
        detailItem(t.phi, item.phi)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailItem(title: String, value: String) {
    if (value.isBlank()) return
    item {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(value)
            }
        }
    }
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Loading : UpdateState
    data class Result(val message: String, val url: String? = null) : UpdateState
}

@Composable
private fun SettingsScreen(
    t: Strings,
    language: AppLanguage,
    themeMode: ThemeMode,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SettingSection(t.language) {
                ChoiceRow(
                    label = "فارسی",
                    selected = language == AppLanguage.FA,
                    onClick = { onLanguageChange(AppLanguage.FA) }
                )
                ChoiceRow(
                    label = "English",
                    selected = language == AppLanguage.EN,
                    onClick = { onLanguageChange(AppLanguage.EN) }
                )
            }
        }
        item {
            SettingSection(t.theme) {
                ThemeMode.entries.forEach { value ->
                    ChoiceRow(
                        label = t.themeLabel(value),
                        selected = themeMode == value,
                        onClick = { onThemeChange(value) }
                    )
                }
            }
        }
        item {
            SettingSection(t.dataSource) {
                Text(t.wordSourceDescription)
            }
        }
        item {
            SettingSection(t.update) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${t.currentVersion}: ${BuildConfig.VERSION_NAME}")
                    Button(
                        enabled = updateState !is UpdateState.Loading,
                        onClick = {
                            updateState = UpdateState.Loading
                            scope.launch { updateState = checkForUpdate(t) }
                        }
                    ) {
                        if (updateState is UpdateState.Loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(t.checkUpdate)
                    }
                    val result = updateState as? UpdateState.Result
                    if (result != null) {
                        Text(result.message)
                        if (result.url != null) {
                            TextButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                            }) {
                                Text(t.openRelease)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                t.dataNotice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private suspend fun checkForUpdate(t: Strings): UpdateState.Result = withContext(Dispatchers.IO) {
    try {
        val connection = (
            URL("https://api.github.com/repos/Fariba-ind/AgroYar/releases/latest")
                .openConnection() as HttpURLConnection
            ).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AgroYar-Android")
        }
        if (connection.responseCode == 404) {
            connection.disconnect()
            return@withContext UpdateState.Result(t.noRelease)
        }
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            return@withContext UpdateState.Result(t.updateError)
        }
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val json = JSONObject(body)
        val latest = json.optString("tag_name").removePrefix("v")
        val page = json.optString("html_url").takeIf { it.startsWith("https://") }
        if (latest.isBlank()) return@withContext UpdateState.Result(t.updateError)

        if (compareVersions(latest, BuildConfig.VERSION_NAME) > 0) {
            UpdateState.Result(t.updateAvailable(latest), page)
        } else {
            UpdateState.Result(t.upToDate)
        }
    } catch (_: Exception) {
        UpdateState.Result(t.updateError)
    }
}

private fun compareVersions(a: String, b: String): Int {
    val left = a.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val right = b.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val size = maxOf(left.size, right.size)
    for (index in 0 until size) {
        val x = left.getOrElse(index) { 0 }
        val y = right.getOrElse(index) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}

private class Strings(private val language: AppLanguage) {
    private fun choose(fa: String, en: String) = if (language == AppLanguage.FA) fa else en

    val appName = choose("اگرویار", "AgroYar")
    val back = choose("بازگشت", "Back")
    val settings = choose("تنظیمات", "Settings")
    val searchIntro = choose(
        "نام علمی، نام تجاری، ماده مؤثره، فرمولاسیون، آفت یا محصول را جست‌وجو کنید.",
        "Search by scientific name, trade name, active ingredient, formulation, target, or crop."
    )
    val searchHint = choose("جست‌وجوی سم", "Search pesticide")
    val wordSourcePending = choose(
        "منبع Word هنوز وارد کاتالوگ نشده است؛ داده ساختگی نمایش داده نمی‌شود.",
        "The Word source has not been imported yet; no fabricated records are shown."
    )
    val wordSourceActive = choose(
        "کاتالوگ از فایل Word مرجع پروژه تولید شده است.",
        "The catalog is generated from the project's canonical Word source."
    )
    val activeIngredient = choose("ماده مؤثره", "Active ingredient")
    val tradeName = choose("نام تجاری", "Trade name")
    val concentration = choose("درصد/غلظت ماده مؤثره", "Active concentration")
    val formulation = choose("فرمولاسیون", "Formulation")
    val category = choose("گروه", "Category")
    val target = choose("هدف مصرف", "Target")
    val modeOfAction = choose("نحوه اثر", "Mode of action")
    val registeredCrops = choose("محصولات مجاز", "Registered crops")
    val dose = choose("دُز و نحوه مصرف", "Dose and application")
    val restrictions = choose("محدودیت‌ها و منع مصرف", "Restrictions")
    val weather = choose("شرایط آب‌وهوایی", "Weather cautions")
    val waterStress = choose("تنش آبی", "Water-stress cautions")
    val phi = choose("فاصله تا برداشت (PHI)", "Pre-harvest interval (PHI)")
    val language = choose("زبان", "Language")
    val theme = choose("حالت نمایش", "Appearance")
    val dataSource = choose("منبع داده", "Data source")
    val wordSourceDescription = choose(
        "فایل Word پروژه منبع مرجع است و هنگام آماده‌سازی نسخه برنامه به JSON قابل‌جست‌وجو تبدیل می‌شود.",
        "The project Word document is the canonical source and is converted into searchable JSON for the app."
    )
    val update = choose("به‌روزرسانی برنامه", "App update")
    val currentVersion = choose("نسخه فعلی", "Current version")
    val checkUpdate = choose("بررسی نسخه جدید", "Check for updates")
    val openRelease = choose("باز کردن صفحه نسخه", "Open release page")
    val noRelease = choose(
        "هنوز نسخه رسمی در GitHub Releases منتشر نشده است.",
        "No official GitHub Release has been published yet."
    )
    val updateError = choose(
        "بررسی نسخه انجام نشد. اتصال اینترنت یا GitHub را بررسی کنید.",
        "Could not check for updates. Check internet/GitHub access."
    )
    val upToDate = choose("برنامه به‌روز است.", "The app is up to date.")
    val dataNotice = choose(
        "نکته ایمنی: دُز، ثبت محصول، دوره کارنس و محدودیت‌ها باید دقیقاً از منبع معتبر ثبت‌شده در فایل Word و برچسب رسمی همان فرآورده استخراج شده باشند.",
        "Safety: dose, crop registration, PHI and restrictions must be traceable to the authoritative source recorded in the Word document and the exact product label."
    )

    fun results(count: Int) = choose("نتایج: $count", "Results: $count")
    fun updateAvailable(version: String) = choose(
        "نسخه جدید $version موجود است.",
        "Version $version is available."
    )

    fun themeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> choose("بر اساس تنظیمات گوشی", "Use system setting")
        ThemeMode.LIGHT -> choose("روشن", "Light")
        ThemeMode.DARK -> choose("تیره", "Dark")
    }
}
