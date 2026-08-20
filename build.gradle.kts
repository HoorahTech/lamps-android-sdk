import org.gradle.api.publish.PublishingExtension

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
                    name = "HupuAndroid"
                    val isSnapshot = version.toString().contains("-SNAPSHOT")
                    url = uri(
                        if (isSnapshot) {
                            "https://nexus.hupu.io/repository/hupu-android-snapshot/"
                        } else {
                            "https://nexus.hupu.io/repository/hupu-android/"
                        }
                    )
                    credentials {
                        username = "kr"
                        password = "FgbI27KC3Jwc"
                    }
                }
            }
        }
    }
}
