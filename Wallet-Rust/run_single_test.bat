@echo off
REM Fast Single Test Runner - For quick iteration (Optimization Step 1 & 2)
REM Usage: run_single_test.bat testCreateVault
REM Or: run_single_test.bat (runs first test by default)

setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PROJECT_ROOT=D:\last\Wallet-Rust"
set "KOTLIN_BINDINGS=%PROJECT_ROOT%\kotlin-bindings"
set "NATIVE_LIB=%PROJECT_ROOT%\core-rust\target\release"
set "GRADLE_HOME=C:\Gradle\gradle-8.5"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"

REM Test name from command line argument (default: testCreateVault)
if "%~1"=="" (
    set "TEST_NAME=testCreateVault"
) else (
    set "TEST_NAME=%~1"
)

echo.
echo ================================================================================
echo FAST SINGLE TEST RUNNER (Optimized - 30-45 seconds)
echo ================================================================================
echo Test: !TEST_NAME!
echo.

REM Verify Java
echo [1/3] Verifying Java...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%
    exit /b 1
)

REM Verify DLL  
echo [2/3] Verifying wallet_core.dll...
if not exist "%NATIVE_LIB%\wallet_core.dll" (
    echo ERROR: wallet_core.dll not found at %NATIVE_LIB%
    echo Run: cargo build --release
    exit /b 1
)
echo Found: wallet_core.dll (using existing, no rebuild)
echo.

REM Run SINGLE test with build cache (system Gradle - no download needed)
echo [3/3] Running single test with build cache...
echo.
pushd "%KOTLIN_BINDINGS%"

REM Verify Gradle exists
if not exist "%GRADLE_BIN%" (
    echo ERROR: Gradle not found at %GRADLE_BIN%
    echo Install Gradle: https://gradle.org/install/
    popd
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Run single test (Step 2: --build-cache, Step 3: no --info for speed)
"%GRADLE_BIN%" test --tests "com.wallet_rust.VaultApiTest.!TEST_NAME!" --build-cache -Djava.library.path="%NATIVE_LIB%" 2>&1

set RESULT=%errorlevel%
popd

if %RESULT% equ 0 (
    echo.
    echo ================================================================================
    echo TEST PASSED: !TEST_NAME!
    echo ================================================================================
    echo Elapsed time: ~30-45 seconds (vs 5 minutes for all tests)
    echo.
    exit /b 0
) else (
    echo.
    echo ================================================================================
    echo TEST FAILED: !TEST_NAME!
    echo ================================================================================
    exit /b 1
)
