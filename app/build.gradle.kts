// 1. Блок плагинов должен быть САМЫМ ПЕРВЫМ
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Если у тебя в TOML есть kotlin-android, раскомментируй строку ниже:
    // alias(libs.plugins.kotlin.android)
}

// 2. Блок android идет отдельно, после плагинов
android {
    namespace = "com.cmf.anicamerax"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cmf.anicamerax"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
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
    }
}

// 3. Настройка компилятора (вынесена из блока android)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.camera.core.ExperimentalZeroShutterLag",
            "-opt-in=androidx.camera.camera2.interop.ExperimentalCamera2Interop"
        )
    }
}

// 4. Зависимости
dependencies {
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Ядро и Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // COMPOSE (Ошибки 'ui' и 'material3' были здесь)
    implementation(platform(libs.androidx.compose.bom))

    // ПРАВИЛЬНО: используем полный путь androidx.ui, так как в TOML это androidx-ui
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.ui.graphics)

    // ПРАВИЛЬНО: используем androidx.material3, так как в TOML это androidx-material3
    implementation(libs.androidx.material3)
    // Базовый набор иконок
    implementation(libs.androidx.compose.material.icons.core)
    // РАСШИРЕННЫЙ набор (FlashOn/FlashOff часто находятся именно здесь)
    implementation(libs.androidx.compose.material.icons.extended)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Тестирование
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}