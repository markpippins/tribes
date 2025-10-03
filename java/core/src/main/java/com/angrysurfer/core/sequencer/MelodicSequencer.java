package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.MelodicScaleSelectionEvent;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.event.StepUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.MelodicSequencerManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.SoundbankManager;
import com.angrysurfer.core.service.UserConfigManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {

    private static final ScheduledExecutorService SHARED_NOTE_SCHEDULER = Executors.newScheduledThreadPool(2);

    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);

    private final ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();

    private Integer id;

    private TimingUpdate lastTimingUpdate;

    private boolean loopingToggled;
    private boolean isPlaying = false; // Flag indicating playback state

    private int currentStep = 0; // Current step in the pattern
    private long tickCounter = 0; // Tick counter
    private int bounceDirection = 1; // Direction for bounce mode
    private Integer currentBar = null;
    private MelodicSequenceData sequenceData = new MelodicSequenceData();
    private Boolean[] scaleNotes; // Computed from root note and scale
    private Quantizer quantizer; // Computed from scale notes
    private int masterTempo;

    private boolean latchEnabled = false;
    private Long nextPatternId = null;
    private long lastNoteTriggeredTime = 0;
    private Player player;
    private int currentTilt = 0;
    private Random rand = new Random();

    private Consumer<NoteEvent> noteEventPublisher;
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;

    public MelodicSequencer(Integer id) {
        setId(id);
        initializePlayer(SequencerConstants.MELODIC_CHANNELS[id]);

        // Initialize with default or first available sequence
        MelodicSequencerManager.getInstance().initializeSequencer(this, null);

        CommandBus.getInstance().register(this, new String[]{
                Commands.REPAIR_MIDI_CONNECTIONS,
                Commands.TIMING_UPDATE,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.REFRESH_ALL_INSTRUMENTS,
                Commands.PLAYER_PRESET_CHANGE_EVENT,
                Commands.PLAYER_PRESET_CHANGED,
                Commands.PLAYER_INSTRUMENT_CHANGE_EVENT,
                Commands.REFRESH_PLAYER_INSTRUMENT,
                Commands.SYSTEM_READY,
                Commands.LOOPING_TOGGLE_EVENT,
                Commands.GLOBAL_SCALE_SELECTION_EVENT,
                Commands.SCALE_SELECTED,
                Commands.ROOT_NOTE_SELECTED
        });

        TimingBus.getInstance().register(this);
        updateQuantizer();
        logger.info("MelodicSequencer {} initialized and registered with CommandBus", id);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setSequenceData(MelodicSequenceData data) {
        this.sequenceData = data;
        updateQuantizer();
    }

    public void updateQuantizer() {
        scaleNotes = sequenceData.createScaleArray(sequenceData.getRootNote(), sequenceData.getScale());
        quantizer = new Quantizer(scaleNotes);
        logger.info("Quantizer updated with root note {} and scale {}",
                sequenceData.getRootNote(), sequenceData.getScale());
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability, int nudge) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate, probability, nudge);
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate);
    }

    public void start() {
        if (isPlaying) {
            return;
        }

        SoundbankManager.getInstance().updatePlayerSound(player, sequenceData.getSoundbankName(),
                sequenceData.getBankIndex(), sequenceData.getPreset());


        currentBar = null;
        currentStep = 0;

        if (getHarmonicTiltValues() == null || getHarmonicTiltValues().isEmpty()) {
            logger.warn("No harmonic tilt values set, using default 0");
            setHarmonicTiltValues(Collections.singletonList(0));
        }
        currentTilt = getHarmonicTiltValues().getFirst();
        if (currentStep >= getSequenceData().getPatternLength()) {
            reset();
        }

        SoundbankManager.getInstance().applyInstrumentPreset(player);

        isPlaying = getSequenceData().isLooping();
        logger.info("Melodic sequencer {} started playback", id);
        CommandBus.getInstance().publish(Commands.SEQUENCER_STATE_CHANGED, this,
                Map.of("sequencerId", id, "state", "started"));
    }

    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            currentStep = 0;
            currentBar = null;
            logger.info("Melodic sequencer playback stopped");
        }
    }

    public void processTick(Long tick) {
        System.out.println("MelodicSequencer.processTick: " + tick);
        if (!isPlaying || tick == null) {
            return;
        }

        logger.info("MelodicSequencer processTick: " + tick);

        tickCounter = tick;

        int ticksForDivision = sequenceData.getTimingDivision().getTicksPerBeat();

        if (ticksForDivision <= 0) {
            ticksForDivision = SequencerConstants.DEFAULT_MASTER_TEMPO; // Emergency fallback
        }

        if (tick % ticksForDivision == 0) {
            int prevStep = currentStep;

            calculateNextStep();

            if (stepUpdateListener != null) {
                stepUpdateListener.accept(new StepUpdateEvent(prevStep, currentStep));
            }

            if (player.getEnabled() & player.getLevel() > 0)
                SHARED_NOTE_SCHEDULER.schedule(() -> triggerNote(currentStep), 0, TimeUnit.MILLISECONDS);
        }
    }

    private void calculateNextStep() {
        int oldStep = currentStep;
        boolean patternCompleted = false;

        switch (sequenceData.getDirection()) {
            case FORWARD -> {
                currentStep++;

                // Check if we've reached the end of the pattern
                if (currentStep >= sequenceData.getPatternLength()) {
                    currentStep = 0;
                    patternCompleted = true;

                    // Handle pattern switching if enabled
                    handlePatternCompletion();
                }
            }

            case BACKWARD -> {
                currentStep--;

                if (currentStep < 0) {
                    currentStep = sequenceData.getPatternLength() - 1;
                    patternCompleted = true;

                    handlePatternCompletion();
                }
            }

            case BOUNCE -> {
                currentStep += bounceDirection;

                if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                    bounceDirection *= -1;

                    if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                        handlePatternCompletion();
                    }
                }
            }

            case RANDOM -> {
                int priorStep = currentStep;

                currentStep = (int) (Math.random() * sequenceData.getPatternLength());

                if (currentStep == 0 && priorStep != 0) {
                    handlePatternCompletion();
                }
            }
        }

        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(oldStep, currentStep));
        }
    }

    private void handlePatternCompletion() {

        if (latchEnabled) {
            int octaveRange = 2;
            int density = 50;
            if (rand.nextBoolean())
                generatePattern(octaveRange, density);

            logger.info("Latch mode: Generated new pattern at cycle end");
        }

        if (nextPatternId != null) {
            Long currentId = sequenceData.getId();

            // Use the manager instead of Redis directly
            if (MelodicSequencerManager.getInstance().applySequenceById(id, nextPatternId)) {
                CommandBus.getInstance().publish(
                        Commands.MELODIC_PATTERN_SWITCHED,
                        this,
                        new PatternSwitchEvent(currentId, nextPatternId));

                nextPatternId = null;
            }
        }
    }

    public int quantizeNote(int noteValue) {
        if (!sequenceData.isQuantizeEnabled() || quantizer == null) {
            return noteValue;
        }

        // Apply octave shift
        int octaveOffset = sequenceData.getOctaveShift() * 12;
        int shiftedNote = noteValue + octaveOffset;

        // Apply quantization
        int quantizedNote = quantizer.quantizeNote(shiftedNote);

        logger.debug("Quantized note {} to {} (with octave shift {})",
                noteValue, quantizedNote, sequenceData.getOctaveShift());

        return quantizedNote;
    }

    public void reset() {
        isPlaying = false;
        currentStep = 0;
        tickCounter = 0;
        bounceDirection = 1;

        if (player != null && player.getInstrument() != null) {
            try {
                logger.debug("All notes off sent during reset");
            } catch (Exception e) {
                logger.error("Error sending all notes off: {}", e.getMessage(), e);
            }
        }

        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(-1, currentStep));
        }

        logger.info("Sequencer reset");
    }


    public void triggerNote(int stepIndex) {
        logger.info("MelodicSequencer triggerNote: " + stepIndex);
        // Skip if not playing or muted (fast check before doing any other processing)
        if (!isPlaying || !sequenceData.isStepActive(stepIndex) || player.isMuted())
            return;

        // Check probability
        if (stepIsProbable(stepIndex)) return;

        int noteValue = sequenceData.isQuantizeEnabled() ? quantizeNote(sequenceData.getNoteValue(stepIndex)) :
                sequenceData.getNoteValue(stepIndex);
        int velocity = sequenceData.getVelocityValue(stepIndex);
        int gate = sequenceData.getGateValue(stepIndex);


        noteValue = noteValue + currentTilt + (player.getFollowSessionOffset() ? SessionManager.getInstance().getActiveSession().getNoteOffset() : 0);

        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNoteTriggeredTime < SequencerConstants.MIN_NOTE_INTERVAL_MS) {
                logger.debug("Ignoring note trigger - too soon after last note ({} ms)",
                        currentTime - lastNoteTriggeredTime);
                return;
            }
            lastNoteTriggeredTime = currentTime;

            NoteEvent event = new NoteEvent(
                    noteValue,
                    velocity,
                    gate
            );

            if (player != null) {
                if (player.getFollowRules()) {
                    if (player.shouldPlay(lastTimingUpdate))
                        player.noteOn(noteValue, velocity, gate);
                } else player.noteOn(noteValue, velocity, gate);
            }

            if (noteEventListener != null) {
                noteEventListener.accept(event);
            }

