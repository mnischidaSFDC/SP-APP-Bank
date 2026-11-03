pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository") }
        maven { url = uri("https://salesforce-marketingcloud.github.io/mobile-sdk-cdp-android/repository") }
        maven { url = uri("https://salesforce-marketingcloud.github.io/Personalization-Android/repository") }
    }
}

rootProject.name = "Salesforce MN"
include(":app")
