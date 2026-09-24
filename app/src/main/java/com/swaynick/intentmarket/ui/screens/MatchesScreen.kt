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

    val matches = remember(parsed, userDistrict) {
        MockDataRepository.findMatches(
            category = parsed.category,
            userDistrict = userDistrict,
            maxPrice = parsed.priceMax,
            keywords = queryText
        )
    }

    var selectedExternalListing by remember { mutableStateOf<ListingItem?>(null) }
    var isDemandSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (intentType == IntentType.DEMAND) "Подходящие предложения" else "Потенциальные покупатели",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Локация: ${userDistrict.name} • Найдено: ${matches.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            if (matches.isEmpty()) {
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
                        text = "В вашем районе пока нет предложений",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Мы сохранили ваш запрос и пришлем Push-уведомление, как только появится подходящий вариант рядом с вами.",
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
                    // Summary Banner
                    item {
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

                    // Matches Feed with Native Ad slots
                    itemsIndexed(matches) { index, listing ->
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
