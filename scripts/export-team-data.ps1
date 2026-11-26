# Xuất toàn bộ dữ liệu local thành MỘT file zip để gửi cho team (qua Drive / Zalo, KHÔNG đưa lên GitHub).
#   - knowledge_hub.sql : sao lưu database MySQL (tài khoản, tài liệu, bài viết, bình luận...)
#   - storage.zip       : file tài liệu, ảnh đại diện, ảnh trong bài viết (backend/storage)
# Kết quả: team-data/knowledge-hub-data-<ngày>.zip
#
# Cách chạy (từ thư mục gốc dự án):  .\scripts\export-team-data.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$config = Join-Path $root 'backend/config/application-dev.properties'
$storage = Join-Path $root 'backend/storage'
$outDir = Join-Path $root 'team-data'

# Đọc tài khoản MySQL từ biến môi trường hoặc backend/config/application-dev.properties
function Read-Setting($name) {
  if (Test-Path $config) {
    $line = Get-Content $config | Where-Object { $_ -match "^\s*$([regex]::Escape($name))\s*=" } | Select-Object -First 1
    if ($line) { return ($line -split '=', 2)[1].Trim() }
  }
  return $null
}
$user = $env:DB_USERNAME; if (-not $user) { $user = Read-Setting 'spring.datasource.username' }; if (-not $user) { $user = 'root' }
$password = $env:DB_PASSWORD; if (-not $password) { $password = Read-Setting 'spring.datasource.password' }

$mysqlBin = Get-ChildItem 'C:\Program Files\MySQL' -Directory -Filter 'MySQL Server*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
$mysqldump = if ($mysqlBin) { Join-Path $mysqlBin.FullName 'bin\mysqldump.exe' } else { 'mysqldump' }

$stamp = (Get-Date).ToString('yyyyMMdd-HHmm')
$work = Join-Path $outDir "work-$stamp"
New-Item -ItemType Directory -Force $work | Out-Null

$previous = $env:MYSQL_PWD
$env:MYSQL_PWD = $password
try {
  # Bước 1: sao lưu database (gồm cả bảng flyway_schema_history để Flyway không chạy lại migration)
  & $mysqldump "-u$user" '--host=127.0.0.1' --default-character-set=utf8mb4 --single-transaction --routines knowledge_hub "--result-file=$work\knowledge_hub.sql"
  if ($LASTEXITCODE -ne 0) { throw 'Sao lưu database thất bại.' }

  # Bước 2: nén thư mục file người dùng (bỏ thư mục backups)
  $folders = Get-ChildItem $storage -Directory | Where-Object { $_.Name -ne 'backups' }
  Compress-Archive -Path $folders.FullName -DestinationPath "$work\storage.zip" -Force

  # Bước 3: gộp thành một file zip duy nhất
  $package = Join-Path $outDir "knowledge-hub-data-$stamp.zip"
  Compress-Archive -Path "$work\knowledge_hub.sql", "$work\storage.zip" -DestinationPath $package -Force
  Write-Host "Đã tạo gói dữ liệu: $package"
  Write-Host 'Gửi file này cho team qua Drive / Zalo. Không đưa lên GitHub (repo đang công khai).'
} finally {
  $env:MYSQL_PWD = $previous
  Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
}
