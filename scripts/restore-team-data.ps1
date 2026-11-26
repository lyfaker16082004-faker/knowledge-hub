# Nạp gói dữ liệu team (do scripts/export-team-data.ps1 tạo) vào máy của bạn.
#   - Tạo database knowledge_hub nếu chưa có, rồi nạp dữ liệu (GHI ĐÈ dữ liệu cũ)
#   - Giải nén file tài liệu / ảnh vào backend/storage
# Nên TẮT backend trước khi chạy. Database cũ được sao lưu vào backend/storage/backups/ trước khi ghi đè.
#
# Cách chạy (từ thư mục gốc dự án):
#   .\scripts\restore-team-data.ps1 -Package "D:\Downloads\knowledge-hub-data-20261126-2230.zip"
param(
  [Parameter(Mandatory = $true)][string]$Package
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$config = Join-Path $root 'backend/config/application-dev.properties'
$storage = Join-Path $root 'backend/storage'
$backups = Join-Path $storage 'backups'

if (-not (Test-Path $Package)) { throw "Không tìm thấy file: $Package" }

function Read-Setting($name) {
  if (Test-Path $config) {
    $line = Get-Content $config | Where-Object { $_ -match "^\s*$([regex]::Escape($name))\s*=" } | Select-Object -First 1
    if ($line) { return ($line -split '=', 2)[1].Trim() }
  }
  return $null
}
$user = $env:DB_USERNAME; if (-not $user) { $user = Read-Setting 'spring.datasource.username' }; if (-not $user) { $user = 'root' }
$password = $env:DB_PASSWORD; if (-not $password) { $password = Read-Setting 'spring.datasource.password' }
if (-not $password) { $password = Read-Host 'Mật khẩu MySQL' -AsSecureString | ForEach-Object { [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($_)) } }

$mysqlBin = Get-ChildItem 'C:\Program Files\MySQL' -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
$mysql = if ($mysqlBin) { Join-Path $mysqlBin.FullName 'bin\mysql.exe' } else { 'mysql' }
$mysqldump = if ($mysqlBin) { Join-Path $mysqlBin.FullName 'bin\mysqldump.exe' } else { 'mysqldump' }

$work = Join-Path ([IO.Path]::GetTempPath()) ("kh-restore-" + [guid]::NewGuid())
New-Item -ItemType Directory -Force $work | Out-Null

$previous = $env:MYSQL_PWD
$env:MYSQL_PWD = $password
try {
  # Bước 1: giải nén gói
  Expand-Archive -Path $Package -DestinationPath $work -Force
  if (-not (Test-Path "$work\knowledge_hub.sql")) { throw 'Gói dữ liệu không hợp lệ (thiếu knowledge_hub.sql).' }

  # Bước 2: tạo database nếu chưa có
  & $mysql "-u$user" '--host=127.0.0.1' -e 'CREATE DATABASE IF NOT EXISTS knowledge_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
  if ($LASTEXITCODE -ne 0) { throw 'Không kết nối được MySQL. Kiểm tra MySQL đang chạy và mật khẩu.' }

  # Bước 3: sao lưu dữ liệu cũ (nếu có) rồi nạp dữ liệu mới
  New-Item -ItemType Directory -Force $backups | Out-Null
  $stamp = (Get-Date).ToString('yyyyMMdd-HHmmss')
  & $mysqldump "-u$user" '--host=127.0.0.1' --default-character-set=utf8mb4 --single-transaction knowledge_hub "--result-file=$backups\knowledge_hub-before-restore-$stamp.sql"
  $sqlFile = "$work\knowledge_hub.sql".Replace([char]92, [char]47)
  & $mysql "-u$user" '--host=127.0.0.1' --default-character-set=utf8mb4 knowledge_hub -e "source $sqlFile"
  if ($LASTEXITCODE -ne 0) { throw 'Nạp database thất bại.' }

  # Bước 4: giải nén file tài liệu / ảnh vào backend/storage
  New-Item -ItemType Directory -Force $storage | Out-Null
  Expand-Archive -Path "$work\storage.zip" -DestinationPath $storage -Force

  Write-Host 'Đã nạp xong dữ liệu team. Khởi động backend rồi mở http://localhost:5173'
  Write-Host 'Tài khoản để đăng nhập: xem docs/demo-accounts.md'
} finally {
  $env:MYSQL_PWD = $previous
  Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
