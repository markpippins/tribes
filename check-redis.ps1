# Check Redis status
Write-Host "Checking Redis status..." -ForegroundColor Cyan
Write-Host ""

# Check if Docker container is running
$container = docker ps --filter "name=beats-redis" --format "{{.Names}}"
if ($container -eq "beats-redis") {
    Write-Host "✓ Redis container is running" -ForegroundColor Green
    
    # Check if Redis is responding
    $result = docker exec beats-redis redis-cli ping 2>$null
    if ($result -eq "PONG") {
        Write-Host "✓ Redis is responding to commands" -ForegroundColor Green
        
        # Get Redis info
        Write-Host ""
        Write-Host "Redis Information:" -ForegroundColor Cyan
        docker exec beats-redis redis-cli INFO server | Select-String "redis_version|os|process_id|uptime_in_seconds"
        
        # Check memory usage
        Write-Host ""
        Write-Host "Memory Usage:" -ForegroundColor Cyan
        docker exec beats-redis redis-cli INFO memory | Select-String "used_memory_human"
        
        # Check number of keys
        Write-Host ""
        Write-Host "Database Keys:" -ForegroundColor Cyan
        docker exec beats-redis redis-cli DBSIZE
    } else {
        Write-Host "✗ Redis container is running but not responding" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Redis container is not running" -ForegroundColor Red
    Write-Host ""
    Write-Host "To start Redis, run:" -ForegroundColor Yellow
    Write-Host "  .\start-redis-docker.ps1" -ForegroundColor White
}
