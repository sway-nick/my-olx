package com.swaynick.intentmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.swaynick.intentmarket.domain.model.Category
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.IntentType
import com.swaynick.intentmarket.domain.usecase.IntentParser
import com.swaynick.intentmarket.ui.theme.PrimaryTeal
import com.swaynick.intentmarket.ui.theme.SecondaryIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFormScreen(
    initialText: String,
    initialType: IntentType,
    onBack: () -> Unit,
    onSubmit: (query: String, type: IntentType, district: District) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = remember(initialText, initialType) {
        IntentParser.parse(initialText.ifBlank { "Генератор 5 кВт" }, initialType)
    }

    // Photo attachments list
    var attachedPhotos by remember {
        mutableStateOf(
            if (initialType == IntentType.SUPPLY) listOf("https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=300&auto=format&fit=crop&q=60") else emptyList()
        )
    }

    // System Photo Picker launcher
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            attachedPhotos = attachedPhotos + uris.map { it.toString() }
        }
    }

    var intentType by remember { mutableStateOf(initialType) }
    var selectedCategory by remember { mutableStateOf(parsed.category) }
    var powerKw by remember { mutableStateOf(parsed.attributes["power_kw"] ?: "5.0") }
    var fuelType by remember { mutableStateOf(parsed.attributes["fuel"] ?: "petrol") }
    var rooms by remember { mutableStateOf(parsed.attributes["rooms"] ?: "2") }
    var priceText by remember { mutableStateOf(parsed.priceMax?.toInt()?.toString() ?: "35000") }
    var selectedDistrict by remember { mutableStateOf(parsed.targetDistrict ?: MockDataRepository.ODESA_DISTRICTS[0]) }
    var isDistrictExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Умная анкета • AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // AI Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PrimaryTeal.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Параметры автоматически заполнены нейросетью из вашего описания. Вы можете поправить любое поле.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selector
            Text(
                text = "Категория",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.values()) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayNameRu) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Attributes based on Category
            if (selectedCategory == Category.POWER_GENERATORS) {
                Text(
                    text = "Мощность генератора (кВт)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = powerKw,
                    onValueChange = { powerKw = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Тип топлива",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("petrol" to "Бензин", "diesel" to "Дизель", "gas" to "Газ").forEach { (key, label) ->
                        FilterChip(
                            selected = fuelType == key,
                            onClick = { fuelType = key },
                            label = { Text(label) }
                        )
                    }
                }
            } else if (selectedCategory == Category.APARTMENT_RENT) {
                Text(
                    text = "Количество комнат",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1", "2", "3", "студия").forEach { roomOpt ->
                        FilterChip(
                            selected = rooms == roomOpt,
                            onClick = { rooms = roomOpt },
                            label = { Text(roomOpt) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // District Selector
            Text(
                text = "Район в Одессе",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedCard(
                onClick = { isDistrictExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = selectedDistrict.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = selectedDistrict.parentArea ?: "Одесса",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Выбрать район",
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
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

            // Price Field
            Text(
                text = if (intentType == IntentType.DEMAND) "Максимальный бюджет (грн)" else "Стоимость (грн)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Photo Upload Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фотографии (${attachedPhotos.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supabase Storage Ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryTeal
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Add Photo Button (Launches system photo picker)
                item {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryTeal)
                            Text(text = "Выбрать", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Selected / Attached Photos
                items(attachedPhotos) { photoUri ->
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            coil.compose.AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { attachedPhotos = attachedPhotos.filter { it != photoUri } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Удалить",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Submit Button
            Button(
                onClick = {
                    val query = when (selectedCategory) {
                        Category.POWER_GENERATORS -> "Генератор $powerKw кВт $fuelType ${selectedDistrict.name}"
                        Category.APARTMENT_RENT -> "Аренда $rooms комнатная квартира ${selectedDistrict.name}"
                        else -> initialText.ifBlank { "Поиск в Одессе" }
                    }
                    onSubmit(query, intentType, selectedDistrict)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (intentType == IntentType.DEMAND) PrimaryTeal else SecondaryIndigo
                )
            ) {
                Text(
                    text = if (intentType == IntentType.DEMAND) "Запустить умный поиск" else "Опубликовать объявление",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
