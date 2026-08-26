import java.util.Properties

val localPublishProperties = Properties().apply {
    file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
val publishPropertyNames = listOf(
    "hupuNexusUsername",
    "hupuNexusPassword",
    "hupu.nexus.username",
    "hupu.nexus.password",
    "mavenCentralUsername",
    "mavenCentralPassword",
    "signingInMemoryKey",
    "signingInMemoryKeyPassword"
)
val projectProperties = gradle.startParameter.projectProperties.toMutableMap()
publishPropertyNames.forEach { key ->
    localPublishProperties.getProperty(key)?.takeIf { it.isNotBlank() }?.let {
        projectProperties[key] = it
    }
}
gradle.startParameter.projectProperties = projectProperties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // CI seeds proprietary third-party AARs into ~/.m2 before building.
        // Keep local publishing disabled for normal developer builds.
        if (System.getenv("CI_MAVEN_DEPS") == "true") {
            mavenLocal()
        }
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://nexus.hupu.io/repository/hupu-android-public/")
        maven("https://artifact.bytedance.com/repository/pangle")
    }
}

rootProject.name = "lamps-android-sdk"
include(":core")
include(":sdk")
include(":pangle")
include(":ylh")
include(":noah")
include(":noah-vendor")
include(":pangle-ads-sdk-pro")
include(":pangle-ads-sdk-tools")
include(":sdk-tools")
include(":demo")
