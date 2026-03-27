package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.util.ErrorHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class SessionHelper {
    private static final Logger logger = LoggerFactory.getLogger(SessionHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final PlayerHelper playerHelper;

    public SessionHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;

        // Configure ObjectMapper to handle empty beans and ignore unknown properties
        this.objectMapper = objectMapper;
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.playerHelper = new PlayerHelper(jedisPool, objectMapper);

        logger.info("SessionHelper initialized with configured ObjectMapper");
    }

    public Session findSessionById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("session:" + id);
            if (json != null) {
                Session session = objectMapper.readValue(json, Session.class);

                // Initialize players set if null
                if (session.getPlayers() == null) {
                    session.setPlayers(new HashSet<>());
                }

                // Check for different player types
                String[] playerTypes = {"Strike", "Note"};

                for (String playerType : playerTypes) {
                    // Load players for this session (using a consistent key format)
                    String playerSetKey = "session:" + id + ":players:" + playerType.toLowerCase();
                    Set<String> playerIds = jedis.smembers(playerSetKey);

                    if (!playerIds.isEmpty()) {
                        logger.info("Found {} {} players for session {}", playerIds.size(), playerType, id);

                        for (String playerId : playerIds) {
                            Player player = playerHelper.findPlayerById(Long.parseLong(playerId), playerType);
                            if (player != null) {
                                player.setSession(session);
                                session.getPlayers().add(player);
                            }
                        }
                    }
                }

                logger.info(String.format("Loaded session %d with %d players", id, session.getPlayers().size()));
                return session;
            }
            return null;
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error finding session: " + e.getMessage(), e);
            throw new RuntimeException("Failed to find session", e);
        }
    }

    public void saveSession(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (session.getId() == null) {
                session.setId(jedis.incr("seq:session"));
            }

            // Clear existing player relationships for all types
            String strikePlayerSetKey = "session:" + session.getId() + ":players:strike";
            String notePlayerSetKey = "session:" + session.getId() + ":players:note";
            jedis.del(strikePlayerSetKey);
            jedis.del(notePlayerSetKey);

            // Save player relationships by type
            if (session.getPlayers() != null) {
                session.getPlayers().forEach(player -> {
                    String className = player.getClass().getSimpleName().toLowerCase();
                    String playerSetKey = "session:" + session.getId() + ":players:" + className;
                    jedis.sadd(playerSetKey, player.getId().toString());

                    logger.debug("Added player {} of type {} to session {}",
                            player.getId(), className, session.getId());
                });
            }

            // Temporarily remove circular references
            Set<Player> players = session.getPlayers();
            session.setPlayers(null);

            // Save session
            String json = objectMapper.writeValueAsString(session);
            jedis.set("session:" + session.getId(), json);

            // Restore references
            session.setPlayers(players);

            logger.info(String.format("Saved session %d with %d players",
                    session.getId(),
                    players != null ? players.size() : 0));
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error saving session: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save session", e);
        }
    }

    public List<Long> getAllSessionIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                try {
                    ids.add(Long.parseLong(key.split(":")[1]));
                } catch (NumberFormatException e) {
                    ErrorHandler.logError("SessionHelper", "Invalid session key: " + key);
                }
            }
            return ids;
        }
    }

    public Long getMinimumSessionId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getMaximumSessionId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public void deleteSession(Long sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Session session = findSessionById(sessionId);
            if (session == null)
                return;

            // Delete all players in the session
            if (session.getPlayers() != null) {
                session.getPlayers().forEach(player -> playerHelper.deletePlayer(player));
            }

            // Delete the session itself
            jedis.del("session:" + sessionId);

            // Notify via command bus
            CommandBus.getInstance().publish(Commands.SESSION_DELETED, this, sessionId);
            logger.info("Successfully deleted session " + sessionId + " and all related entities");
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error deleting session " + sessionId + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    public Session newSession() {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new session");
            Session session = new Session();
            session.setId(jedis.incr("seq:session"));

            // Set default values
            session.setTempoInBPM(120F);
            session.setTicksPerBeat(24);
            session.setBeatsPerBar(4);
            session.setBars(4);
            session.setParts(16);

            // Initialize players set
            session.setPlayers(new HashSet<>());

            // Save the new session
            saveSession(session);
            logger.info("Created new session with ID: " + session.getId());
            return session;
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error creating new session: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create new session", e);
        }
    }

    public Long getPreviousSessionId(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id < session.getId())
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getNextSessionId(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id > session.getId())
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public void clearInvalidSessions() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            for (String key : keys) {
                Session session = findSessionById(Long.parseLong(key.split(":")[1]));
                if (session != null && !session.isValid()) {
                    deleteSession(session.getId());
                }
            }
        }
    }

    public boolean sessionExists(Long sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("session:" + sessionId);
        }
    }

    public Session findFirstValidSession() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            for (String key : keys) {
                Session session = findSessionById(Long.parseLong(key.split(":")[1]));
                if (session != null && session.isValid()) {
                    return session;
                }
            }
            return null;
        }
    }

    /**
     * Find the session containing a specific player
     */
    public Session findSessionForPlayer(Player player) {
        if (player == null || player.getId() == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Determine player type from player class
            String playerType = player.getClass().getSimpleName().toLowerCase();
            logger.debug("Finding session for player {} of type {}", player.getId(), playerType);

            Set<String> sessionKeys = jedis.keys("session:*");
            for (String sessionKey : sessionKeys) {
                if (!sessionKey.contains(":players")) {
                    String sessionId = sessionKey.split(":")[1];
                    String playersKey = "session:" + sessionId + ":players:" + playerType;

                    if (jedis.sismember(playersKey, player.getId().toString())) {
                        logger.info("Found session {} for player {} of type {}",
                                sessionId, player.getId(), playerType);
                        return findSessionById(Long.valueOf(sessionId));
                    }
                }
            }

            logger.warn("No session found for player {} of type {}", player.getId(), playerType);
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error finding session for player: " + e.getMessage(), e);
        }
        return null;
    }


    /**
     * Helper method to add a player to a session
     */
    public void addPlayerToSession(Session session, Player player) {
        if (session == null || player == null) {
            logger.warn("Cannot add player to session: null reference");
            return;
        }

        // Skip default players
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.info("Skipping default player: not adding to session");
            return;
        }

        try {
            // Set up relationships
            player.setSession(session);
            if (session.getPlayers() == null) {
                session.setPlayers(new HashSet<>());
            }
            session.getPlayers().add(player);

            // Save both entities
            playerHelper.savePlayer(player);
            saveSession(session);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to session " + session.getId());
        } catch (Exception e) {
            ErrorHandler.logError("SessionHelper", "Error adding player to session: " + e.getMessage(), e);
            throw new RuntimeException("Failed to add player to session", e);
        }
    }

    /**
     * Find all sessions
     */
    public List<Session> findAllSessions() {
        List<Session> sessions = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");

            for (String key : keys) {
                try {
                    String json = jedis.get(key);
                    Session session = objectMapper.readValue(json, Session.class);
                    if (session != null) {
                        sessions.add(session);
                    }
                } catch (Exception e) {
                    // Skip malformed sessions
                    System.err.println("Error loading session: " + e.getMessage());
                }
            }
        }

        return sessions;
    }

}
