plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.bmpodium.russianbilliards"
    compileSdk = 35
    defaultConfig { applicationId = "com.bmpodium.russianbilliards"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}
