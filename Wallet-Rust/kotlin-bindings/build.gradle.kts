// hassam dev: Gradle build configuration for Kotlin UniFFI bindings (Phase 2)

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
}

group = "com.wallet_rust"
version = "0.1.0"

// hassam dev: UniFFI generation task (Phase 2)
tasks.register<Exec>("generateUniFFI") {
    description = "Generate Kotlin bindings from wallet.udl using uniffi-bindgen"
    group = "build"
    
    // Command to run uniffi-bindgen
    commandLine = listOf(
        "uniffi-bindgen",
        "generate",
        "../wallet.udl",
        "--language", "kotlin",
        "--out-dir", "src/main/kotlin/com/wallet_rust"
    )
    
    // Change working directory if needed
    workingDir = projectDir

    // Skip generation if uniffi-bindgen is not available on PATH.
    onlyIf {
        try {
            val command = if (System.getProperty("os.name").startsWith("Windows")) "where" else "which"
            val processBuilder = ProcessBuilder(command, "uniffi-bindgen")
            val process = processBuilder.start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}

// hassam dev: Make sure generateUniFFI runs before compilation (Phase 2)
tasks.named("compileKotlin") {
    dependsOn("generateUniFFI")
}

dependencies {
    // hassam dev: JUnit 5 for testing (Phase 2)
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    
    // hassam dev: Kotlin standard library (Phase 2)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    
    // hassam dev: Moshi for JSON serialization (Phase 4)
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
}

// hassam dev: JUnit 5 test configuration with native library path (Phase 4)
tasks.test {
    useJUnitPlatform()
    
    testLogging {
        events("passed", "skipped", "failed")
    }
    
    // hassam dev: Configure JVM to load wallet_core.dll from Rust build directory (Phase 4)
    val nativeLibPath = "${projectDir.parent}/core-rust/target/release"
    jvmArgs = listOf("-Djava.library.path=$nativeLibPath")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}
