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
        google()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://nexus.hupu.io/repository/hupu-android-public/")
        maven("https://artifact.bytedance.com/repository/pangle")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/HoorahTech/lamps-android-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orElse(providers.provider { "" })
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orElse(providers.environmentVariable("GH_TOKEN"))
                    .orElse(providers.provider { "" })
                    .get()
            }
        }
    }
}

rootProject.name = "lamps-android-sdk"
include(":core")
include(":pangle")
include(":ylh")
include(":noah")
include(":sdk-tools")
include(":demo")
