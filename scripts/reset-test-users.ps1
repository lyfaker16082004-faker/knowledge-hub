# Reset test accounts in the LOCAL dev MySQL database.
#   - backs up the database to backend/storage/backups/
#   - removes e2e_*, codex_*, user1, user2, mod1 and the documents they uploaded (rows + stored files)
#   - recreates user1 / user2 (member) and mod1 (moderator), password 12345678
# admin and all other accounts are kept. Stop the backend first is not required, but recommended.
#
# Usage (from the repository root):  .\scripts\reset-test-users.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$config = Join-Path $root 'backend/config/application-dev.properties'
$storage = Join-Path $root 'backend/storage/documents'
$backups = Join-Path $root 'backend/storage/backups'

function Read-Setting($name) {
 if (Test-Path $config) {
  $line = Get-Content $config | Where-Object { $_ -match "^\s*$([regex]::Escape($name))\s*=" } | Select-Object -First 1
  if ($line) { return ($line -split '=', 2)[1].Trim() }
 }
 return $null
}

$user = $env:DB_USERNAME; if (-not $user) { $user = Read-Setting 'spring.datasource.username' }; if (-not $user) { $user = 'root' }
$password = $env:DB_PASSWORD; if ($null -eq $password) { $password = Read-Setting 'spring.datasource.password' }
$database = 'knowledge_hub'

$mysqlBin = Get-ChildItem 'C:\Program Files\MySQL' -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
$mysql = if ($mysqlBin) { Join-Path $mysqlBin.FullName 'bin\mysql.exe' } else { 'mysql' }
$mysqldump = if ($mysqlBin) { Join-Path $mysqlBin.FullName 'bin\mysqldump.exe' } else { 'mysqldump' }

$previous = $env:MYSQL_PWD
$env:MYSQL_PWD = $password
try {
 # 1. backup
 New-Item -ItemType Directory -Force $backups | Out-Null
 $stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmss')
 $backup = Join-Path $backups "knowledge_hub-before-reset-test-users-$stamp.sql"
 & $mysqldump "-u$user" '--host=127.0.0.1' --default-character-set=utf8mb4 --single-transaction --routines $database --result-file=$backup
 if ($LASTEXITCODE -ne 0) { Remove-Item $backup -ErrorAction SilentlyContinue; throw 'Backup failed - nothing was changed.' }
 Write-Host "Backup: $backup"

 # 2. stored files of the documents that will be removed
 $keys = & $mysql "-u$user" '--host=127.0.0.1' -N -B $database -e "SELECT f.storage_key FROM document_files f JOIN documents d ON d.id=f.document_id JOIN users u ON u.id=d.author_id WHERE u.username LIKE 'e2e\\_%' OR u.username LIKE 'codex\\_%' OR u.username IN ('user1','user2','mod1')"
 if ($LASTEXITCODE -ne 0) { throw 'Could not read document files.' }

 # 3. reset accounts (single transaction)
 # mysql.exe reads the file itself (UTF-8), so Vietnamese text is not mangled by the PowerShell pipe
 $sqlFile = (Join-Path $PSScriptRoot 'reset-test-users.sql').Replace([char]92, [char]47)
 & $mysql "-u$user" '--host=127.0.0.1' --default-character-set=utf8mb4 -t $database -e "source $sqlFile"
 if ($LASTEXITCODE -ne 0) { throw 'Reset failed - transaction rolled back, database unchanged.' }

 # 4. remove the orphaned files (only plain file names inside the storage folder)
 foreach ($key in @($keys)) {
  if ($key -and $key -notmatch '[\\/]|\.\.') {
   $file = Join-Path $storage $key
   if (Test-Path $file) { Remove-Item $file -Confirm:$false; Write-Host "Removed file: $key" }
  }
 }
 Write-Host ''
 Write-Host 'Test accounts ready (password 12345678): user1, user2 (member), mod1 (moderator).'
} finally {
 $env:MYSQL_PWD = $previous
}
