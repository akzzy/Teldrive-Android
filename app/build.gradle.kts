import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key) ?: defaultValue
}

android {
    namespace = "com.nuvio.app.teldrive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nuvio.app.teldrive"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "DEFAULT_SUPABASE_DSN", "\"${getLocalProperty("TELDRIVE_SUPABASE_DSN")}\"")
        buildConfigField("String", "DEFAULT_JWT_SECRET", "\"${getLocalProperty("TELDRIVE_JWT_SECRET")}\"")
        buildConfigField("String", "DEFAULT_PORT", "\"${getLocalProperty("TELDRIVE_PORT", "8080")}\"")
        buildConfigField("String", "DEFAULT_TG_APP_ID", "\"${getLocalProperty("TELDRIVE_TG_APP_ID")}\"")
        buildConfigField("String", "DEFAULT_TG_APP_HASH", "\"${getLocalProperty("TELDRIVE_TG_APP_HASH")}\"")
        buildConfigField("String", "DEFAULT_TG_UPLOADS_ENCRYPTION_KEY", "\"${getLocalProperty("TELDRIVE_TG_UPLOADS_ENCRYPTION_KEY")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
