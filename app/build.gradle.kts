plugins {
    alias(libs.plugins.android.application)
    id ("androidx.navigation.safeargs")
}

android {
    namespace = "com.alexzava.krypto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alexzava.krypto"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-beta1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("io.github.chochanaresh:filepicker:0.2.5")
    implementation("com.goterl:lazysodium-android:5.1.0@aar")
    implementation("net.java.dev.jna:jna:5.8.0@aar")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("io.github.g0dkar:qrcode-kotlin:4.1.1")
}