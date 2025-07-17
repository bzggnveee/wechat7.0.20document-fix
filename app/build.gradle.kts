plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "n3e.a7020"
    compileSdk = 36

    defaultConfig {
        applicationId = "n3e.a7020"
        minSdk = 24
        targetSdk = 36
        versionCode = 9
        versionName = "v9"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly("de.robv.android.xposed:api:82")
}