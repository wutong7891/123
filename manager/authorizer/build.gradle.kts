@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.lsplugin.apksign)
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileSdkVersionMinor: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val licensePrivateKey = System.getenv("WUTONG_LICENSE_PRIVATE_KEY").orEmpty()

apksign {
    storeFileProperty = "KEYSTORE_FILE"
    storePasswordProperty = "KEYSTORE_PASSWORD"
    keyAliasProperty = "KEY_ALIAS"
    keyPasswordProperty = "KEY_PASSWORD"
}

android {
    namespace = "com.jinfuwei.wutong.authorizer"
    compileSdk {
        version = release(androidCompileSdkVersion) {
            minorApiLevel = androidCompileSdkVersionMinor
        }
    }
    buildToolsVersion = androidBuildToolsVersion

    defaultConfig {
        applicationId = "com.jinfuwei.wutong.authorizer"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "LICENSE_PRIVATE_KEY_B64", "\"$licensePrivateKey\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

base {
    archivesName.set("Wutong-Device-Authorizer")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
