plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}


android {

  namespace = "com.sivpn.cepat"

  compileSdk {
    version = release(36) {
      minorApiLevel = 1
    }
  }


  defaultConfig {

    applicationId = "com.sivpn.edfaec"

    minSdk = 24

    targetSdk = 36

    versionCode = 1

    versionName = "1.0"


    testInstrumentationRunner =
      "androidx.test.runner.AndroidJUnitRunner"
  }



  signingConfigs {

    create("release") {

      val keystorePath =
        System.getenv("KEYSTORE_PATH")
          ?: "${rootDir}/my-upload-key.jks"


      storeFile = file(keystorePath)

      storePassword =
        System.getenv("STORE_PASSWORD")

      keyAlias = "upload"

      keyPassword =
        System.getenv("KEY_PASSWORD")
    }



    create("debugConfig") {

      storeFile =
        file("${rootDir}/debug.keystore")

      storePassword = "android"

      keyAlias = "androiddebugkey"

      keyPassword = "android"
    }
  }



  buildTypes {

    release {

      isCrunchPngs = false

      isMinifyEnabled = false

      proguardFiles(
        getDefaultProguardFile(
          "proguard-android-optimize.txt"
        ),
        "proguard-rules.pro"
      )

      signingConfig =
        signingConfigs.getByName("release")
    }



    debug {

      signingConfig =
        signingConfigs.getByName("debugConfig")
    }
  }



  compileOptions {

    sourceCompatibility =
      JavaVersion.VERSION_11

    targetCompatibility =
      JavaVersion.VERSION_11
  }



  buildFeatures {

    compose = true

    buildConfig = true
  }



  splits {

    abi {

      isEnable = true

      reset()

      include(
        "armeabi-v7a",
        "arm64-v8a",
        "x86",
        "x86_64"
      )

      isUniversalApk = true
    }
  }



  testOptions {

    unitTests {

      isIncludeAndroidResources = true
    }
  }
}




// =================================================
// NATIVE BUILD FLAGS
// =================================================

val buildNativeLibssh2 =
  providers.gradleProperty("buildNativeLibssh2")
    .map(String::toBoolean)
    .orElse(false)




// =================================================
// LIBSSH2 + MBEDTLS
// =================================================

val compileLibssh2 by tasks.registering(Exec::class) {

  group = "native build"

  description =
    "Build libssh.so from libssh2 + mbedtls."

  workingDir = rootDir


  commandLine(
    "bash",
    rootProject.file(
      "compile-libssh2.sh"
    ).absolutePath
  )
}




// =================================================
// HEV SOCKS5 TUNNEL
// =================================================

val compileHevTunnel by tasks.registering(Exec::class) {

  group = "native build"

  description =
    "Build libhev-socks5-tunnel.so."

  workingDir = rootDir


  commandLine(
    "bash",
    rootProject.file(
      "compile-hevtun.sh"
    ).absolutePath
  )
}




// =================================================
// ALL JNI LIBRARY BUILDER
// =================================================

val buildNativeJniLibs by tasks.registering {

  group = "build"

  description =
    "Build libssh.so and libhev-socks5-tunnel.so."


  dependsOn(
    compileLibssh2,
    compileHevTunnel
  )
}




tasks.named("preBuild") {

  if (buildNativeLibssh2.get()) {

    dependsOn(
      compileLibssh2,
      compileHevTunnel
    )
  }
}





// =================================================
// SECRETS
// =================================================

secrets {

  propertiesFileName = ".env"

  defaultPropertiesFileName = ".env.example"
}





// =================================================
// DEPENDENCIES
// =================================================

dependencies {


  implementation(platform(libs.androidx.compose.bom))

  implementation(platform(libs.firebase.bom))


  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.compose.material.icons.core)

  implementation(libs.androidx.compose.material.icons.extended)

  implementation(libs.androidx.compose.material3)

  implementation(libs.androidx.compose.ui)

  implementation(libs.androidx.compose.ui.graphics)

  implementation(libs.androidx.compose.ui.tooling.preview)


  implementation(libs.androidx.core.ktx)

  implementation(libs.androidx.lifecycle.runtime.compose)

  implementation(libs.androidx.lifecycle.runtime.ktx)

  implementation(libs.androidx.lifecycle.viewmodel.compose)


  implementation(libs.androidx.room.ktx)

  implementation(libs.androidx.room.runtime)


  implementation(libs.converter.moshi)

  implementation(libs.firebase.ai)

  implementation(libs.kotlinx.coroutines.android)

  implementation(libs.kotlinx.coroutines.core)

  implementation(libs.logging.interceptor)

  implementation(libs.moshi.kotlin)

  implementation(libs.okhttp)

  implementation(libs.retrofit)


  implementation(
    "com.github.topjohnwu.libsu:core:5.2.2"
  )


  implementation(libs.zxing.core)

  implementation(libs.mmkv)



  testImplementation(
    libs.androidx.compose.ui.test.junit4
  )

  testImplementation(libs.androidx.core)

  testImplementation(libs.androidx.junit)

  testImplementation(libs.junit)

  testImplementation(
    libs.kotlinx.coroutines.test
  )

  testImplementation(libs.robolectric)

  testImplementation(libs.roborazzi)

  testImplementation(libs.roborazzi.compose)

  testImplementation(
    libs.roborazzi.junit.rule
  )



  androidTestImplementation(
    platform(libs.androidx.compose.bom)
  )

  androidTestImplementation(
    libs.androidx.compose.ui.test.junit4
  )

  androidTestImplementation(
    libs.androidx.espresso.core
  )

  androidTestImplementation(
    libs.androidx.junit
  )

  androidTestImplementation(
    libs.androidx.runner
  )



  debugImplementation(
    libs.androidx.compose.ui.test.manifest
  )

  debugImplementation(
    libs.androidx.compose.ui.tooling
  )



  "ksp"(
    libs.androidx.room.compiler
  )

  "ksp"(
    libs.moshi.kotlin.codegen
  )
}