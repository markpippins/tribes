package com.angrysurfer.core.util;

import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiClockSource implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MidiClockSource.class);
    static boolean isInitialized = false;
    // Use singletons directly instead of constructor injection
    // private final LogManager logManager = LogManager.getInstance();
    private final TimingBus timingBus = TimingBus.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();
    public int metronomeChannel = SequencerConstants.MIDI_DRUM_CHANNEL;
    private boolean metronomeAudible = false;
    private int metronomeNote = 60;
    private int metronomeVelocity = 100;
    private Sequence sequence;
    private Sequencer sequencer;
    private Synthesizer synthesizer;

    private synchronized void initialize() {
        // if (!isInitialized)
        try {
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Initializing...");
            setupSequencer();
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer setup complete");
            setupSynthesizer();
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Synthesizer setup complete");
            createSequence();
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequence created");
            connectDevices();
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Devices connected");
        } catch (Exception e) {
            logger.error("SequencerManager: Error initializing MIDI: {}", e.getMessage(), e);
        }

        CommandBus.getInstance().register(this, new String[]{
                Commands.SESSION_SELECTED, Commands.METRONOME_START, Commands.METRONOME_STOP
        });
    }

    // Optimize buffer size and latency settings
    private void setupSequencer() throws MidiUnavailableException {
    if (logger.isDebugEnabled()) logger.debug("SequencerManager: Setting up sequencer...");
        sequencer = MidiSystem.getSequencer(false);
        if (sequencer == null) {
            throw new MidiUnavailableException("Could not obtain MIDI sequencer");
        }

        // Configure sequencer for low latency before opening
        try {
            // Set system properties for better timing - works with most JVM implementations
            System.setProperty("javax.sound.midi.Sequencer#RealTimeSequencing", "true");
            System.setProperty("javax.sound.midi.Sequencer#Latency", "1");
        } catch (Exception e) {
            logger.warn("Could not set sequencer properties: {}", e.getMessage(), e);
        }

        sequencer.open();

        // Try to optimize the sequencer after opening
        try {
            // These are general properties that might work across implementations
            // if (sequencer instanceof javax.sound.midi.RealTimeSequencer) {
            //     // Using real-time sequencer (implementation note)
            // }

            // Additional sequencer tuning
            sequencer.setMicrosecondPosition(0);
            sequencer.setTickPosition(0);
        } catch (Exception e) {
            logger.warn("Warning: Could not optimize sequencer: {}", e.getMessage(), e);
        }

    if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer opened successfully");
    }

    private void setupSynthesizer() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        synthesizer.getChannels()[metronomeChannel].setMute(!metronomeAudible);
    }

    // Modified createSequence to support dynamic parameters
    private void createSequence() throws InvalidMidiDataException {
        createSequence(4); // Default 4/4 time
    }

    private void createSequence(int beatsPerBar) throws InvalidMidiDataException {
        sequence = new Sequence(Sequence.PPQ, getActiveSession().getTicksPerBeat());
        Track track = sequence.createTrack();

        // Add events for one bar with configurable beats per bar
        for (int beat = 0; beat < beatsPerBar; beat++) {
            // Add timing clocks
            for (int clock = 0; clock < getActiveSession().getTicksPerBeat(); clock++) {
                track.add(new MidiEvent(new ShortMessage(0xF8), (long) beat * getActiveSession().getTicksPerBeat() + clock));
            }

            // Add metronome notes
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, metronomeChannel, metronomeNote, metronomeVelocity),
                    (long) beat * getActiveSession().getTicksPerBeat()));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, metronomeChannel, metronomeNote, 0),
                    (long) beat * getActiveSession().getTicksPerBeat() + getActiveSession().getTicksPerBeat() / 2));
        }

        sequencer.setSequence(sequence);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        sequencer.setTempoInBPM(getActiveSession().getTempoInBPM());
        sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
        sequencer.setSlaveSyncMode(Sequencer.SyncMode.MIDI_SYNC);
    }

    // Optimize MIDI clock message handling
    private void connectDevices() throws MidiUnavailableException {
        // Create and connect timing receiver with higher priority
        Receiver timingReceiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage msg) {
                    if (msg.getStatus() == 0xF8) {
                        Session activeSession = getActiveSession();
                        // Directly call onTick first to update the tick counter
                        if (activeSession != null) {
                            activeSession.onTick();
                        }
                    }
                }
            }

            @Override
            public void close() {
            }
        };

        // Connect timing and audio
        Transmitter timingTransmitter = sequencer.getTransmitter();
        timingTransmitter.setReceiver(timingReceiver);

        Transmitter audioTransmitter = sequencer.getTransmitter();
        audioTransmitter.setReceiver(synthesizer.getReceiver());
    }

    public void startSequence() {

        initialize();

        try {
            if (logger.isDebugEnabled()) logger.debug("SequencerManager: Attempting to start sequencer");
            if (sequencer != null && !sequencer.isRunning()) {
                if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer exists and is not running");
                // Important: Make sure any active session is properly initialized
                if (getActiveSession() != null) {
                    if (logger.isDebugEnabled() && getActiveSession() != null) logger.debug("SequencerManager: Active session found: {}", getActiveSession().getId());
                    getActiveSession().beforeStart();
                    if (logger.isDebugEnabled()) logger.debug("SequencerManager: Called session.beforeStart()");
                } else {
                    if (logger.isDebugEnabled()) logger.debug("SequencerManager: No active session found!");
                }

                // Start sequencer
                sequencer.start();
                if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer started");

                // Publish state change - CRITICAL for UI updates
                CommandBus.getInstance().publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
                if (logger.isDebugEnabled()) logger.debug("SequencerManager: Published TRANSPORT_STATE_CHANGED event");
            } else {
                if (logger.isDebugEnabled()) logger.debug("SequencerManager: Cannot start - sequencer is null or already running");
                if (sequencer == null) {
                    if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer is null!");
                } else {
                    if (logger.isDebugEnabled()) logger.debug("SequencerManager: Sequencer is already running!");
                }
            }
        } catch (Exception e) {
            logger.error("SequencerManager: Error starting sequencer: {}", e.getMessage(), e);
        }
    }

    public void stop() {

        try {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0);

                // Publish state change - CRITICAL for UI updates
                CommandBus.getInstance().publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                if (logger.isDebugEnabled()) logger.debug("SequencerManager: Stopped sequencer, publishing state change event");
            }
        } catch (Exception e) {
            logger.warn("Error stopping sequencer: {}", e.getMessage(), e);
        }
    }

    public void cleanup() {
        try {
            // Unregister from timing bus
            timingBus.unregister(this);

            // Clean up sequencer
            // SequencerManager.getInstance().cleanup();
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
            }
            if (synthesizer != null) {
                synthesizer.close();
            }
            // logManager.info("MIDI resources cleaned up");
        } catch (Exception e) {
            logger.warn("Error cleaning up MIDI resources: {}", e.getMessage(), e);
        }
    }

    // Add state query methods
    public boolean isRunning() {
        return sequencer != null && sequencer.isRunning();
    }

    public void updateTimingParameters(float tempoInBPM, int ticksPerBeat, int beatsPerBar) {
        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update sequencer parameters
            sequencer.setTempoInBPM(tempoInBPM);
            createSequence(beatsPerBar);

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    // Add getter methods that checkgetActiveSession() first

    private Session getActiveSession() {
        // Replace with actual session retrieval logic
        return SessionManager.getInstance().getActiveSession();
    }

    private void updateSequencerSettings() {
        if (getActiveSession() == null)
            return;

        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update sequencer parameters from session
            sequencer.setTempoInBPM(getActiveSession().getTempoInBPM());

            // Recreate sequence if PPQ changed
            createSequence(getActiveSession().getBeatsPerBar());

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == Commands.SESSION_SELECTED) {
            updateSequencerSettings();
        }
        if (action.getCommand() == Commands.METRONOME_START) {
            setMetronomeAudible(true);
            if ((Objects.nonNull(synthesizer)) && (synthesizer.isOpen())) {
                synthesizer.getChannels()[metronomeChannel].setMute(false);
            }
        } else if (action.getCommand() == Commands.METRONOME_STOP) {
            setMetronomeAudible(false);
            if ((Objects.nonNull(synthesizer)) && (synthesizer.isOpen())) {
                synthesizer.getChannels()[metronomeChannel].setMute(true);
            }
        }
    }
}