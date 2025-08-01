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


    // Combine and move the lint block to the end of the android block (as you originally intended)
    lint {
        // Fail the build if errors are found
        abortOnError = true

        // Treat all warnings as errors
        warningsAsErrors = true

        // Optional: Enable checking for all issues, including those disabled by default
        checkAllWarnings = true

        // Optional: Don't ignore test sources. Setting this to 'false' means lint WILL check test sources.
        // If you truly want to ignore test sources, set it to 'true'. Given your goal, you likely want to check them.
        checkTestSources = false

        // Optional: Include dependencies
        // 'checkDependencies = true' is usually the default and recommended to catch issues in libraries.
        // Setting it to 'false' might hide issues in transitive dependencies.
        checkDependencies = true // Changed to true, as it's generally a good idea.

        // To output HTML report automatically (often done by default with 'lint' task):
        htmlReport = true
        xmlReport = false // If you only want HTML

        // To specify a baseline file to ignore existing issues (as suggested by lint output):
        // baseline file("lint-baseline.xml")

        // Crucial for your use case: Explicitly enable the HardcodedText check.
        // This issue primarily targets UI elements (XML, Compose Text).
        // For general hardcoded strings in Kotlin logic, Lint's default checks might not cover everything.
        // You might need a custom Lint check or another static analysis tool for that.
        enable.add("HardcodedText") // This is the correct way to enable a specific check
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
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
  //  lintChecks("androidx.compose.ui:ui-tooling:1.6.0") {
  //      exclude(group = "org.jetbrains.skiko", module = "skiko")
  //  }
}
