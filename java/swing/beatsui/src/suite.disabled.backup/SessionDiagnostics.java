package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.SessionHelper;

import java.util.List;

/**
 * Helper class for Session diagnostics
 */
public class SessionDiagnostics {
    
    private final RedisService redisService;
    private final SessionHelper sessionHelper;
    
    /**
     * Constructor
     */
    public SessionDiagnostics() {
        this.redisService = RedisService.getInstance();
        this.sessionHelper = redisService.getSessionHelper();
    }
    
    /**
     * Run diagnostic tests for sessions
     */
    public DiagnosticLogBuilder testSessionDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Session Diagnostics");
        
        try {
            // Test 1: Count active sessions
            List<Session> sessions = sessionHelper.findAllSessions();
            log.addIndentedLine("Found " + sessions.size() + " sessions in Redis", 1);
            
            // List a few sessions for diagnostic purposes
            if (!sessions.isEmpty()) {
                log.addIndentedLine("Sample of sessions:", 1);
                int count = 0;
                for (Session session : sessions) {
                    if (count < 5) { // Only show 5 samples
                        log.addIndentedLine("Session ID: " + session.getId() + 
                                         ", Players: " + (session.getPlayers() != null ? 
                                                      session.getPlayers().size() : 0), 2);
                        count++;
                    } else {
                        break;
                    }
                }
            }
            
            // Test 2: Create and delete a test session
            log.addSection("Test: Create and delete session");
            
            log.addIndentedLine("Creating a new test session", 1);
            Session testSession = sessionHelper.newSession();
            Long sessionId = testSession.getId();
            
            if (sessionId != null) {
                log.addIndentedLine("Successfully created test session with ID: " + sessionId, 1);
            } else {
                log.addError("Failed to create test session with valid ID");
                return log;
            }
            
            // Verify the session was created
            Session retrievedSession = sessionHelper.findSessionById(sessionId);
            if (retrievedSession != null) {
                log.addIndentedLine("Successfully retrieved test session by ID", 1);
            } else {
                log.addError("Failed to retrieve created test session");
            }
            
            // Test session existence
            boolean exists = sessionHelper.sessionExists(sessionId);
            log.addIndentedLine("Session exists check: " + exists, 1);
            if (!exists) {
                log.addError("Session existence check failed for created session");
            }
            
            // Delete the test session
            log.addIndentedLine("Deleting test session", 1);
            sessionHelper.deleteSession(sessionId);
            
            // Verify deletion
            retrievedSession = sessionHelper.findSessionById(sessionId);
            exists = sessionHelper.sessionExists(sessionId);
            
            if (retrievedSession == null && !exists) {
                log.addIndentedLine("Verified session was successfully deleted", 1);
            } else {
                log.addError("Session still exists after deletion attempt");
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test session navigation
     */
    public DiagnosticLogBuilder testSessionNavigation() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Session Navigation Test");
        
        try {
            // Get session IDs
            Long minId = sessionHelper.getMinimumSessionId();
            Long maxId = sessionHelper.getMaximumSessionId();
            
            log.addIndentedLine("Minimum session ID: " + minId, 1);
            log.addIndentedLine("Maximum session ID: " + maxId, 1);
            
            if (minId != null && maxId != null) {
                // Create a session in the middle to test navigation
                Session session = sessionHelper.newSession();
                log.addIndentedLine("Created test session with ID: " + session.getId(), 1);
                
                Long prevId = sessionHelper.getPreviousSessionId(session);
                Long nextId = sessionHelper.getNextSessionId(session);
                
                log.addIndentedLine("Previous session ID: " + prevId, 1);
                log.addIndentedLine("Next session ID: " + nextId, 1);
                
                // Clean up test session
                sessionHelper.deleteSession(session.getId());
                log.addIndentedLine("Deleted test session", 1);
            } else {
                log.addWarning("No sessions found for navigation test");
            }
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Test finding valid sessions
     */
    public DiagnosticLogBuilder testSessionValidity() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Session Validity Test");
        
        try {
            // Get a valid session
            Session validSession = sessionHelper.findFirstValidSession();
            
            if (validSession != null) {
                log.addIndentedLine("Found valid session with ID: " + validSession.getId(), 1);
            } else {
                log.addWarning("No valid sessions found");
            }
            
            // Test clear invalid sessions function
            log.addSection("Test: Clear invalid sessions");
            log.addIndentedLine("Running clearInvalidSessions...", 1);
            
            List<Session> beforeCount = sessionHelper.findAllSessions();
            sessionHelper.clearInvalidSessions();
            List<Session> afterCount = sessionHelper.findAllSessions();
            
            log.addIndentedLine("Sessions before: " + beforeCount.size(), 1);
            log.addIndentedLine("Sessions after: " + afterCount.size(), 1);
            log.addIndentedLine("Removed " + (beforeCount.size() - afterCount.size()) + " invalid sessions", 1);
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}
