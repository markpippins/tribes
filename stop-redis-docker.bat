@echo off
REM Stop Redis Docker container
echo Stopping Redis container...
docker stop beats-redis
docker rm beats-redis
echo Redis stopped and removed
pause
