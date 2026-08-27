import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

group = "io.github.hoorahtech"
version = rootProject.property("PANGLE_ADS_SDK_TOOLS_VERSION") as String

android {
    namespace = "io.github.hoorahtech.ads.sdk.tools"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    compileOnly(files("libs/ads-sdk-tools-7.6.4.2.aar"))
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.3.0")
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
            val source = project.file("libs/ads-sdk-tools-7.6.4.2.aar")
            val target = layout.buildDirectory.file("outputs/aar/pangle-ads-sdk-tools-release.aar").get().asFile
            source.copyTo(target, overwrite = true)
        }
    }
}
