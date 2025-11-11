# Restart Redis Docker container
Write-Host "Restarting Redis..." -ForegroundColor Yellow

docker stop beats-redis 2>$null
docker rm beats-redis 2>$null

Write-Host "Starting fresh Redis instance..." -ForegroundColor Green
docker run -d --name beats-redis -p 6379:6379 redis:latest

Start-Sleep -Seconds 2

$result = docker exec beats-redis redis-cli ping 2>$null
if ($result -eq "PONG") {
    Write-Host "✓ Redis restarted successfully!" -ForegroundColor Green
} else {
    Write-Host "⚠ Redis is starting up..." -ForegroundColor Yellow
}
