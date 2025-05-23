plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.io.ByteArrayOutputStream

/**
 * Run a command and return its stdout (trimmed).
 * If the command fails or produces nothing, returns an empty string.
 */
fun execAndGetStdout(vararg cmd: String): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine(*cmd)
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    return stdout.toString().trim()
}

// Try to read the most recent tag; if there isn't one yet, fall back to short SHA
val gitTag: String = execAndGetStdout("git", "describe", "--tags", "--abbrev=0")
    .takeIf { it.isNotEmpty() }
    ?: execAndGetStdout("git", "rev-parse", "--short", "HEAD")


android {
    namespace = "com.example.nido"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nido"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = gitTag

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // <-- add this line to generate a String resource named "git_tag"
        resValue("string", "git_tag", gitTag)
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}