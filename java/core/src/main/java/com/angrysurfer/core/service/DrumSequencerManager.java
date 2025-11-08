package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;

/**
 * Manager for DrumSequencer instances.
 * Maintains a collection of sequencers and provides methods to create and access them.
 */
public class DrumSequencerManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerManager.class);

    private static DrumSequencerManager instance;

    private final RedisService redisService;

    // Store sequencers in an ArrayList for indexed access
    private final List<DrumSequencer> sequencers;

    private int selectedPadIndex = 0; // Default to first pad

    // Private constructor for singleton pattern
    private DrumSequencerManager() {
        this.redisService = RedisService.getInstance();
        this.sequencers = new ArrayList<>();

        if (Constants.DEBUG)
            RedisService.getInstance().deleteAllDrumSequences();

        CommandBus.getInstance().register(this, new String[]{Commands.SAVE_DRUM_SEQUENCE,
                Commands.LOAD_DRUM_SEQUENCE, Commands.REPAIR_MIDI_CONNECTIONS,
                Commands.ENSURE_MIDI_CONNECTIONS});
    }

    // Singleton access method
    public static synchronized DrumSequencerManager getInstance() {
        if (instance == null) {
            instance = new DrumSequencerManager();
        }
        return instance;
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.SAVE_DRUM_SEQUENCE -> {
                if (action.getData() instanceof DrumSequencer sequencer) {
                    saveSequence(sequencer);
                }
            }
            case Commands.LOAD_DRUM_SEQUENCE -> {
                if (action.getData() instanceof Long id) {
                    loadSequenceById(id);
                }
            }
            case Commands.REPAIR_MIDI_CONNECTIONS -> {
                repairAllMidiConnections();
            }
            case Commands.ENSURE_MIDI_CONNECTIONS -> {
                ensureAllSequencerConnections();
            }
        }
    }

    /**
     * Save a drum sequence
     *
     * @param sequencer The sequencer containing pattern data
     * @return The ID of the saved sequence
     */
    public Long saveSequence(DrumSequencer sequencer) {
        try {
            redisService.saveDrumSequence(sequencer);
            logger.info("Saved drum sequence with ID: {}", sequencer.getSequenceData().getId());
            return sequencer.getSequenceData().getId();
        } catch (Exception e) {
            logger.error("Error saving drum sequence: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Load a drum sequence by ID
     *
     * @param id The sequence ID to load
     * @return The loaded sequence data or null if not found
     */
    public DrumSequenceData loadSequenceById(Long id) {
        try {
            return redisService.findDrumSequenceById(id);
        } catch (Exception e) {
            logger.error("Error loading drum sequence {}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Load a sequence into the given sequencer
     *
     * @param id        The sequence ID to load
     * @param sequencer The sequencer to load into
     * @return true if successful, false otherwise
     */
    public boolean loadSequence(Long id, DrumSequencer sequencer) {
        try {
            DrumSequenceData data = loadSequenceById(id);
            if (data != null) {
                redisService.applyDrumSequenceToSequencer(data, sequencer);
                logger.info("Loaded drum sequence {} into sequencer", id);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error applying drum sequence to sequencer: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a new drum sequence
     *
     * @return The new sequence data
     */
    public DrumSequenceData createNewSequence() {
        try {
            return redisService.newDrumSequence();
        } catch (Exception e) {
            logger.error("Error creating new drum sequence: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the previous sequence ID
     *
     * @param currentId The current sequence ID
     * @return The previous ID or null if none
     */
    public Long getPreviousSequenceId(Long currentId) {
        return redisService.getPreviousDrumSequenceId(currentId);
    }

    /**
     * Get the next sequence ID
     *
     * @param currentId The current sequence ID
     * @return The next ID or null if none
     */
    public Long getNextSequenceId(Long currentId) {
        return redisService.getNextDrumSequenceId(currentId);
    }

    /**
     * Get the first sequence ID
     *
     * @return The first ID or null if none
     */
    public Long getFirstSequenceId() {
        return redisService.getMinimumDrumSequenceId();
    }

    /**
     * Get the last sequence ID
     *
     * @return The last ID or null if none
     */
    public Long getLastSequenceId() {
        return redisService.getMaximumDrumSequenceId();
    }

    /**
     * Check if there are any sequences available
     *
     * @return true if sequences exist, false otherwise
     */
    public boolean hasSequences() {
        List<Long> ids = redisService.getAllDrumSequenceIds();
        return ids != null && !ids.isEmpty();
    }

    /**
     * Refresh the internal list of sequences from the database
     */
    public void refreshSequenceList() {
        try {
            // Force a refresh of the sequence ID list from Redis
            List<Long> sequenceIds = redisService.getAllDrumSequenceIds();
            logger.info("Refreshed drum sequence list, found {} sequences",
                    sequenceIds != null ? sequenceIds.size() : 0);
        } catch (Exception e) {
            logger.error("Error refreshing sequence list: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a new DrumSequencer and add it to the manager.
     *
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer() {
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setId(sequencers.size());
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing drum sequencer {}: {}", sequencer.getId(), e.getMessage(), e);
        }
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer (index: {})", sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Create a new DrumSequencer with a note event listener and add it to the manager.
     *
     * @param noteEventListener Callback for when a note should be played
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer(Consumer<NoteEvent> noteEventListener) {
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setNoteEventListener(noteEventListener);
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing drum sequencer {}: {}", sequencer.getId(), e.getMessage(), e);
        }
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer with note event listener (index: {})",
                sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Create a new DrumSequencer with note event and step update listeners.
     *
     * @param noteEventListener  Callback for when a note should be played
     * @param stepUpdateListener Callback for step updates during playback
     * @return The newly created DrumSequencer
     */
    public synchronized DrumSequencer newSequencer(
            Consumer<NoteEvent> noteEventListener,
            Consumer<DrumStepUpdateEvent> stepUpdateListener) {  // Changed from StepUpdateEvent to DrumStepUpdateEvent
        DrumSequencer sequencer = new DrumSequencer();
        sequencer.setNoteEventListener(noteEventListener);
        sequencer.setStepUpdateListener(stepUpdateListener);
        try {
            sequencer.initialize();
        } catch (Exception e) {
            logger.error("Error initializing drum sequencer {}: {}", sequencer.getId(), e.getMessage(), e);
        }
        sequencers.add(sequencer);
        logger.info("Created new drum sequencer with note event and step update listeners (index: {})",
                sequencers.size() - 1);
        return sequencer;
    }

    /**
     * Get a sequencer by its index in the collection.
     *
     * @param index The index of the sequencer
     * @return The DrumSequencer at the specified index, or null if not found
     */
    public synchronized DrumSequencer getSequencer(int index) {
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
        return sequencers.size();
    }

    /**
     * Get an unmodifiable view of all sequencers.
     *
     * @return Unmodifiable list of all sequencers
     */
    public synchronized List<DrumSequencer> getAllSequencers() {
        return Collections.unmodifiableList(sequencers);
    }

    /**
     * Remove a sequencer from the manager.
     *
     * @param index The index of the sequencer to remove
     * @return true if removed successfully, false otherwise
     */
    public synchronized boolean removeSequencer(int index) {
        if (index >= 0 && index < sequencers.size()) {
            sequencers.remove(index);
            logger.info("Removed drum sequencer at index {}", index);
            return true;
        }
        logger.warn("Failed to remove sequencer at index {}: not found", index);
        return false;
    }

    public List<Long> getAllDrumSequenceIds() {
        return redisService.getAllDrumSequenceIds();
    }

    /**
     * Get the currently selected drum pad index
     *
     * @return The selected pad index
     */
    public synchronized int getSelectedPadIndex() {
        return selectedPadIndex;
    }

    /**
     * Set the currently selected drum pad index
     *
     * @param index The new selected pad index
     */
    public synchronized void setSelectedPadIndex(int index) {
        // Validate the index first
        if (index >= 0 && index < SequencerConstants.DRUM_PAD_COUNT) {
            selectedPadIndex = index;

            // Also update the selected pad index in sequencers
            for (DrumSequencer seq : sequencers) {
                seq.setSelectedPadIndex(index);
            }

            logger.info("Selected pad index set to: {}", index);
        } else {
            logger.warn("Attempted to set invalid pad index: {}", index);
        }
    }

    /**
     * Updates tempo settings on all managed sequencers
     *
     * @param tempoInBPM   The new tempo in BPM
     * @param ticksPerBeat The new ticks per beat value
     */
    public synchronized void updateTempoSettings(float tempoInBPM, int ticksPerBeat) {
        for (DrumSequencer sequencer : sequencers) {
            // sequencer.getData().setTempoInBPM(tempoInBPM);
            // sequencer.getData().setTicksPerBeat(ticksPerBeat);
            sequencer.getSequenceData().setMasterTempo(ticksPerBeat);
        }
        logger.info("Updated tempo settings on {} drum sequencers: {} BPM, {} ticks per beat",
                sequencers.size(), tempoInBPM, ticksPerBeat);
    }

    /**
     * Get the currently active sequencer
     *
     * @return The active DrumSequencer, or null if none available
     */
    public DrumSequencer getActiveSequencer() {
        // If we have sequencers, return the first one (or implement more sophisticated logic)
        if (!sequencers.isEmpty()) {
            return sequencers.getFirst();
        }
        return null;
    }

    /**
     * Ensures all drum sequencers have valid device connections
     */
    public void ensureAllSequencerConnections() {
        logger.info("Ensuring all drum sequencers have valid device connections");

        // Make sure the internal synth is initialized
        ensureInternalSynthAvailable();

        // Get a default device to use as fallback
        MidiDevice defaultDevice = getDefaultMidiDevice();

        // Process each sequencer
        int sequencerCount = sequencers.size();
        int connectedSequencers = 0;

        for (DrumSequencer sequencer : sequencers) {
            if (ensureSequencerConnections(sequencer, defaultDevice)) {
                connectedSequencers++;
            }
        }

        logger.info("Connected {}/{} drum sequencers to active devices",
                connectedSequencers, sequencerCount);
    }

    /**
     * Ensure a specific sequencer has valid device connections for all drum pads
     *
     * @param sequencer     The sequencer to check
     * @param defaultDevice Default device to use if specific one isn't available
     * @return true if all connections were successful
     */
    private boolean ensureSequencerConnections(DrumSequencer sequencer, MidiDevice defaultDevice) {
        if (sequencer == null) {
            return false;
        }

        logger.info("Ensuring all drum pads for sequencer have valid device connections");

        // Connect each drum pad to a valid device
        int connectedCount = 0;
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            if (connectDrumPad(sequencer, i, defaultDevice)) {
                connectedCount++;
            }
        }

        logger.info("Connected {}/{} drum pads to active devices",
                connectedCount, SequencerConstants.DRUM_PAD_COUNT);

        return connectedCount == SequencerConstants.DRUM_PAD_COUNT;
    }

    private void ensureInternalSynthAvailable() {
        Synthesizer synth = MidiService.getInstance().getSynthesizer();
        if (synth == null || !synth.isOpen()) {
            MidiService.getInstance().initialize();
            logger.info("Initialized internal synth for drum sequencer");
        }
    }

    /**
     * Get a default MIDI device to use as fallback
     */
    private MidiDevice getDefaultMidiDevice() {
        MidiDevice device = MidiService.getInstance().getDefaultOutputDevice();

        if (device == null) {
            logger.debug("No default output device available, trying Gervill");
            device = MidiService.getInstance().getDevice(SequencerConstants.GERVILL);

            // Make sure it's open
            if (device != null && !device.isOpen()) {
                try {
                    device.open();
                    logger.info("Opened Gervill device for drum sequencer");
                } catch (Exception e) {
                    logger.error("Could not open Gervill device: {}", e.getMessage());
                    device = null;
                }
            }
        }

        return device;
    }

    /**
     * Connect a specific drum pad to a valid MIDI device
     *
     * @param sequencer     The sequencer containing the drum pad
     * @param drumIndex     The index of the drum pad to connect
     * @param defaultDevice The default device to use as fallback
     * @return true if successfully connected, false otherwise
     */
    private boolean connectDrumPad(DrumSequencer sequencer, int drumIndex, MidiDevice defaultDevice) {
        Player player = sequencer.getPlayer(drumIndex);
        if (player == null) {
            logger.debug("No player for drum pad {}", drumIndex);
            return false;
        }

        // Ensure channel is set correctly
        player.setDefaultChannel(SequencerConstants.MIDI_DRUM_CHANNEL);

        // Ensure instrument is set
        if (player.getInstrument() == null && player.getInstrumentId() != null) {
            player.setInstrument(InstrumentManager.getInstance().getInstrumentById(player.getInstrumentId()));
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
                player.getInstrument().setReceivedChannels(new Integer[]{SequencerConstants.MIDI_DRUM_CHANNEL});
            } else {
                logger.warn("Failed to set instrument for drum {}", drumIndex);
                return false;
            }
        }

        InstrumentWrapper instrument = player.getInstrument();

        // Try to connect the instrument to a device
        return connectInstrumentToDevice(drumIndex, instrument, defaultDevice);
    }

    /**
     * Connect an instrument to a MIDI device
     *
     * @param drumIndex     The drum pad index (for logging)
     * @param instrument    The instrument to connect
     * @param defaultDevice The default device to use if preferred device isn't available
     * @return true if successfully connected, false otherwise
     */
    private boolean connectInstrumentToDevice(int drumIndex, InstrumentWrapper instrument, MidiDevice defaultDevice) {
        // Skip if already connected
        if (instrument.getReceiver() != null) {
            logger.debug("Drum {} already has a valid receiver", drumIndex);
            return true;
        }

        try {
            // Strategy 1: Try to get device by name
            MidiDevice device = null;
            String deviceName = instrument.getDeviceName();

            if (deviceName != null && !deviceName.isEmpty()) {
                device = MidiService.getInstance().getDevice(deviceName);
                if (device != null && !device.isOpen()) {
                    try {
                        device.open();
                        logger.debug("Opened device {} for drum {}", deviceName, drumIndex);
                    } catch (Exception e) {
                        logger.warn("Could not open device {} for drum {}: {}",
                                deviceName, drumIndex, e.getMessage());
                        device = null;
                    }
                }
            }

            // Strategy 2: Use default device if specific device not available
            if (device == null && defaultDevice != null) {
                device = defaultDevice;
                deviceName = defaultDevice.getDeviceInfo().getName();
                instrument.setDeviceName(deviceName);
                logger.debug("Using default device {} for drum {}", deviceName, drumIndex);
            }

            if (device == null) {
                device = MidiService.getInstance().getDevice(SequencerConstants.GERVILL);
                if (device != null) {
                    if (!device.isOpen()) {
                        device.open();
                    }
                    deviceName = SequencerConstants.GERVILL;
                    instrument.setDeviceName(deviceName);
                    logger.debug("Using Gervill synthesizer for drum {}", drumIndex);
                }
            }

            // Now get a receiver for the device
            if (device != null) {
                instrument.setDevice(device);
                Receiver receiver = MidiService.getInstance().getReceiver(deviceName);

                if (receiver != null) {
                    instrument.setReceiver(receiver);
                    instrument.setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);

                    // Find player that owns this instrument to apply preset
                    Player player = findPlayerByInstrument(instrument);
                    if (player != null) {
                        PlayerManager.getInstance().applyInstrumentPreset(player);
                        logger.info("Successfully connected drum {} to device {}", drumIndex, deviceName);
                        PlayerManager.getInstance().savePlayerProperties(player);
                        return true;
                    } else {
                        logger.warn("Could not find player for instrument to apply preset");
                    }
                } else {
                    logger.warn("Failed to get receiver for drum {} from device {}", drumIndex, deviceName);
                }
            } else {
                logger.warn("Could not find any valid device for drum {}", drumIndex);
            }
        } catch (Exception e) {
            logger.error("Error connecting drum {}: {}", drumIndex, e.getMessage());
        }

        return false;
    }

    /**
     * Helper method to find a player by its instrument
     */
    private Player findPlayerByInstrument(InstrumentWrapper instrument) {
        for (DrumSequencer sequencer : sequencers) {
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                Player player = sequencer.getPlayer(i);
                if (player != null && player.getInstrument() == instrument) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * Attempts to repair MIDI connections for all sequencers if they have been lost
     */
    public void repairAllMidiConnections() {
        logger.info("Attempting to repair MIDI connections for all drum sequencers");

        MidiService.getInstance().clearAllReceivers();

        // Process each sequencer
        for (DrumSequencer sequencer : sequencers) {
            repairSequencerConnections(sequencer);
        }

        logger.info("MIDI connection repair completed for all sequencers");
    }

    /**
     * Repair MIDI connections for a specific sequencer
     */
    public void repairSequencerConnections(DrumSequencer sequencer) {
        if (sequencer == null) {
            return;
        }

        logger.info("Repairing MIDI connections for drum sequencer");

        // Try to reconnect all devices for this sequencer
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = sequencer.getPlayer(i);
            if (player != null && player.getInstrument() != null) {
                InstrumentWrapper instrument = player.getInstrument();

                String deviceName = instrument.getDeviceName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = SequencerConstants.GERVILL;
                    instrument.setDeviceName(deviceName);
                }

                MidiDevice device = MidiService.getInstance().getDevice(deviceName);
                if (device == null) {
                    device = MidiService.getInstance().getDefaultOutputDevice();
                    if (device != null) {
                        deviceName = device.getDeviceInfo().getName();
                        instrument.setDeviceName(deviceName);
                    }
                }

                if (device != null) {
                    if (!device.isOpen()) {
                        try {
                            device.open();
                        } catch (Exception e) {
                            logger.warn("Could not open device {} for drum {}: {}",
                                    deviceName, i, e.getMessage());
                            continue;
                        }
                    }

                    instrument.setDevice(device);
                    Receiver receiver = MidiService.getInstance().getReceiver(deviceName);

                    if (receiver != null) {
                        instrument.setReceiver(receiver);
                        logger.info("Successfully reconnected drum {} to device {}", i, deviceName);
                    }
                }
            }
        }

        // Force update instrument settings
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = sequencer.getPlayer(i);
            if (player != null && player.getInstrument() != null) {
                PlayerManager.getInstance().applyInstrumentPreset(player);
            }
        }
    }
}