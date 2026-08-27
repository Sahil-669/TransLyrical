import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}
val spotifyId = keystoreProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
val spotifySecret = keystoreProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: ""
val supabaseKey = keystoreProperties.getProperty("SUPABASE_KEY_SECRET") ?: ""
val geminiKey = keystoreProperties.getProperty("GEMINI_API_KEY") ?: ""

android {
    namespace = "com.example.translyrical"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.translyrical"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyId\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$spotifySecret\"")
        buildConfigField("String", "SUPABASE_KEY_SECRET", "\"$supabaseKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.networking)
    implementation(libs.bundles.di)
    implementation(libs.coroutines.play.services)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation(libs.supabase.postgrest)
    implementation(libs.kotlinx.serialization)
    implementation(libs.navigation.compose)
}