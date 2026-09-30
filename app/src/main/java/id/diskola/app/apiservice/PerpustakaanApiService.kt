package id.diskola.app.apiservice

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Placeholder scaffolding: request/response shapes not yet defined, no UI consumer yet. */
interface PerpustakaanApiService {

    @GET("mobile/app/learning/pustaka/students/homepage-banner")
    suspend fun libraryBanner(): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/homepage-book-newest")
    suspend fun newestBooks(): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/homepage-book-famous")
    suspend fun popularBooks(): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/search-books")
    suspend fun searchBooks(@Query("q") q: String): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/books/{id}/available")
    suspend fun bookAvailability(@Path("id") id: Int): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/rent-archives")
    suspend fun rentArchives(): Map<String, Any>

    @GET("mobile/app/learning/pustaka/students/rents")
    suspend fun activeRents(): Map<String, Any>
}
