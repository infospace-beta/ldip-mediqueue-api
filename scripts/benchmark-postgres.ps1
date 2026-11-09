param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[A-Za-z0-9_-]+$")]
    [string]$SourceDatabase,

    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[A-Za-z0-9_-]+_perf_test$")]
    [string]$TargetDatabase,

    [string]$HostName = "localhost",
    [ValidateRange(1, 65535)]
    [int]$Port = 5432,
    [string]$Username = "postgres",
    [string]$PostgreSqlBinDirectory = (Join-Path $env:ProgramFiles "PostgreSQL\18\bin")
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue) `
        -and (Test-Path -LiteralPath (Join-Path $PostgreSqlBinDirectory "pg_dump.exe"))) {
    $env:PATH = "$PostgreSqlBinDirectory;$env:PATH"
}

foreach ($tool in @("pg_dump", "createdb", "psql")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool is required. Install PostgreSQL client tools or provide -PostgreSqlBinDirectory."
    }
}

$schemaFile = Join-Path $env:TEMP (
    "mediqueue-schema-{0}.sql" -f [Guid]::NewGuid().ToString("N"))

try {
    & pg_dump `
        --host $HostName `
        --port $Port `
        --username $Username `
        --dbname $SourceDatabase `
        --schema-only `
        --no-owner `
        --no-acl `
        --file $schemaFile
    if ($LASTEXITCODE -ne 0) {
        throw "Could not export source schema. No target database was created."
    }

    & createdb `
        --host $HostName `
        --port $Port `
        --username $Username `
        --maintenance-db $SourceDatabase `
        --template template0 `
        --encoding UTF8 `
        $TargetDatabase
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create $TargetDatabase. No existing database was overwritten."
    }
    & psql `
        --host $HostName `
        --port $Port `
        --username $Username `
        --dbname $TargetDatabase `
        --set ON_ERROR_STOP=1 `
        --file $schemaFile
    if ($LASTEXITCODE -ne 0) {
        throw "Could not load the schema-only copy into $TargetDatabase."
    }

    $openEmergencyMigration = Join-Path `
        (Split-Path -Parent $PSScriptRoot) `
        "src\main\resources\db\migration\V10__index_open_emergencies.sql"
    & psql `
        --host $HostName `
        --port $Port `
        --username $Username `
        --dbname $TargetDatabase `
        --set ON_ERROR_STOP=1 `
        --file $openEmergencyMigration
    if ($LASTEXITCODE -ne 0) {
        throw "Could not apply the open-emergency performance index to $TargetDatabase."
    }

    $sampleDataSql = @'
BEGIN;
INSERT INTO hospitals
    (name, registration_number, hospital_type, county, town, status)
SELECT 'Performance Hospital ' || n, 'PERF-' || n,
       'PUBLIC', 'Sample County', 'Sample Town', 'ACTIVE'
FROM generate_series(1, 100) AS n;

INSERT INTO departments (hospital_id, name, code, status)
SELECT h.id, 'Performance Department ' || d, 'D' || d, 'ACTIVE'
FROM hospitals h
CROSS JOIN generate_series(1, 4) AS d
WHERE h.registration_number LIKE 'PERF-%';

INSERT INTO users
    (username, email, full_name, password_hash, role, enabled, on_duty)
SELECT 'perf-patient-' || n, 'perf-patient-' || n || '@example.test',
       'Synthetic Patient ' || n, 'not-a-login-hash', 'PATIENT', TRUE, FALSE
FROM generate_series(1, 50000) AS n;

INSERT INTO users
    (username, email, full_name, password_hash, role, enabled, on_duty, hospital_id)
SELECT 'perf-responder-' || n, 'perf-responder-' || n || '@example.test',
       'Synthetic Responder ' || n, 'not-a-login-hash', 'RESPONDER', TRUE, FALSE,
       ((n - 1) % 100) + 1
FROM generate_series(1, 1000) AS n;

