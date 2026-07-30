import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}
val spotifyId = keystoreProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
val spotifySecret = keystoreProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: ""

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
    implementation(libs.bundles.database)
    implementation(libs.mlkit.language.id)
    implementation(libs.coroutines.play.services)
    implementation(libs.androidx.material.icons.extended)
    ksp(libs.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
}