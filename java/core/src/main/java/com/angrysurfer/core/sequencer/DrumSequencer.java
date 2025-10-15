package com.angrysurfer.core.sequencer;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.api.midi.MIDIConstants;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.DrumStepParametersEvent;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.ReceiverManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.UserConfigManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Core sequencer engine that handles drum pattern sequencing and playback with
 * individual parameters per drum pad.
 */
@Getter
@Setter
public class DrumSequencer implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencer.class);

    // Reference to the data container
    // Add as static fields in both sequencer classes
    private static final ScheduledExecutorService SHARED_NOTE_SCHEDULER = Executors.newScheduledThreadPool(2);

    private final ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    private Integer id;

    private DrumSequenceData sequenceData;
    private Player[] players;
    // Event handling
    private Consumer<DrumStepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;
    private Consumer<NoteEvent> noteEventPublisher;
    // Add field to track if we're using internal synth
    private boolean usingInternalSynth = false;
    private Integer currentBar = null;
    // private Integer currentPart = null;
    private TimingUpdate lastTimingUpdate = null;

    /**
     * Creates a new drum sequencer with per-drum parameters
     */
    public DrumSequencer() {
        // Lightweight constructor: allocate arrays and data structures only.
        // Side-effectful initialization (synth/device access, bus registration,
        // loading sequences) is performed in initialize() to allow deterministic
        // startup ordering.
        players = new Player[SequencerConstants.DRUM_PAD_COUNT];
        this.sequenceData = new DrumSequenceData();
    }

    /**
     * Perform side-effectful initialization that requires other managers/singletons
     * to be available. This should be invoked by the central startup routine
     * (for example App) once SessionManager, DeviceManager, PlayerManager, and
     * InternalSynthManager have been initialized.
     */
    public synchronized void initialize() {
        // Make sure we have a working synthesizer
        if (!InternalSynthManager.getInstance().checkInternalSynthAvailable()) {
            logger.info("Initializing internal synthesizer for drum sequencer");
            InternalSynthManager.getInstance().initializeSynthesizer();
            usingInternalSynth = true;
        }

        // Initialize players array (touches SessionManager, DeviceManager, PlayerManager)
        initializePlayers();

        // Register with command and timing buses
        CommandBus.getInstance().register(this, new String[]{
                Commands.REPAIR_MIDI_CONNECTIONS,
                Commands.TIMING_UPDATE,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.UPDATE_TEMPO,
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_UPDATED,
                Commands.REFRESH_ALL_INSTRUMENTS,
                Commands.REFRESH_PLAYER_INSTRUMENT,
                Commands.PLAYER_INSTRUMENT_CHANGED
        });

        TimingBus.getInstance().register(this);

        // Load first saved sequence (if available) instead of default pattern
        loadFirstSequence();
    }

    /**
     * Initialize all player instances with proper MIDI setup
     */
    private void initializePlayers() {
        // First check if we have an active session
        Session activeSession = SessionManager.getInstance().getActiveSession();
        if (activeSession == null) {
            logger.error("Cannot initialize players - no active session");
            return;
        }

        // First try to get default MIDI device to reuse for all drum pads
        MidiDevice defaultDevice = DeviceManager.getInstance().getDefaultOutputDevice();
        if (defaultDevice == null) {
            logger.warn("No default MIDI output device available, attempting to get Gervill");
            defaultDevice = DeviceManager.getMidiDevice(SequencerConstants.GERVILL);
            if (defaultDevice != null && !defaultDevice.isOpen()) {
                try {
                    defaultDevice.open();
                    logger.info("Opened Gervill device for drum sequencer");
                } catch (Exception e) {
                    logger.error("Could not open Gervill device: {}", e.getMessage());
                }
            }
        }

        logger.info("Creating drum players with active connections");
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            // First check if player already exists in the session
            Player existingPlayer = UserConfigManager.getInstance().getStrikesOrderedById().get(i);
            //findExistingPlayerForDrum(activeSession, i);

            if (existingPlayer != null) {
                logger.info("Using existing player for drum pad {}: {}", i, existingPlayer.getId());
                players[i] = existingPlayer;
                players[i].setRootNote(sequenceData.getRootNotes()[i]);
                // Make sure the player has this sequencer as its owner
                if (players[i].getOwner() != this) {
                    players[i].setOwner(this);
                    //PlayerManager.getInstance().savePlayerProperties(players[i]);
                }

                InstrumentWrapper instrument = UserConfigManager.getInstance().findInstrumentById(existingPlayer.getInstrumentId());
                players[i].setInstrument(instrument);
                activeSession.getPlayers().add(players[i]);
            } else {
                // Create new player
                players[i] = RedisService.getInstance().newStrike();
                players[i].setOwner(this);
                players[i].setDefaultChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
                players[i].setRootNote(sequenceData.getRootNotes()[i]);
                players[i].setName(
                        InternalSynthManager.getInstance().getDrumName(SequencerConstants.MIDI_DRUM_NOTE_OFFSET + i));

                // Use PlayerManager to initialize the instrument
                PlayerManager.getInstance().initializeInternalInstrument(players[i], true, i);
                players[i].getInstrument().setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
                // Initialize device connections
                initializeDrumPadConnections(i, defaultDevice);

                // Add player to session
                activeSession.getPlayers().add(players[i]);

                logger.debug("Initialized drum pad {} with note {}", i, SequencerConstants.MIDI_DRUM_NOTE_OFFSET + i);
            }
        }

        // Save the session with all players
        SessionManager.getInstance().saveActiveSession();
    }

    /**
     * Find a player in the session that belongs to this drum pad
     */
    private Player findExistingPlayerForDrum(Session session, int drumIndex) {
        if (session == null) {
            return null;
        }

        // Look for a player that's associated with this sequencer and has the right
        // drum index
        for (Player p : session.getPlayers()) {
            if (p.getOwner() != null
                    && p.getOwner() instanceof DrumSequencer
                    && p.getClass().getSimpleName().equals("Strike")
                    && p.getRootNote() == SequencerConstants.MIDI_DRUM_NOTE_OFFSET + drumIndex) {

                return p;
            }
        }

        return null;
    }

    /**
     * Initialize a drum pad with proper device connections
     *
     * @param drumIndex     The index of the drum pad to initialize
     * @param defaultDevice The default MIDI device to use if a specific one
     *                      isn't available
     */
    private void initializeDrumPadConnections(int drumIndex, MidiDevice defaultDevice) {
        try {
            Player player = players[drumIndex];
            if (player == null || player.getInstrument() == null) {
                logger.warn("Cannot initialize connections for drum {} - no player or instrument", drumIndex);
                return;
            }

            InstrumentWrapper instrument = player.getInstrument();

            // Set the channel
            instrument.setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
            instrument.setReceivedChannels(new Integer[]{SequencerConstants.MIDI_DRUM_CHANNEL});

            // Try to get a device - first check if instrument already has one specified
            MidiDevice device = null;
            String deviceName = instrument.getDeviceName();

            if (deviceName != null && !deviceName.isEmpty()) {
                try {
                    device = DeviceManager.getInstance().acquireDevice(deviceName);
                    if (device != null && !device.isOpen()) {
                        device.open();
                        logger.debug("Opened specified device {} for drum {}", deviceName, drumIndex);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to open specified device {} for drum {}: {}",
                            deviceName, drumIndex, e.getMessage());
                    device = null;
                }
            }

            // If no device specified or couldn't open it, try the default
            if (device == null && defaultDevice != null) {
                device = defaultDevice;
                instrument.setDeviceName(defaultDevice.getDeviceInfo().getName());
                logger.debug("Using default device for drum {}: {}", drumIndex,
                        defaultDevice.getDeviceInfo().getName());
            }

            // If still no device, try Gervill specifically
            if (device == null) {
                try {
                    device = DeviceManager.getMidiDevice(SequencerConstants.GERVILL);
                    if (device != null) {
                        if (!device.isOpen()) {
                            device.open();
                        }
                        instrument.setDeviceName(SequencerConstants.GERVILL);
                        logger.debug("Using Gervill synthesizer for drum {}", drumIndex);
                    }
                } catch (Exception e) {
                    logger.warn("Could not use Gervill for drum {}: {}", drumIndex, e.getMessage());
                }
            }

            // If we have a device, set it on the instrument
            if (device != null) {
                instrument.setDevice(device);

                // Also ensure the instrument has a receiver
                try {
                    if (instrument.getReceiver() == null) {
                        Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(
                                instrument.getDeviceName(),
                                device);

                        if (receiver != null) {
                            // UPDATED: Now directly set the receiver (no AtomicReference)
                            instrument.setReceiver(receiver);
                            logger.debug("Set receiver for drum {}", drumIndex);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not get receiver for drum {}: {}", drumIndex, e.getMessage());
                }

                // Initialize the instrument's sound
                try {
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                    logger.debug("Applied preset for drum {}", drumIndex);
                } catch (Exception e) {
                    logger.warn("Could not apply preset for drum {}, {}", drumIndex, e.getMessage());
                }
            } else {
                logger.warn("No valid device found for drum {}", drumIndex);
            }
        } catch (Exception e) {
            logger.error("Error initializing connections for drum {}: {}", drumIndex, e.getMessage());
        }
    }

    public int getSwingPercentage() {
        return sequenceData.getSwingPercentage();
    }

    /**
     * Sets the global swing percentage
     *
     * @param percentage Value from 50 (no swing) to 75 (maximum swing)
     */
    public void setSwingPercentage(int percentage) {
        // Limit to valid range
        int value = Math.max(SequencerConstants.MIN_SWING, Math.min(SequencerConstants.MAX_SWING, percentage));
        sequenceData.setSwingPercentage(value);
        logger.info("Swing percentage set to: {}", value);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, -1 // -1 indicates global
                // parameter
        );
    }

    public boolean isSwingEnabled() {
        return sequenceData.isSwingEnabled();
    }

    public void setSwingEnabled(boolean enabled) {
        sequenceData.setSwingEnabled(enabled);
        logger.info("Swing enabled: {}", enabled);

        // Notify UI of parameter change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, -1);
    }

    /**
     * Load a sequence while preserving playback position if sequencer is
     * running
     *
     * @param sequenceId The ID of the sequence to load
     * @return true if sequence loaded successfully
     */
    public boolean loadSequence(long sequenceId) {
        // Don't do anything if trying to load the currently active sequence
        if (sequenceId == sequenceData.getId()) {
            logger.info("Sequence {} already loaded", sequenceId);
            return true;
        }

        // Store current playback state
        boolean wasPlaying = sequenceData.isPlaying();

        // Get the manager
        DrumSequencerManager manager = DrumSequencerManager.getInstance();

        // Load the sequence
        boolean loaded = manager.loadSequence(sequenceId, this);

        if (loaded) {
            logger.info("Loaded drum sequence: {}", sequenceId);
            logger.info(sequenceData.toString());
            sequenceData.setId(sequenceId);
            updateDrumRootNotesFromData();
            // Immediately update visual indicators without resetting
            if (stepUpdateListener != null) {
                for (int drumIndex = 0; drumIndex < SequencerConstants.DRUM_PAD_COUNT; drumIndex++) {
                    // Force an update with the current positions
                    stepUpdateListener
                            .accept(new DrumStepUpdateEvent(drumIndex, -1, sequenceData.getCurrentStep()[drumIndex]));
                }
            }

            // Publish event to notify UI components
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_LOADED, this, sequenceData.getId());

            // Preserve playing state (don't stop if we were playing)
            sequenceData.setPlaying(wasPlaying);

            return true;
        } else {
            logger.warn("Failed to load drum sequence {}", sequenceId);
            return false;
        }
    }

    /**
     * Load the first available sequence
     */
    private void loadFirstSequence() {
        try {
            // Get the manager
            DrumSequencerManager manager = DrumSequencerManager.getInstance();

            // Get the first sequence ID
            Long firstId = manager.getFirstSequenceId();

            if (firstId != null) {
                // Use our loadSequence method
                loadSequence(firstId);
            } else {
                logger.info("No saved drum sequences found, using empty pattern");
            }
        } catch (Exception e) {
            logger.error("Error loading initial drum sequence: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset the sequencer state
     *
     * @param preservePositions whether to preserve current positions
     */
    public void reset(boolean preservePositions) {

        this.lastTimingUpdate = null;
        sequenceData.reset(preservePositions);
        sequenceData.setMasterTempo(SessionManager.getInstance().getActiveSession().getTicksPerBeat());

        // Force the sequencer to generate an event to update visual indicators
        //        if (stepUpdateListener != null) {
        //            for (int drumIndex = 0; drumIndex < SequencerConstants.DRUM_PAD_COUNT; drumIndex++) {
        //                stepUpdateListener
        //                        .accept(new DrumStepUpdateEvent(drumIndex, -1, sequenceData.getCurrentStep()[drumIndex]));
        //            }
        //        }

        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, this);
        logger.debug("Sequencer reset - preservePositions={}", preservePositions);
    }

    /**
     * Reset with default behavior
     */
    public void reset() {
        reset(false);
    }

    /**
     * Process a timing tick
     *
     * @param tick The current tick count
     */
    public void processTick(long tick) {
        if (!sequenceData.isPlaying()) {
            return;
        }

        sequenceData.setTickCounter(tick);

        // Use the standard timing to determine global step changes
        int standardTicksPerStep = TimingDivision.NORMAL.getTicksPerBeat();

        // Update absoluteStep based on the tick count - for the global timing
        if (tick % standardTicksPerStep == 0) {
            // Increment the absoluteStep (cycle through the maximum pattern length)
            int newStep = (sequenceData.getAbsoluteStep() + 1) % sequenceData.getMaxPatternLength();
            sequenceData.setAbsoluteStep(newStep);
            logger.debug("Absolute step: {}", newStep);
        }

        // Reset pattern completion flag at the start of processing
        sequenceData.setPatternJustCompleted(false);

        // Process each drum separately
        for (int drumIndex = 0; drumIndex < SequencerConstants.DRUM_PAD_COUNT; drumIndex++) {
            // Skip if no Player configured
            if (players[drumIndex] == null) {
                continue;
            }

            // Use each drum's timing division
            TimingDivision division = sequenceData.getTimingDivisions()[drumIndex];
            int drumTicksPerStep = division.getTicksPerBeat();

            // Make sure we have a valid minimum value
            if (drumTicksPerStep <= 0) {
                drumTicksPerStep = 24; // Emergency fallback
            }

            // Use modulo for stability
            if (tick % drumTicksPerStep == 0) {
                // Reset pattern completion flag if we're looping
                if (sequenceData.getPatternCompleted()[drumIndex] && sequenceData.getLoopingFlags()[drumIndex]) {
                    sequenceData.getPatternCompleted()[drumIndex] = false;
                }

                // Skip if pattern is completed and not looping
                if (sequenceData.getPatternCompleted()[drumIndex] && !sequenceData.getLoopingFlags()[drumIndex]) {
                    continue;
                }

                // Process the current step for this drum
                processStep(drumIndex);

                logger.debug("Drum {} step processed at tick {} (timing: {})", drumIndex, tick,
                        division.getDisplayName());
            }
        }

        // Check for pattern completion
        if (sequenceData.areAllPatternsCompleted() && sequenceData.getNextPatternId() != null) {
            sequenceData.setPatternJustCompleted(true);

            // Switch to next pattern
            Long currentId = sequenceData.getId();
            loadSequence(sequenceData.getNextPatternId());

            // Notify about pattern switch
            CommandBus.getInstance().publish(Commands.DRUM_PATTERN_SWITCHED, this,
                    new PatternSwitchEvent(currentId, sequenceData.getNextPatternId()));

            // Clear the next pattern ID (one-shot behavior)
            sequenceData.setNextPatternId(null);
        }
    }

    /**
     * Set the next pattern to automatically switch to when the current pattern
     * completes
     *
     * @param patternId The ID of the next pattern, or null to disable automatic
     *                  switching
     */
    public void setNextPatternId(Long patternId) {
        sequenceData.setNextPatternId(patternId);
        logger.info("Set next drum pattern ID: {}", patternId);
    }

    /**
     * Process the current step for a drum
     *
     * @param drumIndex The drum to process
     */
    private void processStep(int drumIndex) {
        // Get the current step for this drum
        int step = sequenceData.getCurrentStep()[drumIndex];

        // Notify listeners of step update BEFORE playing the sound
        if (stepUpdateListener != null) {
            DrumStepUpdateEvent event = new DrumStepUpdateEvent(drumIndex, getPreviousStep(drumIndex), step);
            stepUpdateListener.accept(event);
        }

        // Trigger the drum step
        triggerDrumStep(drumIndex, step);

        // Calculate next step - store previous step for UI updates
        sequenceData.calculateNextStep(drumIndex);
    }

    /**
     * Calculate the previous step based on current direction
     */
    private int getPreviousStep(int drumIndex) {
        Direction direction = sequenceData.getDirections()[drumIndex];
        int currentPos = sequenceData.getCurrentStep()[drumIndex];
        int length = sequenceData.getPatternLengths()[drumIndex];

        return switch (direction) {
            case FORWARD -> (currentPos + length - 1) % length;
            case BACKWARD -> (currentPos + 1) % length;
            case BOUNCE -> {
                // For bounce, it depends on the current bounce direction
                if (sequenceData.getBounceDirections()[drumIndex] > 0) {
                    yield currentPos > 0 ? currentPos - 1 : 0;
                } else {
                    yield currentPos < length - 1 ? currentPos + 1 : length - 1;
                }
            }
            case RANDOM -> currentPos; // For random, just use current position
        };
    }

    /**
     * Trigger a drum step with per-step parameters
     *
     * @param drumIndex The drum pad index
     * @param stepIndex The step index
     */
    private void triggerDrumStep(int drumIndex, int stepIndex) {

        // Get player
        Player player = players[drumIndex];
        if (player == null || player.isMuted()) {
            return;
        }

        // Skip if step is inactive
        if (!sequenceData.isStepActive(drumIndex, stepIndex)) {
            return;
        }

        // Get all step parameters
        int velocity = sequenceData.getStepVelocities()[drumIndex][stepIndex];
        int probability = sequenceData.getStepProbabilities()[drumIndex][stepIndex];
        int decay = sequenceData.getStepDecays()[drumIndex][stepIndex];
        int nudge = sequenceData.getStepNudges()[drumIndex][stepIndex];

        // Check probability
        if (Math.random() * 100 >= probability) {
            return;
        }

        // Apply velocity scaling
        int finalVelocity = (int) (velocity * (sequenceData.getVelocities()[drumIndex] / 127.0));
        if (finalVelocity <= 0) {
            return;
        }


        // Set instrument if needed
        if (player.getInstrument() == null && player.getInstrumentId() != null) {
            player.setInstrument(InstrumentManager.getInstance().getInstrumentById(player.getInstrumentId()));
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
                player.getInstrument().setReceivedChannels(new Integer[]{SequencerConstants.MIDI_DRUM_CHANNEL});
            }
        }

        // Process and send effects before playing the note
        processEffects(drumIndex, stepIndex, player);

        // Apply swing if needed
        if (sequenceData.isSwingEnabled() && stepIndex % 2 == 1) {
            nudge += calculateSwingAmount(drumIndex);
        }

        // Now trigger the note
        final int offset = player.getOffset();
        final int sessionOffset = player.getFollowSessionOffset() ? SessionManager.getInstance().getActiveSession().getNoteOffset() : 0;
        final int finalNoteNumber = player.getRootNote() + offset + sessionOffset;

        int actualVelocity = getSequenceData().isStepAccented(drumIndex, stepIndex) ?
                Math.min(finalVelocity + 20, 126) : finalVelocity;

        final int finalDecay = decay;
        final int finalDrumIndex = drumIndex;
        final int finalActualVelocity = actualVelocity;

        if (nudge > 0) {
            // Delayed note
            SHARED_NOTE_SCHEDULER.schedule(() -> {
                player.noteOn(finalNoteNumber, finalActualVelocity, finalDecay);
                publishNoteEvent(finalDrumIndex, finalActualVelocity, finalDecay);
            }, nudge, TimeUnit.MILLISECONDS);
        } else {
            // Immediate note
            player.noteOn(finalNoteNumber, finalActualVelocity, decay);
            publishNoteEvent(drumIndex, finalActualVelocity, decay);
        }
    }

    /**
     * Publish a note event to listeners
     */
    private void publishNoteEvent(int drumIndex, int velocity, int durationMs) {
        if (noteEventPublisher != null) {
            // Convert drum index back to MIDI note (36=kick, etc.)
            int midiNote = drumIndex + SequencerConstants.MIDI_DRUM_NOTE_OFFSET;
            NoteEvent event = new NoteEvent(midiNote, velocity, durationMs);
            noteEventPublisher.accept(event);
        }
    }

    /**
     * Calculate swing amount in milliseconds based on current tempo and timing
     * division
     */
    private int calculateSwingAmount(int drumIndex) {
        // Get session BPM
        float bpm = SessionManager.getInstance().getActiveSession().getTempoInBPM();
        if (bpm <= 0) {
            bpm = 120; // Default fallback
        }
        // Calculate step duration in milliseconds
        TimingDivision division = sequenceData.getTimingDivisions()[drumIndex];
        float stepDurationMs = 60000f / bpm; // Duration of quarter note in ms

        // Adjust for timing division based on actual enum values
        switch (division) {
            case NORMAL -> stepDurationMs *= 1; // No change for normal timing
            case DOUBLE -> stepDurationMs /= 2; // Double time (faster)
            case HALF -> stepDurationMs *= 2; // Half-time (slower)
            case QUARTER -> stepDurationMs *= 4; // Quarter time (very slow)
            case TRIPLET -> stepDurationMs *= 2.0f / 3.0f; // Triplet feel
            case QUARTER_TRIPLET -> stepDurationMs *= 4.0f / 3.0f; // Quarter note triplets
            case EIGHTH_TRIPLET -> stepDurationMs *= 1.0f / 3.0f; // Eighth note triplets
            case SIXTEENTH -> stepDurationMs *= 1.0f / 4.0f; // Sixteenth notes
            case SIXTEENTH_TRIPLET -> stepDurationMs *= 1.0f / 6.0f; // Sixteenth note triplets
            case BEBOP -> stepDurationMs *= 1; // Same as normal for swing calculations
            case FIVE_FOUR -> stepDurationMs *= 5.0f / 4.0f; // 5/4 time
            case SEVEN_EIGHT -> stepDurationMs *= 7.0f / 8.0f; // 7/8 time
            case NINE_EIGHT -> stepDurationMs *= 9.0f / 8.0f; // 9/8 time
            case TWELVE_EIGHT -> stepDurationMs *= 12.0f / 8.0f; // 12/8 time
            case SIX_FOUR -> stepDurationMs *= 6.0f / 4.0f; // 6/4 time
        }

        // Calculate swing percentage (convert from 50-75% to 0-25%)
        float swingFactor = (sequenceData.getSwingPercentage() - 50) / 100f;

        // Return swing amount in milliseconds
        return (int) (stepDurationMs * swingFactor);
    }

    /**
     * Calculate ticks per step based on timing division
     */
    private int calculateTicksPerStep(TimingDivision timing) {
        // Add safety check to prevent division by zero
        int masterTempo = sequenceData.getMasterTempo();
        if (masterTempo <= 0) {
            logger.warn("Invalid masterTempo value ({}), using default of {}", masterTempo,
                    SequencerConstants.DEFAULT_MASTER_TEMPO);
            masterTempo = SequencerConstants.DEFAULT_MASTER_TEMPO; // Emergency fallback
        }

        double ticksPerBeat = timing.getTicksPerBeat();
        if (ticksPerBeat <= 0) {
            logger.warn("Invalid ticksPerBeat value ({}), using default of {}", ticksPerBeat,
                    SequencerConstants.DEFAULT_TICKS_PER_BEAT);
            ticksPerBeat = SequencerConstants.DEFAULT_TICKS_PER_BEAT; // Emergency fallback
        }

        // Simplified calculation that works consistently
        int result = (int) (masterTempo / (ticksPerBeat / 24.0));

        // Add safety check for the final result
        if (result <= 0) {
            logger.warn("Calculated invalid ticksPerStep ({}), using default of 24", result);
            result = 24; // Emergency fallback for extreme values
        }

        return result;
    }

    /**
     * Update master tempo from session
     */
    public void updateMasterTempo(int sessionTicksPerBeat) {
        sequenceData.setMasterTempo(sessionTicksPerBeat);
        logger.info("Updated master tempo to {}", sessionTicksPerBeat);

        // Recalculate all next step timings based on new tempo
        for (int drumIndex = 0; drumIndex < SequencerConstants.DRUM_PAD_COUNT; drumIndex++) {
            if (sequenceData.getTimingDivisions()[drumIndex] != null) {
                int calculatedTicksPerStep = calculateTicksPerStep(sequenceData.getTimingDivisions()[drumIndex]);
                sequenceData.getNextStepTick()[drumIndex] = sequenceData.getTickCounter() + calculatedTicksPerStep;
            }
        }
    }

    /**
     * Start playback
     */
    public void play() {
        sequenceData.setPlaying(true);

        // Reset step positions to ensure consistent playback
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            // Set all next step ticks to the current tick to trigger immediately
            sequenceData.getNextStepTick()[i] = sequenceData.getTickCounter();

            // Reset pattern completion flags
            sequenceData.getPatternCompleted()[i] = false;
        }

        logger.info("DrumSequencer playback started at tick {}", sequenceData.getTickCounter());
    }

    /**
     * Start playback with reset
     */
    public void start() {
        if (!sequenceData.isPlaying()) {
            reset();
            // ensureDeviceConnections();
            sequenceData.setPlaying(true);
        }
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (sequenceData.isPlaying()) {
            sequenceData.setPlaying(false);
            reset();
        }
    }

    /**
     * Get whether the sequencer is currently playing
     */
    public boolean isPlaying() {
        return sequenceData.isPlaying();
    }

    /**
     * Toggle a step in the pattern
     */
    public void toggleAccent(int drumIndex, int step) {
        if (!isValidDrumAndStep(drumIndex, step)) {
            return;
        }

        boolean newState = !getSequenceData().isStepAccented(drumIndex, step);

        sequenceData.setStepAccent(drumIndex, step, newState);
        CommandBus.getInstance().publish(
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                this,
                new DrumStepParametersEvent(this, getSelectedPadIndex(), this.getSelectedPadIndex())
        );
    }

    public void toggleStep(int drumIndex, int step) {
        if (!isValidDrumAndStep(drumIndex, step)) {
            return;
        }

        boolean newState = !isStepActive(drumIndex, step);

        // Set the new state
        sequenceData.setStepActive(drumIndex, step, newState);
        CommandBus.getInstance().publish(
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                DrumSequenceModifier.class,
                new DrumStepParametersEvent(this, drumIndex, step)
        );
    }

    /**
     * Checks if the drum index and step index are valid
     *
     * @param drumIndex The drum index to check
     * @param stepIndex The step index to check
     * @return true if both indices are valid
     */
    private boolean isValidDrumAndStep(int drumIndex, int stepIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index: {}", drumIndex);
            return false;
        }

        if (stepIndex < 0 || stepIndex >= sequenceData.getMaxPatternLength()) {
            logger.warn("Invalid step index: {} (max: {})",
                    stepIndex, sequenceData.getMaxPatternLength() - 1);
            return false;
        }

        return true;
    }

    /**
     * Get the pattern length for a drum
     */
    public int getPatternLength(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getPatternLength", drumIndex);
            return sequenceData.getDefaultPatternLength();
        }
        return sequenceData.getPatternLengths()[drumIndex];
    }

    /**
     * Get the pattern length for all drums (uses the default pattern length)
     *
     * @return The global pattern length
     */
    public int getPatternLength() {
        // Return the default pattern length as a global value
        // Or use the selected drum's pattern length
        int selectedDrumIndex = sequenceData.getSelectedPadIndex();
        return getPatternLength(selectedDrumIndex);
    }

    /**
     * Set pattern length for the currently selected drum pad
     */
    public void setPatternLength(int length) {
        setPatternLength(sequenceData.getSelectedPadIndex(), length);
    }

    /**
     * Set the pattern length for a drum
     */
    public void setPatternLength(int drumIndex, int length) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && length > 0
                && length <= sequenceData.getMaxPatternLength()) {
            logger.info("Setting pattern length for drum {} to {}", drumIndex, length);
            sequenceData.getPatternLengths()[drumIndex] = length;

            // Ensure the current step is within bounds
            if (sequenceData.getCurrentStep()[drumIndex] >= length) {
                sequenceData.getCurrentStep()[drumIndex] = 0;
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);

            // Reset this drum if we're changing length while playing
            if (sequenceData.isPlaying()) {
                resetDrum(drumIndex);
            }
        } else {
            logger.warn("Invalid pattern length: {} for drum {} (must be 1-{})", length, drumIndex,
                    sequenceData.getMaxPatternLength());
        }
    }

    /**
     * Reset a drum to the beginning of its pattern
     */
    private void resetDrum(int drumIndex) {
        // Reset current step based on playback direction
        switch (sequenceData.getDirections()[drumIndex]) {
            case FORWARD:
                sequenceData.getCurrentStep()[drumIndex] = 0;
                break;
            case BACKWARD:
                sequenceData.getCurrentStep()[drumIndex] = sequenceData.getPatternLengths()[drumIndex] - 1;
                break;
            case BOUNCE:
                sequenceData.getBounceDirections()[drumIndex] = 1; // Start forward
                sequenceData.getCurrentStep()[drumIndex] = 0;
                break;
            case RANDOM:
                sequenceData.getCurrentStep()[drumIndex] = (int) (Math.random() * sequenceData.getPatternLengths()[drumIndex]);
                break;
        }

        // Reset pattern completion flag
        sequenceData.getPatternCompleted()[drumIndex] = false;

        // If playing, also reset the next step time
        if (sequenceData.isPlaying()) {
            // Calculate appropriate step timing based on timing division
            int stepTiming = calculateTicksPerStep(sequenceData.getTimingDivisions()[drumIndex]);
            sequenceData.getNextStepTick()[drumIndex] = sequenceData.getTickCounter() + stepTiming;
        }
    }

    /**
     * Get the direction for a drum
     */
    public Direction getDirection(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getDirection", drumIndex);
            return Direction.FORWARD;
        }
        return sequenceData.getDirections()[drumIndex];
    }

    /**
     * Set the direction for a drum
     */
    public void setDirection(int drumIndex, Direction direction) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            sequenceData.getDirections()[drumIndex] = direction;

            // If playing in bounce mode, make sure bounce direction is set correctly
            if (direction == Direction.BOUNCE) {
                // Initialize bounce direction if needed
                if (sequenceData.getBounceDirections()[drumIndex] == 0) {
                    sequenceData.getBounceDirections()[drumIndex] = 1; // Start forward
                }
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the timing division for a drum
     */
    public TimingDivision getTimingDivision(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for getTimingDivision", drumIndex);
            return TimingDivision.NORMAL;
        }
        return sequenceData.getTimingDivisions()[drumIndex];
    }

    /**
     * Set the timing division for a drum
     */
    public void setTimingDivision(int drumIndex, TimingDivision division) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            sequenceData.getTimingDivisions()[drumIndex] = division;

            // Reset the drum's next step time to apply the new timing
            if (sequenceData.isPlaying()) {
                resetDrum(drumIndex);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get whether a drum is looping
     */
    public boolean isLooping(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            logger.warn("Invalid drum index {} for isLooping", drumIndex);
            return true;
        }
        return sequenceData.getLoopingFlags()[drumIndex];
    }

    /**
     * Set whether a drum should loop
     * The looping state change will take effect at the end of the current cycle
     */
    public void setLooping(int drumIndex, boolean loop) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            // Set the looping flag in the sequence data
            sequenceData.getLoopingFlags()[drumIndex] = loop;

            // Don't immediately reset the drum, let the pattern complete naturally
            // This ensures looping changes only affect subsequent cycles

            // Only if we're re-enabling looping for a completed pattern that's not playing
            // should we reset it immediately
            if (loop && sequenceData.getPatternCompleted()[drumIndex] &&
                    !isPatternPlaying(drumIndex) && sequenceData.isPlaying()) {
                sequenceData.getPatternCompleted()[drumIndex] = false;
                resetDrum(drumIndex);
                logger.info("Restarting drum {} due to looping re-enabled", drumIndex);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
            logger.info("Looping for drum {} set to {} (will take effect after cycle completes)", drumIndex, loop);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the velocity for a drum
     */
    public int getVelocity(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            return sequenceData.getVelocities()[drumIndex];
        }
        return SequencerConstants.DEFAULT_VELOCITY;
    }

    /**
     * Set the velocity for a drum
     */
    public void setVelocity(int drumIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            // Constrain to valid MIDI range
            velocity = Math.max(0, Math.min(SequencerConstants.MAX_MIDI_VELOCITY, velocity));
            sequenceData.getVelocities()[drumIndex] = velocity;

            // If we have a Player object for this drum, update its level
            Player player = getPlayer(drumIndex);
            if (player != null) {
                player.setLevel(velocity);
            }

            // Notify UI of parameter change
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);
        } else {
            logger.warn("Invalid drum index: {}", drumIndex);
        }
    }

    /**
     * Get the velocity for a specific step
     */
    public int getStepVelocity(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepVelocities()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the velocity for a specific step
     */
    public void setStepVelocity(int drumIndex, int stepIndex, int velocity) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepVelocities()[drumIndex][stepIndex] = velocity;
        }
    }

    /**
     * Get the decay for a specific step
     */
    public int getStepDecay(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepDecays()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the decay for a specific step
     */
    public void setStepDecay(int drumIndex, int stepIndex, int decay) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepDecays()[drumIndex][stepIndex] = decay;
        }
    }

    /**
     * Get the probability for a specific step
     */
    public int getStepProbability(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepProbabilities()[drumIndex][stepIndex];
        }
        return SequencerConstants.DEFAULT_PROBABILITY;
    }

    /**
     * Set the probability for a specific step
     */
    public void setStepProbability(int drumIndex, int stepIndex, int probability) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            // Clamp value between 0-100
            sequenceData.getStepProbabilities()[drumIndex][stepIndex] = Math.max(0, Math.min(100, probability));
        }
    }

    /**
     * Get the nudge for a specific step
     */
    public int getStepNudge(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepNudges()[drumIndex][stepIndex];
        }
        return 0;
    }

    /**
     * Set the nudge for a specific step
     */
    public void setStepNudge(int drumIndex, int stepIndex, int nudge) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepNudges()[drumIndex][stepIndex] = nudge;
        }
    }

    /**
     * Get the pan position for a specific step
     */
    public int getStepPan(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepPans()[drumIndex][stepIndex];
        }
        return SequencerConstants.DEFAULT_PAN;
    }

    /**
     * Set the pan position for a specific step
     */
    public void setStepPan(int drumIndex, int stepIndex, int pan) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepPans()[drumIndex][stepIndex] = Math.max(0, Math.min(127, pan));
        }
    }

    /**
     * Get the chorus amount for a specific step
     */
    public int getStepChorus(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepChorus()[drumIndex][stepIndex];
        }
        return SequencerConstants.DEFAULT_CHORUS;
    }

    /**
     * Set the chorus amount for a specific step
     */
    public void setStepChorus(int drumIndex, int stepIndex, int chorus) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepChorus()[drumIndex][stepIndex] = Math.max(0, Math.min(100, chorus));
        }
    }

    /**
     * Get the reverb amount for a specific step
     */
    public int getStepReverb(int drumIndex, int stepIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            return sequenceData.getStepReverb()[drumIndex][stepIndex];
        }
        return SequencerConstants.DEFAULT_REVERB;
    }

    /**
     * Set the reverb amount for a specific step
     */
    public void setStepReverb(int drumIndex, int stepIndex, int reverb) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT && stepIndex >= 0
                && stepIndex < sequenceData.getMaxPatternLength()) {
            sequenceData.getStepReverb()[drumIndex][stepIndex] = Math.max(0, Math.min(100, reverb));
        }
    }

    /**
     * Get the currently selected drum pad index
     */
    public int getSelectedPadIndex() {
        return sequenceData.getSelectedPadIndex();
    }

    /**
     * Set the currently selected drum pad index
     */
    public void setSelectedPadIndex(int index) {
        if (index >= 0 && index < SequencerConstants.DRUM_PAD_COUNT) {
            // Store old selection
            int oldSelection = sequenceData.getSelectedPadIndex();

            // Set new selection
            sequenceData.setSelectedPadIndex(index);

            // Notify listeners of selection change
            CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED, this,
                    new DrumPadSelectionEvent(oldSelection, index));

            logger.info("Selected drum pad index updated to: {}", index);
        } else {
            logger.warn("Invalid drum pad index: {}", index);
        }
    }

    /**
     * Set direction for the currently selected drum pad
     */
    public void setDirection(Direction direction) {
        setDirection(sequenceData.getSelectedPadIndex(), direction);
    }

    /**
     * Set timing division for the currently selected drum pad
     */
    public void setTimingDivision(TimingDivision division) {
        setTimingDivision(sequenceData.getSelectedPadIndex(), division);
    }

    /**
     * Set looping for the currently selected drum pad
     */
    public void setLooping(boolean loop) {
        setLooping(sequenceData.getSelectedPadIndex(), loop);
    }

    /**
     * Get the default pattern length
     */
    public int getDefaultPatternLength() {
        return sequenceData.getDefaultPatternLength();
    }

    /**
     * Get the maximum pattern length
     */
    public int getMaxPatternLength() {
        return sequenceData.getMaxPatternLength();
    }

    /**
     * Set maximum pattern length
     */
    public void setMaxPatternLength(int length) {
        if (length >= sequenceData.getDefaultPatternLength()) {
            sequenceData.setMaxPatternLength(length);
        }
    }

    /**
     * Select a drum pad and notify listeners
     */
    public void selectDrumPad(int padIndex) {
        setSelectedPadIndex(padIndex);
    }

    /**
     * Get the Player object for a specific drum pad
     */
    public Player getPlayer(int drumIndex) {
        if (drumIndex >= 0 && drumIndex < SequencerConstants.DRUM_PAD_COUNT) {
            return players[drumIndex];
        }
        return null;
    }

    /**
     * Check if a specific step is active for a drum
     */
    public boolean isStepActive(int drumIndex, int stepIndex) {
        return sequenceData.isStepActive(drumIndex, stepIndex);
    }

    /**
     * Generate a simple pattern for the specified drum
     */
    public void generatePattern(int density) {
        // Generate pattern for selected drum pad
        int drumIndex = sequenceData.getSelectedPadIndex();
        sequenceData.generatePattern(drumIndex, density);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_PARAMS_CHANGED, this, drumIndex);

        logger.info("Generated pattern for drum {}", drumIndex);
    }

    /**
     * Push the pattern forward by one step for the selected drum pad
     */
    public void pushForward() {
        int drumIndex = sequenceData.getSelectedPadIndex();
        DrumSequenceModifier.pushPatternForward(this, drumIndex);
        logger.info("Pushed pattern forward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, null);
    }

    /**
     * Pull the pattern backward by one step for the selected drum pad
     */
    public void pullBackward() {
        int drumIndex = sequenceData.getSelectedPadIndex();
        DrumSequenceModifier.pullPatternBackward(this, drumIndex);
        logger.info("Pulled pattern backward for drum {}", drumIndex);

        // Notify UI of pattern change
        CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, null);
    }

    /**
     * Required by IBusListener interface
     */
    @Override
    public void onAction(Command cmd) {
        if (cmd == null || cmd.getCommand() == null) {
            return;
        }
        // Handle other commands
        switch (cmd.getCommand()) {
            case Commands.TIMING_UPDATE -> handleTimingUpdate(cmd);
            case Commands.UPDATE_TEMPO -> handleTempoUpdate(cmd);
            case Commands.REPAIR_MIDI_CONNECTIONS ->
                    DrumSequencerManager.getInstance().repairSequencerConnections(this);
            case Commands.TRANSPORT_START -> start();
            case Commands.TRANSPORT_STOP -> reset();
        }
    }

    private void handleTempoUpdate(Command cmd) {
        if (cmd.getData() instanceof Integer ticksPerBeat) {
            updateMasterTempo(ticksPerBeat);
        } else if (cmd.getData() instanceof Float) {
            // If BPM is sent instead, get ticksPerBeat from session
            int tpb = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
            updateMasterTempo(tpb);
        }
    }

    private void handleTimingUpdate(Command action) {
        if (action.getData() instanceof TimingUpdate update) {
            this.lastTimingUpdate = update;
            if (update.bar() != null) {
                // Adjust for 0-based index
                int bar = update.bar() - 1;
                if (currentBar == null || bar != currentBar) {
                    currentBar = bar;
                    logger.debug("Current bar updated to {}", currentBar);

                    for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++)
                        if (sequenceData.getBarMuteValue(i, currentBar) != players[i].isMuted())
                            players[i].setMuted(!players[i].isMuted());

                    for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                        int offset = sequenceData.getBarOffsetValue(i, currentBar);
                        if (offset != players[i].getOffset())
                            players[i].setOffset(offset);
                    }
                }
            }
            // Process tick for note sequencing
            if (update.tick() != null && sequenceData.isPlaying())
                processTick(update.tick());
        }
    }

    /**
     * Process effects for a single step
     */
    private void processEffects(int drumIndex, int stepIndex, Player player) {
        // Skip if the step is inactive or player has no instrument
        if (!sequenceData.isStepActive(drumIndex, stepIndex) || player == null || player.getInstrument() == null) {
            return;
        }

        try {
            // Get current effect values
            int pan = sequenceData.getStepPans()[drumIndex][stepIndex];
            int reverb = sequenceData.getStepReverb()[drumIndex][stepIndex];
            int chorus = sequenceData.getStepChorus()[drumIndex][stepIndex];
            int decay = sequenceData.getStepDecays()[drumIndex][stepIndex];

            // Count how many effects need to be sent
            int effectCount = 0;

            // Only add effects that have changed
            if (pan != sequenceData.getLastPanValues()[drumIndex][stepIndex]) {
                sequenceData.getEffectControllers()[effectCount] = MIDIConstants.CC_PAN;
                sequenceData.getEffectValues()[effectCount] = pan;
                sequenceData.getLastPanValues()[drumIndex][stepIndex] = pan;
                effectCount++;
            }

            if (reverb != sequenceData.getLastReverbValues()[drumIndex][stepIndex]) {
                sequenceData.getEffectControllers()[effectCount] = MIDIConstants.CC_REVERB;
                sequenceData.getEffectValues()[effectCount] = reverb;
                sequenceData.getLastReverbValues()[drumIndex][stepIndex] = reverb;
                effectCount++;
            }

            if (chorus != sequenceData.getLastChorusValues()[drumIndex][stepIndex]) {
                sequenceData.getEffectControllers()[effectCount] = MIDIConstants.CC_CHORUS;
                sequenceData.getEffectValues()[effectCount] = chorus;
                sequenceData.getLastChorusValues()[drumIndex][stepIndex] = chorus;
                effectCount++;
            }

            if (decay != sequenceData.getLastDecayValues()[drumIndex][stepIndex]) {
                sequenceData.getEffectControllers()[effectCount] = MIDIConstants.CC_DELAY; // Using delay CC for decay
                sequenceData.getEffectValues()[effectCount] = decay;
                sequenceData.getLastDecayValues()[drumIndex][stepIndex] = decay;
                effectCount++;
            }

            // Send effects only if needed
            if (effectCount > 0) {
                int[] controllers = Arrays.copyOf(sequenceData.getEffectControllers(), effectCount);
                int[] values = Arrays.copyOf(sequenceData.getEffectValues(), effectCount);

                player.getInstrument().sendBulkCC(controllers, values);
            }
        } catch (Exception e) {
            // Just ignore errors to avoid performance impact
        }
    }

    /**
     * Ensure all drum players have valid device connections Now delegates to
     * DrumSequencerManager
     */
    public void ensureDeviceConnections() {
        DrumSequencerManager.getInstance().repairSequencerConnections(this);
    }

    /**
     * Attempts to repair MIDI connections if they have been lost Now delegates
     * to DrumSequencerManager
     */
    public void repairMidiConnections() {
        DrumSequencerManager.getInstance().repairSequencerConnections(this);
    }

    // Add a method to update root notes
    // Add a getter method for drum root note

    /**
     * Update all drum root notes from the sequence data Called after loading a
     * sequence
     */
    private void updateDrumRootNotesFromData() {
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = players[i];
            int rootNote = sequenceData.getRootNotes()[i];

            if (player != null && rootNote > 0) {
                // Update player root note
                player.setRootNote(rootNote);

                // Update player name if possible
                String drumName = InternalSynthManager.getInstance().getDrumName(rootNote);
                if (drumName != null && !drumName.isEmpty()) {
                    player.setName(drumName);
                }
            }
        }

        // Log results
        logger.info("Updated drum root notes from sequence data");
    }

    /**
     * Refresh a player from its data source
     *
     * @param index The player index to refresh
     */
    public void refreshPlayer(int index) {
        if (index < 0 || index >= players.length || players[index] == null) {
            return;
        }

        Player player = players[index];
        Player refreshedPlayer = PlayerManager.getInstance().getPlayerById(player.getId());

        if (refreshedPlayer != null) {
            // Update our player reference with refreshed data
            players[index] = refreshedPlayer;
            // Ensure owner is set
            refreshedPlayer.setOwner(this);
            logger.debug("Refreshed player at index {}: {}", index, refreshedPlayer.getName());
        }
    }

    /**
     * Calculate ticks per step for the given drum index based on its timing
     * division
     *
     * @param drumIndex The drum index to calculate ticks for
     * @return The number of ticks per step
     */
    public int getTicksPerStep(int drumIndex) {
        // Access the timing division from the array
        if (sequenceData != null && drumIndex >= 0
                && drumIndex < sequenceData.getTimingDivisions().length) {

            TimingDivision division = sequenceData.getTimingDivisions()[drumIndex];
            if (division != null) {
                // Use the ticksPerBeat value directly from the enum
                return division.getTicksPerBeat();
            }
        }

        // Default to NORMAL timing (24 ticks per beat)
        return 24;
    }

    /**
     * Get the ticks per step using default timing (for the overall sequencer)
     *
     * @return Default ticks per step
     */
    public int getTicksPerStep() {
        // Use the first drum as reference or a default value
        if (sequenceData != null) {
            return sequenceData.getTicksPerStep();
        }
        return 24; // Default
    }

    public void ensurePlayerHasInstrument(int i) {
        if (players != null && players.length > i && players[i].getInstrument() == null) {
            PlayerManager.getInstance().initializeInternalInstrument(players[i], true, players[i].getId().intValue());
        }

    }

    public boolean isStepAccented(int drumIndex, int step) {
        return sequenceData.isStepAccented(drumIndex, step);
    }

    /**
     * Check if a specific drum pattern is currently playing
     *
     * @param drumIndex The drum pad index
     * @return true if the pattern is actively playing, false if it's stopped
     */
    private boolean isPatternPlaying(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT) {
            return false;
        }

        // Return false if the pattern is completed or if the sequencer is not playing
        return sequenceData.isPlaying() && !sequenceData.getPatternCompleted()[drumIndex];
    }
}
