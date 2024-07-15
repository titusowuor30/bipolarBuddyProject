plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.bengohub.vitalstracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bengohub.vitalstracker"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
//implementation fileTree(include: ['*.jar'], dir: 'libs')

    implementation (libs.androidx.appcompat.appcompat.v110)
    implementation (libs.androidx.constraintlayout.v113)
    testImplementation (libs.junit.v412)
    implementation (libs.material.v110)
    implementation (libs.apache.commons.math3)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(kotlin("script-runtime"))
}