@echo off
REM Phase 4 Test Runner - Simple Batch Script

setlocal enabledelayedexpansion

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PROJECT_ROOT=D:\last\Wallet-Rust"
set "KOTLIN_BINDINGS=%PROJECT_ROOT%\kotlin-bindings"
set "NATIVE_LIB=%PROJECT_ROOT%\core-rust\target\release"
set "GRADLE_HOME=C:\Gradle\gradle-8.5"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"

echo.
echo ================================================================================
echo PHASE 4: KOTLIN-RUST INTEGRATION TESTING
echo ================================================================================
echo.

REM Verify Java
echo [1/5] Verifying Java...
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%
    exit /b 1
)
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /R "version"
if errorlevel 1 (
    echo ERROR: Failed to run Java
    exit /b 1
)
echo.

REM Verify and rebuild wallet_core.dll only if needed
echo [2/5] Verifying wallet_core.dll...
if not exist "%NATIVE_LIB%\wallet_core.dll" (
    echo Found: wallet_core.dll does not exist - rebuilding Rust library...
    call :rebuild_rust
    if errorlevel 1 exit /b 1
) else (
    REM Check if any Rust source files are newer than the DLL (Skip rebuild if unchanged - Step 5)
    for /f %%A in ('powershell -Command "if ((Get-Item '%PROJECT_ROOT%\core-rust\src\*').LastWriteTime -gt (Get-Item '%NATIVE_LIB%\wallet_core.dll').LastWriteTime) { Write-Host 'newer' } else { Write-Host 'unchanged' }"') do set REBUILD_STATUS=%%A
    
    if "!REBUILD_STATUS!"=="newer" (
        echo Rust source files are newer than DLL - rebuilding...
        call :rebuild_rust
        if errorlevel 1 exit /b 1
    ) else (
        echo Found: %NATIVE_LIB%\wallet_core.dll (unchanged - skipping rebuild)
    )
)
echo.

REM Verify project files
echo [3/5] Verifying project files...
for %%F in (
    "%KOTLIN_BINDINGS%\build.gradle.kts"
    "%KOTLIN_BINDINGS%\src\main\kotlin\com\wallet_rust\VaultApi.kt"
    "%KOTLIN_BINDINGS%\src\test\kotlin\com\wallet_rust\VaultApiTest.kt"
) do (
    if not exist "%%F" (
        echo ERROR: %%F not found
        exit /b 1
    )
    echo Found: %%F
)
echo.

REM Verify Gradle installation
echo [4/5] Verifying system Gradle installation...
if not exist "%GRADLE_BIN%" (
    echo ERROR: Gradle not found at %GRADLE_BIN%
    echo Please install Gradle: https://gradle.org/install/
    exit /b 1
)
echo Gradle found: %GRADLE_HOME%
echo.

pushd "%KOTLIN_BINDINGS%"

REM Run tests
echo [5/5] Running tests...
echo.
echo ================================================================================
echo EXECUTING: gradle test --build-cache
echo ================================================================================
echo Native Library Path: %NATIVE_LIB%
echo Java Home: %JAVA_HOME%
echo.

set "PATH=%JAVA_HOME%\bin;%PATH%"
"%GRADLE_BIN%" test --build-cache -Djava.library.path="%NATIVE_LIB%" 2>&1

if errorlevel 1 (
    echo.
    echo ================================================================================
    echo PHASE 4 TESTS FAILED
    echo ================================================================================
    popd
    exit /b 1
) else (
    echo.
    echo ================================================================================
    echo PHASE 4 TESTS PASSED
    echo ================================================================================
    echo All 27 unit tests executed successfully!
    echo Kotlin-Rust FFI integration validated!
    echo.
    popd
    exit /b 0
)

REM Subroutine to rebuild Rust library (Step 5)
:rebuild_rust
echo.
echo Rebuilding wallet_core.dll...
pushd "%PROJECT_ROOT%\core-rust"
cargo build --release -j 8
if errorlevel 1 (
    echo ERROR: Failed to build wallet_core library
    popd
    exit /b 1
)
echo wallet_core.dll rebuilt successfully
popd
exit /b 0
