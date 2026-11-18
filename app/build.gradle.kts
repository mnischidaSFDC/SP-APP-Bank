import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun localProp(key: String): String =
    localProps.getProperty(key) ?: error("Missing local.properties key: $key")

android {
    namespace = "com.example.salesforcemn"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.salesforce.nextgen"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SFMC_CDP_APP_ID",             "\"${localProp("SFMC_CDP_APP_ID")}\"")
        buildConfigField("String", "SFMC_CDP_ENDPOINT",           "\"${localProp("SFMC_CDP_ENDPOINT")}\"")
        buildConfigField("String", "SFMC_MAM_MODULE_APP_ID",      "\"${localProp("SFMC_MAM_MODULE_APP_ID")}\"")
        buildConfigField("String", "SFMC_MAM_TENANT_ID",          "\"${localProp("SFMC_MAM_TENANT_ID")}\"")
        buildConfigField("String", "SFMC_MAM_ACCESS_TOKEN",       "\"${localProp("SFMC_MAM_ACCESS_TOKEN")}\"")
        buildConfigField("String", "SFMC_MAM_ENDPOINT_URL",       "\"${localProp("SFMC_MAM_ENDPOINT_URL")}\"")
        buildConfigField("String", "SFMC_PERSONALIZATION_CDN_URL","\"${localProp("SFMC_PERSONALIZATION_CDN_URL")}\"")
        buildConfigField("String", "FCM_SENDER_ID",               "\"${localProp("FCM_SENDER_ID")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.salesforce.personalization.sdk)
    implementation(libs.salesforce.personalization.lowcode)
    implementation(libs.salesforce.mobileappmessaging)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.coil)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
