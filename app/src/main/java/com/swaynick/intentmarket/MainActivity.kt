package com.swaynick.intentmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.swaynick.intentmarket.data.repository.MockDataRepository
import com.swaynick.intentmarket.domain.model.District
import com.swaynick.intentmarket.domain.model.IntentType
import com.swaynick.intentmarket.ui.screens.HomeScreen
import com.swaynick.intentmarket.ui.screens.MatchesScreen
import com.swaynick.intentmarket.ui.screens.SmartFormScreen
import com.swaynick.intentmarket.ui.theme.IntentMarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK asynchronously
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {}

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            IntentMarketTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                // State holding active search parameters
                var activeQuery by remember { mutableStateOf("") }
                var activeType by remember { mutableStateOf(IntentType.DEMAND) }
                var activeDistrict by remember { mutableStateOf(MockDataRepository.ODESA_DISTRICTS[0]) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        // Quick Theme Toggle Floating Action Button
                        SmallFloatingActionButton(
                            onClick = { isDarkTheme = !isDarkTheme },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Переключить тему"
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                onSearchMatches = { query, type, district ->
                                    activeQuery = query
                                    activeType = type
                                    activeDistrict = district
                                    navController.navigate("matches")
                                },
                                onOpenSmartForm = { initialQuery, type ->
                                    activeQuery = initialQuery
                                    activeType = type
                                    navController.navigate("smart_form")
                                }
                            )
                        }

                        composable("smart_form") {
                            SmartFormScreen(
                                initialText = activeQuery,
                                initialType = activeType,
                                onBack = { navController.popBackStack() },
                                onSubmit = { query, type, district ->
                                    activeQuery = query
                                    activeType = type
                                    activeDistrict = district
                                    navController.navigate("matches")
                                }
                            )
                        }

                        composable("matches") {
                            MatchesScreen(
                                queryText = activeQuery,
                                intentType = activeType,
                                userDistrict = activeDistrict,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
