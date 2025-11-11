package com.angrysurfer.core.redis;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.*;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.PlaybackService;
import com.angrysurfer.core.service.SequencerService;
import com.angrysurfer.core.util.MelodicSequenceDataDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.sound.midi.MidiDevice;
import java.util.*;

@Getter
@Setter
public class MelodicSequenceDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceDataHelper.class);
    // Constants
    private static final int MAX_STEPS = 16;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public MelodicSequenceDataHelper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        configureObjectMapper();  // Add this line
    }

    private void configureObjectMapper() {
        // Register custom deserializer for MelodicSequenceData
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MelodicSequenceData.class, new MelodicSequenceDataDeserializer());
        this.objectMapper.registerModule(module);

        // Enable more tolerant deserialization
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    /**
     * Find a melodic sequence by ID and sequencer ID
     *
     * @param id          The sequence ID
     * @param sequencerId The sequencer instance ID
     * @return The melodic sequence data or null if not found
     */
    public MelodicSequenceData findMelodicSequenceById(Long id, Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = null;
            String source = "unknown";

            // Try first with the newer key format
            json = jedis.get("melodicseq:" + sequencerId + ":" + id);
            logger.info("findMelodicSequenceById(id: {}, sequencerId: {})", id, sequencerId);
            logger.info(json);
            if (json != null) source = "melodicseq key";

            // If not found, try the older key format
            if (json == null) {
                json = jedis.get("melseq:" + sequencerId + ":" + id);
                if (json != null) source = "melseq key";
            }

            // If still not found, try looking up in the hash for faster lookup
            if (json == null) {
                json = jedis.hget("melodic-sequences:" + sequencerId, String.valueOf(id));
                if (json != null) source = "hash table";
            }

            if (json != null) {
                // Log a portion of the JSON to debug
                logger.debug("Found melodic sequence from {}, JSON excerpt: {}",
                        source, json.length() > 100 ? json.substring(0, 100) + "..." : json);

                try {
                    MelodicSequenceData data = objectMapper.readValue(json, MelodicSequenceData.class);
                    logger.info("Loaded melodic sequence {} for sequencer {} with instrument: {} ({})",
                            id, sequencerId,
                            data.getInstrumentName(),
                            data.getPreset() != null ? "Preset " + data.getPreset() : "No preset");
                    debugData(data);
                    return data;
                } catch (Exception e) {
                    logger.error("Error deserializing melodic sequence: " + e.getMessage(), e);
                    // Return null instead of throwing to make the API more robust
                    return null;
                }
            }

            // If no sequence was found, return null instead of throwing an exception
            logger.warn("No melodic sequence found for id {} and sequencer {}", id, sequencerId);
            return null;
        } catch (Exception e) {
            logger.error("Error finding melodic sequence: " + e.getMessage(), e);
            // Consider returning null instead of throwing to make the API more robust
            return null;
        }
    }

    /**
     * Apply loaded data to a MelodicSequencer including instrument settings
     */
    public void applyToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }

        try {
            // Set basic sequence ID
            // sequencer.setId(data.getId());

            // Apply pattern length
            sequencer.getSequenceData().setPatternLength(data.getPatternLength());

            // Apply direction
            sequencer.getSequenceData().setDirection(data.getDirection());

            // Apply timing division
            sequencer.getSequenceData().setTimingDivision(data.getTimingDivision());

            // Apply looping flag
            sequencer.getSequenceData().setLooping(data.isLooping());

            // Apply octave shift
            sequencer.getSequenceData().setOctaveShift(data.getOctaveShift());

            // Apply quantization settings
            sequencer.getSequenceData().setQuantizeEnabled(data.isQuantizeEnabled());
            // sequencer.setRootNote(data.getRootNote());
            sequencer.getSequenceData().setScale(data.getScale());

            // Apply steps data
            sequencer.getSequenceData().clearPattern();

            // Apply active steps
            List<Boolean> activeSteps = data.getActiveSteps();
            if (activeSteps != null) {
                for (int i = 0; i < Math.min(activeSteps.size(), MAX_STEPS); i++) {
                    if (activeSteps.get(i)) {
                        // Get corresponding note, velocity, and gate values
                        int noteValue = data.getNoteValue(i);
                        int velocityValue = data.getVelocityValue(i);
                        int gateValue = data.getGateValue(i);

                        // Set step data
                        sequencer.setStepData(i, true, noteValue, velocityValue, gateValue);
                    }
                }
            }

            // Apply harmonic tilt values
            List<Integer> tiltValues = data.getHarmonicTiltValues();
            if (tiltValues != null && !tiltValues.isEmpty()) {
                logger.info("Applying {} harmonic tilt values: {}", tiltValues.size(), tiltValues);
                sequencer.setHarmonicTiltValues(tiltValues);
            } else {
                logger.warn("No harmonic tilt values found in sequence data");

                // Initialize with defaults if missing
                int[] defaultTiltValues = new int[sequencer.getSequenceData().getPatternLength()];
                sequencer.getSequenceData().setHarmonicTiltValues(defaultTiltValues);
                logger.info("Initialized default tilt values");
            }

            // Apply mute values explicitly
            List<Integer> muteValues = data.getMuteValues();
            if (muteValues != null && !muteValues.isEmpty()) {
                logger.info("Applying {} mute values from loaded sequence", muteValues.size());
                sequencer.setMuteValues(muteValues);
            } else {
                logger.warn("No mute values found in sequence data, initializing defaults");

                // Initialize with defaults if missing
                int[] defaultMuteValues = new int[sequencer.getSequenceData().getPatternLength()];
                sequencer.getSequenceData().setMuteValues(defaultMuteValues);
                logger.info("Initialized default mute values");
            }

            // Handle player association using the stored player ID
            if (data.getPlayerId() != null) {
                Player existingPlayer = sequencer.getPlayer();

                // If the stored player ID is different from the current player
                if (existingPlayer == null || !data.getPlayerId().equals(existingPlayer.getId())) {
                    logger.info("Sequence data has player ID: {} which differs from current: {}",
                            data.getPlayerId(),
                            existingPlayer != null ? existingPlayer.getId() : "none");

                    // Try to find and use the player with the stored ID
                    Player storedPlayer = PlaybackService.getInstance().getPlayer(data.getPlayerId());
                    if (storedPlayer != null) {
                        logger.info("Found player with ID: {}, applying to sequencer", data.getPlayerId());
                        sequencer.setPlayer(storedPlayer); // Assuming there's a setPlayer method or similar
                    } else {
                        logger.warn("Player with ID {} not found, keeping current player", data.getPlayerId());
                        // Keep using the current player, but update the stored ID for consistency
                        if (existingPlayer != null) {
                            data.setPlayerId(existingPlayer.getId());
                        }
                    }
                }
            } else if (sequencer.getPlayer() != null) {
                // If no player ID in data but sequencer has a player, update the data
                data.setPlayerId(sequencer.getPlayer().getId());
                logger.info("Updated sequence data with current player ID: {}",
                        sequencer.getPlayer().getId());
            }

            // Apply instrument settings to the player with better error handling
            if (sequencer.getPlayer() != null) {
                Player player = sequencer.getPlayer();

                if (player.getInstrument() != null) {
                    InstrumentWrapper instrument = player.getInstrument();

                    // Log the before state
                    logger.info("Before applying - Instrument settings: soundbank='{}', bank={}, preset={}",
                            instrument.getSoundbankName(), instrument.getBankIndex(), instrument.getPreset());

                    // Log what we're loading from saved data
                    logger.info("Loaded values from data: soundbank='{}', bank={}, preset={}",
                            data.getSoundbankName(), data.getBankIndex(), data.getPreset());

                    // Only apply if values are present and valid
                    boolean settingsChanged = false;

                    if (data.getSoundbankName() != null && !data.getSoundbankName().isEmpty()) {
                        instrument.setSoundbankName(data.getSoundbankName());
                        settingsChanged = true;
                    }

                    if (data.getBankIndex() != null && data.getBankIndex() >= 0) {
                        instrument.setBankIndex(data.getBankIndex());
                        settingsChanged = true;
                    }

                    if (data.getPreset() != null && data.getPreset() >= 0) {
                        instrument.setPreset(data.getPreset());
                        settingsChanged = true;
                    }

                    // Device name and other identifiers
                    if (data.getDeviceName() != null && !data.getDeviceName().isEmpty()) {
                        instrument.setDeviceName(data.getDeviceName());
                    }

                    if (data.getInstrumentName() != null && !data.getInstrumentName().isEmpty()) {
                        instrument.setName(data.getInstrumentName());
                    }

                    logger.info("After applying - Instrument settings: soundbank='{}', bank={}, preset={}",
                            instrument.getSoundbankName(), instrument.getBankIndex(), instrument.getPreset());

                    // Only send MIDI program changes if settings actually changed
                    if (settingsChanged) {
                        try {
                            logger.info("Applying instrument preset changes to MIDI device");
                            PlaybackService.getInstance().applyPreset(player);

                            // Verify the changes were applied
                            logger.info("Verified final settings: soundbank='{}', bank={}, preset={}",
                                    player.getInstrument().getSoundbankName(),
                                    player.getInstrument().getBankIndex(),
                                    player.getInstrument().getPreset());

                        } catch (Exception e) {
                            logger.error("Error applying program change: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.info("No instrument settings changes needed");
                    }
                } else {
                    logger.warn("Player has no instrument to apply settings to");
                }
            } else {
                logger.warn("No player available to apply instrument settings");
            }

            // Notify that pattern has updated
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_LOADED,
                    this,
                    new MelodicSequencerEvent(sequencer.getId(), data.getId()));

        } catch (Exception e) {
            logger.error("Error applying sequence data: " + e.getMessage(), e);
        }
    }

    /**
     * Apply melodic sequence data to a sequencer
     */
    public void applyMelodicSequenceToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }

        try {
            // Store current playback state
            boolean wasPlaying = sequencer.isPlaying();

            // Apply the sequence data
            sequencer.setSequenceData(data);

            // Log the mute values after setting the data
            logger.info("Mute values after applying: {}", sequencer.getMuteValues());

            // Update instrument settings if possible
            if (sequencer.getPlayer() != null) {
                Player player = sequencer.getPlayer();
                InstrumentWrapper instrument = player.getInstrument();

                // If no instrument, try to get by ID
                if (instrument == null && data.getInstrumentId() != null) {
                    instrument = PlaybackService.getInstance().getInstrument(data.getInstrumentId());
                    if (instrument != null) {
                        player.setInstrument(instrument);
                        player.setInstrumentId(instrument.getId());
                    }
                }

                // If we have an instrument, apply the saved settings
                if (instrument != null) {
                    // Update from saved data
                    if (data.getPreset() != null) {
                        instrument.setPreset(data.getPreset());
                    }

                    if (data.getBankIndex() != null) {
                        instrument.setBankIndex(data.getBankIndex());
                    }

                    if (data.getSoundbankName() != null) {
                        instrument.setSoundbankName(data.getSoundbankName());
                    }

                    if (data.getDeviceName() != null) {

                        // Try to reconnect to saved device
                        MidiDevice device = MidiService.getInstance().getDevice(data.getDeviceName());
                        if (device != null) {
                            try {
                                if (!device.isOpen()) {
                                    device.open();
                                }

                                instrument.setDevice(device);
//
//                                // Get a receiver
//                                if (instrument.getReceiver() == null) {
//                                    instrument.setReceiver(ReceiverManager.getInstance()
//                                            .getOrCreateReceiver(data.getDeviceName(), device));
//                                }
                            } catch (Exception e) {
                                logger.warn("Could not connect to device {}: {}",
                                        data.getDeviceName(), e.getMessage());
                            }
                        }
                    }

                    // Apply the instrument settings
                    PlaybackService.getInstance().applyPreset(player);

                }
            }

            // Restore playback state
            if (wasPlaying) {
                sequencer.start();
            }

            // Notify that pattern has been updated
            CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCE_UPDATED, this,
                    Map.of("sequencerId", sequencer.getId(), "sequenceId", data.getId()));

        } catch (Exception e) {
            logger.error("Error applying melodic sequence data: " + e.getMessage(), e);
        }
    }

    /**
     * Save a melodic sequence
     */
    public void saveMelodicSequence(MelodicSequencer sequencer) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a data transfer object
            MelodicSequenceData data = sequencer.getSequenceData();

            data.setSequencerId(sequencer.getId());

            // Set or generate ID
            if (data.getId() <= 0) {
                data.setId(jedis.incr("seq:melodicsequence"));
            }

            // Save instrument settings with improved logging
            Player player = sequencer.getPlayer();
            if (player != null) {
                // Store player ID
                data.setPlayerId(player.getId());
                logger.info("Saving player ID: {} with melodic sequence", player.getId());

                if (player.getInstrument() != null) {
                    InstrumentWrapper instrument = player.getInstrument();

                    // Store all instrument information
                    data.setSoundbankName(instrument.getSoundbankName());
                    data.setPreset(instrument.getPreset());
                    data.setBankIndex(instrument.getBankIndex());
                    data.setDeviceName(instrument.getDeviceName());
                    data.setInstrumentId(instrument.getId());
                    data.setInstrumentName(instrument.getName());

                    // Detailed logging to confirm values are set
                    logger.info("Saving instrument settings: soundbank='{}', bank={}, preset={}, device='{}', name='{}'",
                            data.getSoundbankName(),
                            data.getBankIndex(),
                            data.getPreset(),
                            data.getDeviceName(),
                            data.getInstrumentName());
                } else {
                    logger.warn("No instrument available to save for sequence {}", data.getId());
                }
            }

            // Save to Redis
            String json = objectMapper.writeValueAsString(data);

            // Save to both storage formats
            jedis.set("melodicseq:" + sequencer.getId() + ":" + data.getId(), json);
            jedis.hset("melodic-sequences:" + sequencer.getId(), String.valueOf(data.getId()), json);

            logger.info("Saved melodic sequence {} for sequencer {}.",
                    data.getId(), sequencer.getId());
            debugData(data);

            // Notify listeners
            CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCE_SAVED, this,
                    Map.of("sequencerId", sequencer.getId(), "sequenceId", data.getId()));

        } catch (Exception e) {
            logger.error("Error saving melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save melodic sequence", e);
        }
    }

    public void debugData(MelodicSequenceData data) throws JsonProcessingException {
        logger.info("ID: {}, Sequencer: {}, Root: {}, Scale: {}, Instrument: {}, Device: {}, Follow: {}, Looping: {}",
                data.getId(), data.getSequencerId(), data.getInstrumentName(), data.getDeviceName(),
                data.getFollowNoteSequencerId(), data.getRootNote(), data.getScale(), data.getLooping());

        logger.info("note values:");
        logger.info(objectMapper.writeValueAsString(data.getNoteValues()));
        logger.info("gate values:");
        logger.info(objectMapper.writeValueAsString(data.getGateValues()));
        logger.info("active steps:");
        logger.info(objectMapper.writeValueAsString(data.getActiveSteps()));
        logger.info("velocity values:");
        logger.info(objectMapper.writeValueAsString(data.getVelocityValues()));
        logger.info("tilt:");
        logger.info(objectMapper.writeValueAsString(data.getTiltValues()));
        logger.info("mutes:");
        logger.info(objectMapper.writeValueAsString(data.getMuteValues()));
    }

    /**
     * Get all melodic sequence IDs for a specific sequencer
     *
     * @param sequencerId The sequencer ID to get sequences for
     * @return A list of sequence IDs
     */
    public List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Use a Set to avoid duplicate IDs
            Set<Long> uniqueIds = new HashSet<>();

            // Check older key format
            Set<String> oldKeys = jedis.keys("melseq:" + sequencerId + ":*");
            for (String key : oldKeys) {
                try {
                    uniqueIds.add(Long.parseLong(key.split(":")[2]));
                } catch (NumberFormatException e) {
                    logger.error("Invalid melodic sequence key: " + key);
                }
            }

            // Check newer key format
            Set<String> newKeys = jedis.keys("melodicseq:" + sequencerId + ":*");
            for (String key : newKeys) {
                try {
                    uniqueIds.add(Long.parseLong(key.split(":")[2]));
                } catch (NumberFormatException e) {
                    logger.error("Invalid melodic sequence key: " + key);
                }
            }

            // Check hash storage
            Map<String, String> hashEntries = jedis.hgetAll("melodic-sequences:" + sequencerId);
            for (String idStr : hashEntries.keySet()) {
                try {
                    uniqueIds.add(Long.parseLong(idStr));
                } catch (NumberFormatException e) {
                    logger.error("Invalid melodic sequence hash key: " + idStr);
                }
            }

            logger.info("Found {} melodic sequences for sequencer {}", uniqueIds.size(), sequencerId);
            return new ArrayList<>(uniqueIds);
        }
    }

    /**
     * Get the minimum melodic sequence ID
     */
    public Long getMinimumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[2]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the maximum melodic sequence ID
     */
    public Long getMaximumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[2]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Delete a melodic sequence
     */
    public void deleteMelodicSequence(Integer sequencerId, Long melSequenceId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("melseq:" + sequencerId + ":" + melSequenceId);
            logger.info("Deleted melodic sequence {} for sequencer {}", melSequenceId, sequencerId);

            // Notify listeners
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_REMOVED,
                    this,
                    new MelodicSequencerEvent(sequencerId, melSequenceId));
        } catch (Exception e) {
            logger.error("Error deleting melodic sequence {}: {}", melSequenceId, e.getMessage());
            throw new RuntimeException("Failed to delete melodic sequence", e);
        }
    }

    /**
     * Create a new empty melodic sequence
     */
    public MelodicSequenceData newMelodicSequence(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new melodic sequence for sequencer {}", sequencerId);
            MelodicSequenceData data = new MelodicSequenceData();
            data.setId(jedis.incr("seq:melsequence:" + sequencerId));
            data.setSequencerId(sequencerId);

            // Try to get the player ID from the current sequencer
            MelodicSequencer sequencer = SequencerService.getInstance().getMelodicSequencer(sequencerId);
            if (sequencer != null && sequencer.getPlayer() != null) {
                data.setPlayerId(sequencer.getPlayer().getId());
                logger.info("Set player ID: {} on new melodic sequence", sequencer.getPlayer().getId());
            }

            // Set default values
            data.setPatternLength(16);
            data.setDirection(Direction.FORWARD);
            data.setTimingDivision(TimingDivision.NORMAL);
            data.setLooping(true);
            data.setOctaveShift(0);

            // Default quantization settings
            data.setQuantizeEnabled(true);
            data.setRootNote(60);
            data.setScale(Scale.SCALE_CHROMATIC);
            data.setFollowNoteSequencerId(-1);
            data.setFollowTiltSequencerId(-1);

            // Initialize pattern data with arrays and SET THEM on the data object
            boolean[] activeSteps = new boolean[16];
            int[] noteValues = new int[16];
            int[] velocityValues = new int[16];
            int[] gateValues = new int[16];
            int[] harmonicTiltValues = new int[16]; // Create tilt values array
            int[] muteValues = new int[16]; // Create tilt values array
            int[] nudgeValues = new int[16]; // Create tilt values array
            int[] probabilityValues = new int[16]; // Create tilt values array

            // Initialize values
            for (int i = 0; i < 16; i++) {
                activeSteps[i] = false;
                noteValues[i] = 60 + (i % 12);
                velocityValues[i] = 100;
                gateValues[i] = 50;
                harmonicTiltValues[i] = 0;
                muteValues[i] = 0;
                nudgeValues[i] = 0;
                probabilityValues[i] = 100;
            }

            // SET ALL arrays on the data object
            data.setActiveSteps(activeSteps);
            data.setNoteValues(noteValues);
            data.setVelocityValues(velocityValues);
            data.setGateValues(gateValues);
            data.setHarmonicTiltValues(harmonicTiltValues); // Don't forget this line!
            data.setMuteValues(muteValues);
            data.setNudgeValues(nudgeValues);
            data.setProbabilityValues(probabilityValues);

            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("melseq:" + sequencerId + ":" + data.getId(), json);

            logger.info("Created new melodic sequence with ID: {} for sequencer {}", data.getId(), sequencerId);
            return data;
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create new melodic sequence", e);
        }
    }

    /**
     * Get the previous melodic sequence ID
     */
    public Long getPreviousMelodicSequenceId(Integer sequencerId, Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .filter(id -> id < currentId)
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the next melodic sequence ID
     */
    public Long getNextMelodicSequenceId(Integer sequencerId, Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .filter(id -> id > currentId)
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

}