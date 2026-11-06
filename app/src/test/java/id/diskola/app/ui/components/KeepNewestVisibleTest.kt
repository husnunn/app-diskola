package id.diskola.app.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepNewestVisibleTest {

    @Test
    fun aNewHeadOverAListAtItsTopMovesTheListToTheTop() {
        // Seen on device: task 89 was posted, the list refreshed, and the new card sat above the edge until the user scrolled up.
        assertTrue(shouldJumpToTop(previousFirst = 88, firstNow = 89, wasAtTop = true))
    }

    @Test
    fun aListTheUserHasScrolledAwayFromIsLeftWhereItIs() {
        assertFalse(shouldJumpToTop(previousFirst = 88, firstNow = 89, wasAtTop = false))
    }

    @Test
    fun anUnchangedHeadNeverMovesTheList() {
        assertFalse(shouldJumpToTop(previousFirst = 89, firstNow = 89, wasAtTop = true))
    }

    @Test
    fun theFirstFillAndAnEmptiedListDoNotMoveIt() {
        // First load (nothing before), and a list that became empty (nothing now), have no new head to bring into view.
        assertFalse(shouldJumpToTop(previousFirst = null, firstNow = 89, wasAtTop = true))
        assertFalse(shouldJumpToTop(previousFirst = 89, firstNow = null, wasAtTop = true))
    }
}
