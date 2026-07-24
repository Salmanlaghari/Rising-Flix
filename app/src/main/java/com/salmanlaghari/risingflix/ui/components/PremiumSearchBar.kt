package com.salmanlaghari.risingflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salmanlaghari.risingflix.ui.theme.*

@Composable
fun PremiumSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
            .border(1.dp, BorderSoft, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = TextSub,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search Movies, Dramas, Sports, Cartoons...",
                    color = TextSub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(AccentCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = TextSub,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
