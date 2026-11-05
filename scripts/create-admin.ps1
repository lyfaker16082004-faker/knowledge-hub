$ErrorActionPreference = 'Stop'
$backend = Join-Path $PSScriptRoot '../backend'
$existing = @{}
foreach ($key in @('APP_BOOTSTRAP_ENABLED','APP_ADMIN_USERNAME','APP_ADMIN_EMAIL','APP_ADMIN_PASSWORD')) { $existing[$key] = [Environment]::GetEnvironmentVariable($key,'Process') }
try {
 $env:APP_ADMIN_USERNAME = Read-Host 'Tên đăng nhập admin'
 $env:APP_ADMIN_EMAIL = Read-Host 'Email admin'
 $secret = Read-Host 'Mật khẩu (ít nhất 8 ký tự)' -AsSecureString
 $env:APP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
 $env:APP_BOOTSTRAP_ENABLED = 'true'
 Push-Location $backend
 try { & .\mvnw.cmd spring-boot:run } finally { Pop-Location }
} finally {
 foreach ($key in $existing.Keys) { [Environment]::SetEnvironmentVariable($key,$existing[$key],'Process') }
}