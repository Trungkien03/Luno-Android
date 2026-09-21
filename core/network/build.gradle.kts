import java.util.Properties

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.luno.core.network"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("PROJECT_URL") ?: ""}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${localProperties.getProperty("PUBLIC_KEY") ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)


    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Kotlinx Serialization Converter
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    // Supabase & Ktor
    api(platform(libs.supabase.bom))
    api(libs.supabase.postgrest.kt)
    api(libs.supabase.gotrue.kt)
    api(libs.supabase.realtime.kt)
    // OkHttp engine (not ktor-client-android) because Realtime needs WebSocket support.
    api(libs.ktor.client.okhttp)
}
