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
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/HoorahTech/lamps-android-sdk")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: ""
                        password = System.getenv("GITHUB_TOKEN")
                            ?: System.getenv("GH_TOKEN")
                            ?: ""
                    }
                }
            }
            publications.withType<MavenPublication>().configureEach {
                pom {
                    url.set("https://github.com/HoorahTech/lamps-android-sdk")
                    scm {
                        url.set("https://github.com/HoorahTech/lamps-android-sdk")
                        connection.set("scm:git:https://github.com/HoorahTech/lamps-android-sdk.git")
                        developerConnection.set("scm:git:ssh://git@github.com/HoorahTech/lamps-android-sdk.git")
                    }
                }
            }
        }
    }
}
