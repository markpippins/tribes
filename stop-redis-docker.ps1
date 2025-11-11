# Stop Redis Docker container
Write-Host "Stopping Redis container..." -ForegroundColor Yellow
docker stop beats-redis

Write-Host "Removing Redis container..." -ForegroundColor Yellow
docker rm beats-redis

Write-Host "✓ Redis stopped and removed" -ForegroundColor Green
