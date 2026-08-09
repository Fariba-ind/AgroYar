package ir.agroyar.app

import android.content.Context
import android.content.res.Configuration

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

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("agroyar_settings", Context.MODE_PRIVATE)

    fun language(): AppLanguage = runCatching {
        AppLanguage.valueOf(prefs.getString("language", AppLanguage.FA.name) ?: AppLanguage.FA.name)
    }.getOrDefault(AppLanguage.FA)

    fun theme(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM)

    fun saveLanguage(value: AppLanguage) {
        prefs.edit().putString("language", value.name).apply()
    }

    fun saveTheme(value: ThemeMode) {
        prefs.edit().putString("theme", value.name).apply()
    }
}

fun isDarkMode(context: Context, mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> {
        val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        mask == Configuration.UI_MODE_NIGHT_YES
    }
}

class Strings(private val language: AppLanguage) {
    private fun choose(fa: String, en: String) = if (language == AppLanguage.FA) fa else en

    val appName = choose("اگرویار", "AgroYar")
    val smartCompanion = choose("همراه هوشمند کشاورزی", "Smart agriculture companion")
    val settings = choose("تنظیمات", "Settings")
    val back = choose("بازگشت", "Back")
    val pesticideSearch = choose("جست‌وجوی سم", "Pesticide search")
    val cropSearch = choose("جست‌وجو بر اساس محصول", "Search by crop")
    val targetSearch = choose("جست‌وجو بر اساس آفت یا بیماری", "Search by pest or disease")
    val pesticideCatalog = choose("بانک آفت‌کش", "Pesticide catalog")
    val recommendations = choose("توصیه مصرف", "Use recommendations")
    val noResults = choose("نتیجه‌ای پیدا نشد.", "No result found.")
    val searchHintPesticide = choose("نام سم، ماده مؤثره، EC، SC...", "Pesticide, active ingredient, EC, SC...")
    val searchHintCrop = choose("مثلاً گندم، سیب، پسته...", "e.g. wheat, apple, pistachio...")
    val searchHintTarget = choose("مثلاً شته، سفیدک، علف هرز...", "e.g. aphid, mildew, weed...")
    val tradeName = choose("نام تجاری", "Trade name")
    val activeIngredient = choose("ماده مؤثره", "Active ingredient")
    val concentration = choose("درصد/غلظت", "Concentration")
    val formulation = choose("فرمولاسیون", "Formulation")
    val category = choose("گروه", "Category")
    val target = choose("هدف مصرف", "Target")
    val modeOfAction = choose("نحوه اثر", "Mode of action")
    val crops = choose("محصولات/مصارف درج‌شده در منبع", "Crops/uses listed in source")
    val dose = choose("دُز و نحوه مصرف", "Dose and application")
    val restrictions = choose("محدودیت‌ها و منع مصرف", "Restrictions")
    val weather = choose("شرایط آب‌وهوایی", "Weather cautions")
    val waterStress = choose("تنش آبی", "Water-stress cautions")
    val phi = choose("فاصله تا برداشت (PHI)", "Pre-harvest interval (PHI)")
    val crop = choose("محصول", "Crop")
    val recommendedPesticide = choose("آفت‌کش توصیه‌شده", "Recommended pesticide")
    val timing = choose("زمان مصرف", "Application timing")
    val sourceNotes = choose("یادداشت منبع", "Source notes")
    val sourcePage = choose("صفحه منبع", "Source page")
    val languageTitle = choose("زبان", "Language")
    val themeTitle = choose("حالت نمایش", "Appearance")
    val systemTheme = choose("بر اساس تنظیمات گوشی", "System")
    val lightTheme = choose("روشن", "Light")
    val darkTheme = choose("تیره", "Dark")
    val developer = choose("درباره سازنده", "About the developer")
    val developerName = choose("فریبا عسگریان", "Fariba Asgarian")
    val developerRole = choose("طراحی و توسعه AgroYar", "AgroYar design and development")
    val version = choose("نسخه برنامه", "App version")
    val source = choose("منبع داده", "Data source")
    val sourceText = choose("بانک Word مرجع پروژه AgroYar", "Canonical AgroYar project Word bank")
    val dataNotice = choose(
        "این بانک پژوهشی است. پیش از مصرف مزرعه‌ای، برچسب روز فرآورده و وضعیت ثبت رسمی بررسی شود.",
        "This is a research reference. Verify the current product label and official registration before field use."
    )
    val fatalTitle = choose("خطای اجرای AgroYar", "AgroYar startup error")
    val fatalBody = choose("برنامه در حالت ایمن اجرا شد. لطفاً نسخه جدید را نصب کنید یا گزارش خطا را ارسال کنید.", "The app entered safe mode. Install the latest build or share the error report.")
}
