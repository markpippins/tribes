package com.angrysurfer.core.redis;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class InstrumentHelper {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public InstrumentHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;

        // Configure ObjectMapper to ignore unknown properties
        // This is critical when the model evolves but old data exists
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        logger.info("InstrumentHelper initialized with ObjectMapper configured to ignore unknown fields");
    }

    public List<InstrumentWrapper> findAllInstruments() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("instrument:*").stream()
                    .map(key -> findInstrumentById(Long.parseLong(key.split(":")[1])))
                    .filter(i -> i != null)
                    .collect(Collectors.toList());
        }
    }

    public InstrumentWrapper findInstrumentById(Long id) {
        if (id == null) {
            logger.warn("Attempted to find instrument with null ID");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "instrument:" + id;
            String json = jedis.get(key);

            if (json == null) {
                logger.debug("No instrument found with ID: {}", id);
                return null;
            }

            logger.debug("Retrieved JSON for instrument {}: {}", id, json);
            return objectMapper.readValue(json, InstrumentWrapper.class);
        } catch (Exception e) {
            logger.error("Error finding instrument with ID {}", id, e);
            return null;
        }
    }

    public long getNextInstrumentId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:instrument");
        } catch (Exception e) {
            logger.error("Error getting instrument id: " + e.getMessage());
            throw new RuntimeException("Failed to save instrument", e);
        }
    }

    public void saveInstrument(InstrumentWrapper instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Skip saving default instruments
            if (Boolean.TRUE.equals(instrument.getIsDefault())) {
                logger.debug("Skipping Redis save for default instrument: {}", instrument.getName());
                return;
            }

            if (instrument.getId() == null) {
                instrument.setId(jedis.incr("seq:instrument"));
            }
            String json = objectMapper.writeValueAsString(instrument);
            jedis.set("instrument:" + instrument.getId(), json);
            logger.debug("Saved instrument to Redis: {}", instrument.getName());
        } catch (Exception e) {
            logger.error("Error saving instrument: " + e.getMessage());
            throw new RuntimeException("Failed to save instrument", e);
        }
    }

    public void deleteInstrument(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("instrument:" + id);
        } catch (Exception e) {
            logger.error("Error deleting instrument: " + e.getMessage());
            throw new RuntimeException("Failed to delete instrument", e);
        }
    }
}