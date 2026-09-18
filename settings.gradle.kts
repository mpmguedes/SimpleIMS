pluginManagement {
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.0"
        id("com.android.library") version "8.13.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("com.android.settings") version "8.13.0"
}

android {
    compileSdk {
        version = release(36)
    }
    minSdk {
        // TurboIMS Android 12 / API 31 variant.
        // minSdk is what caps *installation*; every framework API used by the
        // app was verified present on an API 31 framework (see docs/ANDROID12_AUDIT.md).
        version = release(31)
    }
    targetSdk {
        // Keep targetSdk at the newest level the project already builds against.
        // minSdk and targetSdk are independent: lowering targetSdk is NOT required
        // to install on Android 12, and lowering it would only disable
        // forward-compat behaviours for no benefit.
        version = release(36)
    }
    buildToolsVersion = "36.1.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "Ims"
include(":app")
include(":stub")
