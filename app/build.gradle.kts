import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
}

abstract class GenerateCardPngAssetsTask : DefaultTask() {
  @get:InputDirectory
  abstract val svgDir: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val rasterizer = findRasterizer()
      ?: error("No SVG rasterizer found on PATH. Install `rsvg-convert` or `magick`.")
    val sourceDir = svgDir.get().asFile
    val generatedDir = outputDir.get().asFile.resolve("generated_cards")
    generatedDir.deleteRecursively()
    generatedDir.mkdirs()

    sourceDir.listFiles { file -> file.extension == "svg" }
      ?.sortedBy { it.name }
      ?.forEach { svg ->
        val output = File(generatedDir, "${svg.nameWithoutExtension}.png")
        when (rasterizer) {
          Rasterizer.Rsvg ->
            runCommand("rsvg-convert", "-w", "720", "-h", "1008", svg.absolutePath, "-o", output.absolutePath)
          Rasterizer.Magick ->
            runCommand("magick", svg.absolutePath, "-resize", "720x1008!", output.absolutePath)
        }
      }
  }

  private fun findRasterizer(): Rasterizer? = when {
    commandOnPath("rsvg-convert") -> Rasterizer.Rsvg
    commandOnPath("magick") -> Rasterizer.Magick
    else -> null
  }

  private fun commandOnPath(name: String): Boolean {
    return System.getenv("PATH")
      ?.split(File.pathSeparatorChar)
      ?.asSequence()
      ?.map { File(it, name) }
      ?.any { it.isFile && it.canExecute() } == true
  }

  private fun runCommand(vararg args: String) {
    val process = ProcessBuilder(*args)
      .inheritIO()
      .start()
    check(process.waitFor() == 0) { "Command failed: ${args.joinToString(" ")}" }
  }

  private enum class Rasterizer { Rsvg, Magick }
}

val generateCardPngAssets by tasks.registering(GenerateCardPngAssetsTask::class) {
  svgDir.set(layout.projectDirectory.dir("src/main/assets/cards"))
  outputDir.set(layout.buildDirectory.dir("generated/card-assets"))
}

android {
  namespace = "se.bernhauser.solitaire"
  compileSdk = 36

  defaultConfig {
    applicationId = "se.bernhauser.solitaire"
    minSdk = 33
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  kotlinOptions {
    jvmTarget = "11"
  }

  sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/card-assets"))
}

tasks.named("preBuild").configure {
  dependsOn(generateCardPngAssets)
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
