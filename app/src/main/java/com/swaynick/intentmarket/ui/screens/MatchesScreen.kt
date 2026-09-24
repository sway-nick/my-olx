package com.swaynick.intentmarket.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swaynick.intentmarket.data.repository.MockDataRepository
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.IntentType
import com.swaynick.intentmarket.domain.model.ListingItem
import com.swaynick.intentmarket.domain.usecase.IntentParser
import com.swaynick.intentmarket.ui.components.AdMobInFeedCard
import com.swaynick.intentmarket.ui.components.ExternalRedirectDialog
import com.swaynick.intentmarket.ui.components.MatchCard
import com.swaynick.intentmarket.ui.theme.AccentGreen
import com.swaynick.intentmarket.ui.theme.PrimaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    queryText: String,
    intentType: IntentType,
    userDistrict: District,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsed = remember(queryText, intentType) {
        IntentParser.parse(queryText, intentType)
    }

    var onlyHotDeals by remember { mutableStateOf(intentType == IntentType.HOT_DEALS) }

    var matches by remember {
        mutableStateOf(
            if (intentType == IntentType.HOT_DEALS) {
                MockDataRepository.getHotDeals(userDistrict)
            } else {
                MockDataRepository.findMatches(
                    category = parsed.category,
                    userDistrict = userDistrict,
                    maxPrice = parsed.priceMax,
                    keywords = queryText
                )
            }
        )
    }
    var isLoadingCloud by remember { mutableStateOf(true) }
    var isFromCloud by remember { mutableStateOf(false) }

    LaunchedEffect(parsed, userDistrict, onlyHotDeals) {
        isLoadingCloud = true
        if (onlyHotDeals || intentType == IntentType.HOT_DEALS) {
            val result = com.swaynick.intentmarket.data.repository.SupabaseRepository.getHotDeals(
                userLat = userDistrict.lat,
                userLon = userDistrict.lon,
                minDiscountPct = 25.0
            )
            result.onSuccess { cloudMatches ->
                if (cloudMatches.isNotEmpty()) {
                    matches = cloudMatches
                    isFromCloud = true
                }
            }
        } else {
            val result = com.swaynick.intentmarket.data.repository.SupabaseRepository.matchDemand(
                category = parsed.category,
                userLat = userDistrict.lat,
                userLon = userDistrict.lon,
                maxPrice = parsed.priceMax
            )
            result.onSuccess { cloudMatches ->
                if (cloudMatches.isNotEmpty()) {
                    matches = cloudMatches
                    isFromCloud = true
                }
            }
        }
        isLoadingCloud = false
    }

    var selectedExternalListing by remember { mutableStateOf<ListingItem?>(null) }
    var isDemandSaved by remember { mutableStateOf(false) }

    val displayedMatches = remember(matches, onlyHotDeals) {
        if (onlyHotDeals && intentType != IntentType.HOT_DEALS) {
            matches.filter { it.isHotDeal }
        } else {
            matches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                onlyHotDeals || intentType == IntentType.HOT_DEALS -> "🔥 Хорошая цена (-25%+)"
                                intentType == IntentType.DEMAND -> "Подходящие предложения"
                                else -> "Потенциальные покупатели"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Локация: ${userDistrict.name} • ${displayedMatches.size} шт.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isFromCloud) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (onlyHotDeals) Color(0xFFFF6B00).copy(alpha = 0.15f) else PrimaryTeal.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (onlyHotDeals) "🔥 Cloud Medians" else "⚡ Cloud PostGIS",
                                        color = if (onlyHotDeals) Color(0xFFE65100) else PrimaryTeal,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isDemandSaved = !isDemandSaved
                            val message = if (isDemandSaved)
                                "Запрос сохранен! Мы уведомим вас через Push при появлении новых совпадений."
                            else
                                "Оповещения отключены."
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (isDemandSaved) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Сохранить спрос",
                            tint = if (isDemandSaved) AccentGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (displayedMatches.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (onlyHotDeals) "Скидок от 25% пока нет" else "В вашем районе пока нет предложений",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (onlyHotDeals)
                            "Как только кто-то выставит товар или квартиру по цене ниже медианы рынка, предложение появится здесь."
                        else
                            "Мы сохранили ваш запрос и пришлем Push-уведомление, как только появится подходящий вариант рядом с вами.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Filter Chips Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !onlyHotDeals,
                                onClick = { onlyHotDeals = false },
                                label = { Text("Все варианты", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = onlyHotDeals,
                                onClick = { onlyHotDeals = true },
                                label = { Text("🔥 Хорошая цена (-25%+)", fontSize = 13.sp, fontWeight = if (onlyHotDeals) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (onlyHotDeals) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF6B00).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFFE65100)
                                )
                            )
                        }
                    }

                    // Summary Banner
                    item {
                        if (onlyHotDeals || intentType == IntentType.HOT_DEALS) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFF6B00).copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "🔥 Сравнение с медианой рынка Одессы",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "Здесь только предложения со скидкой от 25% по сопоставимым данным (грн/м², грн/кВт, модели).",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = PrimaryTeal.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = PrimaryTeal)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Location-First: сортировка по близости",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = PrimaryTeal
                                        )
                                        Text(
                                            text = "Сначала показываются варианты в вашем районе (${userDistrict.name})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Matches Feed with Native Ad slots
                    itemsIndexed(displayedMatches) { index, listing ->
                        MatchCard(
                            listing = listing,
                            onExternalClick = { selectedExternalListing = it }
                        )

                        // Insert Google AdMob Native Ad after every 2 items
                        if (index == 1) {
                            Spacer(modifier = Modifier.height(14.dp))
                            AdMobInFeedCard()
                        }
                    }
                }
            }
        }

        // Rewarded Ad Dialog for External Links
        ExternalRedirectDialog(
            listing = selectedExternalListing,
            onDismiss = { selectedExternalListing = null },
            onConfirm = { listing ->
                selectedExternalListing = null
                // Open external URL in browser / native deep link
                listing.sourceUrl?.let { url ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                }
            }
        )
    }
}
