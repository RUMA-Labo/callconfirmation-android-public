plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
}

android {
    namespace = "jp.ruma.freesoft.kotlinlesson.callConfirmation"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "jp.ruma.freesoft.kotlinlesson.callConfirmation"
        minSdk = 29
        targetSdk = 37
        versionCode = 100
        versionName = "1.00"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_package-name" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
            ),
        )
    }
}


tasks.register("checkClassAbsenceAfterR8") {
    group = "verification"
    description = "Check if specific classes or methods exist after R8 shrinking."

    mustRunAfter("minifyReleaseWithR8", "bundleRelease", "assembleRelease")
    val mappingFileProvider = layout.buildDirectory.file("outputs/mapping/release/mapping.txt").map { it.asFile }
    doLast {
        val forbiddenSimpleNames = setOf("SecureLogger")
        val forbiddenMethods = setOf("debugKeepAliveMessageTask")
        val mappingFile = mappingFileProvider.get()
        if (!mappingFile.exists()) {
            logger.warn("Mapping file not found at: ${mappingFile.absolutePath}. Skipping check.")
            return@doLast
        }
        val foundClasses = mutableListOf<String>()
        val foundMethods = mutableListOf<String>()
        var insideTargetClass = false

        mappingFile.forEachLine { line ->
            if (line.endsWith(":")) {
                val originalFullClassName = line.substringBefore(" ->").trim()
                val simpleName = originalFullClassName.substringAfterLast('.')
                if (forbiddenSimpleNames.contains(simpleName)) {
                    foundClasses.add(originalFullClassName)
                }
                insideTargetClass = originalFullClassName.endsWith("CallConfirmKeepAliveService")
            } else if (insideTargetClass) {
                for (method in forbiddenMethods) {
                    if (line.contains(method)) {
                        foundMethods.add("$method in CallConfirmKeepAliveService")
                    }
                }
            }
        }

        val errors = mutableListOf<String>()
        if (foundClasses.isNotEmpty()) {
            errors.add("Prohibited classes: $foundClasses")
        }
        if (foundMethods.isNotEmpty()) {
            errors.add("Prohibited methods: $foundMethods")
        }

        if (errors.isNotEmpty()) {
            throw GradleException("【ERROR】After ProGuard/R8 processing, prohibited items were detected: ${errors.joinToString(", ")}")
        } else {
            println("【success】No prohibited classes or methods were detected.")
        }
    }
}

// bundleReleaseタスクの実行時に自動でチェックを走らせる場合
tasks.configureEach {
    // APK用の assembleRelease または AAB用の bundleRelease を対象にする
    if (name == "assembleRelease" || name == "bundleRelease") {
        finalizedBy("checkClassAbsenceAfterR8")
    }
}


dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}