$ErrorActionPreference = 'Stop'
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backend'))
$cache = Join-Path $env:USERPROFILE '.m2/repository/com/mysql/mysql-connector-j'
$driver = Get-ChildItem -LiteralPath $cache -Recurse -Filter '*.jar' | Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $driver) { throw 'Run Maven tests first to download the MySQL JDBC driver.' }
Push-Location $backend
try { & java --class-path $driver.FullName (Join-Path $PSScriptRoot 'CleanupBrowserTestUsers.java'); if ($LASTEXITCODE -ne 0) { throw 'Fixture cleanup stopped.' } } finally { Pop-Location }