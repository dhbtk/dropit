import dropitconf.Deps

plugins {
    id("dependencies-plugin")
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-android")
}

android {
    val commitCount: Int by project.extra
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "dropit.mobile"
        minSdkVersion(22)
        targetSdkVersion(30)
        versionCode = commitCount
        versionName = project.version.toString()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
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
    implementation(Deps.dagger)
//    implementation(Deps.daggerAndroid)
    implementation(Deps.daggerAndroidSupport)
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.2.1")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.2")
    val cameraxVersion = "1.0.0-beta12"
// CameraX core library using camera2 implementation
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
// CameraX Lifecycle Library
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
// CameraX View class
    implementation("androidx.camera:camera-view:1.0.0-alpha19")
    implementation("com.google.zxing:core:3.3.0")
    implementation(Deps.retrofit)
    implementation(Deps.okHttp)
    implementation(Deps.okHttpLoggingInterceptor)

    kapt(Deps.daggerCompiler)
    kapt(Deps.daggerAndroidProcessor)

    // test
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidTestJunit)
    androidTestImplementation(Deps.espressoCore)
}
