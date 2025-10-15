package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.core.service.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RuleHelper {
    private static final Logger logger = LoggerFactory.getLogger(RuleHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RuleHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public Rule findRuleById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("rule:" + id);
            return json != null ? objectMapper.readValue(json, Rule.class) : null;
        } catch (Exception e) {
            logger.error("Error finding rule: " + e.getMessage());
            return null;
        }
    }

    public Set<Rule> findRulesForPlayer(Long playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Rule> rules = new HashSet<>();
            String rulesKey = "player:" + playerId + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);

            for (String id : ruleIds) {
                Rule rule = findRuleById(Long.valueOf(id));
                if (rule != null) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    public void saveRule(Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (rule.getId() == null) {
                rule.setId(jedis.incr("seq:rule"));
            }

            // Temporarily remove circular references
            Player player = rule.getPlayer();
            rule.setPlayer(null);

            // Save the rule
            String json = objectMapper.writeValueAsString(rule);
            jedis.set("rule:" + rule.getId(), json);

            // Update player-rule relationship
            if (player != null) {
                String rulesKey = "player:" + player.getId() + ":rules";
                jedis.sadd(rulesKey, rule.getId().toString());
            }

            // Restore references
            rule.setPlayer(player);
        } catch (Exception e) {
            logger.error("Error saving rule: " + e.getMessage());
            throw new RuntimeException("Failed to save rule", e);
        }
    }

    // public static boolean deleteRule(Long ruleId) {
    // try {
    // logger.info("Deleting rule with ID: " + ruleId);
    // RedisService redis = RedisService.getInstance();

    // // Delete rule from Redis
    // long result = redis.deleteKey("rule:" + ruleId);
    // boolean success = result > 0;

    // logger.info("Rule deletion result: " + (success ? "SUCCESS" : "FAILED"));
    // return success;
    // } catch (Exception e) {
    // logger.error("Error deleting rule: " + e.getMessage());
    // // logger.debug("{}", e, e);
    // return false;
    // }
    // }

    public void deleteRule(Long ruleId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Rule rule = findRuleById(ruleId);
            if (rule != null && rule.getPlayer() != null) {
                String rulesKey = "player:" + rule.getPlayer().getId() + ":rules";
                jedis.srem(rulesKey, ruleId.toString());
            }
            jedis.del("rule:" + ruleId);
        } catch (Exception e) {
            logger.error("Error deleting rule: " + e.getMessage());
            throw new RuntimeException("Failed to delete rule", e);
        }
    }

    public Long getNextRuleId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:rule");
        } catch (Exception e) {
            logger.error("Error getting next rule ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next rule ID", e);
        }
    }

    public boolean isValidNewRule(Player player, Rule newRule) {
        if (player == null || player.getRules() == null || newRule == null) {
            return false;
        }

        // Check if there's already a rule with the same operator and part
        return player.getRules().stream()
                .noneMatch(existingRule -> existingRule.getComparison() == newRule.getComparison()
                        && existingRule.getOperator() == newRule.getOperator()
                        && existingRule.getPart() == newRule.getPart());
    }

    public void addRuleToPlayer(Player player, Rule rule) {
        if (!isValidNewRule(player, rule)) {
            throw new IllegalArgumentException("A rule with this operator already exists for part "
                    + (rule.getPart() == 0 ? "All" : rule.getPart()));
        }

        try (Jedis jedis = jedisPool.getResource()) {
            saveRule(rule);
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.sadd(rulesKey, rule.getId().toString());
            if (player.getRules() == null) {
                player.setRules(new HashSet<>());
            }
            player.addRule(rule);
        }
    }

    public void removeRuleFromPlayer(Player player, Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.srem(rulesKey, rule.getId().toString());

            // Update player's rules set
            if (player.getRules() != null) {
                player.removeRule(rule);
            }

            // Delete the actual rule from Redis
            jedis.del("rule:" + rule.getId());
        }
    }

    public Player findPlayerForRule(Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("player:*");
            for (String playerKey : playerKeys) {
                String playerId = playerKey.split(":")[1];
                String rulesKey = "player:" + playerId + ":rules";
                if (jedis.sismember(rulesKey, rule.getId().toString())) {
                    // Use RedisService to find player to avoid circular dependency
                    return RedisService.getInstance().findPlayerById(Long.valueOf(playerId));
                }
            }
        }
        return null;
    }

    public Rule createNewRule() {
        try (Jedis jedis = jedisPool.getResource()) {
            Rule rule = new Rule();
            rule.setId(jedis.incr("seq:rule"));
            saveRule(rule);
            return rule;
        }
    }
}