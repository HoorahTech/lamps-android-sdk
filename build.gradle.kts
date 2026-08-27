import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.util.Properties

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}

fun configuredSecret(vararg keys: String): String? = keys.asSequence()
    .mapNotNull { key ->
        providers.gradleProperty(key).orNull
            ?: System.getenv(key)
            ?: localProperties.getProperty(key)
    }
    .map(String::trim)
    .firstOrNull(String::isNotEmpty)

// Central Portal reads org.gradle.project.* properties. Promote local values before
// the publishing plugin configures its repository credentials.
listOf(
    "hupuNexusUsername",
    "hupuNexusPassword",
    "hupu.nexus.username",
    "hupu.nexus.password",
    "mavenCentralUsername",
    "mavenCentralPassword",
    "signingInMemoryKey",
    "signingInMemoryKeyPassword"
).forEach { key ->
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() }?.let { value ->
        // The publishing plugin reads providers.gradleProperty(), so promote local
        // secrets through Gradle's actual project-property map.
        val projectProperties = gradle.startParameter.projectProperties.toMutableMap()
        projectProperties[key] = value
        gradle.startParameter.projectProperties = projectProperties
    }
}

val publishTarget = providers.gradleProperty("LAMPS_PUBLISH_TARGET")
    .orElse("maven")
    .get()
    .trim()
    .lowercase()
require(publishTarget == "maven" || publishTarget == "mavencentral") {
    "LAMPS_PUBLISH_TARGET must be 'maven' or 'mavenCentral', but was '$publishTarget'"
}

plugins {
    id("com.android.library") version "8.5.2" apply false
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.7.10" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

subprojects {
    group = "io.github.hoorahtech"
    version = rootProject.property("LAMPS_VERSION") as String

    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "HupuMaven"
                    url = uri("https://nexus.hupu.io/repository/hupu-android/")
                    credentials {
                        username = configuredSecret(
                            "hupuNexusUsername",
                            "hupu.nexus.username",
                            "HUPU_NEXUS_USERNAME"
                        ).orEmpty()
                        password = configuredSecret(
                            "hupuNexusPassword",
                            "hupu.nexus.password",
                            "HUPU_NEXUS_PASSWORD"
                        ).orEmpty()
                    }
                }
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set("Lamps Android SDK")
                    description.set("Lamps Android SDK and advertising channel integrations")
                    url.set("http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("hoorahtech")
                            name.set("Hoorahtech")
                        }
                    }
                    scm {
                        url.set("http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                        connection.set("scm:git:http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                        developerConnection.set("scm:git:http://gitlab.hupu.com/HPBase/lamps-android-sdk.git")
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("signing") {
        extensions.configure<SigningExtension> {
            val key = localProperties.getProperty("signingInMemoryKey")
            val password = localProperties.getProperty("signingInMemoryKeyPassword")
            if (!key.isNullOrBlank() && password != null) {
                useInMemoryPgpKeys(key, password)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

val releaseLibraryModules = listOf(
    "core", "sdk", "pangle", "ylh", "noah", "sdk-tools"
)
val excludedFromPublishAllModules = setOf(
    "pangle-ads-sdk-pro", "pangle-ads-sdk-tools", "noah-vendor"
)
val vendorModules = excludedFromPublishAllModules.toList()
val deliverableLibraryModules = listOf("core", "sdk", "pangle", "ylh", "noah", "sdk-tools")
val releaseVersion = rootProject.property("LAMPS_VERSION").toString()
val sdkLibDir = rootProject.layout.projectDirectory.dir("sdk_lib").asFile

val collectReleaseAars = tasks.register("collectReleaseAars") {
    group = "publishing"
    description = "Collect all release AARs into sdk_lib with versioned names."
    dependsOn(deliverableLibraryModules.map { ":$it:assembleRelease" })

    doLast {
        delete(sdkLibDir)
        sdkLibDir.mkdirs()
        deliverableLibraryModules.forEach { moduleName ->
            val aar = rootProject.file("$moduleName/build/outputs/aar/$moduleName-release.aar")
            check(aar.isFile) { "Release AAR not found: ${aar.absolutePath}" }
            aar.copyTo(
                rootProject.file("sdk_lib/lamps-$moduleName-$releaseVersion.aar"),
                overwrite = true
            )
        }
        logger.lifecycle("Collected ${deliverableLibraryModules.size} AARs in ${sdkLibDir.absolutePath}")
    }
}

val publishModules = releaseLibraryModules.filterNot(excludedFromPublishAllModules::contains)
val publishTasks = if (publishTarget == "maven") {
    publishModules.map { ":$it:publishAllPublicationsToHupuMavenRepository" }
} else {
    publishModules.map { ":$it:publishToMavenCentral" }
}
collectReleaseAars.configure { mustRunAfter(publishTasks) }

val publishAll = tasks.register("publishAll") {
    group = "publishing"
    description = "Publish all release libraries to Maven Central and collect versioned AARs."
    dependsOn(publishTasks)
    dependsOn(collectReleaseAars)
}

val vendorPublishTasks = if (publishTarget == "maven") {
    vendorModules.map { ":$it:publishMavenPublicationToHupuMavenRepository" }
} else {
    vendorModules.map { ":$it:publishToMavenCentral" }
}

tasks.register("publishVendors") {
    group = "publishing"
    description = "Publish vendor libraries to the configured repository."
    dependsOn(vendorPublishTasks)
}

tasks.register("push") {
    group = "publishing"
    description = "Publish all SDK libraries and collect their AARs in sdk_lib."
    dependsOn(publishAll)
}
