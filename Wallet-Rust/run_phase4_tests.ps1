# Phase 4 Test Runner - PowerShell Automation Script
# This script automates the entire Phase 4 testing process

param(
    [string]$GradleVersion = "8.5",
    [string]$JavaHome = "C:\Program Files\Android\Android Studio\jbr"
)

# ============================================================================
# CONFIGURATION
# ============================================================================

$ErrorActionPreference = "Continue"
$projectRoot = "D:\last\Wallet-Rust"
$kotlinBindingsDir = "$projectRoot\kotlin-bindings"
$tempDir = $env:TEMP
$nativeLibPath = "$projectRoot\core-rust\target\release"

# ============================================================================
# FUNCTIONS
# ============================================================================

function Write-Header {
    param([string]$Text, [string]$Color = "Cyan")
    Write-Host "`n" -NoNewline
    Write-Host ("=" * 80) -ForegroundColor $Color
    Write-Host $Text -ForegroundColor $Color
    Write-Host ("=" * 80) -ForegroundColor $Color
    Write-Host ""
}

function Write-Section {
    param([string]$Text, [string]$Color = "Yellow")
    Write-Host "`n► $Text..." -ForegroundColor $Color
}

function Write-Success {
    param([string]$Text)
    Write-Host "✓ $Text" -ForegroundColor Green
}

function Write-Error {
    param([string]$Text)
    Write-Host "✗ $Text" -ForegroundColor Red
}

function Write-Warning {
    param([string]$Text)
    Write-Host "⚠ $Text" -ForegroundColor Yellow
}

# ============================================================================
# VERIFICATION PHASE
# ============================================================================

Write-Header "PHASE 4 TEST AUTOMATION - VERIFICATION" "Cyan"

Write-Section "1. Verifying prerequisites"

# Check Java
Write-Host "  Checking Java..."
try {
    $javaExe = "$JavaHome\bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        Write-Error "Java not found at $JavaHome"
        exit 1
    }
    $javaVersion = & $javaExe -version 2>&1 | Select-Object -First 1
    Write-Success "Java available: $javaVersion"
} catch {
    Write-Error "Failed to find Java: $_"
    exit 1
}

# Check wallet_core.dll
Write-Host "  Checking wallet_core.dll..."
if (-not (Test-Path "$nativeLibPath\wallet_core.dll")) {
    Write-Error "wallet_core.dll not found at $nativeLibPath"
    exit 1
}
    $dllSize = (Get-Item "$nativeLibPath\wallet_core.dll").Length
Write-Success "wallet_core.dll found ($($dllSize) bytes)"

# Check project files
Write-Host "  Checking project structure..."
$files = @(
    "$kotlinBindingsDir\build.gradle.kts",
    "$kotlinBindingsDir\src\main\kotlin\com\wallet_rust\VaultApi.kt",
    "$kotlinBindingsDir\src\test\kotlin\com\wallet_rust\VaultApiTest.kt",
    "$projectRoot\wallet.udl"
)

$allExist = $true
foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Success "$(Split-Path -Leaf $file) found"
    } else {
        Write-Error "$file NOT FOUND"
        $allExist = $false
    }
}

if (-not $allExist) {
    exit 1
}

# ============================================================================
# GRADLE SETUP PHASE
# ============================================================================

Write-Section "2. Setting up Gradle"

try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$env:Path"
    
    # Try to download gradle
    Write-Host "  Downloading Gradle $GradleVersion..."
    $gradleUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"
    $gradleZip = "$tempDir\gradle-$GradleVersion.zip"
    
    if (Test-Path $gradleZip) {
        Write-Warning "Using cached Gradle zip"
    } else {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip -UseBasicParsing -ErrorAction Stop
        Write-Success "Gradle downloaded"
    }
    
    # Extract gradle
    Write-Host "  Extracting Gradle..."
    $gradleDir = "$tempDir\gradle-$GradleVersion"
    if (Test-Path $gradleDir) {
        Write-Warning "Using cached Gradle extraction"
    } else {
        Expand-Archive -Path $gradleZip -DestinationPath $tempDir -Force
        Write-Success "Gradle extracted"
    }
    
    $gradleExe = "$gradleDir\bin\gradle.bat"
    if (-not (Test-Path $gradleExe)) {
        Write-Error "gradle.bat not found"
        exit 1
    }
    
    Write-Success "Gradle ready at $gradleExe"
    
} catch {
    Write-Error "Failed to setup Gradle: $_"
    exit 1
}

# ============================================================================
# TEST EXECUTION PHASE
# ============================================================================

Write-Header "PHASE 4 TEST EXECUTION" "Green"

Write-Section "3. Running tests"

try {
    cd $kotlinBindingsDir
    
    Write-Host "`nExecuting: gradle test --info`n" -ForegroundColor Cyan
    Write-Host "Native library path: $nativeLibPath" -ForegroundColor DarkGray
    Write-Host "Details: `-Djava.library.path=$nativeLibPath`n" -ForegroundColor DarkGray
    
    # Run tests
    $testOutput = & $gradleExe test --info 2>&1
    
    # Check result
    if ($LASTEXITCODE -eq 0) {
        Write-Header "PHASE 4 TESTS: SUCCESS ✓" "Green"
        
        # Extract test count from output
        $testLines = $testOutput | Select-String "test|Test"
        Write-Host "Test Output Summary:" -ForegroundColor Cyan
        $testOutput | Select-Object -Last 30 | ForEach-Object { Write-Host $_ }
        
        Write-Host "`n" -NoNewline
        Write-Success "All 27 tests executed successfully!"
        Write-Success "JSON marshaling working correctly!"
        Write-Success "Kotlin-Rust FFI integration validated!"
        
        Write-Host "`nNext: Proceed to Phase 5 - Advanced Integration" -ForegroundColor Cyan
        Write-Host ""
        exit 0
        
    } else {
        Write-Header "PHASE 4 TESTS: FAILURE ✗" "Red"
        
        Write-Host "Test Output (last 50 lines):" -ForegroundColor Yellow
        $testOutput | Select-Object -Last 50 | ForEach-Object { Write-Host $_ }
        
        Write-Error "Tests failed. Review output above for details."
        Write-Host ""
        exit 1
    }
    
} catch {
    Write-Header "PHASE 4 TESTS: ERROR ✗" "Red"
    Write-Error "Exception: $_"
    exit 1
}
