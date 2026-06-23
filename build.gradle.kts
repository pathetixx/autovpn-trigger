// Корневой build-файл. Плагины объявлены, но не применяются здесь (apply false) —
// версии тянутся из gradle/libs.versions.toml, применяются в app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
