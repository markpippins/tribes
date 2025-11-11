@echo off
REM Start Redis using Docker
echo Starting Redis in Docker...
docker run -d --name beats-redis -p 6379:6379 redis:latest
echo Redis started on port 6379
echo.
echo To check if Redis is running:
echo   docker ps
echo.
echo To stop Redis:
echo   docker stop beats-redis
echo   docker rm beats-redis
echo.
echo To test connection:
echo   redis-cli ping
pause
