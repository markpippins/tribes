# Redis Setup for Beat Generator

## Quick Start

### Using Docker (Recommended)

**Start Redis:**
```powershell
.\start-redis-docker.ps1
```

**Stop Redis:**
```powershell
.\stop-redis-docker.ps1
```

**Restart Redis:**
```powershell
.\restart-redis-docker.ps1
```

**Check Redis Status:**
```powershell
.\check-redis.ps1
```

### Manual Commands

**Start Redis container:**
```bash
docker run -d --name beats-redis -p 6379:6379 redis:latest
```

**Stop and remove Redis:**
```bash
docker stop beats-redis
docker rm beats-redis
```

**Test connection:**
```bash
docker exec -it beats-redis redis-cli ping
# Should return: PONG
```

**View logs:**
```bash
docker logs beats-redis
```

**Access Redis CLI:**
```bash
docker exec -it beats-redis redis-cli
```

## Troubleshooting

### Port Already in Use

If you get an error that port 6379 is already in use:

1. **Find the process using the port:**
```powershell
netstat -ano | findstr :6379
```

2. **Kill the process:**
```powershell
# Replace <PID> with the process ID from above
taskkill /PID <PID> /F
```

3. **Or stop any existing Redis containers:**
```bash
docker ps -a | findstr redis
docker stop beats-redis
docker rm beats-redis
```

### Redis Not Responding

1. **Check if container is running:**
```bash
docker ps
```

2. **Check container logs:**
```bash
docker logs beats-redis
```

3. **Restart Redis:**
```powershell
.\restart-redis-docker.ps1
```

### Application Can't Connect

1. **Verify Redis is running:**
```powershell
.\check-redis.ps1
```

2. **Test connection:**
```bash
docker exec -it beats-redis redis-cli ping
```

3. **Check Redis configuration in application:**
   - Default host: `localhost`
   - Default port: `6379`
   - Configuration file: `java/core/src/main/java/com/angrysurfer/core/config/RedisConfig.java`

## Redis Data Management

### View all keys:
```bash
docker exec -it beats-redis redis-cli KEYS "*"
```

### Clear all data:
```bash
docker exec -it beats-redis redis-cli FLUSHALL
```

### Backup data:
```bash
docker exec beats-redis redis-cli SAVE
docker cp beats-redis:/data/dump.rdb ./redis-backup.rdb
```

### Restore data:
```bash
docker cp ./redis-backup.rdb beats-redis:/data/dump.rdb
docker restart beats-redis
```

## Native Windows Redis (Alternative)

If you prefer to run Redis natively on Windows:

1. **Install using Chocolatey:**
```powershell
choco install redis-64
```

2. **Start Redis:**
```powershell
redis-server
```

3. **Test connection:**
```powershell
redis-cli ping
```

## Configuration

Default Redis configuration:
- Host: `localhost`
- Port: `6379`
- No password (development only)

To change configuration, edit:
`java/core/src/main/java/com/angrysurfer/core/config/RedisConfig.java`
