package com.swaynick.intentmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swaynick.intentmarket.data.repository.MockDataRepository
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.IntentType
import com.swaynick.intentmarket.domain.usecase.IntentParser
import com.swaynick.intentmarket.ui.components.ModeSelector
import com.swaynick.intentmarket.ui.theme.PrimaryTeal
import com.swaynick.intentmarket.ui.theme.SecondaryIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSearchMatches: (query: String, type: IntentType, district: District) -> Unit,
    onOpenSmartForm: (initialQuery: String, type: IntentType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(IntentType.DEMAND) }
    var inputText by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf(MockDataRepository.ODESA_DISTRICTS[0]) } // Таирова
    var isDistrictExpanded by remember { mutableStateOf(false) }

    val quickSuggestions = remember(selectedType) {
        if (selectedType == IntentType.DEMAND) {
            listOf(
                "Генератор 5 кВт до 40 000 грн",
                "2к квартира Аркадия до 15 000 грн",
                "Электрик сегодня Таирова",
                "Инверторный генератор Центр",
                "1к квартира возле моря"
            )
        } else {
            listOf(
                "Продам генератор Honda 5.5 кВт 35000 грн",
                "Сдам 2к квартиру в Аркадии 15000 грн",
                "Услуги электрика, подключение генераторов",
                "Сдам 1к квартиру студию на Таирова 9000 грн"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Header & Slogan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IntentMarket",
                    style = MaterialTheme.typography.headlineLarge,
                    color = PrimaryTeal,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Умный поиск спроса и предложений • Одесса",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dual Mode Selector: "🔍 Ищу" vs "📦 Предлагаю"
        ModeSelector(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Location Selector Card (Location-First)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { isDistrictExpanded = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Локация поиска • ${selectedDistrict.parentArea ?: "Одесса"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedDistrict.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTeal.copy(alpha = 0.12f),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Выбрать",
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (isDistrictExpanded) {
            com.swaynick.intentmarket.ui.components.OdesaLocationDialog(
                currentDistrict = selectedDistrict,
                onDistrictSelected = {
                    selectedDistrict = it
                    isDistrictExpanded = false
                },
                onDismissRequest = { isDistrictExpanded = false }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Natural Language Input Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedType == IntentType.DEMAND) "Что вам нужно?" else "Что вы предлагаете?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (selectedType == IntentType.DEMAND)
                                "Опишите своими словами:\n«Нужен генератор 5 кВт до 40 тысяч на Таирова»"
                            else
                                "Опишите ваше предложение:\n«Сдам 2к квартиру в Аркадии 15000 грн»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Button(
                    onClick = {
                        val query = inputText.ifBlank { "Генератор 5 кВт Одесса" }
                        onSearchMatches(query, selectedType, selectedDistrict)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == IntentType.DEMAND) PrimaryTeal else SecondaryIndigo
                    )
                ) {
                    Icon(
                        imageVector = if (selectedType == IntentType.DEMAND) Icons.Default.Bolt else Icons.Default.CheckCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedType == IntentType.DEMAND) "Найти подходящие варианты" else "Разместить и найти покупателей",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Smart Form Link
                TextButton(
                    onClick = {
                        onOpenSmartForm(inputText, selectedType)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Открыть умную анкету с фото",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Suggestions
        Text(
            text = "Популярные запросы прямо сейчас:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(quickSuggestions) { suggestion ->
                SuggestionChip(
                    onClick = {
                        inputText = suggestion
                        val parsed = IntentParser.parse(suggestion, selectedType)
                        val district = parsed.targetDistrict ?: selectedDistrict
                        onSearchMatches(suggestion, selectedType, district)
                    },
                    label = { Text(suggestion, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
