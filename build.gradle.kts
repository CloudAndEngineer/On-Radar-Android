// AGP 9는 Kotlin을 내장하고 있어서 앱 모듈에 kotlin-android 플러그인을 따로 적용하지 않습니다.
// 아래 kotlin 플러그인들은 Compose 컴파일러·직렬화와 같은 Kotlin 버전을 쓰도록 맞추는 용도입니다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
