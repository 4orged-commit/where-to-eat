import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing details are kept OUTSIDE the project folder (same key folder as Ledger), so sharing the project
// never takes them along. Without them the release build is simply left unsigned.
val keystoreProps = Properties().apply {
    val candidates = listOfNotNull(
        System.getenv("BUDGET_KEYSTORE_PROPERTIES")?.let(::File),
        File(System.getProperty("user.home"), "BudgetTrackerKeys/keystore.properties"),
        rootProject.file("keystore.properties"),
    )
    candidates.firstOrNull { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.wheretoeat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wheretoeat"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

// After every release build the signed APK is copied next to the project as WhereToEat-<version>.apk and into
// Google Drive under Projects/Where to Eat (only the newest version is kept there).
val publishRelease = tasks.register("publishRelease") {
    val versionName = android.defaultConfig.versionName
    val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    val projectCopy = rootProject.file("../WhereToEat-$versionName.apk")
    val driveDir = System.getenv("WHERETOEAT_DRIVE_DIR")?.let(::File)
        ?: (('D'..'Z').map { File("$it:\\My Drive") } + File(System.getProperty("user.home"), "My Drive"))
            .firstOrNull { it.isDirectory }?.let { File(it, "Projects/Where to Eat") }
    doLast {
        val signed = apk.get().asFile
        if (!signed.exists()) {
            logger.warn("WhereToEat: no signed app-release.apk (is the signing key set up?), nothing copied.")
            return@doLast
        }
        signed.copyTo(projectCopy, overwrite = true)
        logger.lifecycle("WhereToEat: copied to $projectCopy")
        if (driveDir == null) {
            logger.lifecycle("WhereToEat: Google Drive for desktop not found, so not copied to Drive.")
            return@doLast
        }
        driveDir.mkdirs()
        driveDir.listFiles { f -> f.name.startsWith("WhereToEat-") && f.name.endsWith(".apk") }?.forEach { it.delete() }
        signed.copyTo(File(driveDir, "WhereToEat-$versionName.apk"), overwrite = true)
        logger.lifecycle("WhereToEat: copied to Google Drive, $driveDir")
    }
}
tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy(publishRelease) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
