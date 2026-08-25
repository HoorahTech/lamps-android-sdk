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
include(":sdk-tools")
include(":demo")
