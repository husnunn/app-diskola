import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "id.diskola.app"
    compileSdk = 36

    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    val webClientId = properties.getProperty("WEB_CLIENT_ID") ?: ""

    defaultConfig {
        applicationId = "id.diskola.app"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
        // Google Maps key for Agenda Mingguan's check-in map; read from local.properties (never committed).
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: ""
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_URL", properties.getProperty("API_URL_PROD", ""))
            buildConfigField("String", "ASSETS_URL", properties.getProperty("ASSETS_URL_PROD", "\"\""))
            buildConfigField("String", "PORTAL_URL", "\"https://portal.diskola.id\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isDebuggable = true
            buildConfigField("String", "API_URL", properties.getProperty("API_URL_DEV", ""))
            buildConfigField("String", "ASSETS_URL", properties.getProperty("ASSETS_URL_DEV", "\"\""))
            buildConfigField("String", "PORTAL_URL", "\"https://dev.portal.diskola.id\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.app.update.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Compose Navigation + ViewModel + type-safe routes
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt + WorkManager (AKM question/media background sync)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Sign-in with Google (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image / animation loading for Compose
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    // Runtime permissions for Compose
    implementation(libs.accompanist.permissions)

    implementation(libs.glide)
    kapt(libs.compiler)

    // OkHttp & Logging
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Retrofit + Moshi
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.timber)
    implementation(libs.gson)

    debugImplementation(libs.library)
    releaseImplementation(libs.library.no.op)

    // Lokasi (GPS)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Selfie camera for Presensi Dinas Luar
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // QR scan for Jurnal "Hadiri kelas" (no preview UI, no camera permission of our own)
    implementation(libs.play.services.code.scanner)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.identity)

    implementation(libs.androidx.multidex)
    // Firebase BoM — mengelola versi otomatis
    implementation(platform(libs.firebase.bom.v3271))
    implementation(libs.google.firebase.config.ktx)
    implementation(libs.google.firebase.analytics.ktx)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.messaging)
    implementation(libs.google.firebase.inappmessaging)
    implementation(libs.google.firebase.storage.ktx)
    implementation( libs.firebase.auth)
    implementation("com.google.firebase:firebase-analytics")


    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.dexter)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.lottie)

    // Native LaTeX/MathJax formula rendering for AKM question text (no WebView)
    implementation(libs.jlatexmath.android)

}
