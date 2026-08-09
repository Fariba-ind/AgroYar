package ir.agroyar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface DashboardRoute {
    data object Dashboard : DashboardRoute
    data class PesticideSearch(val initialQuery: String = "") : DashboardRoute
    data class CropSearch(val initialQuery: String = "") : DashboardRoute
    data class TargetSearch(val initialQuery: String = "") : DashboardRoute
    data class RecommendationDetail(val recommendation: DetailedRecommendation) : DashboardRoute
}

private class DashboardStrings(private val language: AppLanguage) {
    private fun choose(fa: String, en: String) = if (language == AppLanguage.FA) fa else en

    val greeting = choose("همراه هوشمند کشاورزی", "Your smart agriculture companion")
    val intro = choose(
        "اطلاعات آفت‌کش‌ها و توصیه‌های مصرف را از بانک مرجع AgroYar جست‌وجو کنید.",
        "Search AgroYar's reference catalog for pesticides and crop-use recommendations."
    )
    val pesticideSearch = choose("جست‌وجوی سم", "Pesticide search")
    val pesticideSearchDesc = choose(
        "نام علمی، نام تجاری، ماده مؤثره یا فرمولاسیون",
        "Scientific name, trade name, active ingredient or formulation"
    )
    val cropSearch = choose("جست‌وجو بر اساس محصول", "Search by crop")
    val cropSearchDesc = choose(
        "محصول را انتخاب کنید و آفت‌کش‌ها، دُز و زمان مصرف را ببینید",
        "Choose a crop to see pesticides, rates and application timing"
    )
    val targetSearch = choose("جست‌وجو بر اساس آفت یا بیماری", "Search by pest or disease")
    val targetSearchDesc = choose(
        "آفت، بیماری یا علف هرز را جست‌وجو کنید",
        "Search a pest, disease or weed"
    )
    val pesticideCatalog = choose("بانک آفت‌کش", "Pesticide catalog")
    val detailedRecommendations = choose("توصیه مصرف", "Use recommendations")
    val records = choose("رکورد", "records")
    val sourceNotice = choose(
        "داده‌ها از فایل Word مرجع پروژه استخراج شده‌اند. برای مصرف مزرعه‌ای، برچسب روز فرآورده و منابع رسمی ملاک نهایی هستند.",
        "Data are extracted from the project's canonical Word source. For field use, the current product label and official sources remain authoritative."
    )
    val searchPesticideHint = choose("نام سم، ماده مؤثره، EC، SC...", "Pesticide, active ingredient, EC, SC...")
    val searchCropHint = choose("مثلاً گندم، سیب، پسته...", "e.g. wheat, apple, pistachio...")
    val searchTargetHint = choose("مثلاً شته، سفیدک، علف هرز...", "e.g. aphid, mildew, weed...")
    val suggestions = choose("پیشنهادها", "Suggestions")
    val results = choose("نتایج", "Results")
    val noResults = choose("نتیجه‌ای در بانک مرجع پیدا نشد.", "No matching record was found in the reference catalog.")
    val back = choose("بازگشت", "Back")
    val tradeName = choose("نام تجاری", "Trade name")
    val activeIngredient = choose("ماده مؤثره", "Active ingredient")
    val formulation = choose("فرمولاسیون", "Formulation")
    val crop = choose("محصول", "Crop")
    val target = choose("آفت / بیماری / علف هرز", "Pest / disease / weed")
    val pesticide = choose("آفت‌کش توصیه‌شده", "Recommended pesticide")
    val dose = choose("دُز مصرف", "Application rate")
    val timing = choose("زمان مصرف", "Application timing")
    val sourceNotes = choose("یادداشت منبع", "Source notes")
    val sourcePage = choose("صفحه منبع", "Source page")
    val recommendationDetail = choose("جزئیات توصیه", "Recommendation detail")
    val searchThisPesticide = choose("جست‌وجوی این سم در بانک آفت‌کش‌ها", "Find this pesticide in the catalog")
}

