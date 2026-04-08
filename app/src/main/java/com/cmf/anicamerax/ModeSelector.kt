package com.cmf.anicamerax.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cmf.anicamerax.models.PhotoMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModeSelector(
    selectedMode: PhotoMode,
    onModeSelected: (PhotoMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = PhotoMode.entries
    val initialIndex = modes.indexOf(selectedMode)
    val pagerState = rememberPagerState(initialPage = initialIndex) { modes.size }

    LaunchedEffect(pagerState.currentPage) {
        onModeSelected(modes[pagerState.currentPage])
    }

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) { index ->
            val mode = modes[index]
            val isSelected = index == pagerState.currentPage
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "modeTextColor"
            )

            Text(
                text = mode.title.uppercase(),
                color = textColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Индикатор под выбранным режимом
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .width(60.dp)
        )
    }
}