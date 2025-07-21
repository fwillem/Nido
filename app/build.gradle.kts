plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

import java.io.ByteArrayOutputStream
        import java.text.SimpleDateFormat
        import java.util.Date

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

// Get current branch name
val branchName: String = execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD")
    .ifEmpty { "unknown" }

// Current build date/time, formatted as yyyy-MM-dd_HH-mm-ss
val buildTime: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())

// Concatenate for unique version name
val versionName: String = "$branchName-$buildTime"

android {
    namespace = "com.example.nido"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nido"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = versionName  // <-- Branch name and build time!

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject string resources for branch name and build time
        resValue("string", "branch_name", branchName)
        resValue("string", "build_time", buildTime)
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

    // ADD THIS sourceSets BLOCK
    sourceSets {
        getByName("main") {
            // Change 'java.srcDirs' to 'kotlin.srcDirs'
            kotlin.srcDirs("src/main/kotlin")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
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
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
