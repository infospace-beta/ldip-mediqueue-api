param(
    [string]$Database,
    [string]$HostName,
    [int]$Port,
    [string]$Username,
    [ValidatePattern("^(?:[01]\d|2[0-3]):[0-5]\d$")]
    [string]$ScheduleTime = "02:00",
    [string]$TaskName = "MediQueue PostgreSQL Daily Backup",
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

function Escape-PgPassField([string]$Value) {
    return $Value.Replace("\", "\\").Replace(":", "\:")
}

$settings = @{}
$envFile = Join-Path $ProjectRoot ".env"
if (Test-Path -LiteralPath $envFile) {
    Get-Content -LiteralPath $envFile | ForEach-Object {
        if ($_ -match '^\s*(DB_URL|DB_USERNAME|DB_PASSWORD)\s*=(.*)$') {
            $settings[$matches[1]] = $matches[2].Trim()
        }
    }
}

if (-not $HostName -and $settings["DB_URL"]) {
    $connectionUri = [Uri]($settings["DB_URL"] -replace '^jdbc:', "")
    $HostName = $connectionUri.Host
    if (-not $Port) {
        $Port = $connectionUri.Port
    }
    if (-not $Database) {
        $Database = $connectionUri.AbsolutePath.TrimStart("/")
    }
}
if (-not $Username) {
    $Username = $settings["DB_USERNAME"]
}
$password = $settings["DB_PASSWORD"]

if (-not $Database -or -not $HostName -or -not $Port -or -not $Username) {
    throw "Provide database connection details or configure DB_URL and DB_USERNAME in .env."
}
if (-not $password) {
    throw "DB_PASSWORD is required in the local .env file to configure the protected PostgreSQL password file."
}
if (-not (Get-Command Register-ScheduledTask -ErrorAction SilentlyContinue)) {
    throw "Windows Task Scheduler cmdlets are unavailable."
}
if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
    throw "Task '$TaskName' already exists. Inspect it before changing or replacing it."
}

$escapedHost = Escape-PgPassField $HostName
$escapedDatabase = Escape-PgPassField $Database
$escapedUsername = Escape-PgPassField $Username
$escapedPassword = Escape-PgPassField $password
$pgpassDirectory = Join-Path $env:APPDATA "postgresql"
$pgpassPath = Join-Path $pgpassDirectory "pgpass.conf"
New-Item -ItemType Directory -Path $pgpassDirectory -Force | Out-Null

$entry = "${escapedHost}:${Port}:*:${escapedUsername}:${escapedPassword}"
$existingEntries = @()
if (Test-Path -LiteralPath $pgpassPath) {
    $existingEntries = @(Get-Content -LiteralPath $pgpassPath |
        Where-Object {
            $exactPrefix = '^{0}:{1}:{2}:{3}:' -f `
                [regex]::Escape($escapedHost),
                $Port,
                [regex]::Escape($escapedDatabase),
                [regex]::Escape($escapedUsername)
            $wildcardPrefix = '^{0}:{1}:\*:{2}:' -f `
                [regex]::Escape($escapedHost),
                $Port,
                [regex]::Escape($escapedUsername)
            $_ -notmatch $exactPrefix -and $_ -notmatch $wildcardPrefix
        })
}
$allEntries = @($existingEntries) + $entry
[System.IO.File]::WriteAllText(
    $pgpassPath,
    (($allEntries -join "`n") + "`n"),
    [System.Text.UTF8Encoding]::new($false))

$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$icacls = Join-Path $env:SystemRoot "System32\icacls.exe"
& $icacls $pgpassPath /inheritance:r /grant:r "${currentUser}:F" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Could not restrict pgpass.conf permissions. Remove that file and correct its ACL before scheduling backups."
}

$scriptPath = Join-Path $PSScriptRoot "run-scheduled-backup.ps1"
$backupDirectory = Join-Path $env:LOCALAPPDATA "MediQueue\backups"
$powershell = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"
$arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`" -Database `"$Database`" -HostName `"$HostName`" -Port $Port -Username `"$Username`" -BackupDirectory `"$backupDirectory`""
$action = New-ScheduledTaskAction -Execute $powershell -Argument $arguments
$time = [DateTime]::ParseExact($ScheduleTime, "HH:mm", $null)
$trigger = New-ScheduledTaskTrigger -Daily -At $time
$principal = New-ScheduledTaskPrincipal `
    -UserId $currentUser `
    -LogonType Interactive `
    -RunLevel Limited
$settingsForTask = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Principal $principal `
    -Settings $settingsForTask `
    -Description "Creates a timestamped MediQueue PostgreSQL backup each day at $ScheduleTime." |
    Out-Null

Write-Output "Scheduled task '$TaskName' created for daily execution at $ScheduleTime."
Write-Output "PostgreSQL password file is protected for the current Windows user."
