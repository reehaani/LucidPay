// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties
import java.io.FileInputStream

plugins {
  id("com.android.application") // This should already be here
  id("org.jetbrains.kotlin.android") // This should already be here
  id("com.google.devtools.ksp") // <<< ADD THIS LINE
}

android {
  namespace = "com.lucid.autoalerts"
  compileSdk = 34 // Changed from 35

  defaultConfig {
    applicationId = "com.lucid.autoalerts"
    minSdk = 24
    targetSdk = 34 // Changed from 35
    versionCode = 105
    versionName = "1.0.3"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }

    // Supabase credentials (obfuscated via BuildConfig)
    buildConfigField("String", "SUPABASE_URL", "\"https://smtwoguflqphdjmehwix.supabase.co\"")
    buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNtdHdvZ3VmbHFwaGRqbWVod2l4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUzNjUyMDgsImV4cCI6MjA4MDk0MTIwOH0.Qf5vgG1zGZgoQzHfzvQUAnbFUNIq_vAC37uPvCCj0ug\"")
    // Release signing certificate SHA-256 (used for anti-tamper). Keep in sync with your release keystore.
    buildConfigField("String", "EXPECTED_SIGNER_CERT_SHA256", "\"a87341ca40291427e4b6a2be9148dba8807b503308c0393c5ac97366e787f9d6\"")
    // Fixed six-digit app code per release (update when version bumps)
    buildConfigField("String", "APP_CODE", "\"482917\"")

    // Streamlabs OAuth app client id (public). Must match the Streamlabs app used by the Edge Function secrets.
    buildConfigField("String", "STREAMLABS_OAUTH_CLIENT_ID", "\"a09cfd0b-8a96-4eb0-80b5-afd04066c888\"")
  }

  // Signing configuration for release builds
  signingConfigs {
    create("release") {
      // IMPORTANT: For production release, create a keystore file:
      // keytool -genkeypair -v -keystore lucidpay-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lucidpay
      // Store keystore file in project root (DO NOT commit to version control)
      // Set environment variables: KEYSTORE_PASSWORD and KEY_PASSWORD

      val keystorePropertiesFile = rootProject.file("keystore.properties")
      if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

        storeFile = file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      } else {
        // Fallback: Use environment variables
        val keystorePath = System.getenv("LUCIDPAY_KEYSTORE_PATH") ?: "../lucidpay-release.jks"
        val keystoreFile = file(keystorePath)

        if (keystoreFile.exists()) {
          storeFile = keystoreFile
          storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
          keyAlias = System.getenv("KEY_ALIAS") ?: "lucidpay"
          keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        } else {
          // WARNING: No release keystore found - will fall back to debug signing
          // This triggers Google Play Protect warnings!
          // Create keystore using instructions above
        }
      }
    }
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      // Keep debug builds fast and debuggable
      isDebuggable = true
    }

    release {
      isMinifyEnabled = true
      isShrinkResources = true
      isDebuggable = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

      // Use release signing if keystore exists, otherwise fall back to debug
      val keystorePropertiesFile = rootProject.file("keystore.properties")
      val keystoreEnvPath = System.getenv("LUCIDPAY_KEYSTORE_PATH") ?: "../lucidpay-release.jks"
      val hasKeystore = keystorePropertiesFile.exists() || file(keystoreEnvPath).exists()
      if (!hasKeystore) {
        throw GradleException(
          "Release signing configuration is missing. Provide keystore.properties or set LUCIDPAY_KEYSTORE_PATH before building a release."
        )
      }
      signingConfig = signingConfigs.getByName("release")

      // Optimize for maximum size reduction
      ndk {
        debugSymbolLevel = "NONE"
      }
    }

    // Some devices/ROMs + Android Studio installs can fail with INSTALL_BASELINE_PROFILE_FAILED when
    // the APK contains baseline profile assets. This build type produces a signed release APK
    // without embedded baseline profiles (safe for sideload distribution).
    create("releaseNoProfile") {
      initWith(getByName("release"))
      matchingFallbacks += listOf("release")
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true // Explicitly enable buildConfig
  }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = "11" }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

androidComponents {
  onVariants(selector().withBuildType("releaseNoProfile")) { variant ->
    // Exclude baseline profile assets from the APK (they live under assets/dexopt/)
    // Note: patterns are APK-path based and typically start with "/".
    variant.packaging.resources.excludes.add("/assets/dexopt/**")
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-core:1.6.8") // Using core icons to save size
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
  implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
  implementation("androidx.core:core-ktx:1.13.1") // Added this line
  implementation("androidx.core:core-splashscreen:1.0.1") // Added Splash Screen Library

  // Room Dependencies - ADD THESE LINES
  val room_version = "2.6.1" // Use the latest stable version
  implementation("androidx.room:room-runtime:$room_version")
  implementation("androidx.room:room-ktx:$room_version")
  ksp("androidx.room:room-compiler:$room_version")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.appcompat:appcompat:1.7.0") // For AlertDialog

  // WorkManager Dependencies
  val work_version = "2.9.0"
  implementation("androidx.work:work-runtime-ktx:$work_version")

  // Security library for EncryptedSharedPreferences
  implementation("androidx.security:security-crypto:1.1.0-alpha06")

  // Google Sign-In (for Phase 1.5 - Trial abuse prevention)
  implementation("com.google.android.gms:play-services-auth:20.7.0")

  // Coil for image loading (for profile pictures)
  implementation("io.coil-kt:coil-compose:2.5.0")

  // ════════════════════════════════════════════════════════════════════════════════
  // LICENSE SYSTEM - Supabase Dependencies (Optional - for future use)
  // ════════════════════════════════════════════════════════════════════════════════
  // NOTE: Currently using OkHttp directly for Supabase API calls (already included above)
  // Uncomment below if you want to use the official Supabase Kotlin client later:
  //
  // val supabase_version = "2.0.0"
  // implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabase_version")
  // implementation("io.github.jan-tennert.supabase:realtime-kt:$supabase_version")
  // implementation("io.ktor:ktor-client-android:2.3.5")
  //
  // For now, the licensing system works with OkHttp (no additional dependencies needed)
  // ════════════════════════════════════════════════════════════════════════════════

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
