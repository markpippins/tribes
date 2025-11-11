# Start Redis using Docker
Write-Host "Starting Redis in Docker..." -ForegroundColor Green

# Check if container already exists
$existing = docker ps -a --filter "name=beats-redis" --format "{{.Names}}"

if ($existing -eq "beats-redis") {
    Write-Host "Redis container already exists. Starting it..." -ForegroundColor Yellow
    docker start beats-redis
} else {
    Write-Host "Creating new Redis container..." -ForegroundColor Green
    docker run -d --name beats-redis -p 6379:6379 redis:latest
}

Write-Host ""
Write-Host "Redis started on port 6379" -ForegroundColor Green
Write-Host ""
Write-Host "To check if Redis is running:" -ForegroundColor Cyan
Write-Host "  docker ps" -ForegroundColor White
Write-Host ""
Write-Host "To view Redis logs:" -ForegroundColor Cyan
Write-Host "  docker logs beats-redis" -ForegroundColor White
Write-Host ""
Write-Host "To stop Redis:" -ForegroundColor Cyan
Write-Host "  docker stop beats-redis" -ForegroundColor White
Write-Host ""
Write-Host "To test connection:" -ForegroundColor Cyan
Write-Host "  docker exec -it beats-redis redis-cli ping" -ForegroundColor White
Write-Host ""

# Test connection
Write-Host "Testing Redis connection..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
$result = docker exec beats-redis redis-cli ping 2>$null
if ($result -eq "PONG") {
    Write-Host "✓ Redis is responding!" -ForegroundColor Green
} else {
    Write-Host "⚠ Redis may still be starting up. Wait a few seconds and try: docker exec -it beats-redis redis-cli ping" -ForegroundColor Yellow
}
