plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose") version "1.7.3"
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":innertube"))
    implementation(project(":spotify"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Coil 3 for multiplatform image loading
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Desktop Audio Engine
    implementation("uk.co.caprica:vlcj:4.8.3")
}

compose.desktop {
    application {
        mainClass = "com.meld.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Meld"
            packageVersion = "0.9.2"
            description = "Meld Desktop Music Player"
            copyright = "© 2026 Meld Contributors"
            vendor = "Meld"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            windows {
                packageVersion = "1.0.2"
                menu = true
                shortcut = true
                upgradeUuid = "3B410109-D19A-4E67-83E1-513417A70E84"
                // iconFile.set(project.file("icons/meld.ico"))
            }
        }
    }
}
