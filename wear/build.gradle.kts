plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.atakmap.android.wickr.plugin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.atakmap.android.wickr.plugin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../keygen/android_keystore")
            storePassword = "tnttnt" as String
            keyAlias = "wintec_mapping" as String
            keyPassword = "tnttnt" as String
        }
        create("release") {
            storeFile = file("../keygen/android_keystore")
            storePassword = "tnttnt" as String
            keyAlias = "wintec_mapping" as String
            keyPassword = "tnttnt" as String
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("sdk")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks += listOf("odk")
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
}

dependencies {
    implementation(project(":common"))
    implementation(files("libs/priv-health-tracking-mock-2023.aar"))

    // Android
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Wear
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.wear:wear:1.3.0")

    // RxJava
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")

    // Koin
    implementation("io.insert-koin:koin-android:3.4.2")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}
