pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TruckDelivery"
include(":app")

// Enable configuration cache
gradle.startParameter.isConfigurationCacheEnabled = true

// Enable build cache
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}

// Configure Gradle
gradle.beforeProject {
    // Set project-wide Gradle properties
    extra["compileSdk"] = 34
    extra["targetSdk"] = 34
    extra["minSdk"] = 24

    // Set version numbers
    extra["versionCode"] = 1
    extra["versionName"] = "1.0.0"

    // Set dependency versions
    extra["kotlinVersion"] = "1.8.20"
    extra["composeVersion"] = "1.4.3"
    extra["coroutinesVersion"] = "1.7.3"
    extra["lifecycleVersion"] = "2.6.2"
    extra["navigationVersion"] = "2.7.5"
}
