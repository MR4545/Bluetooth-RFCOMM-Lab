plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mr4545.rfcommlab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mr4545.rfcommlab"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
