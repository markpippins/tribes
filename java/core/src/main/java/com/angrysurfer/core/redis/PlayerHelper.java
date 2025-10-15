package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class PlayerHelper {
    private static final Logger logger = LoggerFactory.getLogger(PlayerHelper.class.getName());

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RuleHelper ruleHelper;

    public PlayerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.ruleHelper = new RuleHelper(jedisPool, objectMapper);
    }

    private String getPlayerKey(String className, Long id) {
        return String.format("player:%s:%d", className.toLowerCase(), id);
    }

    /**
     * Find a player by ID, automatically determining the player type
     * 
     * @param id The player ID to look up
     * @return The player with the specified ID, or null if not found
     */
    public Player findPlayerById(Long id) {
        if (id == null) {
            logger.warn("findPlayerById called with null ID");
            return null;
        }

        logger.debug("Looking up player by ID: {}", id);

        // Try to find the player as a Note first
        Player player = findPlayerById(id, "Note");

        // If not found, try as a Strike
        if (player == null) {
            player = findPlayerById(id, "Strike");
        }

        // Log result
        if (player != null) {
            logger.debug("Found player: {} (ID: {}, Type: {})",
                    player.getName(), player.getId(), player.getPlayerClassName());
        } else {
            logger.debug("No player found with ID: {}", id);
        }

        return player;
    }

    public Player findPlayerById(Long id, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Normalize the class name (capitalize first letter for consistency)
            String normalizedClassName = className;
            if (className != null && !className.isEmpty()) {
                normalizedClassName = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
            }

            String json = jedis.get(getPlayerKey(normalizedClassName, id));
            if (json != null) {
                // Dynamically determine class based on normalized className parameter
                Class<? extends Player> playerClass;
                switch (normalizedClassName) {
                    case "Note":
                        playerClass = Note.class;
                        break;
                    case "Strike":
                        playerClass = Strike.class;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported player class: " + className);
                }

                // Use the correct class for deserialization
                Player player = objectMapper.readValue(json, playerClass);

                // Load rules for this player
                Set<String> ruleIds = jedis.smembers("player:" + id + ":rules");
                if (!ruleIds.isEmpty()) {
                    Set<Rule> rules = new HashSet<>();
                    for (String ruleId : ruleIds) {
                        String ruleJson = jedis.get("rule:" + ruleId);
                        if (ruleJson != null) {
                            Rule rule = objectMapper.readValue(ruleJson, Rule.class);
                            rules.add(rule);
                        }
                    }
                    player.setRules(rules);
                } else {
                    player.setRules(new HashSet<>());
                }

                logger.info(String.format("Loaded player %d with %d rules", id, player.getRules().size()));
                return player;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding player: " + e.getMessage(), e);
            ErrorHandler.logError("PlayerHelper", "Failed to find player", e);
            throw new RuntimeException("Failed to find player", e);
        }
    }

    public Set<Player> findPlayersForSession(Long sessionId, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Player> players = new HashSet<>();
            String playersKey = String.format("session:%d:players:%s", sessionId, className);
            Set<String> playerIds = jedis.smembers(playersKey);

            for (String id : playerIds) {
                Player player = findPlayerById(Long.valueOf(id), className);
                if (player != null) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    public Long nextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    static final int CACHE_SIZE = 50;

    public Long[] getCachedPlayerIds() {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < CACHE_SIZE; i++)
            ids.add(nextPlayerId());

        return ids.toArray(new Long[ids.size()]);

    }
    // public Long[] getPlayerIdsForSession() {
    // try (Jedis jedis = jedisPool.getResource()) {
    // Set<String> keys = jedis.keys("player:*");
    // Long[] ids = new Long[keys.size()];
    // int i = 0;
    // for (String key : keys) {
    // ids[i++] = Long.valueOf(key.split(":")[2]);
    // }
    // return ids;
    // } catch (Exception e) {
    // logger.error("Error getting player IDs: " + e.getMessage());
    // throw new RuntimeException("Failed to get player IDs", e);
    // }
    // }

    /**
     * Save player to Redis
     * This handles instrument references and session updates
     */
    public void savePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (player == null) {
                logger.warn("Cannot save null player");
                return;
            }

            // Skip saving default players
            if (Boolean.TRUE.equals(player.getIsDefault())) {
                logger.debug("Skipping Redis save for default player: {}", player.getName());
                return;
            }

            logger.debug("Saving player ID: {} with name: {}", player.getId(), player.getName());

            // Save the instrument first if it exists and is not default
            if (player.getInstrument() != null && !Boolean.TRUE.equals(player.getInstrument().getIsDefault())) {
                InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
                instrumentHelper.saveInstrument(player.getInstrument());

                // Ensure the player's instrumentId is set correctly
                player.setInstrumentId(player.getInstrument().getId());

                logger.debug("Associated instrument ID: {} with name: {}",
                        player.getInstrumentId(), player.getInstrument().getName());
            }

            // If the player belongs to a session, update the session as well
            Session session = player.getSession();
            if (session != null) {
                String playersKey = String.format("session:%d:players:%s", 
                        session.getId(), player.getPlayerClassName());
                jedis.sadd(playersKey, player.getId().toString());
            }

            // Now proceed with existing player saving logic...
            if (player.getId() == null) {
                player.setId(jedis.incr("seq:player"));
            }

            // Save rules first and maintain relationships
            String rulesKey = String.format("player:%d:rules", player.getId());
            jedis.del(rulesKey); // Clear existing rules

            if (player.getRules() != null) {
                for (Rule rule : player.getRules()) {
                    if (rule.getId() == null) {
                        rule.setId(jedis.incr("seq:rule"));
                    }
                    // Save rule
                    String ruleJson = objectMapper.writeValueAsString(rule);
                    jedis.set("rule:" + rule.getId(), ruleJson);
                    // Add to player's rules set
                    jedis.sadd(rulesKey, rule.getId().toString());
                }
            }

            // Store references before removing
            Set<Rule> rules = new HashSet<>(player.getRules() != null ? player.getRules() : new HashSet<>());
            
            // Temporarily remove circular references
            player.setSession(null);
            player.setRules(null);

            // Save the player
            String json = objectMapper.writeValueAsString(player);
            String playerKey = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.set(playerKey, json);

            // Restore references
            player.setSession(session);
            player.setRules(rules);

            logger.info("Saved player {} with {} rules", player.getId(), rules.size());
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage(), e);
            ErrorHandler.logError("PlayerHelper", "Failed to save player", e);
        }
    }

    public void deletePlayerById(Long id, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            String playerKey = getPlayerKey(className, id);
            jedis.del(playerKey);
            logger.info("Deleted player with ID: " + id);
        } catch (Exception e) {
            logger.error("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    public void deletePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove player's rules
            if (player.getRules() != null) {
                player.getRules().forEach(rule -> ruleHelper.deleteRule(rule.getId()));
            }

            // Remove player from session's player set
            if (player.getSession() != null) {
                String playersKey = String.format("session:%d:players:%s",
                        player.getSession().getId(),
                        player.getPlayerClassName());
                jedis.srem(playersKey, player.getId().toString());
            }

            // Delete the player
            String key = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    public Long getNextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    public void addPlayerToSession(Session session, Player player) {
        logger.info("Adding player " + player.getId() + " to session " + session.getId());

        try {
            // Set up relationships
            player.setSession(session);
            if (session.getPlayers() == null) {
                session.setPlayers(new HashSet<>());
            }
            session.getPlayers().add(player);

            // Save both entities
            savePlayer(player);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to session " + session.getId());
        } catch (Exception e) {
            logger.error("Error adding player to session: " + e.getMessage());
            throw new RuntimeException("Failed to add player to session", e);
        }
    }

    public Player newNote() {
        Session session = SessionManager.getInstance().getActiveSession();
        Player player = new Note();
        player.initialize("Note", session, null, null);

        player.setId(getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        player.setMinVelocity(60);
        player.setMaxVelocity(127);
        player.setLevel(100);
        savePlayer(player);
        return player;

    }

    public Player newStrike() {
        Session session = SessionManager.getInstance().getActiveSession();
        Player player = new Strike();
        player.initialize("Strike", session, null, null);

        player.setId(getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        savePlayer(player);
        return player;
    }
}