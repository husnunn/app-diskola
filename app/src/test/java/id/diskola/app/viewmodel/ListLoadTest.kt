package id.diskola.app.viewmodel

import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ListLoadTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @After
    fun tearDown() = scope.cancel()

    /** A screen keeps both flows collected; [ListLoad]'s own flows only run while someone does. */
    private fun <T> ListLoad<T>.shown(): ListLoad<T> {
        scope.launch { items.collect {} }
        scope.launch { loading.collect {} }
        return this
    }

    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = withTimeout(5_000) { first(predicate) }

    @Test
    fun loading_staysUp_untilRoomHasAnswered() = runBlocking<Unit> {
        val room = MutableSharedFlow<List<Int>>(replay = 1)
        val load = ListLoad(scope, room).shown()

        delay(150)
        assertTrue("an empty list before Room answers is unknown, not empty", load.loading.value)

        room.emit(emptyList())
        load.loading.await { !it }
        assertTrue(load.items.value.isEmpty())
    }

    @Test
    fun loading_isUp_whileAFetchRuns() = runBlocking<Unit> {
        val load = ListLoad(scope, MutableStateFlow(emptyList<Int>())).shown()
        load.loading.await { !it }

        val release = CompletableDeferred<Unit>()
        val fetch = scope.async { load.fetch { release.await() } }
        load.loading.await { it }

        release.complete(Unit)
        fetch.await()
        load.loading.await { !it }
    }

    @Test
    fun fetch_waitsUntilTheListHasCaughtUpWithWhatWasWritten() = runBlocking<Unit> {
        val db = MutableStateFlow(emptyList<Int>())
        // Room hands a change over some time after the write — the lag that used to leave an empty list behind a finished load.
        val room: Flow<List<Int>> = db.onEach { delay(150) }
        val load = ListLoad(scope, room).shown()
        load.loading.await { !it }

        load.fetch { db.value = listOf(1, 2) }

        assertEquals("loading must not drop before the rows are shown", listOf(1, 2), load.items.value)
        load.loading.await { !it }
    }

    @Test
    fun fetch_doesNotWait_whenTheListReallyIsEmpty() = runBlocking<Unit> {
        val load = ListLoad(scope, MutableStateFlow(emptyList<Int>()), catchUpTimeoutMillis = 5_000).shown()
        load.loading.await { !it }

        val took = measureTimeMillis { load.fetch { /* the server had nothing */ } }

        assertTrue("took ${took}ms", took < 1_000)
        load.loading.await { !it }
    }

    @Test
    fun fetch_givesUp_afterTheTimeout_whenTheListNeverShowsTheRows() = runBlocking<Unit> {
        val collections = AtomicInteger()
        // The list's own collection never delivers; a fresh read of Room finds a row.
        val room = flow {
            if (collections.getAndIncrement() == 0) {
                emit(emptyList<Int>())
                awaitCancellation()
            } else {
                emit(listOf(1))
            }
        }
        val load = ListLoad(scope, room, catchUpTimeoutMillis = 150).shown()
        load.loading.await { !it }

        val took = measureTimeMillis { load.fetch { } }

        assertTrue("waited ${took}ms, expected about the timeout", took in 140..3_000)
        assertTrue(load.items.value.isEmpty())
        load.loading.await { !it }
    }

    @Test
    fun fetch_rethrows_andLowersLoading() = runBlocking<Unit> {
        val load = ListLoad(scope, MutableStateFlow(emptyList<Int>())).shown()
        load.loading.await { !it }

        assertThrows(IllegalStateException::class.java) {
            runBlocking { load.fetch<Unit> { throw IllegalStateException("offline") } }
        }

        load.loading.await { !it }
        assertFalse(load.loading.value)
    }

    @Test
    fun overlappingFetches_keepLoadingUp_untilBothFinish() = runBlocking<Unit> {
        val load = ListLoad(scope, MutableStateFlow(emptyList<Int>())).shown()
        load.loading.await { !it }

        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val a = scope.async { load.fetch { first.await() } }
        val b = scope.async { load.fetch { second.await() } }
        load.loading.await { it }

        first.complete(Unit)
        a.await()
        delay(150)
        assertTrue("one fetch is still running", load.loading.value)

        second.complete(Unit)
        b.await()
        load.loading.await { !it }
    }
}
