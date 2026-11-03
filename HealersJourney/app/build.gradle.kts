plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val lifeRpgUploadStore = providers.environmentVariable("LIFERPG_UPLOAD_STORE_FILE").orNull
val lifeRpgUploadPassword = providers.environmentVariable("LIFERPG_UPLOAD_PASSWORD").orNull

android {
    namespace = "com.healersjourney.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.life.rpg"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        // OAuth 2.0 client ID for Google Drive sync. Replace with your own
        // Android/Web client ID from Google Cloud Console to enable sign-in.
        buildConfigField("String", "DRIVE_OAUTH_CLIENT_ID", "\"REPLACE_WITH_OAUTH_CLIENT_ID\"")
        buildConfigField("String", "DRIVE_OAUTH_REDIRECT", "\"com.life.rpg:/oauth2redirect\"")

        // Required by AppAuth's RedirectUriReceiverActivity intent filter.
        manifestPlaceholders["appAuthRedirectScheme"] = "com.life.rpg"
    }

    signingConfigs {
        if (lifeRpgUploadStore != null && lifeRpgUploadPassword != null) {
            create("lifeRpgUpload") {
                storeFile = file(lifeRpgUploadStore)
                storePassword = lifeRpgUploadPassword
                keyAlias = "life-rpg-upload"
                keyPassword = lifeRpgUploadPassword
            }
        }
    }

    buildTypes {
        release {
            if (lifeRpgUploadStore != null && lifeRpgUploadPassword != null) {
                signingConfig = signingConfigs.getByName("lifeRpgUpload")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("net.openid:appauth:0.11.1")
    implementation("com.google.android.gms:play-services-games-v2:22.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
