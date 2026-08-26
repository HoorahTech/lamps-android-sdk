import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

group = "io.github.hoorahtech"
version = "7.6.4.2"

android {
    namespace = "io.github.hoorahtech.ads.sdk.tools"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    compileOnly(files("libs/ads-sdk-tools-7.6.4.2-hupu.aar"))
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.activity:activity:1.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    implementation("androidx.recyclerview:recyclerview:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.0")
    implementation("io.github.didi:drouter-api:2.1.3")
    implementation("androidx.databinding:viewbinding:7.0.4")
    implementation("androidx.databinding:databinding-common:7.0.4")
    implementation("androidx.databinding:databinding-runtime:7.0.4")
    implementation("androidx.databinding:databinding-adapters:7.0.4")
    implementation("androidx.databinding:databinding-ktx:7.0.4")
}

mavenPublishing {
    coordinates("io.github.hoorahtech", "pangle-ads-sdk-tools", version.toString())
    configure(AndroidSingleVariantLibrary(sourcesJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}

tasks.configureEach {
    if (name == "bundleReleaseAar") {
        doLast {
            val source = project.file("libs/ads-sdk-tools-7.6.4.2-hupu.aar")
            val target = layout.buildDirectory.file("outputs/aar/pangle-ads-sdk-tools-release.aar").get().asFile
            source.copyTo(target, overwrite = true)
        }
    }
}
