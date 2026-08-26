plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.lamps.sdk.tools"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        release {
            // This tool depends on all channel modules; do not embed their classes in the AAR.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    publishing {
        singleVariant("release")
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            artifactId = "sdk-tools"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":pangle"))
    implementation(project(":ylh"))
    implementation(project(":noah"))
    implementation("com.pangle.cn:ads-sdk-tools:7.6.4.2-hupu")
    implementation("com.qq.e.union:tools:2.4")
}
