import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "dropit.mobile"
        minSdkVersion(22)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        named("release"){
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // project libs
    implementation(project(":common"))
    // support libs
    implementation(Deps.appCompat)
    implementation(Deps.constraintLayout)
    implementation(Deps.recyclerView)
    implementation(Deps.material)
    implementation(Deps.legacySupportV4)
    // other dependencies
    implementation(Deps.androidRetrofuture)
    implementation(Deps.ankoSqlite)
    // test
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidTestJunit)
    androidTestImplementation(Deps.espressoCore)
}
