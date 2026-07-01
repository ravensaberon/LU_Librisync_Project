param(
    [string]$DbUrl = "jdbc:mysql://127.0.0.1:3306/latte_and_letters?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true",
    [string]$DbUsername = "root",
    [string]$DbPassword,
    [switch]$SkipPasswordPrompt
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenCandidates = @(
    (Join-Path $projectRoot "tools\apache-maven-3.9.15\bin\mvn.cmd"),
    (Join-Path $projectRoot "tools\apache-maven-3.9.14\bin\mvn.cmd"),
    "C:\Users\labar\Downloads\apache-maven-3.9.9\bin\mvn.cmd"
)
$systemMaven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($systemMaven) {
    $mavenCandidates += $systemMaven.Source
}
$mavenCmd = $mavenCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
$mysqlCli = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$projectLocalMavenRepo = Join-Path $projectRoot ".m2\repository"
$userHomeMavenRepo = Join-Path $env:USERPROFILE ".m2\repository"
$fallbackJavaHome = Join-Path $env:USERPROFILE ".jdks\temurin-17"
$localEnvFile = Join-Path $PSScriptRoot "local-dev-env.env"

if (-not $mavenCmd) {
    throw "Maven was not found in the project tools folder, Downloads fallback, or system PATH."
}

function Test-PathHasReparsePoint {
    param(
        [string]$PathToCheck
    )

    if (-not (Test-Path $PathToCheck)) {
        return $false
    }

    $currentPath = Resolve-Path $PathToCheck
    while ($currentPath) {
        $item = Get-Item -LiteralPath $currentPath -ErrorAction SilentlyContinue
        if ($item -and ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            return $true
        }

        $parent = Split-Path -Parent $currentPath
        if (-not $parent -or $parent -eq $currentPath) {
            break
        }

        $currentPath = $parent
    }

    return $false
}

$mavenRepo = $projectLocalMavenRepo
if (Test-PathHasReparsePoint $projectLocalMavenRepo) {
    $mavenRepo = $userHomeMavenRepo
}

New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null

if (Test-Path (Join-Path $fallbackJavaHome "bin\java.exe")) {
    $env:JAVA_HOME = $fallbackJavaHome
    if (-not (($env:Path -split ';') -contains (Join-Path $fallbackJavaHome "bin"))) {
        $env:Path = (Join-Path $fallbackJavaHome "bin") + ";" + $env:Path
    }
}

if (Test-Path $localEnvFile) {
    Get-Content $localEnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $separatorIndex = $line.IndexOf("=")
        if ($separatorIndex -lt 1) {
            return
        }

        $name = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1).Trim()
        Set-Item -Path ("Env:" + $name) -Value $value
    }
}

if (-not $PSBoundParameters.ContainsKey("DbPassword") -and -not $SkipPasswordPrompt) {
    $securePassword = Read-Host "Enter MySQL password for user '$DbUsername' (press Enter if none)" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $DbPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

if (-not $PSBoundParameters.ContainsKey("DbPassword") -and $SkipPasswordPrompt -and $env:LATTE_AND_LETTERS_DB_PASSWORD) {
    $DbPassword = $env:LATTE_AND_LETTERS_DB_PASSWORD
}

$env:LATTE_AND_LETTERS_DB_URL = $DbUrl
$env:LATTE_AND_LETTERS_DB_USERNAME = $DbUsername
$env:LATTE_AND_LETTERS_DB_PASSWORD = $DbPassword

Write-Host ""
Write-Host "Latte and Letters local run configuration" -ForegroundColor Cyan
Write-Host "DB URL     : $DbUrl"
Write-Host "DB Username: $DbUsername"
Write-Host "Maven Repo : $mavenRepo"

if (Test-Path $mysqlCli) {
    Write-Host "Checking MySQL access..." -ForegroundColor DarkCyan
    $mysqlArgs = @("-u", $DbUsername, "-e", "USE latte_and_letters; SELECT 'Database connection OK' AS status;")
    if (-not [string]::IsNullOrEmpty($DbPassword)) {
        $mysqlArgs = @("-u", $DbUsername, "-p$DbPassword", "-e", "USE latte_and_letters; SELECT 'Database connection OK' AS status;")
    }

    & $mysqlCli @mysqlArgs 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "MySQL connection test failed. The app may still start, but please verify your username/password and that the 'latte_and_letters' database exists."
    }
}

Push-Location $projectRoot
try {
    & $mavenCmd "-o" "-Dmaven.repo.local=$mavenRepo" spring-boot:run
} finally {
    Pop-Location
}
