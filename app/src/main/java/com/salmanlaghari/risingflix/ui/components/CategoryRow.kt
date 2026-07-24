package com.salmanlaghari.risingflix.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.data.Category
import com.salmanlaghari.risingflix.data.MovieItem
import com.salmanlaghari.risingflix.ui.theme.AccentCyan
import com.salmanlaghari.risingflix.ui.theme.TextMain

@Composable
fun CategoryRow(
    category: Category,
    onVideoClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (category.items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored dot representing category
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(2.dp)
                        .aspectRatio(1f)
                ) {
                    Text(
                        text = "•",
                        color = AccentCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = category.name,
                    color = TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "See All →",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Card Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(category.items, key = { it.id }) { video ->
                PremiumVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}
