package com.drpsphca.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PhcaNavy = Color(0xFF17345C)
private val PhcaGold = Color(0xFFF2B544)

@Composable
fun SharedApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PhcaNavy,
            secondary = PhcaGold,
            background = Color(0xFFF7F8FA),
            surface = Color.White,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "DRPS PHCA",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Your community, connected.",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Shared Compose UI is running on ${platform()}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
