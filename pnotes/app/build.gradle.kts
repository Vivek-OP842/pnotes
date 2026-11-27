import org.gradle.api.GradleException
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------------
// Release signing
//
// A release must be signed with the same key as every build already published, so the keystore is
// never committed: values come from the gitignored keystore.properties, or from Gradle properties /
// environment variables (which is how CI signs without writing secrets to disk). See
// keystore.properties.example for the format and how to generate the key. With nothing configured a
// release build is signed with the *debug* key: still installable, but it can never be published or
// upgrade-install over a real release.
// ---------------------------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

// File value, else Gradle property, else environment variable. Blank counts as unset, so a
// half-filled `storePassword=` line does not silently win over CI's -P flag.
fun signingValue(key: String, gradleProperty: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: providers.gradleProperty(gradleProperty).orNull?.trim()?.takeIf(String::isNotEmpty)
        ?: providers.environmentVariable(environmentVariable).orNull?.trim()?.takeIf(String::isNotEmpty)

// Absent / "false" / "0" / "off" mean off, so the flag can also be passed mechanically.
fun flagIsOn(gradleProperty: String, environmentVariable: String): Boolean {
    val raw = providers.gradleProperty(gradleProperty).orNull
        ?: providers.environmentVariable(environmentVariable).orNull
        ?: return false
    return raw.trim().lowercase() !in setOf("", "false", "0", "no", "off")
}

val releaseStoreFilePath = signingValue("storeFile", "pnotes.storeFile", "PNOTES_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "pnotes.storePassword", "PNOTES_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "pnotes.keyAlias", "PNOTES_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "pnotes.keyPassword", "PNOTES_KEY_PASSWORD")

val missingSigningValues = listOfNotNull(
    "storeFile".takeIf { releaseStoreFilePath == null },
    "storePassword".takeIf { releaseStorePassword == null },
    "keyAlias".takeIf { releaseKeyAlias == null },
    "keyPassword".takeIf { releaseKeyPassword == null },
)
// storeFile is resolved against the project root (not app/), matching keystore.properties.example.
val releaseStoreFile = releaseStoreFilePath?.let { rootProject.file(it) }
val keystoreFileMissing = releaseStoreFile != null && !releaseStoreFile.exists()

// Why release signing is unavailable, or null when a release build will really be signed.
val releaseSigningProblem: String? = when {
    keystoreFileMissing -> "'${releaseStoreFile?.absolutePath}' does not exist"
    missingSigningValues.isNotEmpty() && keystorePropertiesFile.exists() ->
        "keystore.properties is missing ${missingSigningValues.joinToString(", ")}"
    missingSigningValues.isNotEmpty() ->
        "no keystore.properties, -Ppnotes.* property or PNOTES_* environment variable found"
    else -> null
}
val hasReleaseSigning = releaseSigningProblem == null
val requireReleaseSigning = flagIsOn("pnotes.requireReleaseSigning", "PNOTES_REQUIRE_RELEASE_SIGNING")

if (requireReleaseSigning && !hasReleaseSigning) {
    throw GradleException(
        "Release signing is required (pnotes.requireReleaseSigning) but is not configured: " +
            "$releaseSigningProblem. See keystore.properties.example.",
    )
}

// Google Sign-In and Play Billing identifiers are read from a gitignored auth.properties so no
// account-specific value is ever committed. Absent file => empty client id, which the sign-in
// layer reports as unconfigured rather than silently pretending to succeed.
val authPropertiesFile = rootProject.file("auth.properties")
val authProperties = Properties().apply {
    if (authPropertiesFile.exists()) authPropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.pnotes"
    compileSdk = 36
    // ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.pnotes"
        minSdk = 26
        targetSdk = 36
        versionCode = 50
        versionName = "0.8.13"
        buildConfigField(
            "String",
            "OAUTH_CLIENT_ID",
            "\"${authProperties.getProperty("oauthClientId", "")}\"",
        )
        buildConfigField(
            "String",
            "PREMIUM_PRODUCT_ID",
            "\"${authProperties.getProperty("premiumProductId", "premium_lifetime")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${authProperties.getProperty("supabaseUrl", "")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${authProperties.getProperty("supabaseAnonKey", "")}\"",
        )
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // externalNativeBuild {
    //     cmake {
    //         path = file("src/main/cpp/CMakeLists.txt")
    //         version = "3.22.1"
    //     }
    // }

    packaging {
        jniLibs {
            // The tree-sitter parser tables gzip ~6x: compressed packaging keeps the
            // APK at ~12MB instead of ~30MB, at the cost of extract-on-install.
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Use unified com.pnotes package name so debug and release target the exact same app
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            // R8 shrinks and optimises release builds with full mode optimizations
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable += listOf("PropertyEscape", "OldTargetApi", "AndroidGradlePluginVersion", "GradleDependency", "SuspiciousIndentation", "RestrictedApi")
    }
}

// Say it out loud when a release artifact is actually requested, rather than on every Gradle run.
if (!hasReleaseSigning) {
    val warning = "Release builds are signed with the DEBUG key: $releaseSigningProblem. " +
        "Such an APK cannot be published or upgrade-install over a real release. " +
        "See keystore.properties.example."
    tasks.matching { it.name.contains("Release") && it.name != "printReleaseSigning" }.configureEach {
        doFirst { logger.warn(warning) }
    }
}

// `./gradlew printReleaseSigning` answers "what is this APK actually signed with?" before a
// three-minute release build, and proves the configured credentials really open the keystore.
tasks.register("printReleaseSigning") {
    group = "help"
    description = "Reports which key a release build will be signed with."
    val keystorePath = releaseStoreFile?.absolutePath
    val keyAliasValue = releaseKeyAlias
    val storePasswordValue = releaseStorePassword
    val keyPasswordValue = releaseKeyPassword
    val problem = releaseSigningProblem
    val missing = missingSigningValues.toList()
    val required = requireReleaseSigning

    doLast {
        if (problem != null) {
            println("Release signing: NOT configured -> release builds use the debug key")
            println("  reason: $problem")
            if (missing.isNotEmpty()) println("  missing: ${missing.joinToString(", ")}")
            println("  fix:    copy keystore.properties.example to keystore.properties and fill it in")
            println(if (required) "  note:   release signing is required here, so this build would fail" else "  note:   installable, but unpublishable")
            return@doLast
        }

        println("Release signing: configured")
        println("  storeFile: $keystorePath")
        println("  keyAlias:  $keyAliasValue")
        try {
            // getInstance(File, password) auto-detects JKS vs PKCS12, so a hand-rolled keytool
            // keystore reads the same as one exported from Android Studio.
            val keyStore = KeyStore.getInstance(
                File(keystorePath),
                (storePasswordValue ?: "").toCharArray(),
            )
            val certificate = keyStore.getCertificate(keyAliasValue)
            if (certificate == null) {
                println("  WARNING:  alias '$keyAliasValue' has no certificate in this keystore")
            } else {
                val digest = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
                println("  SHA-256:  ${digest.joinToString(":") { "%02X".format(it) }}")
                val usableKey = keyStore.getKey(keyAliasValue, (keyPasswordValue ?: "").toCharArray()) != null
                println("  key:      ${if (usableKey) "private key usable" else "UNUSABLE"}")
            }
        } catch (e: Exception) {
            println("  WARNING:  could not read the keystore with these values (${e.message}); a release build will fail")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-Xbackend-threads=0",
        )
    }
}

// Baseline profile (merged from the Compose/AndroidX library profiles) is enabled:
// The profile pre-compiles hot framework code at install time for faster launch.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pdfbox.android)
    implementation(libs.androidsvg)
    implementation(libs.google.mlkit.digital.ink)
    implementation(libs.androidx.input.motionprediction)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Supabase & Ktor
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
