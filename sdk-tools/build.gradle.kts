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
    implementation("io.github.hoorahtech:pangle-ads-sdk-tools:${rootProject.property("PANGLE_ADS_SDK_TOOLS_VERSION")}") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
    implementation("com.qq.e.union:tools:${rootProject.property("GDT_TOOLS_VERSION")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
}
