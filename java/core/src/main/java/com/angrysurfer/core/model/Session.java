package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.MidiClockSource;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Session implements Serializable, IBusListener {

    @JsonIgnore
    @Transient
    static Logger logger = LoggerFactory.getLogger(Session.class.getCanonicalName());
    @JsonIgnore
    @Transient
    private final MidiClockSource sequencerManager = new MidiClockSource();
    @JsonIgnore
    @Transient
    public boolean isActive = false;
    @JsonIgnore
    @Transient
    public boolean modified = false;
    @Transient
    Set<Long> activePlayerIds = new HashSet<>();
    @JsonIgnore
    private Object soloLock;
    private Long id;
    @JsonIgnore
    @Transient
    private Set<Player> addList = new HashSet<>();
    @JsonIgnore
    @Transient
    private Set<Player> removeList = new HashSet<>();
    // Replace Cyclers with simple variables initialized to 1 instead of 0
    @JsonIgnore
    @Transient
    private long tick = 1;
    @JsonIgnore
    @Transient
    private double beat = 1.0;
    @JsonIgnore
    @Transient
    private int bar = 1;
    @JsonIgnore
    @Transient
    private int part = 1;
    @JsonIgnore
    @Transient
    private long tickCount = 0;
    @JsonIgnore
    @Transient
    private int beatCount = 0;
    @JsonIgnore
    @Transient
    private int barCount = 0;
    @JsonIgnore
    @Transient
    private int partCount = 0;
    @Transient
    private boolean done = false;
    @JsonIgnore
    private transient Set<Player> players = new HashSet<>();
    @Transient
    private double granularBeat = 0.0;
    private Integer bars = SequencerConstants.DEFAULT_BAR_COUNT;
    private Integer beatsPerBar = SequencerConstants.DEFAULT_BEATS_PER_BAR;
    private Integer beatDivider = SequencerConstants.DEFAULT_BEAT_DIVIDER;
    private Integer partLength = SequencerConstants.DEFAULT_PART_LENGTH;
    private Integer maxTracks = SequencerConstants.DEFAULT_MAX_TRACKS;
    private Long songLength = SequencerConstants.DEFAULT_SONG_LENGTH;
    private Integer swing = SequencerConstants.DEFAULT_SWING;
    private Integer ticksPerBeat = SequencerConstants.DEFAULT_PPQ;
    private Float tempoInBPM = SequencerConstants.DEFAULT_BPM;
    private Integer loopCount = SequencerConstants.DEFAULT_LOOP_COUNT;
    private Integer parts = SequencerConstants.DEFAULT_PART_COUNT;
    private Integer noteOffset = 0;
    private String name;
    private String notes = "Session Notes";
    private String scale = Scale.SCALE_CHROMATIC;
    private String rootNote = Scale.SCALE_NOTES[0]; // Default to C
    @Transient
    private boolean paused = false;
    @Transient
    @JsonIgnore
    private Set<MuteGroup> muteGroups = new HashSet<>();
    @JsonIgnore
    @Transient
    private CommandBus commandBus = CommandBus.getInstance();
    @JsonIgnore
    @Transient
    private TimingBus timingBus = TimingBus.getInstance(); // Add this
    @JsonIgnore
    @Transient
    private Set<IBusListener> tickListeners = new HashSet<>();

    // For better performance, track active players separately
    @JsonIgnore
    @Transient
    private Set<Player> activePlayers = new HashSet<>();

    @JsonIgnore
    private transient ConcurrentLinkedQueue<IBusListener> timingListeners;

    // Add this to Session constructor to ensure proper registration
    public Session() {
        setSongLength(Long.MAX_VALUE);

        // Initialize timing fields first
        if (timingListeners == null) {
            timingListeners = new ConcurrentLinkedQueue<>();
        }
        // NOTE: Do not register on buses inside constructor to avoid side-effects during deserialization
        // Bus registration is deferred to initialize(). This keeps construction side-effect-free for Phase 0.
    }

    /**
     * Perform side-effectful initialization such as registering with the CommandBus and TimingBus.
     * Call this after constructing a Session instance to complete setup.
     */
    public synchronized void initialize() {
        if (timingListeners == null) {
            timingListeners = new ConcurrentLinkedQueue<>();
        }

        try {
            CommandBus.getInstance().register(this, new String[]{Commands.TIMING_PARAMETERS_CHANGED});
            timingBus.register(this);
        } catch (Exception e) {
            logger.error("Failed to initialize Session buses: {}", e.getMessage(), e);
        }
    }

    public Session(float tempoInBPM, int bars, int beatsPerBar, int ticksPerBeat, int parts, int partLength) {
        this.tempoInBPM = tempoInBPM;
        this.bars = bars;
        this.beatsPerBar = beatsPerBar;
        this.ticksPerBeat = ticksPerBeat;
        this.parts = parts;
        this.partLength = partLength > 0 ? partLength : 1; // Ensure partLength is at least 1

        // Initialize counters
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;
    }

    // Add this method for proper initialization during deserialization
    @JsonIgnore
    private ConcurrentLinkedQueue<IBusListener> getTimingListeners() {
        if (timingListeners == null) {
            timingListeners = new ConcurrentLinkedQueue<>();
        }
        return timingListeners;
    }

    // Update the register method to use the getter
    public void register(IBusListener listener) {
        if (listener != null) {
            getTimingListeners().add(listener);
        }
    }

    // Update the unregister method similarly
    public void unregister(IBusListener listener) {
        if (listener != null && timingListeners != null) {
            getTimingListeners().remove(listener);
        }
    }

    public int getMetronomChannel() {
        return 9;
    }

    public void addPlayer(Player player) {
        // Skip default players
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.info("Skipping default player: {}", player);
            return;
        }

        logger.info("addPlayer() - adding player: {}", player);
        if (isRunning())
            synchronized (getAddList()) {
                getAddList().add(player);
            }
        else {
            getPlayers().add(player);
        }

        player.setSession(this);
        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, player);

        // Auto-register as tick listener if session is running
        if (isRunning() && player.getEnabled()) {
            registerTickListener(player);
        }
    }

    public void removePlayer(Player player) {
        logger.info("addPlayer() - removing player: {}", player);
        if (isRunning())
            synchronized (getRemoveList()) {
                getRemoveList().add(player);
            }
        else {
            getPlayers().remove(player);
        }

        player.setSession(null);
        CommandBus.getInstance().publish(Commands.PLAYER_ADDED, this, player);

        // Unregister from tick listeners
        unregisterTickListener(player);
    }

    public Player getPlayer(Long playerId) {
        logger.info("getPlayer() - playerId: {}", playerId);
        return getPlayers().stream().filter(p -> p.getId().equals(playerId)).findFirst().orElseThrow();
    }

    public Player getPlayerById(Long id) {
        if (players == null || id == null) {
            return null;
        }

        for (Player player : players) {
            if (id.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

    public void setPartLength(int partLength) {
        this.partLength = partLength;
        CommandBus.getInstance().publish(Commands.SESSION_UPDATED, this, this);
    }

    /**
     * Make this a complete reset of all state
     */
    public void reset() {
    if (logger.isDebugEnabled()) logger.debug("Session: Resetting session state");

        // Reset all position variables to 1 - consistent starting points
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1; // Always start at part 1

        // Counters still start at 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;
    if (logger.isDebugEnabled()) logger.debug("Session: All counters reset");
        // Reset player state
        if (getPlayers() != null) {
            if (logger.isDebugEnabled()) logger.debug("Session: Resetting {} players", getPlayers().size());
            getPlayers().forEach(p -> {
                if (p.getSkipCycler() != null) {
                    p.getSkipCycler().reset();
                }
                p.setEnabled(false);
                if (logger.isDebugEnabled()) logger.debug("Session: Reset player {}", p.getId());
            });
        }

        // Clear lists
        if (addList != null) {
            addList.clear();
            if (logger.isDebugEnabled()) logger.debug("Session: Add list cleared");
        }
        if (removeList != null) {
            removeList.clear();
            if (logger.isDebugEnabled()) logger.debug("Session: Remove list cleared");
        }

        // Reset state flags
        done = false;
        isActive = false;
        paused = false;
    if (logger.isDebugEnabled()) logger.debug("Session: State flags reset");

        // Clear mute groups
        clearMuteGroups();
    if (logger.isDebugEnabled()) logger.debug("Session: Mute groups cleared");

    if (logger.isDebugEnabled()) logger.debug("Session: Reset complete");
    }

    private void clearMuteGroups() {
    }

    private void createMuteGroups() {
    }

    private void updateMuteGroups() {
    }

    public double getBeatDivision() {
        return 1.0 / beatDivider;
    }

    /**
     * Clean up at the end of playback
     */
    public void afterEnd() {
        logger.info("afterEnd() - resetting position and stopping all notes");

        // Reset all position variables to 1
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;

        // Reset counters to 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;

        // Reset granular beat tracking
        setGranularBeat(0.0);

        // Stop all notes
        stopAllNotes();
    }

    /**
     * Initialize all timing variables before session playback starts
     */
    public void beforeStart() {
        reset();
        // Add tick listener
        // setupTickListener();

        // Set up players
        if (getPlayers() != null && !getPlayers().isEmpty()) {
            // logger.debug("Session: Setting up {} players", getPlayers().size());
            getPlayers().forEach(p -> {
                // logger.debug("Session: Setting up player {} ({})", p.getId(), p.getName());
                p.setEnabled(true);
                if (p.getRules() != null) {
                    // logger.debug(" - Player has {} rules", p.getRules().size());
                }
            });
        } else {
            logger.info("Session: No players to set up");
        }

        // Register with timing bus
        timingBus.register(this);
        logger.info("Session: Registered with timing bus");

        // Notify about tempo to all sequencers
        CommandBus.getInstance().publish(Commands.UPDATE_TEMPO, this, ticksPerBeat);

        // Set active state
        isActive = true;
        logger.info("Session: Session marked as active");

        // Publish session starting event
        CommandBus.getInstance().publish(Commands.SESSION_STARTING, this);
        logger.info("Session: Published SESSION_STARTING event");
    }

    /**
     * Helper method to stop all active notes
     */
    public void stopAllNotes() {
        IntStream.range(0, 128).forEach(note -> getPlayers().forEach(p -> {
            try {
                if (p.getInstrument() != null) {
                    p.getInstrument().noteOff(note, 0);
                }
            } catch (Exception e) {
                logger.error("Error stopping note {} on channel {}: {}", note, p.getChannel(), e.getMessage(), e);
            }
        }));
    }

    public Float getBeatDuration() {
        logger.debug("getBeatDuration() - calculated duration: {}",
                60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar());
        return 60000 / getTempoInBPM() / getTicksPerBeat() / getBeatsPerBar();
    }


    public Boolean hasSolos() {
        synchronized (soloLock) {
            for (Player player : players) {
                if (player.isSolo()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isRunning() {
        return sequencerManager != null && sequencerManager.isRunning();
    }

    public synchronized boolean isValid() {
        return (Objects.nonNull(getPlayers()) && !getPlayers().isEmpty()
                && getPlayers().stream().
                anyMatch(p -> Objects.nonNull(p.getRules()) && !p.getRules().isEmpty()));
    }

    public void play() {
        reset();
        initializeDevices();
        syncPlayersWithTickListeners();
        sequencerManager.startSequence();
    }

    /**
     * Initialize devices for all players
     */
    public void initializeDevices() {
        // Get available devices once to avoid repeated queries
        List<MidiDevice> availableDevices;
        try {
            availableDevices = DeviceManager.getInstance().getAvailableOutputDevices();
        } catch (MidiUnavailableException e) {
            logger.error("Failed to get available devices: {}", e.getMessage());
            return;
        }

        // Make a defensive copy of players to avoid concurrent modification
        Set<Player> playersCopy = new HashSet<>(players);

        // Initialize each player's device
        for (Player player : playersCopy) {
            try {
                // Only initialize enabled players
                if (player.getEnabled()) {
                    initializePlayerDevice(player, availableDevices);
                    initializePlayerPreset(player);
                }
            } catch (Exception e) {
                logger.error("Error initializing player {}: {}",
                        player.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Initialize a player's MIDI device connection
     *
     * @param player  The player to initialize
     * @param devices List of available MIDI output devices
     */
    private void initializePlayerDevice(Player player, List<MidiDevice> devices) {
        logger.info("Initializing device for player {}", player.getName());

        // Verify player has an instrument
        InstrumentWrapper instrument = player.getInstrument();
        if (instrument == null) {
            logger.warn("Player {} has no instrument configured", player.getName());
            return;
        }

        try {
            // Check if device is already connected
            if (instrument.getDevice() != null && instrument.getDevice().isOpen()) {
                logger.info("Device for player {} is already open: {}",
                        player.getName(), instrument.getDeviceInfo().getName());
                return;
            }

            // Get device name from instrument
            String deviceName = instrument.getDeviceName();
            if (deviceName == null) {
                logger.warn("Instrument {} has no device name", instrument.getName());
                return;
            }

            // Find matching device by name
            MidiDevice device = null;
            for (MidiDevice availableDevice : devices) {
                if (availableDevice.getDeviceInfo().getName().equals(deviceName)) {
                    device = availableDevice;
                    break;
                }
            }

            // If device not found by exact match, try partial match
            if (device == null) {
                for (MidiDevice availableDevice : devices) {
                    if (availableDevice.getDeviceInfo().getName().contains(deviceName) ||
                            deviceName.contains(availableDevice.getDeviceInfo().getName())) {
                        device = availableDevice;
                        logger.info("Using partial device name match: {} -> {}",
                                deviceName, availableDevice.getDeviceInfo().getName());
                        break;
                    }
                }
            }

            // If still no device, get from DeviceManager
            if (device == null) {
                device = DeviceManager.getMidiDevice(deviceName);
            }

            // Connect and open device if found
            if (device != null) {
                // Connect device to instrument
                instrument.setDevice(device);

                // Open device if not already open
                if (!device.isOpen()) {
                    device.open();
                }

                // Mark instrument as available if device is open
                instrument.setAvailable(device.isOpen());
                logger.info("Connected player {} to device: {}, available: {}",
                        player.getName(), device.getDeviceInfo().getName(), instrument.getAvailable());
            } else {
                logger.error("Could not find MIDI device '{}' for player {}",
                        deviceName, player.getName());
            }
        } catch (Exception e) {
            logger.error("Error initializing device for player {}: {}",
                    player.getName(), e.getMessage(), e);
        }
    }

    /**
     * Initialize a player's preset by sending appropriate MIDI commands
     */
    private void initializePlayerPreset(Player player) {
        if (player == null) {
            logger.warn("Cannot initialize null player");
            return;
        }

        // Check if instrument is null and try to assign a default one if needed
        if (player.getInstrument() == null) {
            logger.warn("Player {} has null instrument, attempting to assign default", player.getId());
            try {
                // Try to find or create an internal instrument
                InstrumentWrapper defaultInstrument = InstrumentManager.getInstance()
                        .findOrCreateInternalInstrument(player.getChannel());

                if (defaultInstrument != null) {
                    // Assign default instrument
                    player.setInstrument(defaultInstrument);
                    player.setUsingInternalSynth(true);

                    defaultInstrument.setAssignedToPlayer(true);


                    // Save the player to persist this change
                    PlayerManager.getInstance().savePlayerProperties(player);

                    logger.info("Assigned default internal instrument to player {}", player.getId());
                } else {
                    logger.error("Could not create default instrument for player {}", player.getId());
                    return; // Skip this player
                }
            } catch (Exception e) {
                logger.error("Failed to create default instrument: {}", e.getMessage());
                return; // Skip this player
            }
        }

        // Now check again before proceeding
        if (player.getInstrument() == null) {
            logger.error("Player {} still has null instrument after recovery attempt", player.getId());
            return; // Skip this player
        }

        try {
            // Get current instrument settings
            InstrumentWrapper instrument = player.getInstrument();
            int channel = player.getChannel();
            Integer preset = instrument.getPreset();

            if (preset != null) {
                // Apply bank select if needed
                if (instrument.getBankMSB() != 0 || instrument.getBankLSB() != 0) {
                    instrument.controlChange(0, instrument.getBankMSB());
                    instrument.controlChange(32, instrument.getBankLSB());
                    logger.debug("Sent bank select MSB: {}, LSB: {} for player {}",
                            instrument.getBankMSB(), instrument.getBankLSB(), player.getId());
                }

                // Send program change
                instrument.programChange(preset, 0);
                logger.info("Initialized player {} with instrument {} preset {} on channel {}",
                        player.getId(), instrument.getName(), preset, channel);
            }
        } catch (Exception e) {
            logger.error("Error initializing preset for player {}: {}",
                    player.getId(), e.getMessage());
        }
    }

    public void stop() {
        sequencerManager.stop();
        stopAllNotes();
        getPlayers().forEach(p -> p.setEnabled(false));

        // Reset to 1 instead of 0
        tick = 1;
        beat = 1.0;
        bar = 1;
        part = 1;

        // Reset counters to 0
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;

        setGranularBeat(0.0);

        sequencerManager.cleanup();
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() != null) {
            // // System.out.println("Session received action: " + action.getCommand());
            switch (action.getCommand()) {
                case Commands.TIMING_PARAMETERS_CHANGED -> sequencerManager.updateTimingParameters(getTempoInBPM(),
                        getTicksPerBeat(), getBeatsPerBar());
            }
        }
    }

    // Refactored onTick method with fixed references
    public void onTick() {

        try {
            tickCount++;

            // Calculate beat from tick
            int newBeat = (int) (tickCount / ticksPerBeat);
            if (newBeat > beatCount) {
                beat = (beat % beatsPerBar) + 1; // This cycles from 1 to parts
                beatCount = newBeat;
                timingBus.publish(Commands.TIMING_BEAT, this,
                        new TimingUpdate(null, beat, bar, part, tickCount, beatCount, barCount, partCount));
            } else if (tick == 1) {
                timingBus.publish(Commands.TIMING_BEAT, this,
                        new TimingUpdate(tick, beat, bar, part, tickCount, beatCount, barCount, partCount));
            }

            // Calculate bar from beat
            int newBar = beatCount / beatsPerBar;
            if (newBar > barCount) {
                bar = (bar % bars) + 1; // This cycles from 1 to parts
                barCount = newBar;
                timingBus.publish(Commands.TIMING_BAR, this,
                        new TimingUpdate(null, null, bar, part, tickCount, beatCount, barCount, partCount));
            } else if (tick == 1) {
                timingBus.publish(Commands.TIMING_BAR, this,
                        new TimingUpdate(null, null, bar, part, tickCount, beatCount, barCount, partCount));
            }

            // Part calculations on bar change - fix to only increment at partLength
            // boundaries
            int newPart = barCount / partLength;
            if (newPart > partCount) {
                part = (part % parts) + 1; // This cycles from 1 to parts
                partCount++;
                logger.debug("Part changed to {} (partCount={}) at bar {}", part, partCount, barCount);
                timingBus.publish(Commands.TIMING_PART, this,
                        new TimingUpdate(null, null, null, part, tickCount, beatCount, barCount, partCount));
            } else if (tick == 1) {
                timingBus.publish(Commands.TIMING_PART, this,
                        new TimingUpdate(null, null, null, part, tickCount, beatCount, barCount, partCount));
            }
        } catch (Exception e) {
            logger.error("Error in onTick", e);
        }

        timingBus.publish(Commands.TIMING_UPDATE, this,
                new TimingUpdate(tick, beat, bar, part, tickCount, beatCount, barCount, partCount));
        tick = tick % ticksPerBeat + 1;

    }

    public void syncToSequencer() {
        sequencerManager.updateTimingParameters(getTempoInBPM(), getTicksPerBeat(), getBeatsPerBar());
    }

    public void setTempoInBPM(float tempoInBPM) {
        this.tempoInBPM = tempoInBPM;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setTicksPerBeat(int ticksPerBeat) {
        this.ticksPerBeat = ticksPerBeat;
        // Notify about tempo change
        CommandBus.getInstance().publish(Commands.UPDATE_TEMPO, this, ticksPerBeat);

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setBeatsPerBar(int beatsPerBar) {
        this.beatsPerBar = beatsPerBar;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setBars(int bars) {
        this.bars = bars;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void setParts(int parts) {
        this.parts = parts;

        if (isRunning()) {
            syncToSequencer();
        }
        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    public void updatePlayer(Player updatedPlayer) {
        if (updatedPlayer == null || updatedPlayer.getId() == null) {
            return;
        }

        if (players == null) {
            players = new LinkedHashSet<>();
        }

        players.removeIf(p -> p.getId().equals(updatedPlayer.getId()));

        updatedPlayer.setSession(this);
        players.add(updatedPlayer);

        // Update tick listener registration based on enabled state
        if (updatedPlayer.getEnabled()) {
            registerTickListener(updatedPlayer);
        } else {
            unregisterTickListener(updatedPlayer);
        }

        CommandBus.getInstance().publish(Commands.SESSION_CHANGED, this, this);
    }

    /**
     * Register a timing listener that needs every tick event
     */
    public void registerTickListener(IBusListener listener) {
        if (listener != null) {
            if (tickListeners == null) {
                tickListeners = new HashSet<>();
            }
            tickListeners.add(listener);
        }
    }

    /**
     * Remove a timing listener
     */
    public void unregisterTickListener(IBusListener listener) {
        if (listener != null && tickListeners != null) {
            tickListeners.remove(listener);
        }
    }

    // Add this method to Session class to sync players with tickListeners
    private void syncPlayersWithTickListeners() {
        if (tickListeners == null) {
            tickListeners = new HashSet<>();
        }

        // Add all players to tickListeners
        if (players != null) {
            for (Player player : players) {
                if (player.getEnabled()) {
                    // Register player as a tick listener if not already registered
                    registerTickListener(player);
                }
            }
        }
    }

    /**
     * Add or update a player in the session
     *
     * @param player The player to add or update
     */
    public void addOrUpdatePlayer(Player player) {
        if (player == null || player.getId() == null) {
            logger.warn("Cannot add null player or player without ID");
            return;
        }

        // Skip default players - they should not be stored in sessions
        if (Boolean.TRUE.equals(player.getIsDefault())) {
            logger.debug("Skipping default player {} - not storing in session", player.getId());
            return;
        }

        // Store in players collection
        players.add(player);

        // Also store the player's instrument if it exists and is not default
        if (player.getInstrument() != null && !Boolean.TRUE.equals(player.getInstrument().getIsDefault())) {
            InstrumentManager.getInstance().updateInstrument(player.getInstrument());
        }

        // Flag session as modified
        setModified(true);
    }

}
