import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }

val signingProperties = Properties()
val signingPropertiesPath = System.getenv("SIGNING_PROPERTIES")
val signingPropertiesFile = signingPropertiesPath?.let(::file)
if (signingPropertiesFile?.isFile == true) signingPropertiesFile.inputStream().use(signingProperties::load)

android {
    namespace = "com.hunterbrewer.livingroomlauncher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.hunterbrewer.livingroomlauncher"
        minSdk = 23
        targetSdk = 34
        versionCode = 10
        versionName = "0.5.4"
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        if (signingPropertiesFile?.isFile == true) {
            create("release") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies { testImplementation("junit:junit:4.13.2") }