WITH synthetic_tickets AS (
    SELECT n,
           ((n - 1) % 50000) + 1 AS patient_id,
           ((n - 1) % 400) + 1 AS department_id,
           CASE
               WHEN n > 50000 THEN
                   CASE WHEN n % 2 = 0 THEN 'COMPLETED' ELSE 'CANCELLED' END
               ELSE
                   CASE (n % 10)
                       WHEN 0 THEN 'WAITING'
                       WHEN 1 THEN 'WAITING'
                       WHEN 2 THEN 'WAITING'
                       WHEN 3 THEN 'WAITING'
                       WHEN 4 THEN 'WAITING'
                       WHEN 5 THEN 'CALLED'
                       WHEN 6 THEN 'SKIPPED'
                       WHEN 7 THEN 'COMPLETED'
                       WHEN 8 THEN 'COMPLETED'
                       ELSE 'CANCELLED'
                   END
           END AS status,
           CURRENT_TIMESTAMP - (n % 90) * INTERVAL '1 day'
               - (n % 1440) * INTERVAL '1 minute' AS created_at
    FROM generate_series(1, 200000) AS n
)
INSERT INTO queue_tickets
    (patient_id, department_id, status, called_at, completed_at, created_at, updated_at)
SELECT patient_id,
       department_id,
       status,
       CASE WHEN status IN ('CALLED', 'SKIPPED', 'COMPLETED')
            THEN created_at + INTERVAL '10 minutes'
            ELSE NULL
       END,
       CASE WHEN status = 'COMPLETED'
            THEN created_at + INTERVAL '30 minutes'
            ELSE NULL
       END,
       created_at,
       CURRENT_TIMESTAMP
FROM synthetic_tickets;

INSERT INTO emergency_cases
    (hospital_id, patient_id, reported_by_id, assigned_responder_id,
     title, description, severity, status, resolved_at, created_at, updated_at)
SELECT ((n - 1) % 100) + 1,
       ((n - 1) % 50000) + 1,
       ((n - 1) % 50000) + 1,
       CASE WHEN n % 4 = 0 THEN NULL ELSE 50000 + (((n - 1) % 100) + 1) END,
       'Synthetic emergency ' || n,
       'Synthetic performance-test case',
       CASE (n % 4)
           WHEN 0 THEN 'LOW'
           WHEN 1 THEN 'MEDIUM'
           WHEN 2 THEN 'HIGH'
           ELSE 'CRITICAL'
       END,
       CASE (n % 4)
           WHEN 0 THEN 'REPORTED'
           WHEN 1 THEN 'ASSIGNED'
           WHEN 2 THEN 'RESPONDING'
           ELSE 'RESOLVED'
       END,
       CASE WHEN n % 4 = 3 THEN CURRENT_TIMESTAMP - (n % 60) * INTERVAL '1 day'
            ELSE NULL
       END,
       CURRENT_TIMESTAMP - (n % 90) * INTERVAL '1 day',
       CURRENT_TIMESTAMP
FROM generate_series(1, 50000) AS n;

ANALYZE queue_tickets;
ANALYZE emergency_cases;
COMMIT;
'@

    & psql `
        --host $HostName `
        --port $Port `
        --username $Username `
        --dbname $TargetDatabase `
        --set ON_ERROR_STOP=1 `
        --command $sampleDataSql
    if ($LASTEXITCODE -ne 0) {
        throw "Synthetic sample-data loading failed in $TargetDatabase."
    }

    $plans = @'
EXPLAIN (ANALYZE, BUFFERS)
SELECT id FROM queue_tickets
WHERE department_id = 21 AND status = 'WAITING'
ORDER BY created_at ASC
LIMIT 1;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, status, created_at FROM queue_tickets
WHERE patient_id = 4242
ORDER BY created_at DESC
LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, severity, created_at FROM emergency_cases
WHERE hospital_id = 17 AND status IN ('REPORTED', 'ASSIGNED', 'RESPONDING')
ORDER BY created_at DESC
LIMIT 50;
'@

    & psql `
        --host $HostName `
        --port $Port `
        --username $Username `
        --dbname $TargetDatabase `
        --set ON_ERROR_STOP=1 `
        --command $plans
    if ($LASTEXITCODE -ne 0) {
        throw "Query-plan checks failed in $TargetDatabase."
    }

    Write-Output "Synthetic query benchmarks completed in $TargetDatabase."
    Write-Output "This database was created from schema only; no source rows were copied."
    Write-Output "The performance database is retained for inspection and manual removal."
}
finally {
    if (Test-Path -LiteralPath $schemaFile) {
        Remove-Item -LiteralPath $schemaFile
    }
}
