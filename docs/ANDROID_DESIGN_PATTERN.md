# Android Design Pattern — Diskola App

> **Untuk Prosite / AI Agent:** Dokumen ini adalah **satu-satunya sumber kebenaran** arsitektur Android
> di project ini. Sebelum menulis kode apa pun, baca seluruh dokumen ini. Aturan di sini **tidak boleh
> dilanggar** kecuali user secara eksplisit memintanya. Jika ada konflik antara dokumen ini dan
> instruksi sesi, tanyakan ke user — jangan memutuskan sendiri.

---

## Daftar Isi

1. [Gambaran Umum Arsitektur](#1-gambaran-umum-arsitektur)
2. [Struktur Folder](#2-struktur-folder)
3. [Konfigurasi Build](#3-konfigurasi-build)
4. [Dependency Injection (Hilt)](#4-dependency-injection-hilt)
5. [Network Layer](#5-network-layer)
6. [API Service Pattern](#6-api-service-pattern)
7. [ViewModel Pattern](#7-viewmodel-pattern)
8. [Data Classes / Model Layer](#8-data-classes--model-layer)
9. [Room Database](#9-room-database)
10. [Navigasi](#10-navigasi)
11. [UI Layer (Composables)](#11-ui-layer-composables)
12. [Komponen Bersama (Shared Components)](#12-komponen-bersama-shared-components)
13. [Design System / Tema](#13-design-system--tema)
14. [Utilities](#14-utilities)
15. [WorkManager](#15-workmanager)
16. [Konvensi Penamaan](#16-konvensi-penamaan)
17. [Library & Versi](#17-library--versi)
18. [Rules Wajib](#18-rules-wajib)
19. [Anti-Pattern yang Dilarang](#19-anti-pattern-yang-dilarang)
20. [Checklist Sebelum Bilang Selesai](#20-checklist-sebelum-bilang-selesai)

---

## 1. Gambaran Umum Arsitektur

Project ini menggunakan arsitektur **MVVM (Model-View-ViewModel)** dengan pola clean layer:

```
UI Layer (Composable)
       ↕  event / state
ViewModel Layer (StateFlow)
       ↕  coroutine suspend fun
API Service / Repository Layer (Retrofit)
       ↕  Interceptor chain
Network (OkHttp + Moshi)
       ↕
Room Database (offline cache)
```

**Keputusan arsitektur utama:**
- **Single Activity** (`MainActivity`) — semua navigasi lewat Compose NavHost
- **Jetpack Compose** untuk semua UI — tidak ada Fragment, tidak ada XML layout screen
- **Hilt** untuk dependency injection (auto-discovery via `@HiltViewModel`)
- **Retrofit + Moshi** untuk HTTP — tidak ada Gson, tidak ada kotlinx.serialization di network layer
- **Coroutines + StateFlow** — tidak ada LiveData, tidak ada RxJava
- **Room** untuk offline storage — hanya dipakai di fitur yang memang butuh offline (AKM exam)
- **WorkManager** untuk background task berat (download exam AKM)

---

## 2. Struktur Folder

```
app/src/main/java/id/diskola/app/
├── App.kt                          # Application class (@HiltAndroidApp)
├── GlideModule.kt                  # Glide image loader config
│
├── apiservice/                     # Semua Retrofit service interface + interceptors
│   ├── AbsensiApiService.kt
│   ├── AgendaApiService.kt
│   ├── AkunApiService.kt
│   ├── AsesmenApiService.kt
│   ├── AuthApiService.kt
│   ├── CommonApiService.kt
│   ├── DanaPartisipasiApiService.kt
│   ├── EntrepreneurApiService.kt
│   ├── IzinApiService.kt
│   ├── ...ApiService.kt            # Pola: <Domain>ApiService.kt
│   ├── RequestInterceptor.kt       # Auth header + connectivity check
│   ├── ResponseInterceptor.kt      # Error parsing + auto-logout 401
│   ├── LogFileInterceptor.kt       # Log to file (via @LogFile annotation)
│   └── BaseUrlInterceptor.kt       # MockApiInterceptor (via @UseMock annotation)
│
├── database/                       # Room database
│   ├── LocalDatabase.kt            # @Database class
│   ├── DatabaseConverter.kt        # @TypeConverters
│   └── DatabaseUtil.kt
│
├── dataclass/
│   ├── ResponData/                 # API request/response models
│   │   ├── LoginResponse.kt
│   │   ├── AttendanceModels.kt
│   │   ├── GeneralModel.kt
│   │   └── ...Models.kt / ...Response.kt
│   ├── akm/                        # AKM UI-layer models (mapped from API)
│   │   └── AkmUiModels.kt
│   ├── localDb/                    # Room entities + DAOs
│   │   ├── User.kt                 # @Entity + UserDao
│   │   ├── AkmSyncedExam.kt        # @Entity + status download
│   │   └── AkmSyncDao.kt
│   └── mock/                       # UI-only placeholder (belum ada API)
│       ├── NotificationModels.kt
│       ├── PembayaranModels.kt
│       └── AkunModels.kt
│
├── di/module/                      # Hilt DI modules
│   ├── ApiModule.kt                # Retrofit + OkHttp + semua ApiService
│   ├── MoshiModule.kt              # Moshi instance + custom adapters
│   ├── DbModule.kt                 # Room database
│   └── PreferenceModule.kt         # SharedPreferences
│
├── network/                        # Firebase messaging
│   └── FirebaseMessagingService.kt
│
├── ui/
│   ├── MainActivity.kt             # SATU-SATUNYA Activity
│   ├── components/                 # Shared Composable components (design system)
│   │   ├── AppButton.kt
│   │   ├── AppCard.kt (Card.kt)
│   │   ├── AppTextField.kt (TextField.kt)
│   │   ├── AppDialog.kt
│   │   ├── AppBottomSheet.kt
│   │   ├── DetailScaffold.kt
│   │   ├── States.kt               # LoadingState, EmptyState, ErrorState
│   │   ├── Chip.kt
│   │   ├── TileGrid.kt
│   │   ├── Background.kt           # AmbientGradientBackground
│   │   ├── ListRow.kt
│   │   ├── Payment.kt
│   │   ├── WalletHeader.kt
│   │   ├── PinKeypad.kt
│   │   ├── QrViewfinder.kt
│   │   ├── IconMapping.kt
│   │   ├── Tabs.kt
│   │   └── Assessment.kt
│   ├── navigation/
│   │   ├── Route.kt                # Sealed interface type-safe routes
│   │   └── AppNavHost.kt           # NavHost + sub-graphs
│   ├── screens/                    # Feature screens, dikelompokkan per domain
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt
│   │   │   ├── PasswordScreen.kt
│   │   │   └── SplashScreen.kt
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── MainTabsScreen.kt
│   │   ├── absensi/
│   │   ├── akm/
│   │   ├── akun/
│   │   ├── materi/
│   │   ├── notifikasi/
│   │   └── pembayaran/
│   └── theme/
│       ├── Theme.kt                # DiskolaTheme entry point
│       ├── Color.kt                # M3 color scheme + ExtendedColors
│       ├── Type.kt                 # Lato font + ExtendedTypography
│       ├── Shape.kt                # DiskolaShapes + DiskolaExtraShapes
│       ├── Spacing.kt              # Spacing object + ScreenHorizontalPadding
│       ├── Adaptive.kt             # WindowWidth enum + contentContainer()
│       └── Motion.kt               # Animasi constants (PRESS_SCALE)
│
├── utils/
│   ├── PreferenceClass.kt          # SharedPreferences wrapper (Hilt-injected)
│   ├── AppErrorHandler.kt          # ApiException → pesan Indonesia
│   ├── Utils.kt                    # isInternetAvailable(), dll
│   ├── IntentUtil.kt               # logOut()
│   ├── DateUtil.kt
│   ├── StringUtil.kt
│   ├── FileUtils.kt
│   ├── DeviceUtil.kt
│   ├── AssetUrl.kt
│   ├── NotifUtil.kt
│   └── HtmlMathRenderer.kt
│
├── viewmodel/
│   ├── BaseViewModel.kt            # WAJIB diextend semua ViewModel
│   ├── AuthViewModel.kt
│   ├── AbsensiViewModel.kt
│   ├── AkmViewModel.kt
│   ├── AkunViewModel.kt
│   ├── MateriViewModel.kt
│   ├── NotifikasiViewModel.kt
│   ├── PembayaranViewModel.kt
│   └── GeneralViewModel.kt
│
└── worker/
    └── AkmDownloadWorker.kt        # @HiltWorker, CoroutineWorker
```

**Satu modul app** — tidak ada multi-module. Semua kode di `app/`.

---

## 3. Konfigurasi Build

### Package & SDK

```kotlin
// app/build.gradle.kts
applicationId = "id.diskola.app"
compileSdk = 36
targetSdk = 36
minSdk = 27
versionName = "1.0.0"
```

### Build Variants

- **debug** — `applicationIdSuffix = ".debug"`, `debuggable = true`
- **release** — minify + ProGuard aktif

### BuildConfig dari `local.properties`

```properties
# local.properties (TIDAK dicommit ke git)
API_URL_DEV=https://dev.api.diskola.id/api/
API_URL_PROD=https://api.diskola.id/api/
ASSETS_URL=https://assets.diskola.id/
WEB_CLIENT_ID=xxxxxxx.apps.googleusercontent.com
```

Dibaca di `build.gradle.kts` dan diinjeksikan sebagai `BuildConfig.API_URL`, `BuildConfig.ASSETS_URL`, `BuildConfig.WEB_CLIENT_ID`.

### Plugin Wajib

```kotlin
plugins {
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}
```

### Build Features

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
}
```

---

## 4. Dependency Injection (Hilt)

### Application Class

```kotlin
@HiltAndroidApp
class App : MultiDexApplication(), Application.ActivityLifecycleCallbacks, Configuration.Provider {
    @Inject lateinit var preferenceClass: PreferenceClass
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        // restore theme, in-app update check, Firebase init
    }
}
```

### Modul DI

#### `ApiModule.kt` — `@InstallIn(SingletonComponent::class)`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit = ...

    @Provides @Singleton
    fun provideOkHttpClient(...): OkHttpClient {
        // Chain interceptor (WAJIB urutan ini):
        // ChuckerInterceptor → MockApiInterceptor → RequestInterceptor
        // → ResponseInterceptor → HttpLoggingInterceptor → LogFileInterceptor
    }

    // Satu @Provides @Singleton per service interface:
    @Provides @Singleton fun provideAuthApiService(retrofit: Retrofit): AuthApiService = ...
    @Provides @Singleton fun provideAbsensiApiService(retrofit: Retrofit): AbsensiApiService = ...
    // ... dst
}
```

#### `MoshiModule.kt`

```kotlin
@Provides @Singleton
fun provideMoshi(): Moshi = Moshi.Builder()
    .add(ObjectToListAdapter())       // JSON object → List satu elemen
    .add(NullToEmptyString.ADAPTER)   // null string → ""
    .addLast(KotlinJsonAdapterFactory())
    .build()
```

#### `DbModule.kt`

```kotlin
@Provides @Singleton
fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase =
    Room.databaseBuilder(context, LocalDatabase::class.java, "ebede.db")
        .fallbackToDestructiveMigration()
        .build()
```

#### `PreferenceModule.kt`

```kotlin
@Provides @Reusable
fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
    context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
```

---

## 5. Network Layer

### Interceptor Chain (urutan wajib)

```
Request masuk
    ↓
ChuckerInterceptor          (debug HTTP inspector)
    ↓
MockApiInterceptor           (redirect ke mock URL jika @UseMock)
    ↓
RequestInterceptor           (cek internet, tambah Authorization header)
    ↓
ResponseInterceptor          (parse error, auto-logout 401)
    ↓
HttpLoggingInterceptor       (log request/response)
    ↓
LogFileInterceptor           (log ke file jika @LogFile)
    ↓
Network
```

### `RequestInterceptor`

```kotlin
class RequestInterceptor @Inject constructor(
    private val prefs: PreferenceClass
) : Interceptor {
    override fun intercept(chain: Chain): Response {
        if (!Utils.isInternetAvailable()) throw ApiException(code = 0)

        val token = prefs.get<String>("user_token", "")
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }
}
```

### `ResponseInterceptor` & `ApiException`

```kotlin
class ApiException(
    override val message: String?,
    val responseCode: Int?,
    val errorTypes: Array<String> = emptyArray(),
    val data: Map<String, Any> = emptyMap()
) : IOException(message)
```

- HTTP 401 → `intentUtil.get().logOut()` + navigate ke MainActivity dengan extra `logout_message`
- HTTP error lain → parse `"message"` / `"error"` dari JSON body, throw `ApiException`
- Binary response (image/video/audio) → bypass body re-read untuk hindari corrupt

### Custom Annotations

```kotlin
// Timeout per endpoint
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timeout(val value: Long, val unit: TimeUnit = TimeUnit.SECONDS)

// Log ke file
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogFile

// Gunakan mock URL
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseMock
```

---

## 6. API Service Pattern

### Aturan Service Interface

Semua API service adalah **Kotlin interface** di package `id.diskola.app.apiservice`.

```kotlin
// Contoh: AuthApiService.kt
interface AuthApiService {
    @POST("auth/check-account")
    suspend fun checkAccount(@Body request: CheckAccountRequest): CheckAccountApiResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginAccountRequest): LoginAccountApiResponse

    @GET("schools")
    suspend fun getSchools(@Query("q") query: String): SchoolListResponse
}
```

**Aturan wajib:**
1. Semua fungsi adalah `suspend fun` — wajib, tanpa pengecualian
2. Request body typed: gunakan data class (`@Body request: SomeRequest`), **bukan** `Map<String, Any>`
3. `@Body Map<String, Any>` hanya boleh sebagai *scaffold placeholder* sementara + ada komentar `// TODO: buat data class`
4. Multipart upload:
   ```kotlin
   @Multipart
   @POST("materi/upload")
   suspend fun uploadMateri(
       @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
       @Part file: MultipartBody.Part
   ): ActionMessageResponse
   ```
5. Naming: `<Domain>ApiService` — contoh `MateriApiService`, `AbsensiApiService`

---

## 7. ViewModel Pattern

### `BaseViewModel` — WAJIB diextend semua ViewModel

```kotlin
open class BaseViewModel : ViewModel() {

    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    protected val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = "" }

    protected fun emitError(message: String) {
        _errorMessage.value = message
    }

    protected fun handleError(throwable: Throwable, customMessage: String? = null) {
        _errorMessage.value = customMessage ?: AppErrorHandler.getMessage(throwable)
    }

    protected fun launchWithHandling(
        showLoading: Boolean = true,
        showError: Boolean = true,
        customMessage: String? = null,
        block: suspend () -> Unit
    ): Job {
        // WAJIB: set loading SEBELUM launch (lihat rules §1.3)
        if (showLoading) _loading.value = true
        return viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showError) handleError(e, customMessage)
            } finally {
                if (showLoading) _loading.value = false
            }
        }
    }
}
```

### Concrete ViewModel

```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApiService: AuthApiService,
    private val prefs: PreferenceClass
) : BaseViewModel() {

    private val _loginState = MutableStateFlow<LoginAccountUserData?>(null)
    val loginState: StateFlow<LoginAccountUserData?> = _loginState.asStateFlow()

    fun login(request: LoginAccountRequest) {
        launchWithHandling {
            val response = authApiService.login(request)
            _loginState.value = response.data
        }
    }
}
```

**Aturan ViewModel:**
1. Semua ViewModel extend `BaseViewModel`
2. Annotasi `@HiltViewModel` — wajib
3. State diberi tahu via `StateFlow`, **bukan** `LiveData`
4. Private `MutableStateFlow`, public `asStateFlow()` — selalu pair ini
5. Semua network call dibungkus `launchWithHandling { }`
6. **Tidak ada** business logic di Composable, tidak ada network call di Composable
7. Max ~500 baris per ViewModel — jika lebih, pecah per sub-domain
8. Naming: `<Domain>ViewModel` — contoh `AuthViewModel`, `MateriViewModel`

---

## 8. Data Classes / Model Layer

### Package Structure

```
dataclass/
├── ResponData/    ← Request & Response dari API
├── akm/           ← UI models (mapped dari API models)
├── localDb/       ← Room entities + DAOs
└── mock/          ← Placeholder UI (belum ada API backing)
```

### API Models (`ResponData/`)

```kotlin
// WAJIB: @JsonClass untuk Moshi KSP codegen
@JsonClass(generateAdapter = true)
data class LoginAccountRequest(
    @Json(name = "nisn") val nisn: String,
    @Json(name = "password") val password: String,
    @Json(name = "firebase_token") val firebaseToken: String
)

@JsonClass(generateAdapter = true)
data class LoginAccountApiResponse(
    @Json(name = "status") val status: Boolean,
    @Json(name = "message") @NullToEmptyString val message: String,
    @Json(name = "data") val data: LoginAccountUserData?
)
```

**Aturan model:**
1. Semua data class untuk API wajib `@JsonClass(generateAdapter = true)`
2. Field yang bisa null dari server tapi tidak boleh null di UI: gunakan `@NullToEmptyString`
3. Naming request: `<Action>Request` — `LoginAccountRequest`, `CheckAccountRequest`
4. Naming response: `<Action>ApiResponse` atau `<Domain>Response` — `LoginAccountApiResponse`
5. Jangan pakai nama yang sama untuk model berbeda di package berbeda (konflik naming)
6. Field `@Json(name = "...")` wajib dicantumkan untuk semua field (jangan andalkan reflection)

### UI Models

```kotlin
// Di package akm/ atau langsung di dataclass domain
data class AkmQuestionUiModel(
    val id: Int,
    val text: String,
    val options: List<AkmOptionUiModel>,
    val selectedOptionId: Int? = null,
    val localMediaPath: String? = null    // null = belum download
)
```

### Room Entities (`localDb/`)

```kotlin
@Entity(tableName = "akm_synced_exam", indices = [Index(value = ["exam_id"], unique = true)])
@JsonClass(generateAdapter = true)
data class AkmSyncedExam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Json(name = "exam_id") val examId: String,
    @Json(name = "download_status") val downloadStatus: DownloadStatus = DownloadStatus.NOT_STARTED,
    @Json(name = "schedule") val schedule: AkmScheduleData? = null
)
```

### Mock Models (`mock/`)

Dipakai sementara untuk UI yang belum punya API. Wajib diberi komentar:

```kotlin
// MOCK — belum ada API. Ganti dengan model real saat endpoint tersedia.
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val isRead: Boolean
)
```

---

## 9. Room Database

### Database Class

```kotlin
@Database(
    version = 2,
    exportSchema = false,
    entities = [User::class, AkmSyncedExam::class]
)
@TypeConverters(DbConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun akmSyncDao(): AkmSyncDao
}
```

**Aturan Room:**
1. **Satu file `ebede.db` = satu `@Database`** — jangan buat database kedua yang menunjuk file sama
2. `fallbackToDestructiveMigration()` — migrasi dengan hapus data, bukan migrasi bertahap (dev build)
3. TypeConverters wajib ada untuk semua tipe non-primitive (List, data class, enum)
4. DAO selalu di package `localDb/` bersamaan dengan entity-nya

---

## 10. Navigasi

### Pola: Single-Activity + Compose Navigation dengan Type-Safe Routes

#### `Route.kt` — Sealed Interface

```kotlin
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Onboarding : Route

    sealed interface Auth : Route {
        @Serializable data object Login : Auth
        @Serializable data class Password(
            val userData: CheckAccountUserData  // data diteruskan sebagai field, bukan String
        ) : Auth
        @Serializable data object Sso : Auth
        @Serializable data object ResetPassword : Auth
    }

    @Serializable data object Main : Route

    sealed interface MainTab : Route {
        @Serializable data object Pembelajaran : MainTab
        @Serializable data object Pembayaran : MainTab
        @Serializable data object Akun : MainTab
    }

    sealed interface Akm : Route {
        @Serializable data object List : Akm
        @Serializable data class Detail(val id: String) : Akm
        @Serializable data class Question(val id: String) : Akm
        @Serializable data class Score(val id: String) : Akm
    }
    // ... dst
}
```

**Aturan Route:**
1. Semua `object` / `class` di `Route` harus `@Serializable`
2. Data antar screen diteruskan sebagai **field constructor data class**, bukan sebagai string query param
3. Setiap grup fitur jadi `sealed interface` bersarang dalam `Route`

#### `AppNavHost.kt`

```kotlin
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> { SplashScreen(...) }
        composable<Route.Onboarding> { OnboardingScreen(...) }

        // Auth sub-graph, diextract ke extension function
        authGraph(navController)

        composable<Route.Main> {
            MainTabsScreen(navController = navController)
        }
        // ... fitur lain
    }
}

// Extension function untuk sub-graph
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<Route.Auth.Login> {
        val viewModel: AuthViewModel = hiltViewModel()  // shared dalam auth back-stack
        LoginScreen(viewModel = viewModel, ...)
    }
    composable<Route.Auth.Password> { backStack ->
        val route = backStack.toRoute<Route.Auth.Password>()
        PasswordScreen(userData = route.userData, ...)
    }
}
```

#### ViewModel Scoping Lintas Screen

```kotlin
// AkmViewModel di-share dari Route.Akm.List ke semua sub-screen AKM
@Composable
fun AkmDetailScreen(navController: NavHostController) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(Route.Akm.List)
    }
    val viewModel: AkmViewModel = hiltViewModel(parentEntry)
    ...
}
```

#### `MainTabsScreen.kt`

```kotlin
@Composable
fun MainTabsScreen(navController: NavHostController) {
    val windowWidth = LocalWindowWidth.current
    // Compact → BottomNavigationBar
    // Medium/Expanded → NavigationRail
    when (windowWidth) {
        WindowWidth.Compact -> BottomNavLayout(...)
        else -> NavigationRailLayout(...)
    }
}
```

**Urutan navigasi yang WAJIB diikuti:**
```
Splash → Onboarding → Login (NISN) → Password → Home (MainTabs)
```
Jangan mengubah urutan ini kecuali diminta user secara eksplisit.

---

## 11. UI Layer (Composables)

### Anatomi Screen

```kotlin
// Pola wajib untuk setiap screen
@Composable
fun MateriListScreen(
    // 1. Navigation callbacks sebagai lambdas — bukan NavController langsung
    onBack: () -> Unit,
    onNavigateToDetail: (materiId: String) -> Unit,
    modifier: Modifier = Modifier,
    // 2. ViewModel parameter dengan default hiltViewModel() untuk preview
    viewModel: MateriViewModel = hiltViewModel(),
) {
    // 3. Collect state dengan lifecycle awareness
    val uiState by viewModel.materiList.collectAsStateWithLifecycle()
    val isLoading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // 4. One-time effect — HATI-HATI fetch di sini (lihat rules §1.1)
    LaunchedEffect(Unit) {
        // Hanya panggil fetch jika data belum ada atau wajib real-time
        if (uiState == null) viewModel.loadMateri()
    }

    // 5. Error snackbar
    val snackbarState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) {
            snackbarState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    // 6. Render state
    DetailScaffold(title = "Materi", onBack = onBack) {
        when {
            isLoading -> LoadingState()
            uiState.isNullOrEmpty() -> EmptyState(message = "Belum ada materi")
            else -> MateriListContent(items = uiState!!, onItemClick = onNavigateToDetail)
        }
    }
}
```

**Aturan UI:**
1. Screen menerima navigation callbacks sebagai lambdas — **tidak pernah** menerima `NavController`
2. State di-collect dengan `collectAsStateWithLifecycle()` (bukan `collectAsState()`)
3. Setiap screen **wajib** punya state: loading, empty, error, dan success
4. `LaunchedEffect(Unit)` fetch hanya jika data belum ada atau memang wajib real-time (lihat rules §1.1)
5. Error selalu di-clear setelah ditampilkan (`viewModel.clearError()`)
6. Business logic: **nol** — semua di ViewModel

### Background / Scaffold Pola

```kotlin
// BENAR: Background dibungkus SEKALI di luar NavHost (di MainActivity atau AppNavHost)
AmbientGradientBackground {
    AppNavHost(navController = rememberNavController())
}

// SALAH: Jangan rebuild background di tiap screen
@Composable
fun LoginScreen(...) {
    // ❌ JANGAN ini — background tidak perlu dibangun ulang tiap screen
    Box(modifier = Modifier.background(Brush.verticalGradient(...))) {
        ...
    }
}
```

### Pull-to-Refresh

```kotlin
// Data non-real-time: refresh HANYA lewat pull-to-refresh
val pullRefreshState = rememberPullToRefreshState()
PullToRefreshBox(state = pullRefreshState, onRefresh = { viewModel.refreshMateri() }) {
    LazyColumn { ... }
}
```

---

## 12. Komponen Bersama (Shared Components)

Semua komponen di `ui/components/`. **Gunakan komponen ini, jangan buat baru yang mirip.**

### `AppButton`

```kotlin
// 4 variant wajib
AppButton.Filled(text = "Login", onClick = {}, loading = isLoading)
AppButton.Tonal(text = "Lanjut", onClick = {})
AppButton.Outlined(text = "Batal", onClick = {})
AppButton.Text(text = "Lupa password?", onClick = {})

// Dengan ikon
AppButton.Filled(
    text = "Upload",
    onClick = {},
    leadingIcon = Icons.Default.Upload
)
```

### `AppTextField`

```kotlin
AppTextField(
    value = nisn,
    onValueChange = { nisn = it },
    label = "NISN",
    placeholder = "Masukkan NISN",
    trailingContent = { /* ikon atau tombol */ }
)

// Read-only / picker field
AppTextField(
    value = selectedDate,
    onValueChange = {},
    label = "Tanggal",
    readOnly = true,
    onClick = { showDatePicker = true }
)
```

### `States` (Loading / Empty / Error)

```kotlin
// WAJIB ada di setiap screen
when {
    isLoading -> LoadingState()                                     // skeleton + spinner
    errorMessage.isNotBlank() -> ErrorState(message = errorMessage) // dengan tombol retry
    items.isEmpty() -> EmptyState(message = "Tidak ada data")
    else -> /* konten */
}
```

### `DetailScaffold`

```kotlin
// Untuk screen dengan tombol back
DetailScaffold(
    title = "Detail Absensi",
    onBack = onBack,
    actions = { /* optional trailing actions */ }
) {
    /* konten */
}
```

### `AmbientGradientBackground`

```kotlin
// Dibungkus sekali di luar NavHost, bukan di tiap screen
AmbientGradientBackground(modifier = Modifier.fillMaxSize()) {
    content()
}
```

---

## 13. Design System / Tema

### Entry Point

```kotlin
// Di MainActivity atau root composable
DiskolaTheme(darkTheme = isDarkMode) {
    AmbientGradientBackground {
        AppNavHost(navController = rememberNavController())
    }
}
```

### Color Tokens

```kotlin
// M3 colors
MaterialTheme.colorScheme.primary          // Teal utama (#006A60 light)
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurface

// Extended colors (custom)
MaterialTheme.extendedColors.success
MaterialTheme.extendedColors.warning
MaterialTheme.extendedColors.ambientGlowPrimary
MaterialTheme.extendedColors.brandGradientDark
MaterialTheme.extendedColors.examSurface
MaterialTheme.extendedColors.penaltyContainer
```

### Typography

```kotlin
// Font: Lato (bundled di res/font/)
MaterialTheme.typography.headlineMedium    // M3 standard
MaterialTheme.typography.bodyLarge

// Extended typography
MaterialTheme.extendedTypography.overline  // 10sp, Black weight, 2sp letter spacing
```

### Spacing

```kotlin
// SELALU gunakan token ini, jangan hardcode angka dp
import id.diskola.app.ui.theme.Spacing

Modifier.padding(horizontal = Spacing.lg)   // 16dp
Modifier.padding(vertical = Spacing.sm)     // 8dp

// Screen gutter standard
Modifier.padding(horizontal = ScreenHorizontalPadding)  // 20dp
```

**Token lengkap:**

| Token | Nilai |
|-------|-------|
| `Spacing.xs` | 4dp |
| `Spacing.sm` | 8dp |
| `Spacing.md` | 12dp |
| `Spacing.lg` | 16dp |
| `Spacing.xl` | 20dp |
| `Spacing.xxl` | 24dp |
| `Spacing.xxxl` | 32dp |
| `Spacing.huge` | 40dp |
| `Spacing.massive` | 48dp |
| `ScreenHorizontalPadding` | 20dp |

### Shape Tokens

```kotlin
// M3 shapes
MaterialTheme.shapes.medium           // 14dp

// Extended shapes
DiskolaExtraShapes.button             // 14dp
DiskolaExtraShapes.card               // 14dp
DiskolaExtraShapes.dialog             // 24dp
DiskolaExtraShapes.bottomSheetTop     // 28dp (top corners only)
DiskolaExtraShapes.avatar             // CircleShape
DiskolaExtraShapes.searchPill         // 24dp
```

### Adaptive Layout

```kotlin
// Deteksi ukuran layar
val windowWidth = LocalWindowWidth.current

when (windowWidth) {
    WindowWidth.Compact  -> /* phone layout */
    WindowWidth.Medium   -> /* tablet portrait */
    WindowWidth.Expanded -> /* tablet landscape / desktop */
}

// Batasi lebar konten di layar besar
Modifier.contentContainer()   // center + max 720dp
```

---

## 14. Utilities

### `PreferenceClass` (Hilt-injected)

```kotlin
@Inject lateinit var prefs: PreferenceClass

// Baca
val token = prefs.get<String>("user_token", defaultValue = "")
val userId = prefs.get<Int>("user_id", defaultValue = 0)

// Tulis
prefs.put("user_token", token)

// Hapus satu key
prefs.remove("user_token")

// Hapus semua (logout)
prefs.clear()
```

### `AppErrorHandler`

```kotlin
// Mengonversi Throwable ke pesan Indonesia yang user-friendly
val message = AppErrorHandler.getMessage(throwable)
// ApiException code 0 → "Tidak ada koneksi internet"
// ApiException code 401 → "Sesi berakhir, silakan login kembali"
// ApiException code 4xx → pesan dari server
// SocketTimeoutException → "Koneksi timeout, coba lagi"
```

### `IntentUtil`

```kotlin
// Logout: clear prefs + navigate ke MainActivity
@Inject lateinit var intentUtil: IntentUtil
intentUtil.logOut()
```

---

## 15. WorkManager

### Pola `@HiltWorker`

```kotlin
@HiltWorker
class AkmDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val asesmenApiService: AsesmenApiService,
    private val akmSyncDao: AkmSyncDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val examId = inputData.getString("exam_id") ?: return Result.failure()

        return try {
            akmSyncDao.updateStatus(examId, DownloadStatus.DOWNLOADING)
            // download logic...
            akmSyncDao.updateStatus(examId, DownloadStatus.DOWNLOADED)
            Result.success()
        } catch (e: Exception) {
            akmSyncDao.updateStatus(examId, DownloadStatus.FAILED)
            Result.failure()
        }
    }

    companion object {
        fun buildRequest(examId: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<AkmDownloadWorker>()
                .setInputData(workDataOf("exam_id" to examId))
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .build()
    }
}
```

**Aturan WorkManager:**
1. Semua Worker yang butuh DI: `@HiltWorker` + `@AssistedInject`
2. `WorkManagerInitializer` di-disable di Manifest, diganti `HiltWorkerFactory` di `App.kt`
3. **Jangan** `GlobalScope.launch` di dalam Worker — pakai scope yang disediakan `CoroutineWorker`
4. Output file disimpan di `filesDir` (internal storage), bukan `externalFilesDir`

---

## 16. Konvensi Penamaan

| Layer | Konvensi | Contoh |
|-------|----------|--------|
| Package root | `id.diskola.app` | — |
| Package per layer | `id.diskola.app.<layer>` | `id.diskola.app.viewmodel` |
| API Service (interface) | `<Domain>ApiService` | `AuthApiService`, `MateriApiService` |
| ViewModel | `<Domain>ViewModel` | `AuthViewModel`, `AbsensiViewModel` |
| Screen Composable | `<Feature>Screen` | `LoginScreen`, `HomeScreen` |
| Route (object) | `Route.<Domain>` | `Route.Auth.Login`, `Route.Main` |
| Route (class dengan data) | `Route.<Domain>.<Sub>(param)` | `Route.Akm.Detail(id)` |
| Request model | `<Action>Request` | `LoginAccountRequest`, `CheckAccountRequest` |
| Response model | `<Action>ApiResponse` atau `<Domain>Response` | `LoginAccountApiResponse` |
| UI model | `<Domain>UiModel` atau `<Domain>Item` | `AkmQuestionUiModel`, `SchoolItem` |
| Room entity | `@Entity(tableName = "snake_case")` | `"akm_synced_exam"`, `"users"` |
| StateFlow (private) | `_name: MutableStateFlow<T>` | `_loginState` |
| StateFlow (public) | `name: StateFlow<T> = _name.asStateFlow()` | `loginState` |
| Composable callback | `on<Verb>` | `onBack`, `onNavigateToDetail`, `onAccountVerified` |
| Extended theme token | `MaterialTheme.extended<X>.tokenName` | `extendedColors.success` |
| DI module | `<Target>Module` | `ApiModule`, `DbModule` |
| Worker | `<Domain>Worker` | `AkmDownloadWorker` |
| Utility class | `<Domain>Util` atau `<Domain>Utils` | `DateUtil`, `AppErrorHandler` |

---

## 17. Library & Versi

Semua versi dikelola via **Version Catalog** di `gradle/libs.versions.toml`.

| Library | Versi |
|---------|-------|
| Kotlin | 2.1.0 |
| AGP (Android Gradle Plugin) | 8.8.2 |
| Compose BOM | 2025.11.00 |
| Hilt | 2.56.2 |
| Navigation Compose | 2.9.6 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Moshi | 1.15.1 (KSP codegen) |
| Room | 2.7.2 |
| WorkManager | 2.10.2 |
| Firebase BOM | 32.7.1 |
| Coil Compose | 2.7.0 |
| Lottie | 6.5.2 |
| Timber | 5.0.1 |
| Chucker | 3.5.2 |
| kotlinx.serialization | 1.7.3 |
| KSP | (via plugin) |

**Aturan dependency:**
1. Tambah versi baru selalu di `libs.versions.toml`, jangan hardcode di `build.gradle.kts`
2. Serialization JSON: **Moshi** untuk network — jangan tambah Gson ke project baru
3. Image loading: **Coil** — jangan tambah Glide (Glide sudah ada untuk legacy, jangan diperluas)
4. `compileSdk` dan `targetSdk` selalu di angka terbaru yang stable

---

## 18. Rules Wajib

> File lengkap: [`docs/rules-global.md`](rules-global.md)

### §1 — Disiplin Fetch Data

**1.1 Tidak boleh fetch ulang setiap halaman dibuka**, kecuali data memang wajib real-time:
- `check-account` / status sesi login ✅
- Saldo wallet / status aktivasi wallet ✅
- Status transaksi pending ✅
- Aksi eksplisit user (submit, konfirmasi bayar) ✅
- **Semua data lain** (materi, tugas, jadwal, riwayat) → load dari cache/Room dulu ❌ jangan fetch ulang

**Cara refresh yang benar:** Pull-to-refresh eksplisit dari user.

**1.2 Tidak boleh memanggil endpoint yang sama dua kali** dalam satu siklus. Sebelum tambah pemanggilan API baru, grep dulu: `grep -rn "namaFungsiApi("`.

**1.3 Guard race condition — set flag SEBELUM `launch`:**

```kotlin
// ❌ SALAH
fun load() {
    viewModelScope.launch {
        if (isLoading) return@launch  // jendela race masih terbuka!
        isLoading = true
        ...
    }
}

// ✅ BENAR (sudah dihandle di BaseViewModel.launchWithHandling)
fun load() {
    if (_loading.value) return  // cek sebelum launch
    launchWithHandling { ... }
}
```

### §2 — Alur Harus Sama dengan Legacy

- Urutan navigasi: `Splash → Onboarding → Login (NISN) → Password → Home`
- Teks tombol, label, pesan error: **identik** dengan legacy, jangan parafrase
- Kondisi tampil/sembunyinya elemen UI: cek `docs/PAGE_UI_INVENTORY.md`
- Jika ada alur yang terasa aneh: **tulis di `docs/FLOW_QUESTIONS.md`**, jangan diam-diam ubah

### §3 — Sumber Kebenaran Dokumentasi

Sebelum mengasumsikan bentuk UI atau field API, cek berurutan:
1. `docs/rules-global.md` — aturan ini
2. `docs/PAGE_UI_INVENTORY.md` — elemen UI tiap halaman
3. `docs/api/` — kontrak request/response (355 endpoint)
4. `docs/features/` — dokumentasi mendalam per fitur
5. `android-portal/*.kt` — kode sumber legacy (baca langsung, jangan tebak dari nama)

---

## 19. Anti-Pattern yang Dilarang

| # | Dilarang | Alasan |
|---|----------|--------|
| 1 | `GlobalScope.launch` di Service/Worker/Socket | Job lepas dari lifecycle, bisa jalan setelah komponen mati |
| 2 | Bitmap >500KB sebagai background full-screen | Pernah ditemukan bitmap 8.8MB di-decode tiap layar |
| 3 | Dua `@Database` menunjuk file yang sama | Risiko data terhapus oleh `fallbackToDestructiveMigration` |
| 4 | Moshi + Gson bersamaan | Pilih satu — project ini pakai Moshi |
| 5 | `@Body data: Any` / `Map<String, Any>` untuk typed request | Kontrak API tidak eksplisit, rawan salah kirim field |
| 6 | Business logic / network call di Composable/Activity | Harus di ViewModel |
| 7 | ViewModel >500 baris yang handle banyak domain | Pecah per sub-domain |
| 8 | Nama class yang sama untuk model berbeda di package berbeda | Rawan tertukar sumber data |
| 9 | Background/chrome dibangun ulang di tiap screen | Bungkus sekali di luar NavHost (`AmbientGradientBackground`) |
| 10 | Teks statis yang terlihat dinamis tanpa komentar | Tandai `// HARDCODED` jika memang tidak dari API |
| 11 | Fetch di `LaunchedEffect(Unit)` tanpa guard cache | Menyebabkan fetch berulang saat screen di-navigate back |
| 12 | `LiveData` — dilarang di kode baru | Proyek ini full `StateFlow` |
| 13 | Fragment — dilarang di kode baru | Proyek ini full Compose |
| 14 | NavController diteruskan langsung ke screen Composable | Gunakan lambda callbacks |

---

## 20. Checklist Sebelum Bilang Selesai

Sebelum melaporkan task selesai ke user, pastikan semua ini sudah dilakukan:

- [ ] **Tidak ada endpoint terpanggil berulang** — verifikasi via Chucker/network log, buka-tutup layar
- [ ] **Alur dan teks sesuai dokumentasi** — cek `docs/PAGE_UI_INVENTORY.md` dan `docs/api/`
- [ ] **Tidak ada `GlobalScope`** di kode yang baru ditambahkan
- [ ] **Tidak ada business logic di Composable** — semua di ViewModel
- [ ] **Tidak ada `@Body Map<String, Any>`** untuk endpoint yang harusnya typed
- [ ] **Tidak ada `LiveData` atau Fragment** di kode baru
- [ ] **State loading/empty/error ada** di setiap screen baru
- [ ] **ViewModel extend `BaseViewModel`** dan pakai `launchWithHandling`
- [ ] **Flag loading di-set SEBELUM `launch`** (atau pakai `launchWithHandling`)
- [ ] **Penyimpangan dari alur lama** (jika ada) sudah dicatat di `docs/FLOW_QUESTIONS.md`
- [ ] **Build berjalan** — `./gradlew :app:assembleDebug` tidak error

---

*Dokumen ini dikelola secara manual — update setiap kali ada keputusan arsitektur baru yang disepakati bersama.*
