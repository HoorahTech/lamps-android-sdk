plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hupu.games"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hupu.games"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        register("release") {
            storeFile = file("hupu_news.news")
            storePassword = "cmhaha123"
            keyAlias = "hupu_android"
            keyPassword = "cmhaha123"
            enableV2Signing = true
            enableV1Signing = true
        }
        getByName("debug") {
            storeFile = file("hupu_news.news")
            storePassword = "cmhaha123"
            keyAlias = "hupu_android"
            keyPassword = "cmhaha123"
            enableV2Signing = true
            enableV1Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val lampsVersion = rootProject.property("LAMPS_VERSION") as String
val useLocalLamps = rootProject.findProperty("useLocalLamps")?.toString()?.toBoolean() ?: true

dependencies {
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    if (useLocalLamps) {
        implementation(project(":sdk"))
        implementation(project(":pangle"))
        implementation(project(":ylh"))
        implementation(project(":noah"))
        debugImplementation(project(":sdk-tools"))
    } else {
        implementation("io.github.hoorahtech:pangle:$lampsVersion")
        implementation("io.github.hoorahtech:ylh:$lampsVersion")
        implementation("io.github.hoorahtech:noah:$lampsVersion")
        implementation("io.github.hoorahtech:sdk:$lampsVersion")
        debugImplementation("io.github.hoorahtech:sdk-tools:$lampsVersion")
    }
}