@Composable
fun AgroDashboard(
    language: AppLanguage,
    onOpenPesticide: (Pesticide) -> Unit
) {
    val t = remember(language) { DashboardStrings(language) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val pesticides = remember { WordCatalogRepository.load(context) }
    val recommendations = remember { DetailedRecommendationsRepository.load(context) }
    var route by remember { mutableStateOf<DashboardRoute>(DashboardRoute.Dashboard) }

    when (val current = route) {
        DashboardRoute.Dashboard -> DashboardHome(
            t = t,
            pesticideCount = pesticides.size,
            recommendationCount = recommendations.size,
            onPesticides = { route = DashboardRoute.PesticideSearch() },
            onCrops = { route = DashboardRoute.CropSearch() },
            onTargets = { route = DashboardRoute.TargetSearch() }
        )
        is DashboardRoute.PesticideSearch -> PesticideSearchPage(
            t = t,
            catalog = pesticides,
            initialQuery = current.initialQuery,
            onBack = { route = DashboardRoute.Dashboard },
            onOpen = onOpenPesticide
        )
        is DashboardRoute.CropSearch -> RecommendationSearchPage(
            t = t,
            mode = RecommendationSearchMode.CROP,
            recommendations = recommendations,
            initialQuery = current.initialQuery,
            onBack = { route = DashboardRoute.Dashboard },
            onOpen = { route = DashboardRoute.RecommendationDetail(it) }
        )
        is DashboardRoute.TargetSearch -> RecommendationSearchPage(
            t = t,
            mode = RecommendationSearchMode.TARGET,
            recommendations = recommendations,
            initialQuery = current.initialQuery,
            onBack = { route = DashboardRoute.Dashboard },
            onOpen = { route = DashboardRoute.RecommendationDetail(it) }
        )
        is DashboardRoute.RecommendationDetail -> RecommendationDetailPage(
            t = t,
            recommendation = current.recommendation,
            onBack = { route = DashboardRoute.Dashboard },
            onSearchPesticide = { query -> route = DashboardRoute.PesticideSearch(query) }
        )
    }
}

@Composable
private fun DashboardHome(
    t: DashboardStrings,
    pesticideCount: Int,
    recommendationCount: Int,
    onPesticides: () -> Unit,
    onCrops: () -> Unit,
    onTargets: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Agriculture,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("AgroYar", fontSize = 25.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(t.greeting, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text(t.intro, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataBadge("$pesticideCount", t.pesticideCatalog)
                    DataBadge("$recommendationCount", t.detailedRecommendations)
                }
            }
        }

        item {
            DashboardActionCard(
                icon = Icons.Default.Science,
                title = t.pesticideSearch,
                description = t.pesticideSearchDesc,
                onClick = onPesticides
            )
        }
        item {
            DashboardActionCard(
                icon = Icons.Default.Agriculture,
                title = t.cropSearch,
                description = t.cropSearchDesc,
                onClick = onCrops
            )
        }
        item {
            DashboardActionCard(
                icon = Icons.Default.BugReport,
                title = t.targetSearch,
                description = t.targetSearchDesc,
                onClick = onTargets
            )
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(t.sourceNotice, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DataBadge(value: String, label: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DashboardActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(13.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit, backDescription: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PesticideSearchPage(
    t: DashboardStrings,
    catalog: List<Pesticide>,
    initialQuery: String,
    onBack: () -> Unit,
    onOpen: (Pesticide) -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val normalized = query.trim().lowercase()
    val results = remember(normalized, catalog) {
        if (normalized.isBlank()) catalog else catalog.filter { item ->
            listOf(
                item.scientificName,
                item.tradeNames.joinToString(" "),
                item.activeIngredient,
                item.formulation,
                item.category,
                item.target,
                item.registeredCrops,
                item.modeOfAction
            ).any { it.lowercase().contains(normalized) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader(t.pesticideSearch, onBack, t.back)
        SearchField(query, { query = it }, t.searchPesticideHint)
        Spacer(Modifier.height(10.dp))
        Text("${t.results}: ${results.size}", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty()) {
            EmptySearchState(t.noResults)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(results, key = { it.id }) { pesticide ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(pesticide) },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                pesticide.scientificName.ifBlank { pesticide.tradeNames.firstOrNull().orEmpty() },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 17.sp
                            )
                            if (pesticide.tradeNames.isNotEmpty()) Text("${t.tradeName}: ${pesticide.tradeNames.joinToString()}")
                            if (pesticide.activeIngredient.isNotBlank()) Text("${t.activeIngredient}: ${pesticide.activeIngredient}")
                            if (pesticide.formulation.isNotBlank()) Text("${t.formulation}: ${pesticide.formulation}")
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

private enum class RecommendationSearchMode { CROP, TARGET }

@Composable
private fun RecommendationSearchPage(
    t: DashboardStrings,
    mode: RecommendationSearchMode,
    recommendations: List<DetailedRecommendation>,
    initialQuery: String,
    onBack: () -> Unit,
    onOpen: (DetailedRecommendation) -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val normalized = query.trim().lowercase()
    val title = if (mode == RecommendationSearchMode.CROP) t.cropSearch else t.targetSearch
    val hint = if (mode == RecommendationSearchMode.CROP) t.searchCropHint else t.searchTargetHint
    val suggestions = remember(recommendations, mode) {
        val values = if (mode == RecommendationSearchMode.CROP) {
            recommendations.map { it.crop }
        } else {
            recommendations.map { it.target }
        }
        values.filter { it.isNotBlank() }.distinct().sorted().take(10)
    }
    val results = remember(normalized, recommendations, mode) {
        if (normalized.isBlank()) emptyList() else recommendations.filter { recommendation ->
            val primary = if (mode == RecommendationSearchMode.CROP) recommendation.crop else recommendation.target
            primary.lowercase().contains(normalized)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader(title, onBack, t.back)
        SearchField(query, { query = it }, hint)
        if (query.isBlank() && suggestions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(t.suggestions, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(suggestions) { suggestion ->
                    AssistChip(onClick = { query = suggestion }, label = { Text(suggestion, maxLines = 1) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${t.results}: ${results.size}", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (query.isNotBlank() && results.isEmpty()) {
            EmptySearchState(t.noResults)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(results, key = { it.id }) { recommendation ->
                    RecommendationCard(t, recommendation) { onOpen(recommendation) }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(hint) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun RecommendationCard(
    t: DashboardStrings,
    recommendation: DetailedRecommendation,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(recommendation.crop, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
            if (recommendation.target.isNotBlank()) Text("${t.target}: ${recommendation.target}")
            if (recommendation.recommendedPesticides.isNotBlank()) {
                Text(
                    "${t.pesticide}: ${recommendation.recommendedPesticides}",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (recommendation.dose.isNotBlank()) Text("${t.dose}: ${recommendation.dose}")
            if (recommendation.timing.isNotBlank()) Text("${t.timing}: ${recommendation.timing}")
        }
    }
}

@Composable
private fun RecommendationDetailPage(
    t: DashboardStrings,
    recommendation: DetailedRecommendation,
    onBack: () -> Unit,
    onSearchPesticide: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageHeader(t.recommendationDetail, onBack, t.back) }
        detailCard(t.crop, recommendation.crop)
        detailCard(t.target, recommendation.target)
        detailCard(t.pesticide, recommendation.recommendedPesticides)
        detailCard(t.formulation, recommendation.formulation)
        detailCard(t.dose, recommendation.dose)
        detailCard(t.timing, recommendation.timing)
        detailCard(t.sourceNotes, recommendation.sourceNotes)
        detailCard(t.sourcePage, recommendation.pdfPage)
        if (recommendation.recommendedPesticides.isNotBlank()) {
            item {
                Button(
                    onClick = { onSearchPesticide(recommendation.recommendedPesticides) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(t.searchThisPesticide)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(t.sourceNotice, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detailCard(title: String, value: String) {
    if (value.isBlank()) return
    item {
        Card(shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(value)
            }
        }
    }
}

@Composable
private fun EmptySearchState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
