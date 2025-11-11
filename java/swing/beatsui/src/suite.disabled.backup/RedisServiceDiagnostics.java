package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.redis.RedisService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for Redis diagnostics
 */
public class RedisServiceDiagnostics {

    /**
     * Run all Redis diagnostics
     */
    public static DiagnosticLogBuilder runAllRedisDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Redis Diagnostics");
        
        try {
            // Run connection test
            log.addSection("1. Redis Connection Test");
            DiagnosticLogBuilder connectionLog = testRedisConnection();
            log.addLine(connectionLog.buildWithoutHeader());
            
            if (connectionLog.hasErrors()) {
                // If connection fails, we can't continue
                log.addError("Redis connection failed. Cannot continue with other tests.");
                for (String error : connectionLog.getErrors()) {
                    log.addError(error);
                }
                return log;
            }
            
            // Run session diagnostics
            log.addSection("2. Session Diagnostics");
            SessionDiagnostics sessionHelper = new SessionDiagnostics();
            DiagnosticLogBuilder sessionLog = sessionHelper.testSessionDiagnostics();
            log.addLine(sessionLog.buildWithoutHeader());
            
            // Run session navigation diagnostics
            log.addSection("3. Session Navigation");
            DiagnosticLogBuilder navigationLog = sessionHelper.testSessionNavigation();
            log.addLine(navigationLog.buildWithoutHeader());
            
            // Run user config diagnostics
            log.addSection("4. User Configuration Diagnostics");
            UserConfigDiagnostics configHelper = new UserConfigDiagnostics();
            //DiagnosticLogBuilder configLog = configHelper.testUserConfigDiagnostics();
            //log.addLine(configLog.buildWithoutHeader());
            
            // Run instrument diagnostics
            log.addSection("5. Instrument Diagnostics");
            InstrumentDiagnostics instrumentHelper = new InstrumentDiagnostics();
            DiagnosticLogBuilder instrumentLog = instrumentHelper.testInstrumentOperations();
            log.addLine(instrumentLog.buildWithoutHeader());
            
            // Run player/instrument integrity diagnostics
            log.addSection("6. Player/Instrument Integrity");
            DiagnosticLogBuilder integrityLog = instrumentHelper.testPlayerInstrumentIntegrity();
            log.addLine(integrityLog.buildWithoutHeader());
            
            // Compile errors
            for (DiagnosticLogBuilder subLog : 
                  new DiagnosticLogBuilder[]{connectionLog, sessionLog, navigationLog, instrumentLog, integrityLog}) {
//                new DiagnosticLogBuilder[]{connectionLog, sessionLog, navigationLog, configLog, instrumentLog, integrityLog}) {
                for (String error : subLog.getErrors()) {
                    log.addError(error);
                }
                for (String warning : subLog.getWarnings()) {
                    log.addWarning(warning);
                }
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test Redis connection
     */
    public static DiagnosticLogBuilder testRedisConnection() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Redis Connection Test");
        
        try {
            // Get Redis pool from service
            RedisService redisService = RedisService.getInstance();
            JedisPool jedisPool = redisService.getJedisPool();
            
            if (jedisPool == null) {
                log.addError("JedisPool is null. Redis service may not be initialized.");
                return log;
            }
            
            // Test basic connection
            try (Jedis jedis = jedisPool.getResource()) {
                String pingResponse = jedis.ping();
                log.addIndentedLine("Redis ping response: " + pingResponse, 1);
                
                if (!"PONG".equals(pingResponse)) {
                    log.addError("Redis ping did not return expected response.");
                }
                
                // Get server info
                String redisVersion = jedis.info("server").split("\n")[1].split(":")[1].trim();
                log.addIndentedLine("Redis version: " + redisVersion, 1);
                
                // Check key space
                log.addIndentedLine("Checking Redis keyspace...", 1);
                Set<String> keys = jedis.keys("*");
                
                if (keys.isEmpty()) {
                    log.addWarning("Redis database is completely empty. This might be a fresh instance.");
                } else {
                    log.addIndentedLine("Found " + keys.size() + " keys in Redis", 1);
                    
                    // Group keys by prefix for a summary
                    Set<String> prefixes = new HashSet<>();
                    for (String key : keys) {
                        String prefix = key.contains(":") ? key.split(":")[0] : key;
                        prefixes.add(prefix);
                    }
                    
                    log.addIndentedLine("Key prefixes found:", 1);
                    for (String prefix : prefixes) {
                        long count = keys.stream().filter(k -> k.startsWith(prefix + ":") || k.equals(prefix)).count();
                        log.addIndentedLine(prefix + ": " + count + " keys", 2);
                    }
                }
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
