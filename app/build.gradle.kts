import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.io.ByteArrayOutputStream
import java.util.Locale
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Helper function to run Git commands
fun execAndGetStdout(vararg cmd: String): String {
    val stdout = ByteArrayOutputStream()
    try {
        project.exec {
            commandLine(*cmd)
            standardOutput = stdout
            isIgnoreExitValue = true
            errorOutput = System.err
        }
    } catch (e: Exception) {
        println("Warning: Failed to execute command '${cmd.joinToString(" ")}': ${e.message}")
    }
    return stdout.toString().trim()
}

// Calculate Git tag or short SHA (runs during Gradle configuration phase)
val gitTag: String = execAndGetStdout("git", "describe", "--tags", "--abbrev=0")
    .takeIf { it.isNotEmpty() }
    ?: execAndGetStdout("git", "rev-parse", "--short", "HEAD").takeIf { it.isNotEmpty() }
    ?: "unknownVersion"

// --- Custom Gradle Task for Renaming APK ---
abstract class RenameApkTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputApkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val outputFilename: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        val artifacts: BuiltArtifacts = builtArtifactsLoader.get().load(inputApkDirectory.get())
            ?: throw RuntimeException("Cannot load artifacts from $inputApkDirectory")

        // Note: This logic still assumes a single main APK. If you enable splits,
        // you might need to iterate through artifacts.elements and generate
        // appropriate names for each split APK file.
        if (artifacts.elements.size != 1) {
            println("Warning: Expected 1 artifact, found ${artifacts.elements.size} in input directory. Renaming the first one found.")
        }

        val artifact: BuiltArtifact = artifacts.elements.firstOrNull()
            ?: throw RuntimeException("No build artifact found in ${inputApkDirectory.get().asFile.path}")

        val originalApkPath = Paths.get(artifact.outputFile)
        if (!Files.exists(originalApkPath)) {
            throw RuntimeException("Original APK file not found at ${originalApkPath}")
        }

        val targetFilename = outputFilename.get()
        val targetFile = outputDirectory.file(targetFilename).get().asFile

        targetFile.parentFile.mkdirs()

        Files.copy(originalApkPath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        println("Renamed APK saved to: ${targetFile.path}")
    }
}


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
        debug {}
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
    composeOptions {
        // kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

// --- Wire the Custom Task using the Modern Android Components API ---
androidComponents {
    onVariants(selector().all()) { variant ->
        val buildTypeName = variant.buildType ?: "unknownBuildType"
        val desiredFilename = "Nido-${buildTypeName}-v${gitTag}.apk"
        val taskName = "rename${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Apk"

        val renameTaskProvider = project.tasks.register<RenameApkTask>(taskName) {
            // Wire task configuration properties
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            inputApkDirectory.set(variant.artifacts.get(SingleArtifact.APK)) // Input: Original APK directory
            outputFilename.set(desiredFilename) // Input: The name we want
            outputDirectory.set(project.layout.buildDirectory.dir("outputs/renamed_apks/${variant.name}")) // Output: Custom directory
        }

        // Hook the task into the build process
        variant.artifacts.use(renameTaskProvider)
            .wiredWithDirectories( // Wire directory properties
                RenameApkTask::inputApkDirectory,
                RenameApkTask::outputDirectory
            )
            // Use toTransformMany because SingleArtifact.APK is ContainsMany
            .toTransformMany(SingleArtifact.APK)
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
}