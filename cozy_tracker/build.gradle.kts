plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.thesurajkamble.cozy_tracker.lib"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

group = (findProperty("GROUP") as String?) ?: "io.github.thesurajkamble"
version = (findProperty("VERSION_NAME") as String?) ?: "0.1.0-SNAPSHOT"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "io.github.thesurajkamble"
                artifactId = "cozy-tracker"
                version = "0.1.0"

                pom {
                    name.set("Cozy Tracker")
                    description.set("A Compose Lazy List view time tracker")
                    inceptionYear.set("2026")
                    url.set("https://github.com/thesurajkamble/cozy-tracker")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("thesurajkamble")
                            name.set("Suraj Kamble")
                            url.set("https://github.com/surajkamble/")
                        }
                    }
                    scm {
                        url.set("https://github.com/thesurajkamble/cozy-tracker")
                        connection.set("scm:git:git://github.com/surajkamble/cozy_tracker.git")
                        developerConnection.set("scm:git:ssh://github.com/thesurajkamble/cozy-tracker.git")
                    }
                }
            }
        }
    }
}