package com.salmanlaghari.risingflix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.ui.theme.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlueBg)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // --- USER PROFILE HEADER ---
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.04f))
                .border(2.dp, AccentCyan, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Avatar",
                tint = AccentCyan,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Salman Laghari",
            color = TextMain,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Premium Badge
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(listOf(GoldAccent, Color(0xFFFFA500))),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "PREMIUM MEMBER",
                color = TrueBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SETTINGS LIST COLUMN ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Preferences",
                color = TextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            SettingsItem(icon = Icons.Default.PlayArrow, title = "Video Playback Quality", value = "1080p (Full HD)")
            SettingsItem(icon = Icons.Default.ArrowDownward, title = "Download Settings", value = "WiFi Only")
            SettingsItem(icon = Icons.Default.Info, title = "Notifications", value = "Enabled")
            SettingsItem(icon = Icons.Default.Info, title = "Privacy & Safety")
            SettingsItem(icon = Icons.Default.Info, title = "About Licensing")
            SettingsItem(icon = Icons.Default.Delete, title = "Log Out", tint = Color.Red.copy(alpha = 0.8f))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // App version
        Text(
            text = "Rising Flix v1.0.0 (Build 10000)",
            color = TextSub.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String? = null,
    tint: Color = AccentCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurfaceDark, RoundedCornerShape(14.dp))
            .border(1.dp, BorderSoft.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(16.dp)
            .clickable { /* Simulate configuration interactions */ },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    color = TextSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSub.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
