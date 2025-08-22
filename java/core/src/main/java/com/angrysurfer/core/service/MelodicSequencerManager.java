package com.angrysurfer.core.service;

import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.DefaultBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.MelodicSequenceDataHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for MelodicSequencer instances. Maintains a collection of sequencers
 * and provides methods to create and access them.
 */
public class MelodicSequencerManager extends DefaultBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerManager.class);

    private static MelodicSequencerManager instance;

    // Store sequencers in a Map for access by ID
    private final Map<Integer, MelodicSequencer> sequencerMap;

    // Also maintain the list for backwards compatibility
    private final List<MelodicSequencer> sequencers;

    // Map to store sequence data by sequencer ID
    private final Map<Integer, Map<Long, MelodicSequenceData>> sequenceDataMap;
    boolean init = false;

    // Private constructor for singleton pattern
    private MelodicSequencerManager() {
        sequencerMap = new ConcurrentHashMap<>();
        sequencers = new ArrayList<>();
        sequenceDataMap = new ConcurrentHashMap<>();

        // Initialize with empty data maps for common sequencer IDs
        for (int i = 0; i < SequencerConstants.MELODIC_CHANNELS.length; i++) {
            sequenceDataMap.put(i, new HashMap<>());
        }

        loadAllMelodicSequenceData();

        registerCommand(Commands.SAVE_ALL_MELODIC, this::handleSaveAll);
    }

    // Singleton access method
    public static synchronized MelodicSequencerManager getInstance() {
        if (instance == null) {
            instance = new MelodicSequencerManager();
        }
        return instance;
    }

    /**
     * Create a new sequencer with specified ID and channel
     *
     * @param id The sequencer ID
     * @return A new MelodicSequencer instance
     */
    public MelodicSequencer newSequencer(int id) {
        // Create a new sequencer with the specified ID and channel
        MelodicSequencer sequencer = new MelodicSequencer(id);
        sequencer.setSequenceData(new MelodicSequenceData());

        // Add to both map and list
        sequencerMap.put(id, sequencer);
        sequencers.add(sequencer);

        CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCER_ADDED, this, sequencer);

        logger.info("Created new melodic sequencer with ID {} on channel {}", id,
                SequencerConstants.MELODIC_CHANNELS[id % SequencerConstants.MELODIC_CHANNELS.length]);
        return sequencer;
    }

    /**
     * Get a sequencer by its ID.
     *
     * @param id The ID of the sequencer
     * @return The MelodicSequencer with the specified ID, or null if not found
     */
    public synchronized MelodicSequencer getSequencerById(int id) {
        return sequencerMap.get(id);
    }

    /**
     * Get a sequencer by its index in the collection. This maintained for
     * backward compatibility.
     *
     * @param index The index of the sequencer
     * @return The MelodicSequencer at the specified index, or null if not found
     */
    public synchronized MelodicSequencer getSequencer(int index) {
        if (index >= 0 && index < sequencers.size()) {
            return sequencers.get(index);
        }
        logger.warn("Sequencer with index {} not found", index);
        return null;
    }

    /**
     * Get the number of sequencers currently managed.
     *
     * @return The number of sequencers
     */
    public synchronized int getSequencerCount() {
        return sequencerMap.size();
    }

    /**
     * Get an unmodifiable view of all sequencers.
     *
     * @return Unmodifiable list of all sequencers
     */
    public synchronized List<MelodicSequencer> getAllSequencers() {
        return Collections.unmodifiableList(sequencers);
    }

    /**
     * Load all available melodic sequence data from Redis into memory. This
     * populates the sequenceDataMap for more efficient access.
     */
    public void loadAllMelodicSequenceData() {
        // Clear existing data
        sequenceDataMap.values().forEach(Map::clear);
        if (Constants.DEBUG) {
            for (int sequencerId = 0; sequencerId < SequencerConstants.MELODIC_CHANNELS.length; sequencerId++) {
                int finalSequencerId = sequencerId;
                RedisService.getInstance().getMelodicSequencerHelper().getAllMelodicSequenceIds(finalSequencerId)
                        .forEach(id -> RedisService.getInstance().getMelodicSequencerHelper()
                                .deleteMelodicSequence(finalSequencerId, id));
            }
        }

        // For each possible sequencer ID
        for (int sequencerId = 0; sequencerId < SequencerConstants.MELODIC_CHANNELS.length; sequencerId++) {
            // Get all sequence IDs for this sequencer
            List<Long> ids = RedisService.getInstance().getAllMelodicSequenceIds(sequencerId);

            if (ids == null || ids.isEmpty()) {
                logger.info("No melodic sequences found for sequencer ID {}", sequencerId);
                continue;
            }

            logger.info("Loading {} melodic sequences for sequencer ID {}", ids.size(), sequencerId);

            // Load each sequence and add to the map
            for (Long id : ids) {
                try {
                    MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(id, sequencerId);
                    if (data != null) {
                        sequenceDataMap.get(sequencerId).put(id, data);
                        logger.debug("Loaded sequence ID {} for sequencer {}", id, sequencerId);
                    } else {
                        logger.warn("Failed to load sequence ID {} for sequencer {}", id, sequencerId);
                    }
                } catch (Exception e) {
                    logger.error("Error loading sequence ID {} for sequencer {}: {}",
                            id, sequencerId, e.getMessage(), e);
                }
            }

            logger.info("Successfully loaded {} melodic sequences for sequencer ID {}",
                    sequenceDataMap.get(sequencerId).size(), sequencerId);
        }
    }

    /**
     * Get the sequence data for a specific sequencer and sequence ID. This uses
     * the in-memory map if available, otherwise loads from Redis.
     *
     * @param sequencerId The sequencer ID
     * @param sequenceId  The sequence ID
     * @return The MelodicSequenceData, or null if not found
     */
    public MelodicSequenceData getSequenceData(int sequencerId, long sequenceId) {
        // Check if we have it in our map
        Map<Long, MelodicSequenceData> sequencerDataMap = sequenceDataMap.get(sequencerId);
        if (sequencerDataMap != null && sequencerDataMap.containsKey(sequenceId)) {
            return sequencerDataMap.get(sequenceId);
        }

        // Not found in map, try loading from Redis
        MelodicSequenceData data = RedisService.getInstance().findMelodicSequenceById(sequenceId, sequencerId);

        // If found, add to our map for future reference
        if (data != null && sequencerDataMap != null) {
            sequencerDataMap.put(sequenceId, data);
        }

        return data;
    }

    /**
     * Get all sequence data for a specific sequencer.
     *
     * @param sequencerId The sequencer ID
     * @return A map of sequence ID to sequence data
     */
    public Map<Long, MelodicSequenceData> getAllSequenceData(int sequencerId) {
        // Make sure the map exists
        if (!sequenceDataMap.containsKey(sequencerId)) {
            sequenceDataMap.put(sequencerId, new HashMap<>());
        }

        // Return a copy to prevent modification
        return new HashMap<>(sequenceDataMap.get(sequencerId));
    }

    /**
     * Check if there are any sequences available for a specific sequencer
     *
     * @param sequencerId The sequencer ID to check for
     * @return true if sequences exist, false otherwise
     */
    public boolean hasSequences(Integer sequencerId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return true;
        }

        // Fall back to checking Redis
        List<Long> ids = RedisService.getInstance().getAllMelodicSequenceIds(sequencerId);
        return ids != null && !ids.isEmpty();
    }

    /**
     * Get the first sequence ID for a specific sequencer
     *
     * @param sequencerId The sequencer ID
     * @return The first ID or null if none
     */
    public Long getFirstSequenceId(Integer sequencerId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return sequenceDataMap.get(sequencerId).keySet().stream()
                    .min(Long::compareTo)
                    .orElse(null);
        }

        // Fall back to Redis
        return RedisService.getInstance().getMinimumMelodicSequenceId(sequencerId);
    }

    /**
     * Get the last sequence ID for a specific sequencer
     *
     * @param sequencerId The sequencer ID
     * @return The last ID or null if none
     */
    public Long getLastSequenceId(Integer sequencerId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return sequenceDataMap.get(sequencerId).keySet().stream()
                    .max(Long::compareTo)
                    .orElse(null);
        }

        // Fall back to Redis
        return RedisService.getInstance().getMaximumMelodicSequenceId(sequencerId);
    }

    /**
     * Get the previous sequence ID
     *
     * @param sequencerId The sequencer ID
     * @param currentId   The current sequence ID
     * @return The previous ID or null if none
     */
    public Long getPreviousSequenceId(Integer sequencerId, Long currentId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return sequenceDataMap.get(sequencerId).keySet().stream()
                    .filter(id -> id < currentId)
                    .max(Long::compareTo)
                    .orElse(null);
        }

        // Fall back to Redis
        return RedisService.getInstance().getPreviousMelodicSequenceId(sequencerId, currentId);
    }

    /**
     * Get the next sequence ID
     *
     * @param sequencerId The sequencer ID
     * @param currentId   The current sequence ID
     * @return The next ID or null if none
     */
    public Long getNextSequenceId(Integer sequencerId, Long currentId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return sequenceDataMap.get(sequencerId).keySet().stream()
                    .filter(id -> id > currentId)
                    .min(Long::compareTo)
                    .orElse(null);
        }

        // Fall back to Redis
        return RedisService.getInstance().getNextMelodicSequenceId(sequencerId, currentId);
    }

    /**
     * Save a melodic sequence
     *
     * @param sequencer The sequencer containing pattern data
     * @return The ID of the saved sequence
     */
    public Long saveSequence(MelodicSequencer sequencer) {
        try {
            if (sequencer == null || sequencer.getId() == null) {
                logger.warn("Cannot save sequence - sequencer is null or has no ID");
                return null;
            }

            // Update instrument settings in the sequence data
            MelodicSequenceModifier.updateInstrumentSettingsInSequenceData(sequencer);

            // Now save
            RedisService.getInstance().saveMelodicSequence(sequencer);

            // Update our in-memory map
            int sequencerId = sequencer.getId();
            long sequenceId = sequencer.getSequenceData().getId();

            // Create map if it doesn't exist
            if (!sequenceDataMap.containsKey(sequencerId)) {
                sequenceDataMap.put(sequencerId, new HashMap<>());
            }

            // Create deep copy of sequence data to avoid reference issues
            MelodicSequenceData dataCopy = new MelodicSequenceData();
            // ... copy properties ...

            // Store the copy in our map
            sequenceDataMap.get(sequencerId).put(sequenceId, dataCopy);

            logger.info("Saved melodic sequence with ID: {} for sequencer {}",
                    sequenceId, sequencerId);
            return sequenceId;
        } catch (Exception e) {
            logger.error("Error saving melodic sequence: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get all sequence IDs for a specific sequencer
     *
     * @param sequencerId The ID of the sequencer
     * @return List of all sequence IDs for this sequencer
     */
    public List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        // Check our in-memory map first
        if (sequenceDataMap.containsKey(sequencerId) && !sequenceDataMap.get(sequencerId).isEmpty()) {
            return new ArrayList<>(sequenceDataMap.get(sequencerId).keySet());
        }

        // Fall back to Redis
        return RedisService.getInstance().getAllMelodicSequenceIds(sequencerId);
    }

    /**
     * Apply a sequence to a sequencer by ID
     *
     * @param sequencerId The ID of the sequencer
     * @param sequenceId  The ID of the sequence to apply
     * @return true if successful, false otherwise
     */
    public boolean applySequenceById(int sequencerId, long sequenceId) {
        MelodicSequencer sequencer = getSequencerById(sequencerId);
        if (sequencer == null) {
            logger.warn("Cannot apply sequence - sequencer with ID {} not found", sequencerId);
            return false;
        }

        // Get the sequence data
        MelodicSequenceData data = getSequenceData(sequencerId, sequenceId);
        if (data == null) {
            logger.warn("Cannot apply sequence - sequence data with ID {} not found for sequencer {}",
                    sequenceId, sequencerId);
            return false;
        }

        // Update our in-memory map if it's not already there
        if (!sequenceDataMap.containsKey(sequencerId) ||
                !sequenceDataMap.get(sequencerId).containsKey(sequenceId)) {
            if (!sequenceDataMap.containsKey(sequencerId)) {
                sequenceDataMap.put(sequencerId, new HashMap<>());
            }
            sequenceDataMap.get(sequencerId).put(sequenceId, data);
            logger.debug("Added sequence {} to in-memory cache for sequencer {}", sequenceId, sequencerId);
        }

        // Apply the sequence
        RedisService.getInstance().applyMelodicSequenceToSequencer(data, sequencer);
        return true;
    }

    /**
     * Updates tempo settings on all managed sequencers
     *
     * @param tempoInBPM   The new tempo in BPM
     * @param ticksPerBeat The new ticks per beat value
     */
    public synchronized void updateTempoSettings(float tempoInBPM, int ticksPerBeat) {
        for (MelodicSequencer sequencer : sequencerMap.values()) {
            sequencer.setMasterTempo(ticksPerBeat);
        }
        logger.info("Updated tempo settings on {} melodic sequencers: {} BPM, {} ticks per beat",
                sequencerMap.size(), tempoInBPM, ticksPerBeat);
    }

    /**
     * Initialize a sequencer with default or loaded sequence data. If no
     * sequence is found with the given ID, it will: 1. Try to load the first
     * available sequence for this sequencer 2. If no sequences exist, create
     * and save a new default sequence
     *
     * @param sequencer  The sequencer to initialize
     * @param sequenceId Optional specific sequence ID to load (can be null)
     * @return true if initialization was successful
     */
    public boolean initializeSequencer(MelodicSequencer sequencer, Long sequenceId) {
        if (sequencer == null || sequencer.getId() == null) {
            logger.warn("Cannot initialize null sequencer or sequencer with null ID");
            return false;
        }

        // Case 1: Load specific sequence if ID is provided
        if (sequenceId != null) {
            return applySequenceById(sequencer.getId(), sequenceId);
        }

        // Case 2: Try to load the first available sequence
        Long firstId = getFirstSequenceId(sequencer.getId());
        if (firstId != null) {
            return applySequenceById(sequencer.getId(), firstId);
        }

        // Case 3: No sequences available, create a new default sequence
        return createDefaultSequence(sequencer);
    }

    /**
     * Create a default sequence for a sequencer and save it
     *
     * @param sequencer The sequencer to create a sequence for
     * @return true if successful
     */
    public boolean createDefaultSequence(MelodicSequencer sequencer) {
        if (sequencer == null || sequencer.getId() == null) {
            return false;
        }

        try {
            // Create new sequence data
            MelodicSequenceData data = new MelodicSequenceData();
            data.setPatternLength(16);
            data.setDirection(Direction.FORWARD);
            data.setTimingDivision(TimingDivision.NORMAL);
            data.setLooping(true);

            // Set ID for new sequence
            long newId = RedisService.getInstance().getNextSequenceId();
            data.setId(newId);

            // Update sequencer with this data
            sequencer.setSequenceData(data);

            // Update instrument settings
            MelodicSequenceModifier.updateInstrumentSettingsInSequenceData(sequencer);

            // Save the new sequence
            saveSequence(sequencer);

            logger.info("Created and saved default sequence {} for sequencer {}",
                    newId, sequencer.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error creating default sequence for sequencer {}: {}",
                    sequencer.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the first sequence ID, creating a new sequence if none exists
     *
     * @param sequencerId The sequencer ID
     * @return The first sequence ID or newly created ID
     */
    public Long getFirstSequenceIdWithFallback(Integer sequencerId) {
        // First try to get existing ID
        Long firstId = getFirstSequenceId(sequencerId);

        // If none exists and we have a valid sequencer, create one
        if (firstId == null && sequencerId != null) {
            MelodicSequencer sequencer = getSequencerById(sequencerId);
            if (sequencer != null) {
                createDefaultSequence(sequencer);
                // Now we should have one
                firstId = getFirstSequenceId(sequencerId);
            }
        }

        return firstId;
    }

    // Get the currently active sequencer - for backward compatibility
    public MelodicSequencer getActiveSequencer() {
        // If we have sequencers, return the first one (or implement more sophisticated logic)
        if (!sequencers.isEmpty()) {
            return sequencers.getFirst();
        }
        return null;
    }

    public void repairMidiConnections(MelodicSequencer sequencer) {
        logger.info("Attempting to repair MIDI connections for melodic sequencer {}", sequencer.getId());

        Player player = sequencer.getPlayer();
        Integer id = sequencer.getId();

        try {
            if (player == null) {
                logger.warn("No player for sequencer {}, creating", sequencer.getId());
                initializePlayer(sequencer);
                return;
            }

            if (player.getInstrument() == null) {
                logger.warn("Player has no instrument, initializing...");
                PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
            }

            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                logger.error("Failed to initialize instrument");
                return;
            }

            String deviceName = instrument.getDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = SequencerConstants.GERVILL; // Default to Gervill
                instrument.setDeviceName(deviceName);
            }

            MidiDevice device = DeviceManager.getMidiDevice(deviceName);
            if (device == null) {
                device = DeviceManager.getInstance().getDefaultOutputDevice();
                if (device != null) {
                    deviceName = device.getDeviceInfo().getName();
                    instrument.setDeviceName(deviceName);
                }
            }

            if (device != null) {
                if (!device.isOpen()) {
                    device.open();
                }
                instrument.setDevice(device);

                Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(deviceName);
                if (receiver != null) {
                    logger.info("Successfully reconnected sequencer {} to device {}", id, deviceName);
                    SoundbankManager.getInstance().applyInstrumentPreset(player);

                } else {
                    logger.warn("Failed to get receiver for sequencer {}", id);
                }
            } else {
                logger.warn("Could not get device for sequencer {}", id);
            }

            PlayerManager.getInstance().savePlayerProperties(player);
        } catch (Exception e) {
            logger.error("Error repairing melodic sequencer {}: {}", id, e.getMessage());
        }
    }

    private void initializePlayer(MelodicSequencer sequencer) {

        int playerChannel = SequencerConstants.MELODIC_CHANNELS[sequencer.getId()];

        Session session = SessionManager.getInstance().getActiveSession();
        if (session == null) {
            logger.error("Cannot initialize player - no active session");
            return;
        }

        Optional<Note> opt = UserConfigManager.getInstance().getCurrentConfig().getDefaultNotes()
                .stream().filter(p -> p.getChannel().equals(playerChannel)).findFirst();

        Player player;

        if (opt.isPresent()) {

            player = opt.get();
            logger.info("Using existing player {} for sequencer {}", player.getId(), sequencer.getId());
            player.setDefaultChannel(playerChannel);
            player.setOwner(this);
            player.setMelodicPlayer(true);

        } else {
            logger.info("Creating new player for melodic sequencer {}", sequencer.getId());
            // player = RedisService.getInstance().newNote();
            player = new Note();
            player.setId(RedisService.getInstance().getPlayerHelper().getNextPlayerId());
            player.setRules(new HashSet<>()); // Ensure rules are initialized
            player.setMinVelocity(60);
            player.setMaxVelocity(127);
            player.setLevel(100);
            player.setIsDefault(true);
            player.setName("Melo " + sequencer.getId() + 1);
            player.setDefaultChannel(playerChannel);
        }

        if (player.getInstrument() != null) {
            DeviceManager.getInstance();
            MidiDevice device = DeviceManager.getMidiDevice(player.getInstrument().getDeviceName());
            if (device != null) {
                if (!device.isOpen()) {
                    try {
                        device.open();
                    } catch (MidiUnavailableException e) {
                        throw new RuntimeException(e);
                    }
                }
                player.getInstrument().setDevice(device);
                player.getInstrument().setAssignedToPlayer(true);
            } else {
                PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
            }
        }

        if (sequencer.getSequenceData() != null) {
            applySequenceDataToInstrument(sequencer);
        }

        // initializePlayer(player);
        session.getPlayers().add(player);
        PlayerManager.getInstance().savePlayerProperties(player);
        SessionManager.getInstance().saveSession(session);
        logger.info("Added new player to session {}: {}", session.getId(), player.getId());
    }

    private void applySequenceDataToInstrument(MelodicSequencer sequencer) {

        Player player = sequencer.getPlayer();
        MelodicSequenceData sequenceData = sequencer.getSequenceData();

        if (sequenceData == null || player == null || player.getInstrument() == null) {
            return;
        }

        if (!player.getInstrument().isInternalSynth() && player.getInstrument().getChannel() != SequencerConstants.MIDI_DRUM_CHANNEL) {

            if (sequenceData.getSoundbankName() != null) {
                player.getInstrument().setSoundBank(sequenceData.getSoundbankName());
            }

            if (sequenceData.getBankIndex() != null) {
                player.getInstrument().setBankIndex(sequenceData.getBankIndex());
            }

            if (sequenceData.getPreset() != null) {
                player.getInstrument().setPreset(sequenceData.getPreset());
            }

        }

        logger.debug("Applied sequence data settings to instrument: preset:{}, bank:{}, soundbank:{}",
                player.getInstrument().getPreset(), player.getInstrument().getBankIndex(), player.getInstrument().getSoundBank());
    }

    private void handleSaveAll(Command cmd) {
        getAllSequencers().forEach(seq -> {
            RedisService.getInstance().saveMelodicSequence(seq);
        });
    }

    /**
     * Delete all melodic sequences from the database
     *
     * @return The number of sequences deleted, or -1 if an error occurred
     */
    public void deleteAllSequences() {
        try {
            // Get the helper that interacts with the database
            MelodicSequenceDataHelper helper = RedisService.getInstance().getMelodicSequencerHelper();

            // Get all sequence IDs first to count them
            for (int[] id = new int[]{0}; id[0] < 15; id[0]++) {
                helper.getAllMelodicSequenceIds(id[0]).forEach(sequenceId -> helper.deleteMelodicSequence(id[0],
                        sequenceId));
            }
        } catch (Exception e) {
            logger.error("Error deleting all melodic sequences", e);
        }
    }
}
