package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.SessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic helper for SessionManager
 */
public class SessionManagerDiagnostics {

    /**
     * Run diagnostics on SessionManager
     */
    public DiagnosticLogBuilder testSessionManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("SessionManager Diagnostics");
        
        try {
            SessionManager manager = SessionManager.getInstance();
            log.addLine("SessionManager instance obtained: " + (manager != null));
            
            // Get active session
            Session activeSession = manager.getActiveSession();
            log.addLine("Active session: " + (activeSession != null ? 
                "ID: " + activeSession.getId() : "None"));
            
            if (activeSession != null) {
                testSession(activeSession, log);
            } else {
                log.addWarning("No active session found, limited testing possible");
            }
            
            // Test Redis connection
            log.addSection("Session Storage Status");
            RedisService redisService = RedisService.getInstance();
            
            try {
                List<Long> allSessionIds = redisService.getAllSessionIds();
                log.addLine("Found " + allSessionIds.size() + " sessions in storage");
                
                if (!allSessionIds.isEmpty()) {
                    Long minId = redisService.getMinimumSessionId();
                    Long maxId = redisService.getMaximumSessionId();
                    log.addLine("Session ID range: " + minId + " - " + maxId);
                    
                    // Check if active session is in range
                    if (activeSession != null) {
                        boolean inRange = activeSession.getId() >= minId && activeSession.getId() <= maxId;
                        log.addLine("Active session ID " + activeSession.getId() + 
                                  " is " + (inRange ? "in" : "out of") + " range");
                        
                        if (!inRange) {
                            log.addWarning("Active session ID is outside the range of stored sessions");
                        }
                    }
                }
            } catch (Exception e) {
                log.addError("Failed to query sessions from storage: " + e.getMessage());
            }
            
            // Test navigation
            log.addSection("Session Navigation");
            log.addLine("Can move back: " + manager.canMoveBack());
            log.addLine("Can move forward: " + manager.canMoveForward());
            
            if (manager.canMoveBack()) {
                try {
                    // Get previous session ID for inspection
                    Long prevId = redisService.getPreviousSessionId(activeSession);
                    log.addLine("Previous session ID: " + prevId);
                } catch (Exception e) {
                    log.addWarning("Failed to get previous session ID: " + e.getMessage());
                }
            }
            
            if (manager.canMoveForward()) {
                try {
                    // Get next session ID for inspection
                    Long nextId = redisService.getNextSessionId(activeSession);
                    log.addLine("Next session ID: " + nextId);
                } catch (Exception e) {
                    log.addWarning("Failed to get next session ID: " + e.getMessage());
                }
            }
            
            // Test transport control
            log.addSection("Transport Status");
            if (activeSession != null) {
                log.addLine("Is playing: " + activeSession.isRunning());
                log.addLine("Is recording: " + manager.isRecording());
                log.addLine("Current tick: " + activeSession.getTick());
                log.addLine("Tempo: " + activeSession.getTempoInBPM() + " BPM");
                log.addLine("Ticks per beat: " + activeSession.getTicksPerBeat());
                log.addLine("Beats per bar: " + activeSession.getBeatsPerBar());
                log.addLine("Bars: " + activeSession.getBars());
                log.addLine("Parts: " + activeSession.getParts());
                log.addLine("Transpose offset: " + activeSession.getNoteOffset() + " semitones");
            }
            
            // Test command bus registration
            log.addSection("Event System Registration");
            CommandBus commandBus = CommandBus.getInstance();
            // log.addLine("SessionManager is registered with CommandBus: " + commandBus.isRegistered(manager));
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test session integrity and contents
     */
    private void testSession(Session session, DiagnosticLogBuilder log) {
        log.addSection("Session Details");
        log.addLine("ID: " + session.getId());
        log.addLine("Tempo: " + session.getTempoInBPM() + " BPM");
        log.addLine("Time signature: " + session.getBeatsPerBar() + "/" + 4);
        log.addLine("Bars: " + session.getBars());
        log.addLine("Parts: " + session.getParts());
        
        // Check players
        Set<Player> players = session.getPlayers();
        log.addLine("Player count: " + (players != null ? players.size() : 0));
        
        if (players != null && !players.isEmpty()) {
            int drumPlayers = 0;
            int melodicPlayers = 0;
            int playersWithRules = 0;
            int playersWithInstruments = 0;
            int totalRules = 0;
            Set<Long> playerIds = new HashSet<>();
            Set<Long> duplicateIds = new HashSet<>();
            
            for (Player player : players) {
                // Count player types
                if (player.isDrumPlayer()) {
                    drumPlayers++;
                } else {
                    melodicPlayers++;
                }
                
                // Check for duplicate IDs
                if (!playerIds.add(player.getId())) {
                    duplicateIds.add(player.getId());
                }
                
                // Count players with instruments
                if (player.getInstrument() != null) {
                    playersWithInstruments++;
                }
                
                // Count rules
                if (player.getRules() != null && !player.getRules().isEmpty()) {
                    playersWithRules++;
                    totalRules += player.getRules().size();
                }
            }
            
            log.addIndentedLine("Drum players: " + drumPlayers, 1);
            log.addIndentedLine("Melodic players: " + melodicPlayers, 1);
            log.addIndentedLine("Players with instruments: " + playersWithInstruments, 1);
            log.addIndentedLine("Players with rules: " + playersWithRules, 1);
            log.addIndentedLine("Total rules: " + totalRules, 1);
            
            if (!duplicateIds.isEmpty()) {
                log.addWarning("Found " + duplicateIds.size() + " duplicate player IDs");
                for (Long id : duplicateIds) {
                    log.addIndentedLine("Duplicate ID: " + id, 1);
                }
            }
        }
        
        // Test validity
        log.addSection("Session Validity");
        boolean isValid = session.isValid();
        log.addLine("Session is valid: " + isValid);
        
        if (!isValid) {
            log.addWarning("Session is not valid, which may affect functionality");
        }
        
        // Test play/stop
        log.addSection("Transport Test");
        
        try {
            boolean wasPlaying = session.isRunning();
            
            if (!wasPlaying) {
                log.addLine("Testing play functionality...");
                session.play();
                log.addLine("Session.play() called successfully");
                log.addLine("Session is now playing: " + session.isRunning());
                
                if (!session.isRunning()) {
                    log.addWarning("Session did not start playing after play() call");
                }
                
                // If we started playback, stop it
                if (session.isRunning()) {
                    log.addLine("Testing stop functionality...");
                    session.stop();
                    log.addLine("Session.stop() called successfully");
                    log.addLine("Session is now playing: " + session.isRunning());
                    
                    if (session.isRunning()) {
                        log.addWarning("Session did not stop playing after stop() call");
                    }
                }
            } else {
                log.addLine("Session is already playing, skipping play/stop test");
            }
        } catch (Exception e) {
            log.addError("Error during transport test: " + e.getMessage());
        }
    }
    
    /**
     * Test session persistence
     */
    public DiagnosticLogBuilder testSessionPersistence() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Session Persistence Diagnostics");
        
        try {
            SessionManager manager = SessionManager.getInstance();
            Session activeSession = manager.getActiveSession();
            
            if (activeSession == null) {
                log.addError("No active session found");
                return log;
            }
            
            log.addLine("Testing persistence of session ID: " + activeSession.getId());
            
            // Save the session
            log.addSection("Save Test");
            try {
                RedisService redisService = RedisService.getInstance();
                redisService.saveSession(activeSession);
                log.addLine("Session saved successfully");
            } catch (Exception e) {
                log.addError("Failed to save session: " + e.getMessage());
                return log;
            }
            
            // Reload the session
            log.addSection("Reload Test");
            try {
                RedisService redisService = RedisService.getInstance();
                Session reloaded = redisService.findSessionById(activeSession.getId());
                
                if (reloaded == null) {
                    log.addError("Failed to reload session after saving");
                    return log;
                }
                
                log.addLine("Session reloaded successfully");
                
                // Compare attributes
                log.addLine("ID match: " + reloaded.getId().equals(activeSession.getId()));
                log.addLine("Tempo match: " + (reloaded.getTempoInBPM() == activeSession.getTempoInBPM()));
                log.addLine("Players count match: " + 
                          (reloaded.getPlayers().size() == activeSession.getPlayers().size()));
                
                // Compare player sets
                Set<Long> originalPlayerIds = new HashSet<>();
                Set<Long> reloadedPlayerIds = new HashSet<>();
                
                for (Player player : activeSession.getPlayers()) {
                    originalPlayerIds.add(player.getId());
                }
                
                for (Player player : reloaded.getPlayers()) {
                    reloadedPlayerIds.add(player.getId());
                }
                
                boolean allPlayersFound = originalPlayerIds.equals(reloadedPlayerIds);
                log.addLine("All players found in reloaded session: " + allPlayersFound);
                
                if (!allPlayersFound) {
                    log.addWarning("Player sets don't match between original and reloaded session");
                    
                    Set<Long> missingPlayers = new HashSet<>(originalPlayerIds);
                    missingPlayers.removeAll(reloadedPlayerIds);
                    
                    Set<Long> extraPlayers = new HashSet<>(reloadedPlayerIds);
                    extraPlayers.removeAll(originalPlayerIds);
                    
                    if (!missingPlayers.isEmpty()) {
                        log.addIndentedLine("Missing players in reloaded session:", 1);
                        for (Long id : missingPlayers) {
                            log.addIndentedLine("Player ID: " + id, 2);
                        }
                    }
                    
                    if (!extraPlayers.isEmpty()) {
                        log.addIndentedLine("Extra players in reloaded session:", 1);
                        for (Long id : extraPlayers) {
                            log.addIndentedLine("Player ID: " + id, 2);
                        }
                    }
                }
            } catch (Exception e) {
                log.addError("Failed to reload session: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
