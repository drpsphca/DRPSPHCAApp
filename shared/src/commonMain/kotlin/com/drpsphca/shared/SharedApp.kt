package com.drpsphca.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PhcaNavy = Color(0xFF17345C)
private val PhcaGold = Color(0xFFF2B544)

private data class AppSection(
    val title: String,
    val label: String,
    val marker: String,
    val url: String,
)

private val appSections = listOf(
    AppSection("Home", "Home", "H", "https://drpsphca.com/"),
    AppSection("Blog", "Blog", "B", "https://drpsphca.com/blog"),
    AppSection("Newsletter", "Newsletter", "N", "https://drpsphca.com/newsletter"),
    AppSection("TV Ad Listings", "TVAL", "TV", "https://tval.drpsphca.com/"),
    AppSection("PBB Updates", "PBB", "P", "https://pbbupdates.drpsphca.com/"),
    AppSection("Bite-sized", "Bites", "+", "https://app.drpsphca.com/"),
)

@Composable
fun SharedApp() {
    var selectedSection by rememberSaveable { mutableStateOf(0) }
    val section = appSections[selectedSection]

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PhcaNavy,
            secondary = PhcaGold,
            background = Color(0xFFF7F8FA),
            surface = Color.White,
        ),
    ) {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "DRPS PHCA",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    appSections.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedSection == index,
                            onClick = { selectedSection = index },
                            icon = { Text(item.marker, fontWeight = FontWeight.Bold) },
                            label = { Text(item.label, maxLines = 1) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                PlatformWebContent(url = section.url, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
