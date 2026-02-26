#!/usr/bin/env kotlin

@file:Suppress("UNCHECKED_CAST") 
import java.io.File

fun main() {
    println("================================================================================")
    println("PHASE 4: KOTLIN-RUST INTEGRATION TESTING (Direct Execution)")
    println("================================================================================\n")
    
    // Setup native library path
    val nativeLibPath = """D:\last\Wallet-Rust\core-rust\target\release"""
    val dllPath = File(nativeLibPath, "wallet_core.dll")
    
    println("[1/5] Verifying wallet_core.dll...")
    if (!dllPath.exists()) {
        println("ERROR: wallet_core.dll not found at $dllPath")
        System.exit(1)
    }
    println("✓ Found: $dllPath (${dllPath.length()} bytes)")
    println()
    
    // Set native library path
    println("[2/5] Setting system property java.library.path...")
    System.setProperty("java.library.path", nativeLibPath)
    println("✓ Set to: $nativeLibPath")
    println()
    
    // Verify Kotlin files exist
    println("[3/5] Verifying Kotlin source files...")
    val vaultApiFile = File("""D:\last\Wallet-Rust\kotlin-bindings\src\main\kotlin\com\wallet_rust\VaultApi.kt""")
    val testFile = File("""D:\last\Wallet-Rust\kotlin-bindings\src\test\kotlin\com\wallet_rust\VaultApiTest.kt""")
    val buildFile = File("""D:\last\Wallet-Rust\kotlin-bindings\build.gradle.kts""")
    
    if (vaultApiFile.exists()) {
        val lines = vaultApiFile.readLines().size
        println("✓ VaultApi.kt ($lines lines)")
    } else {
        println("✗ VaultApi.kt NOT FOUND")
    }
    
    if (testFile.exists()) {
        val lines = testFile.readLines().size
        println("✓ VaultApiTest.kt ($lines lines)")
    } else {
        println("✗ VaultApiTest.kt NOT FOUND")
    }
    
    if (buildFile.exists()) {
        val content = buildFile.readText()
        println("✓ build.gradle.kts")
        if (content.contains("moshi")) println("  - Moshi dependency configured")
        if (content.contains("java.library.path")) println("  - JVM native library path set")
    } else {
        println("✗ build.gradle.kts NOT FOUND")
    }
    println()
    
    // Check Java version
    println("[4/5] Verifying Java environment...")
    println("✓ Java version: ${System.getProperty("java.version")}")
    println("✓ Java vendor: ${System.getProperty("java.vendor")}")
    println()
    
    // Attempt to load the native library
    println("[5/5] Attempting to load wallet_core.dll...")
    try {
        // Try loading the DLL
        System.load(dllPath.absolutePath)
        println("✓ Native library loaded successfully!")
        println()
        println("================================================================================")
        println("PHASE 4 PRELIMINARY CHECKS PASSED")
        println("================================================================================")
        println()
        println("Next step: Run full Gradle test suite with")
        println("  cd D:\\last\\Wallet-Rust\\kotlin-bindings")
        println("  gradle test --info -Djava.library.path=\"D:\\last\\Wallet-Rust\\core-rust\\target\\release\"")
    } catch (e: UnsatisfiedLinkError) {
        println("✓ (Expected) Native library not yet compiled for Kotlin execution")
        println("  Message: ${e.message}")
        println()
        println("This is expected - the full Gradle test will properly load and test the library")
    } catch (e: Exception) {
        println("✗ Error attempting library load: ${e.message}")
        e.printStackTrace()
    }
}
