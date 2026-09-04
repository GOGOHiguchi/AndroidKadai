plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false // ✨ この行を追加！
}