//            logger.debug("Triggered step {} - note:{} vel:{} gate:{} tilt:{}",
//                    stepIndex, noteValue, velocity, gate, currentTilt);

        } catch (Exception e) {
            logger.error("Error triggering note: {}", e.getMessage(), e);
        }
    }

    private boolean stepIsProbable(int stepIndex) {
        int probability = sequenceData.getProbabilityValue(stepIndex);
        if (probability < 100) {
            int rand = (int) (Math.random() * 100);

            if (rand >= probability) {
                logger.debug("Step {} skipped due to probability ({} < {})",
                        stepIndex, rand, probability);
                return true;
            }
        }
        return false;
    }

    public void notifyPatternUpdated() {
        CommandBus.getInstance().publish(
                Commands.MELODIC_PATTERN_UPDATED,
                this,
                sequenceData);

        logger.debug("Pattern updated notification sent");
    }

    private void initializePlayer(int playerChannel) {
        Session session = SessionManager.getInstance().getActiveSession();
        if (session == null) {
            logger.error("Cannot initialize player - no active session");
            return;
        }

        Optional<Note> opt = UserConfigManager.getInstance().getCurrentConfig().getDefaultNotes()
                .stream().filter(p -> p.getChannel().equals(playerChannel)).findFirst();

        if (opt.isPresent()) {

            player = opt.get();
            logger.info("Using existing player {} for sequencer {}", player.getId(), id);
            player.setDefaultChannel(playerChannel);
            player.setOwner(this);
            player.setMelodicPlayer(true);

        } else {
            logger.info("Creating new player for melodic sequencer {}", id);
            player = RedisService.getInstance().newNote();
            player.setRules(new HashSet<>()); // Ensure rules are initialized
            player.setMinVelocity(60);
            player.setMaxVelocity(127);
            player.setLevel(100);
            player.setIsDefault(true);
            player.setName("Melo " + getId() + 1);
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
            } else PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
        }

        if (sequenceData != null) {
            applySequenceDataToInstrument();
        }

        session.getPlayers().add(player);
        PlayerManager.getInstance().savePlayerProperties(player);
        SessionManager.getInstance().saveSession(session);
        logger.info("Added new player to session {}: {}", session.getId(), player.getId());
    }

    private void applySequenceDataToInstrument() {
        if (sequenceData == null || player == null || player.getInstrument() == null) {
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();

        if (instrument.isInternalSynth() && instrument.getChannel() != SequencerConstants.MIDI_DRUM_CHANNEL) {
            PlayerManager.getInstance().initializeInternalInstrument(player, false, getId());

            SoundbankManager.getInstance().updatePlayerSound(player, sequenceData.getSoundbankName(),
                    sequenceData.getBankIndex(), sequenceData.getPreset());
            //SoundbankManager.getInstance().applyInstrumentPreset(player);
        }

        logger.debug("Applied sequence data settings to instrument: preset:{}, bank:{}, soundbank:{}",
                instrument.getPreset(), instrument.getBankIndex(), instrument.getSoundBank());
    }


    /**
     * Generate a random pattern
     *
     * @param octaveRange The number of octaves to use (1-4)
     * @param density     The note density (0-100)
     * @return true if pattern was generated successfully
     */
    public boolean generatePattern(int octaveRange, int density) {


        boolean result = MelodicSequenceModifier.generatePattern(this, octaveRange, density);

        if (result) {
            // Notify pattern updated (this is a backup in case the modifier doesn't publish)
            CommandBus.getInstance().publish(
                    Commands.MELODIC_PATTERN_UPDATED,
                    this,
                    this.sequenceData);

            // Notify that pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    this,
                    new MelodicSequencerEvent(
                            this.getId(), sequenceData.getId()));
        }

        return result;
    }

    @Override
    public void onAction(Command action) {
        if (action == null) {
            return;
        }

        switch (action.getCommand()) {

            case Commands.LOOPING_TOGGLE_EVENT -> handleLoopToggleEvent(action);

            case Commands.GLOBAL_SCALE_SELECTION_EVENT -> {
                // Apply global scale change to all sequencers
                if (action.getData() instanceof String scale) {
                    sequenceData.setScale(scale);
                    updateQuantizer();
                    logger.info("Applied global scale change to sequencer {}: {}", id, scale);
                }
            }

            case Commands.SCALE_SELECTED -> handleScaleSelected(action);


            case Commands.ROOT_NOTE_SELECTED -> handleRootNoteSelected(action);

            case Commands.REPAIR_MIDI_CONNECTIONS -> {
                MelodicSequencerManager.getInstance().repairMidiConnections(this);
            }

            case Commands.TIMING_UPDATE -> handleTimingUpdate(action);

            case Commands.TRANSPORT_START -> {
                logger.info("Received TRANSPORT_START command");
                masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                logger.info("Master tempo set to {} ticks per beat", masterTempo);
                start();
            }

            case Commands.TRANSPORT_STOP -> {
                logger.info("Received TRANSPORT_STOP command");
                stop();
            }

            case Commands.REFRESH_ALL_INSTRUMENTS, Commands.PLAYER_PRESET_CHANGE_EVENT,
                 Commands.PLAYER_PRESET_CHANGED, Commands.PLAYER_INSTRUMENT_CHANGE_EVENT,
                 Commands.REFRESH_PLAYER_INSTRUMENT -> {
                SoundbankManager.getInstance().applyInstrumentPreset(player);
                MelodicSequenceModifier.updateInstrumentSettingsInSequenceData(this);
            }

        }
    }

    private void handleRootNoteSelected(Command action) {
        // Apply root note changes
        if (action.getData() instanceof String rootNote) {
            sequenceData.setRootNoteFromString(rootNote);
            updateQuantizer();
            logger.info("Applied root note change to sequencer {}: {}", id, rootNote);
        }
    }

    private void handleScaleSelected(Command action) {
        // Handle sequencer-specific scale changes
        if (action.getData() instanceof MelodicScaleSelectionEvent(Integer sequencerId, String scale)) {
            // Only apply if it's meant for this sequencer
            if (sequencerId != null && sequencerId.equals(id)) {
                sequenceData.setScale(scale);
                updateQuantizer();
                logger.info("Applied sequencer-specific scale change: {}", scale);
            }
        } else if (action.getData() instanceof String scale) {
            // Legacy support for string-only data
            sequenceData.setScale(scale);
            updateQuantizer();
            logger.info("Applied scale change to sequencer {}: {}", id, scale);
        }
    }

    /**
     * Handle a timing update from the transport
     *
     * @param action the Command containing the timing update with tick and bar info
     */
    private void handleTimingUpdate(Command action) {

        if (action.getData() instanceof TimingUpdate update) {

            lastTimingUpdate = update;
            // Process bar for tilt and mute updates
            if (update.bar() != null) {
                int newBar = update.bar() - 1; // Adjust for 0-based index

                // Only process if bar actually changed
                if (currentBar == null || newBar != currentBar) {
                    currentBar = newBar;
                    logger.debug("Current bar updated to {}", currentBar);

                    // Process tilt values
                    if (getHarmonicTiltValues() != null && getHarmonicTiltValues().size() > newBar) {
                        currentTilt = getHarmonicTiltValues().get(currentBar);
                        logger.debug("Current tilt value for bar {}: {}", currentBar, currentTilt);
                    }

                    if (sequenceData.getLengthModifierValues() != null && sequenceData.getLengthModifierValues().length >= newBar) {
                        int adjustment = sequenceData.getLengthModifierValue(newBar);
                        if (adjustment != 0)
                            sequenceData.setPatternLength(sequenceData.getPatternLength() + adjustment);
                    }


                    if (sequenceData.getMuteValues() != null && sequenceData.getMuteValues().size() > currentBar) {
                        int muteValue = sequenceData.getMuteValue(currentBar);
                        boolean shouldMute = muteValue > 0;

                        // Only update if mute state changes
                        if (shouldMute != player.isMuted()) {
                            player.setMuted(shouldMute);
                            player.setEnabled(!shouldMute);
                            logger.debug("Bar {}: Player {} {}",
                                    currentBar, player.getName(),
                                    shouldMute ? "muted" : "unmuted");
                        }
                    }

                    if (loopingToggled)
                        handleLoopToggled();
                }
            }
            // Process tick for note sequencing
            if (update.tick() != null && isPlaying) {
                processTick(update.tick());
            }
        }
    }

    public List<Integer> getHarmonicTiltValues() {
        if (sequenceData == null) {
            logger.error("sequenceData is null in getHarmonicTiltValues()");
            return new ArrayList<>();
        }

        int[] rawValues = sequenceData.getHarmonicTiltValuesRaw();
        if (rawValues == null) {
            logger.error("Raw harmonic tilt values array is null in sequencer");
            return new ArrayList<>();
        }

        List<Integer> result = Arrays.stream(rawValues).boxed().collect(Collectors.toList());
        logger.debug("getHarmonicTiltValues(): returning {} values from raw array of length {}",
                result.size(), rawValues.length);

        return result;
    }

    public void setHarmonicTiltValues(List<Integer> tiltValues) {
        if (tiltValues == null || tiltValues.isEmpty()) {
            logger.warn("Attempted to set null or empty tilt values");
            return;
        }

        int[] tiltArray = new int[Math.max(sequenceData.getPatternLength(), tiltValues.size())];

        for (int i = 0; i < tiltValues.size(); i++) {
            tiltArray[i] = tiltValues.get(i);
        }

        sequenceData.setHarmonicTiltValues(tiltArray);

        logger.info("Set {} harmonic tilt values in sequencer", tiltValues.size());
    }

    public List<Integer> getMuteValues() {
        if (sequenceData == null) {
            logger.error("sequenceData is null in getMuteValues()");
            return new ArrayList<>();
        }

        return sequenceData.getMuteValues();
    }

    /**
     * Set mute values from a list
     */
    public void setMuteValues(List<Integer> muteValues) {
        if (sequenceData == null || muteValues == null) {
            logger.error("Cannot set mute values: sequenceData or muteValues is null");
            return;
        }

        int[] muteArray = new int[Math.max(sequenceData.getPatternLength(), muteValues.size())];
        for (int i = 0; i < muteValues.size(); i++) {
            muteArray[i] = muteValues.get(i);
        }

        sequenceData.setMuteValues(muteArray);
        logger.info("Set {} mute values in sequencer", muteValues.size());
    }

    /**
     * Handle loop toggle events from UI components
     *
     * @param action the Command containing the loop toggle event
     */
    private void handleLoopToggleEvent(Command action) {
        if (action.getData() instanceof MelodicSequencer sequencer && sequencer.getId().equals(id))
            loopingToggled = true;
    }

    private void handleLoopToggled() {

        boolean loopingState = getSequenceData().isLooping();
        if (!loopingState && isPlaying)
            isPlaying = false;

            // If looping was re-enabled and the sequencer isn't playing but should be
        else if (loopingState && !isPlaying && sequenceData.isLooping()) {
            // Restart playback
            // currentStep = 0;
            isPlaying = true;
            logger.info("Restarting melodic sequencer {} due to looping re-enabled", id);
            CommandBus.getInstance().publish(Commands.SEQUENCER_STATE_CHANGED, this,
                    Map.of("sequencerId", id, "state", "restarted"));
        }

        loopingToggled = false;
    }


}

