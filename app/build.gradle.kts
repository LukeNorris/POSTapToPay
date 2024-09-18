import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0" // Ensure the version matches your Kotlin version
}



android {
    namespace = "com.adyen.postaptopay"
    compileSdk = 34

    val localPropertiesFile = rootProject.file("local.properties")
    val localProperties = Properties()

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { stream ->
            localProperties.load(stream)
        }
    }

    // Read the 'env' variable from local.properties
    val env = localProperties.getProperty("ENV", "live")
    // Set the schemeName based on the environment
    val schemeName = when (env) {
        "test" -> "adyenpayments-test"
        "dev" -> "adyenpayments-dev"
        else -> "adyenpayments"
    }

    defaultConfig {
        applicationId = "com.adyen.postaptopay"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Add the manifest placeholder
        manifestPlaceholders["schemeName"] = schemeName

        buildConfigField("String", "ADYEN_API_KEY", "\"${localProperties["ADYEN_API_KEY"]}\"")
        buildConfigField("String", "ADYEN_MERCHANT_ACCOUNT", "\"${localProperties["ADYEN_MERCHANT_ACCOUNT"]}\"")
        buildConfigField("String", "PASSPHRASE", "\"${localProperties["PASSPHRASE"]}\"")
        buildConfigField("String", "KEY_IDENTIFIER", "\"${localProperties["KEY_IDENTIFIER"]}\"")
        buildConfigField("String", "KEY_VERSION", "\"${localProperties["KEY_VERSION"]}\"")
        buildConfigField("String", "SCHEME_NAME", "\"$schemeName\"")
        buildConfigField("String", "APP_LABEL", "\"POS TapToPay - $env\"")
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
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.2")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.compose.ui:ui:1.4.1")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.json:json:20210307")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("com.adyen:adyen-java-api-library:5.0.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.glassfish:javax.json:1.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
