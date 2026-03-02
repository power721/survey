# Build frontend and copy to backend static resources

$ErrorActionPreference = "Stop"

$ROOT = $PSScriptRoot
$FRONTEND = "$ROOT\frontend"
$STATIC = "$ROOT\backend\src\main\resources\static"

Write-Host "Building frontend..." -ForegroundColor Cyan
Set-Location $FRONTEND
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend build failed" -ForegroundColor Red
    exit 1
}

Write-Host "Copying dist to backend static resources..." -ForegroundColor Cyan
if (Test-Path $STATIC) {
    Remove-Item -Recurse -Force $STATIC
}
Copy-Item -Recurse "$FRONTEND\dist" $STATIC

Write-Host "Done. Static files copied to $STATIC" -ForegroundColor Green
Set-Location $ROOT
