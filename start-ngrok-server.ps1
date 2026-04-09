param(
    [int]$Port = 8080,
    [string]$FrontendBaseUrl = "",
    [string]$EnvFile = ".env",
    [switch]$UseJar,
    [string]$JarPath = ""
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "[ngrok-deploy] $Message" -ForegroundColor Cyan
}

function Import-EnvFile {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw ".env file not found: $Path"
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf("=")
        if ($separatorIndex -lt 1) {
            continue
        }

        $name = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1)
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Get-EnvValue {
    param(
        [string]$Name,
        [string]$Default = ""
    )

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }

    return $value
}

function Merge-OriginPatterns {
    param(
        [string]$Existing,
        [string[]]$Additional
    )

    $items = @()

    if (-not [string]::IsNullOrWhiteSpace($Existing)) {
        $items += ($Existing -split ",")
    }

    if ($Additional) {
        $items += $Additional
    }

    return ($items |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique) -join ","
}

function Wait-ForNgrokUrl {
    param(
        [int]$Retries = 20,
        [int]$DelaySeconds = 1
    )

    for ($attempt = 1; $attempt -le $Retries; $attempt++) {
        try {
            $response = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -Method Get
            $httpsTunnel = $response.tunnels | Where-Object { $_.proto -eq "https" } | Select-Object -First 1

            if ($httpsTunnel.public_url) {
                return $httpsTunnel.public_url
            }
        }
        catch {
        }

        Start-Sleep -Seconds $DelaySeconds
    }

    throw "Failed to get ngrok public URL. Check whether ngrok is installed and logged in."
}

Write-Step "Checking required commands"

if (-not (Get-Command ngrok -ErrorAction SilentlyContinue)) {
    throw "ngrok command not found. Install ngrok first: https://ngrok.com/download"
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "java command not found. Install JDK 17 or higher first."
}

Import-EnvFile -Path $EnvFile

Write-Step "Starting ngrok tunnel on port $Port"
$ngrokProcess = Start-Process -FilePath "ngrok" -ArgumentList @("http", "$Port") -PassThru

try {
    $publicUrl = Wait-ForNgrokUrl
    $backendBaseUrl = $publicUrl.TrimEnd("/")
    $frontendUrl = if ([string]::IsNullOrWhiteSpace($FrontendBaseUrl)) { $backendBaseUrl } else { $FrontendBaseUrl.TrimEnd("/") }

    $existingOrigins = Get-EnvValue -Name "CORS_ALLOWED_ORIGIN_PATTERNS" -Default "http://localhost:*,http://127.0.0.1:*"
    $mergedOrigins = Merge-OriginPatterns -Existing $existingOrigins -Additional @($backendBaseUrl, $frontendUrl)

    [Environment]::SetEnvironmentVariable("CORS_ALLOWED_ORIGIN_PATTERNS", $mergedOrigins, "Process")
    [Environment]::SetEnvironmentVariable("GITHUB_CALLBACK_URL", "$backendBaseUrl/api/auth/github/callback", "Process")
    [Environment]::SetEnvironmentVariable("FRONTEND_BASE_URL", $frontendUrl, "Process")
    [Environment]::SetEnvironmentVariable("COOKIE_SECURE", "true", "Process")

    Write-Step "ngrok public URL detected"
    Write-Host "Backend URL         : $backendBaseUrl" -ForegroundColor Green
    Write-Host "GitHub Callback URL : $backendBaseUrl/api/auth/github/callback" -ForegroundColor Green
    Write-Host "Frontend Base URL   : $frontendUrl" -ForegroundColor Green
    Write-Host "CORS Origins        : $mergedOrigins" -ForegroundColor Green
    Write-Host ""
    Write-Host "Update your GitHub OAuth callback URL if needed before testing login." -ForegroundColor Yellow

    if ($UseJar) {
        if ([string]::IsNullOrWhiteSpace($JarPath)) {
            $latestJar = Get-ChildItem -Path "build\libs\*.jar" -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1

            if (-not $latestJar) {
                throw "No JAR found in build\libs. Build first or pass -JarPath."
            }

            $JarPath = $latestJar.FullName
        }

        if (-not (Test-Path $JarPath)) {
            throw "JAR file not found: $JarPath"
        }

        Write-Step "Starting Spring Boot from JAR"
        & java "-Dserver.port=$Port" -jar $JarPath
    }
    else {
        if (-not (Test-Path ".\gradlew.bat")) {
            throw "gradlew.bat not found. Run this script from the project root."
        }

        Write-Step "Starting Spring Boot with Gradle"
        & .\gradlew.bat bootRun
    }
}
finally {
    if ($ngrokProcess -and -not $ngrokProcess.HasExited) {
        Write-Step "Stopping ngrok tunnel"
        Stop-Process -Id $ngrokProcess.Id -Force
    }
}
