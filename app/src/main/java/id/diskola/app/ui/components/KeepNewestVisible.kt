package id.diskola.app.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Keeps a keyed list that sits at its top showing its newest row.
 *
 * A keyed `LazyColumn` pins the row the user is looking at: when a refresh puts a new row **above** the first visible one
 * (a task just posted, a submission just sent), the viewport stays on the old first row and the new one waits just above
 * the edge, so the list looks as if nothing arrived until the user scrolls up (seen on device: a freshly posted task was
 * missing from both the teacher's and the student's list). Pass the key of the first row; when it changes and the list was at
 * its top, the list jumps back to the top. A list the user has scrolled away from is never moved.
 *
 * [listState] is read during composition on purpose: at that point it still describes the layout from before the new data was
 * measured, which is exactly the "was the user at the top?" answer this needs.
 */
@Composable
fun KeepNewestVisibleEffect(listState: LazyListState, firstKey: Any?) {
    val wasAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
    val previous = remember { arrayOfNulls<Any>(1) }
    LaunchedEffect(firstKey) {
        val before = previous[0]
        previous[0] = firstKey
        if (shouldJumpToTop(before, firstKey, wasAtTop)) listState.scrollToItem(0)
    }
}

/** The decision, apart from Compose so it can be tested: only a *changed* head over a list that was at its top moves it. */
internal fun shouldJumpToTop(previousFirst: Any?, firstNow: Any?, wasAtTop: Boolean): Boolean =
    wasAtTop && previousFirst != null && firstNow != null && previousFirst != firstNow
