plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.onradar.todak"
    // 최신 Compose·AndroidX·OkHttp가 컴파일 SDK 37을 요구합니다. 실행 기기 범위는 minSdk가 정합니다.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.onradar.todak"
        minSdk = 28 // 시연 폰: Galaxy Note 8 (Android 9)
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Core API 주소. 배포 주소가 정해지면 바꿉니다. (API 명세서: CORE_BASE_URL)
        buildConfigField("String", "CORE_BASE_URL", "\"https://todak-core.onrender.com/api/\"")
        // true면 백엔드 없이 앱 안의 가짜 데이터로 동작합니다 (시연 모드). release는 항상 실서버.
        buildConfigField("boolean", "USE_MOCK", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_MOCK", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
}
