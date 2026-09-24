package com.swaynick.intentmarket.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.swaynick.intentmarket.data.repository.MockDataRepository
import com.swaynick.intentmarket.domain.model.AdministrativeArea
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.ui.theme.PrimaryTeal
import com.swaynick.intentmarket.ui.theme.SecondaryIndigo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdesaLocationDialog(
    currentDistrict: District,
    onDistrictSelected: (District) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf<AdministrativeArea?>(
        MockDataRepository.ODESA_ADMIN_AREAS.find { area ->
            area.subdistricts.any { it.id == currentDistrict.id }
        } ?: MockDataRepository.ODESA_ADMIN_AREAS[0]
    ) }

    // GPS Location Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectGpsLocation(context) { closest ->
                onDistrictSelected(closest)
                Toast.makeText(context, "Определено: ${closest.name} (${closest.parentArea ?: "Одесса"})", Toast.LENGTH_SHORT).show()
                onDismissRequest()
            }
        } else {
            Toast.makeText(context, "Доступ к GPS отклонен. Выберите район вручную.", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Локация поиска",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Одесса и районы (OLX-навигатор)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Auto-detect Button (1-Click)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                detectGpsLocation(context) { closest ->
                                    onDistrictSelected(closest)
                                    Toast.makeText(context, "📍 Определено: ${closest.name}", Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                }
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                    color = PrimaryTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Определить по GPS (1 клик)",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Автоматический выбор ближайшего района",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск микрорайона (напр. Таирова)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (searchQuery.isNotBlank()) {
                    // Filtered Flat List
                    val filtered = MockDataRepository.ODESA_DISTRICTS.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                (it.parentArea != null && it.parentArea.contains(searchQuery, ignoreCase = true))
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { district ->
                            DistrictItemRow(
                                district = district,
                                isSelected = district.id == currentDistrict.id,
                                onSelect = {
                                    onDistrictSelected(district)
                                    onDismissRequest()
                                }
                            )
                        }
                    }
                } else {
                    // Cascading Navigation: Administrative Areas -> Microdistricts
                    Row(modifier = Modifier.weight(1f)) {
                        // Left: Administrative Area tabs
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(MockDataRepository.ODESA_ADMIN_AREAS) { area ->
                                val isAreaSelected = selectedArea?.id == area.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedArea = area },
                                    color = if (isAreaSelected) SecondaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = area.name.replace(" район", ""),
                                            fontWeight = if (isAreaSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isAreaSelected) SecondaryIndigo else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isAreaSelected) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = SecondaryIndigo,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right: Subdistricts of Selected Area
                        val subdistricts = selectedArea?.subdistricts ?: emptyList()
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.52f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(subdistricts) { district ->
                                DistrictItemRow(
                                    district = district,
                                    isSelected = district.id == currentDistrict.id,
                                    onSelect = {
                                        onDistrictSelected(district)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictItemRow(
    district: District,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        color = if (isSelected) PrimaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = district.name,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface
                )
                if (district.parentArea != null) {
                    Text(
                        text = district.parentArea,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun detectGpsLocation(context: Context, onResult: (District) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var bestLocation: Location? = null

        val providers = locationManager?.getProviders(true) ?: emptyList()
        for (provider in providers) {
            try {
                val l = locationManager?.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            } catch (_: SecurityException) {}
        }

        if (bestLocation != null) {
            val closest = MockDataRepository.findClosestDistrict(bestLocation.latitude, bestLocation.longitude)
            onResult(closest)
        } else {
            // Default to center if GPS signal unavailable
            onResult(MockDataRepository.ODESA_DISTRICTS[0])
        }
    } catch (_: Exception) {
        onResult(MockDataRepository.ODESA_DISTRICTS[0])
    }
}
