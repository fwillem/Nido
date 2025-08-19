plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

import java.io.ByteArrayOutputStream
        import java.time.LocalDateTime
        import java.time.format.DateTimeFormatter

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

// Try to read the most recent tag; if none, fall back to short SHA
val gitTag: String = execAndGetStdout("git", "describe", "--tags", "--abbrev=0")
    .takeIf { it.isNotEmpty() }
    ?: execAndGetStdout("git", "rev-parse", "--short", "HEAD")

// Get current branch name, strip "heads/" prefix if present
val branchRaw: String = execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD")
val branchName: String = branchRaw.removePrefix("heads/")

// Build time in a consistent format
val buildTime: String = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

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

        // Inject Git metadata as string resources
        resValue("string", "git_tag", gitTag)
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

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkAllWarnings = true
        checkTestSources = false
        checkDependencies = true
        htmlReport = true
        xmlReport = false
        enable.add("HardcodedText")
    }
}

tasks.named("check") {
    dependsOn("lintDebug")
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
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
