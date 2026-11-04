package id.diskola.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Asks for the next page once the last visible row of a [LazyListState] is within [threshold] rows of the end (legacy's paging boundary
 * callback). [itemCount] is the number of rows of the list, header rows included; [onLoadMore] must itself ignore a call while a page is
 * loading or when there is none left, because it is called once each time the end comes into reach.
 */
@Composable
fun LoadMoreEffect(listState: LazyListState, itemCount: Int, onLoadMore: () -> Unit, threshold: Int = 3) {
    val count by rememberUpdatedState(itemCount)
    val reachedEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            count > 0 && lastVisible >= count - threshold
        }
    }
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(reachedEnd) { if (reachedEnd) latestOnLoadMore() }
}
