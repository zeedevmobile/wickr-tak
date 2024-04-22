plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.atakmap.android.wickr.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.atakmap.android.wickr.wear"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {

    implementation(files("libs/priv-health-tracking-mock-2023.aar"))

    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.activity:activity-ktx:1.9.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // RxJava
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")

    // Koin
    implementation("io.insert-koin:koin-android:3.4.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}