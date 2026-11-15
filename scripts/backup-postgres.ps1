param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [ValidatePattern("^[A-Za-z0-9_-]+$")]
    [string]$Database,

    [string]$HostName = "localhost",
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$BackupDirectory = (Join-Path $env:LOCALAPPDATA "MediQueue\backups"),
    [string]$PostgreSqlBinDirectory = (Join-Path $env:ProgramFiles "PostgreSQL\18\bin")
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue) `
        -and (Test-Path -LiteralPath (Join-Path $PostgreSqlBinDirectory "pg_dump.exe"))) {
    $env:PATH = "$PostgreSqlBinDirectory;$env:PATH"
}

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump is required. Install the PostgreSQL client tools and add them to PATH."
}

New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
$backupPath = Join-Path $BackupDirectory "$Database-$timestamp.dump"

if (Test-Path -LiteralPath $backupPath) {
    throw "Refusing to overwrite existing backup: $backupPath"
}

& pg_dump `
    --host $HostName `
    --port $Port `
    --username $Username `
    --dbname $Database `
    --format custom `
    --no-owner `
    --no-acl `
    --file $backupPath

if ($LASTEXITCODE -ne 0) {
    if (Test-Path -LiteralPath $backupPath) {
        Remove-Item -LiteralPath $backupPath
    }
    throw "pg_dump failed with exit code $LASTEXITCODE."
}

Write-Output "Backup created: $backupPath"
