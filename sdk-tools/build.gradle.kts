import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
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

}

mavenPublishing {
    configure(AndroidSingleVariantLibrary(sourcesJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":pangle"))
    implementation(project(":ylh"))
    implementation(project(":noah"))
    implementation(project(":pangle-ads-sdk-tools"))
    compileOnly(files("../pangle-ads-sdk-tools/libs/ads-sdk-tools-7.6.4.2-hupu.aar"))
    implementation("com.qq.e.union:tools:2.4")
}
