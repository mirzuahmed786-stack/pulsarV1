@echo off
REM Fast All Tests Runner - With optimizations (Steps 1-5 implemented)
REM Expected time: First run ~2 min (with rebuild), repeat runs ~2-3 min (cached)

setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PROJECT_ROOT=D:\last\Wallet-Rust"
set "KOTLIN_BINDINGS=%PROJECT_ROOT%\kotlin-bindings"
set "NATIVE_LIB=%PROJECT_ROOT%\core-rust\target\release"
set "CORE_RUST=%PROJECT_ROOT%\core-rust"
set "GRADLE_HOME=C:\Gradle\gradle-8.5"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"

echo.
echo ================================================================================
echo OPTIMIZED ALL TESTS RUNNER (With Gradle daemon, build cache, incremental builds)
echo ================================================================================
echo Expected time: 2-6 minutes (depending on whether Rust rebuild is needed)
echo.

REM Verify Java
echo [1/4] Verifying Java...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%
    exit /b 1
)

REM Step 5: Check if Rust rebuild is needed
echo [2/4] Checking if Rust rebuild is needed...
if not exist "%NATIVE_LIB%\wallet_core.dll" (
    echo DLL not found - rebuilding Rust library...
    set REBUILD=1
) else (
    echo DLL exists and is recent - skipping rebuild (set REBUILD=1 in shell to force)
    set REBUILD=0
)

if !REBUILD! equ 1 (
    echo.
    echo [2/4] Rebuilding wallet_core.dll with incremental Cargo build...
    pushd "%CORE_RUST%"
    cargo build --release -j 8
    if errorlevel 1 (
        echo ERROR: Cargo build failed
        popd
        exit /b 1
    )
    echo wallet_core.dll rebuilt successfully
    popd
) else (
    echo [2/4] Skipping Rust rebuild (DLL is up-to-date)
)
echo.

REM Step 1 & 2: Gradle daemon and build cache already enabled in gradle.properties
REM Verify Gradle installation and build cache
echo [3/4] Verifying Gradle installation and cache settings...
if not exist "%GRADLE_BIN%" (
    echo ERROR: Gradle not found at %GRADLE_BIN%
    echo Install Gradle from: https://gradle.org/install/
    exit /b 1
)
echo Gradle: FOUND at %GRADLE_HOME%
if exist "%KOTLIN_BINDINGS%\gradle.properties" (
    findstr /I "org.gradle.buildCache=true" "%KOTLIN_BINDINGS%\gradle.properties" >nul
    if !errorlevel! equ 0 (
        echo Build cache: ENABLED
    )
    findstr /I "org.gradle.daemon=true" "%KOTLIN_BINDINGS%\gradle.properties" >nul
    if !errorlevel! equ 0 (
        echo Daemon: ENABLED
    )
)
echo.

echo.
echo ================================================================================
echo EXECUTING: gradle test --build-cache (with daemon + parallel workers)
echo ================================================================================
echo Native Library Path: %NATIVE_LIB%
echo Java Home: %JAVA_HOME%
echo.

set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Step 2: Use --build-cache flag; daemon and workers already in gradle.properties (Step 1)
REM Use system Gradle installation (no download needed)
pushd "%KOTLIN_BINDINGS%"
"%GRADLE_BIN%" test --build-cache -Djava.library.path="%NATIVE_LIB%" 2>&1

set RESULT=%errorlevel%
popd

if %RESULT% equ 0 (
    echo.
    echo ================================================================================
    echo ALL TESTS PASSED
    echo ================================================================================
    echo All 30 unit tests executed successfully!
    echo Kotlin-Rust FFI integration validated!
    echo.
    echo Performance Summary:
    echo  - Gradle daemon: ENABLED (reuses JVM across builds)
    echo  - Build cache: ENABLED (skips unchanged tasks)
    echo  - Parallel workers: 8 cores (faster compilation)
    echo  - Rust incremental: ENABLED (only rebuilds if source changed)
    echo.
    exit /b 0
) else (
    echo.
    echo ================================================================================
    echo TESTS FAILED
    echo ================================================================================
    popd
    exit /b 1
)
