plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.vvb2060.ims"
    defaultConfig {
        applicationId = "io.github.turboims.pixel"
        versionCode = 5
        versionName = "3.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs["debug"]
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "**"
        }
    }
    lint {
        checkReleaseBuilds = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    dependenciesInfo {
        includeInApk = false
    }
}

dependencies {
    compileOnly(project(":stub"))
    implementation(libs.shizuku.provider)
    implementation(libs.shizuku.api)
    implementation(libs.hiddenapibypass)

    // CarrierConfigCompat encodes behaviour that differs between Android 12/13/14.
    // Robolectric lets those branches be exercised as real unit tests on the JVM
    // instead of relying on a physical device, which is exactly the part of this
    // port that could otherwise regress silently.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
