package com.lksnext.ParkingIMayordomo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun subtleScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val maxScroll = scrollState.maxValue
    if (maxScroll <= 0) return

    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
    ) {
        val thumbHeight = size.height * (size.height / (size.height + maxScroll))
        val thumbOffset = (size.height - thumbHeight) * (scrollState.value.toFloat() / maxScroll)
        drawRoundRect(
            color = scrollbarColor,
            topLeft = Offset(0f, thumbOffset),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }
}

@Composable
fun subtleScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    val layoutInfo = lazyListState.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    if (totalItemsCount == 0) return

    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (visibleItemsInfo.isEmpty()) return

    val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
    if (viewportHeight <= 0) return

    // Estimate total content height based on average visible item height
    val firstItem = visibleItemsInfo.first()
    val lastItem = visibleItemsInfo.last()
    val visibleItemsHeight = (lastItem.offset + lastItem.size - firstItem.offset).toFloat()
    val avgItemHeight = visibleItemsHeight / visibleItemsInfo.size
    val totalContentHeight = avgItemHeight * totalItemsCount + layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding

    if (totalContentHeight <= viewportHeight) return

    // Calculate thumb height and offset
    val scrollOffset = lazyListState.firstVisibleItemIndex * avgItemHeight + lazyListState.firstVisibleItemScrollOffset
    val maxScroll = totalContentHeight - viewportHeight
    
    val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
    ) {
        val thumbHeight = size.height * (viewportHeight / totalContentHeight)
        val thumbOffset = (size.height - thumbHeight) * (scrollOffset / maxScroll)
        drawRoundRect(
            color = scrollbarColor,
            topLeft = Offset(0f, thumbOffset.coerceIn(0f, size.height - thumbHeight)),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )
    }
}
