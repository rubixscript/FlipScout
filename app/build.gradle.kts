import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.triplet.play")
}

// Secrets live in files that are git-ignored; nothing sensitive is committed.
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val serviceAccountJson = rootProject.file("play-service-account.json")

android {
    namespace = "com.rubixscript.flipscout"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rubixscript.flipscout"
        minSdk = 26; targetSdk = 36
        // Play needs a strictly increasing versionCode. Minutes since epoch is monotonic with no
        // counter file to merge; override with -PversionCode=N if you ever need a specific value.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: (System.currentTimeMillis() / 60_000).toInt()
        versionName = "0.1"
    }
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

play {
    if (serviceAccountJson.exists()) serviceAccountCredentials.set(serviceAccountJson)
    defaultToAppBundles.set(true)
    // internal | alpha | beta | production. Override: ./gradlew publishReleaseBundle -Ptrack=beta
    track.set((findProperty("track") as String?) ?: "internal")
    // A brand-new app can only take DRAFT releases until its first review; use -PreleaseStatus=completed after.
    releaseStatus.set(
        com.github.triplet.gradle.androidpublisher.ReleaseStatus.valueOf(
            ((findProperty("releaseStatus") as String?) ?: "draft").uppercase()
        )
    )
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    testImplementation("junit:junit:4.13.2")}
