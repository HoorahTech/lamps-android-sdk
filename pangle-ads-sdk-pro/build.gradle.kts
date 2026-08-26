import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

group = "io.github.hoorahtech"
version = "7.6.1.2"

android {
    namespace = "io.github.hoorahtech.ads.sdk.pro"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    compileOnly(files("libs/ads-sdk-pro-7.6.1.2.aar"))
}

mavenPublishing {
    coordinates("io.github.hoorahtech", "pangle-ads-sdk-pro", version.toString())
    configure(AndroidSingleVariantLibrary(sourcesJar = false))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
}

tasks.configureEach {
    if (name == "bundleReleaseAar") {
        doLast {
            val source = project.file("libs/ads-sdk-pro-7.6.1.2.aar")
            val target = layout.buildDirectory.file("outputs/aar/pangle-ads-sdk-pro-release.aar").get().asFile
            source.copyTo(target, overwrite = true)
        }
    }
}
