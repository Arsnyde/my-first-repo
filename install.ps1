<#
.SYNOPSIS
    Builds Cobblemon Battle Clarity and installs it into a Minecraft mods folder.

.DESCRIPTION
    Self-contained. Run it from anywhere - if it is not already sitting inside a checkout of the
    repository it downloads the source itself. Git is not required.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\install.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\install.ps1 -ModsDir "D:\SomeOtherInstance\mods"
#>
[CmdletBinding()]
param(
    [string] $ModsDir = "C:\Users\snyde\curseforge\minecraft\Instances\My Cobblemon Mods Test\mods",
    [string] $WorkDir = (Join-Path $env:USERPROFILE "battleclarity-build"),
    [string] $RepoZipUrl = "https://github.com/Arsnyde/my-first-repo/archive/refs/heads/main.zip"
)

$ErrorActionPreference = 'Stop'

function Write-Step { param($m) Write-Host "`n==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m" -ForegroundColor Green }
function Write-Warn { param($m) Write-Host "    $m" -ForegroundColor Yellow }

Write-Host "Cobblemon Battle Clarity - build and install" -ForegroundColor White

# --- 1. Java -------------------------------------------------------------------------------------
Write-Step "Checking Java"
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    throw "Java not found on PATH. Install the Temurin 21 JDK from https://adoptium.net/temurin/releases/?version=21 (tick 'Set JAVA_HOME variable'), open a NEW terminal, and run this again."
}
$verText = (& java -version 2>&1) -join ' '
if ($verText -match '"(\d+)[.\"]') {
    $major = [int]$Matches[1]
    if ($major -eq 21) { Write-Ok "Java $major" }
    else { Write-Warn "Java $major found, not 21. Gradle will try to download a Java 21 toolchain automatically; if the build fails on the toolchain, install Temurin 21." }
} else {
    Write-Warn "Could not parse the Java version. Continuing anyway."
}

# --- 2. Source -----------------------------------------------------------------------------------
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ((Test-Path (Join-Path $scriptDir 'settings.gradle')) -and (Test-Path (Join-Path $scriptDir 'gradlew.bat'))) {
    $projectDir = $scriptDir
    Write-Step "Using the checkout this script lives in"
    Write-Ok $projectDir
} else {
    Write-Step "Downloading the source"
    New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null
    $zip = Join-Path $WorkDir 'battleclarity-src.zip'
    $extract = Join-Path $WorkDir 'src'
    if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }

    $oldProgress = $ProgressPreference
    $ProgressPreference = 'SilentlyContinue'   # the progress bar makes this 10x slower
    try { Invoke-WebRequest -Uri $RepoZipUrl -OutFile $zip -UseBasicParsing }
    finally { $ProgressPreference = $oldProgress }

    Expand-Archive -Path $zip -DestinationPath $extract -Force
    # Clear the mark-of-the-web so Windows does not block gradlew.bat
    Get-ChildItem $extract -Recurse -File | Unblock-File -ErrorAction SilentlyContinue

    $projectDir = (Get-ChildItem $extract -Directory | Select-Object -First 1).FullName
    if (-not $projectDir) { throw "The downloaded archive did not contain a project folder." }
    Write-Ok $projectDir
}

# --- 3. Build ------------------------------------------------------------------------------------
Write-Step "Building (first run downloads ~1-2 GB and can take 5-15 minutes)"
Write-Host "    It will look stuck on 'createMinecraftArtifacts'. It is not - Minecraft is being decompiled." -ForegroundColor DarkGray

Push-Location $projectDir
try {
    & .\gradlew.bat build --console=plain
    $buildExit = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($buildExit -ne 0) {
    Write-Host "`nBUILD FAILED." -ForegroundColor Red
    Write-Host "This mod has not been compiled before, so this is expected to need a fix or two." -ForegroundColor Yellow
    Write-Host "Capture the errors and send them back:" -ForegroundColor Yellow
    Write-Host "    cd `"$projectDir`"" -ForegroundColor White
    Write-Host "    .\gradlew.bat build --console=plain > build-log.txt 2>&1" -ForegroundColor White
    Write-Host "Then send build-log.txt (or just the lines containing 'error:')." -ForegroundColor Yellow
    exit 1
}

$jar = Get-ChildItem (Join-Path $projectDir 'build\libs') -Filter 'battleclarity-*.jar' -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "The build reported success but no jar was found in build\libs." }
Write-Ok "Built $($jar.Name)"

# --- 4. Install ----------------------------------------------------------------------------------
Write-Step "Installing into the mods folder"
if (-not (Test-Path -LiteralPath $ModsDir)) {
    throw "Mods folder not found:`n    $ModsDir`nCheck the path, or pass the right one with -ModsDir '<path>'. The folder is deliberately not created for you, because a typo would silently make a useless one."
}

$old = Get-ChildItem -LiteralPath $ModsDir -Filter 'battleclarity-*.jar' -ErrorAction SilentlyContinue
foreach ($o in $old) {
    Write-Warn "Removing previous build: $($o.Name)"
    Remove-Item -LiteralPath $o.FullName -Force
}

Copy-Item -LiteralPath $jar.FullName -Destination $ModsDir -Force
Write-Ok "Installed to $ModsDir"

# --- 5. Sanity-check the instance ----------------------------------------------------------------
Write-Step "Checking the rest of the instance"
$names = (Get-ChildItem -LiteralPath $ModsDir -Filter '*.jar' | ForEach-Object { $_.Name.ToLower() }) -join ' '

if ($names -match 'cobblemon')                      { Write-Ok   "Cobblemon found" }
else { Write-Warn "Cobblemon NOT found - required. Install the NeoForge 1.21.1 build." }

if ($names -match 'kotlinforforge|kotlin_for_forge|kff') { Write-Ok "Kotlin For Forge found" }
else { Write-Warn "Kotlin For Forge NOT found - required. Cobblemon does not bundle it and will crash on startup without it." }

if ($names -match 'sodium')   { Write-Ok   "Sodium found - supported, the Sodium hook will engage" }
if ($names -match 'embeddium'){ Write-Warn "Embeddium found - NOT supported yet. It forks Sodium under different package names and needs its own hook, so blocks will not hide while it is installed." }

Write-Host "`nDone. Launch the instance and start a battle indoors." -ForegroundColor Green
Write-Host "If nothing happens, search logs\latest.log for 'battleclarity' and 'Mixin apply failed'." -ForegroundColor DarkGray
