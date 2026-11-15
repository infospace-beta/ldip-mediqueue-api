param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$BackupFile,

    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[A-Za-z0-9_-]+_restore_test$")]
    [string]$TargetDatabase,

    [string]$MaintenanceDatabase = "postgres",
    [string]$HostName = "localhost",
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$PostgreSqlBinDirectory = (Join-Path $env:ProgramFiles "PostgreSQL\18\bin")
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command createdb -ErrorAction SilentlyContinue) `
        -and (Test-Path -LiteralPath (Join-Path $PostgreSqlBinDirectory "createdb.exe"))) {
    $env:PATH = "$PostgreSqlBinDirectory;$env:PATH"
}

foreach ($tool in @("createdb", "pg_restore", "psql")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool is required. Install the PostgreSQL client tools and add them to PATH."
    }
}

& createdb `
    --host $HostName `
    --port $Port `
    --username $Username `
    --maintenance-db $MaintenanceDatabase `
    --template template0 `
    --encoding UTF8 `
    $TargetDatabase

if ($LASTEXITCODE -ne 0) {
    throw "Could not create $TargetDatabase. No existing database was overwritten."
}

& pg_restore `
    --host $HostName `
    --port $Port `
    --username $Username `
    --dbname $TargetDatabase `
    --exit-on-error `
    --no-owner `
    --no-acl `
    $BackupFile

if ($LASTEXITCODE -ne 0) {
    throw "Restore failed. The isolated target database $TargetDatabase may be partially restored; inspect it before removing it."
}

& psql `
    --host $HostName `
    --port $Port `
    --username $Username `
    --dbname $TargetDatabase `
    --set ON_ERROR_STOP=1 `
    --command "select version, success from flyway_schema_history order by installed_rank"

if ($LASTEXITCODE -ne 0) {
    throw "Restore completed, but Flyway history verification failed."
}

Write-Output "Restore verification completed for isolated database $TargetDatabase."
