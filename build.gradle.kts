import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

plugins {
    id("com.android.library") version "8.5.2" apply false
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

subprojects {
    group = "com.lamps"
    version = rootProject.property("LAMPS_VERSION") as String

    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "HupuNexus"
                    url = uri("https://nexus.hupu.io/repository/hupu-android/")
                    credentials {
                        username = providers.gradleProperty("hupuNexusUsername")
                            .orElse(providers.environmentVariable("HUPU_NEXUS_USERNAME"))
                            .orElse(localProperties.getProperty("hupu.nexus.username"))
                            .orNull ?: ""
                        password = providers.gradleProperty("hupuNexusPassword")
                            .orElse(providers.environmentVariable("HUPU_NEXUS_PASSWORD"))
                            .orElse(localProperties.getProperty("hupu.nexus.password"))
                            .orNull ?: ""
                    }
                }
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    url.set("http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                    scm {
                        url.set("http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                        connection.set("scm:git:http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                        developerConnection.set("scm:git:http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                    }
                }
            }
        }
    }
}

val releaseLibraryModules = listOf("core", "sdk", "pangle", "ylh", "noah", "sdk-tools")
val releaseVersion = rootProject.property("LAMPS_VERSION").toString()
val sdkLibDir = rootProject.layout.projectDirectory.dir("sdk_lib").asFile

val collectReleaseAars = tasks.register("collectReleaseAars") {
    group = "publishing"
    description = "Collect all release AARs into sdk_lib with versioned names."
    dependsOn(releaseLibraryModules.map { ":$it:assembleRelease" })

    doLast {
        delete(sdkLibDir)
        sdkLibDir.mkdirs()
        releaseLibraryModules.forEach { moduleName ->
            val aar = rootProject.file("$moduleName/build/outputs/aar/$moduleName-release.aar")
            check(aar.isFile) { "Release AAR not found: ${aar.absolutePath}" }
            aar.copyTo(
                rootProject.file("sdk_lib/lamps-$moduleName-$releaseVersion.aar"),
                overwrite = true
            )
        }
        logger.lifecycle("Collected ${releaseLibraryModules.size} AARs in ${sdkLibDir.absolutePath}")
    }
}

val publishTasks = releaseLibraryModules.map { ":$it:publish" }
collectReleaseAars.configure { mustRunAfter(publishTasks) }

val publishAll = tasks.register("publishAll") {
    group = "publishing"
    description = "Publish all release libraries to Hupu Nexus and collect versioned AARs."
    dependsOn(publishTasks)
    dependsOn(collectReleaseAars)
}

tasks.register("push") {
    group = "publishing"
    description = "Publish all SDK libraries and collect their AARs in sdk_lib."
    dependsOn(publishAll)
}
