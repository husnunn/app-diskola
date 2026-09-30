package id.diskola.app.repository

import id.diskola.app.apiservice.AuthApiService
import id.diskola.app.dataclass.localDb.SchoolDao
import id.diskola.app.dataclass.localDb.toEntity
import id.diskola.app.dataclass.localDb.toItem
import id.diskola.app.dataclass.ResponData.SchoolItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val PAGE_SIZE = 20

/**
 * The school picker's local-only search + paging (doc `02-auth-login-sesi.md` §4.2, decision Q3).
 * The legacy app's `ListSekolahPage` used a Paging 2 `BoundaryCallback` over a Room cache — this is
 * the same idea without a paging library: [observe] is the reactive local search, [ensureFirstPage]
 * / [loadNextPage] / [refresh] are the three ways the legacy boundary callback filled that cache.
 */
class SchoolRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val schoolDao: SchoolDao,
) {
    fun observe(query: String): Flow<List<SchoolItem>> =
        schoolDao.observe(query.trim()).map { list -> list.map { it.toItem() } }

    /** Only fetches when the cache is empty — legacy's `onZeroItemsLoaded`, and only while the
     * search box is empty (a query that matches nothing locally never triggers a fetch). */
    suspend fun ensureFirstPage() {
        if (schoolDao.count() == 0) fetchPage(skip = 0)
    }

    /** `onItemAtEndLoaded` — only called by the UI while the search box is empty and [hasNextPage]. */
    suspend fun loadNextPage(currentCount: Int): Boolean = fetchPage(skip = currentCount)

    /** Pull-to-refresh — legacy always resets to page 0 on refresh, even mid-search. */
    suspend fun refresh(): Boolean {
        schoolDao.clear()
        return fetchPage(skip = 0)
    }

    /** @return true if the server might still have more (page came back full). */
    private suspend fun fetchPage(skip: Int): Boolean {
        val response = authApiService.getSchools(skip = skip, take = PAGE_SIZE, name = "")
        schoolDao.insertAll(response.data.map { it.toEntity() })
        return response.data.size >= PAGE_SIZE
    }
}
