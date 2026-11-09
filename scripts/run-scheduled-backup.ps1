param(
    [Parameter(Mandatory = $true)]
    [string]$Database,
    [Parameter(Mandatory = $true)]
    [string]$HostName,
    [Parameter(Mandatory = $true)]
    [int]$Port,
    [Parameter(Mandatory = $true)]
    [string]$Username,
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory
)

$ErrorActionPreference = "Stop"
$logDirectory = Join-Path $env:LOCALAPPDATA "MediQueue\logs"
$logFile = Join-Path $logDirectory "scheduled-backup.log"

try {
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $backupScript = Join-Path $PSScriptRoot "backup-postgres.ps1"
    $output = @(& $backupScript `
        -Database $Database `
        -HostName $HostName `
        -Port $Port `
        -Username $Username `
        -BackupDirectory $BackupDirectory 2>&1)
    if (-not $?) {
        throw "Backup script reported an error."
    }

    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $output | ForEach-Object {
        Add-Content -LiteralPath $logFile -Value "$timestamp $_"
    }
}
catch {
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    try {
        New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
        Add-Content -LiteralPath $logFile `
            -Value "$timestamp Backup failed: $($_.Exception.Message)"
    }
    catch {
        Write-Error "Backup failed, and the task log could not be written: $($_.Exception.Message)"
    }
    exit 1
}

exit 0
