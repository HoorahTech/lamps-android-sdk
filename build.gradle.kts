import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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
                    url = uri("https://nexus.hupu.io/repository/hupu-android-public/")
                    credentials {
                        username = providers.gradleProperty("hupuNexusUsername")
                            .orElse(providers.environmentVariable("HUPU_NEXUS_USERNAME"))
                            .orNull ?: ""
                        password = providers.gradleProperty("hupuNexusPassword")
                            .orElse(providers.environmentVariable("HUPU_NEXUS_PASSWORD"))
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
