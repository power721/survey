# Build project with Maven

$ErrorActionPreference = "Stop"

Write-Host "Building project with Maven..." -ForegroundColor Cyan
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed" -ForegroundColor Red
    exit 1
}

Write-Host "Done. Backend JAR built in backend/target/" -ForegroundColor Green
