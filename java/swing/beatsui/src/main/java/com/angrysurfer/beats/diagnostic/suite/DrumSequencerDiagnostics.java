package com.angrysurfer.beats.diagnostic.suite;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.DrumSequencerManager;

/**
 * Helper class for DrumSequencer diagnostics
 */
public class DrumSequencerDiagnostics {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerDiagnostics.class);

    /**
     * Runs comprehensive diagnostics on the DrumSequencer
     */
    public DiagnosticLogBuilder runDrumSequencerDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("DrumSequencer Diagnostics");

        try {
            // Get the active DrumSequencer
            DrumSequencer sequencer = getActiveDrumSequencer();
            if (sequencer == null) {
                log.addError("No active DrumSequencer found");
                return log;
            }

            // 1. Check sequencer status
            log.addSection("1. Sequencer Status");
            log.addIndentedLine("Playing: " + sequencer.isPlaying(), 1)
                    .addIndentedLine("BPM: " + sequencer.getSequenceData().getMasterTempo(), 1)
                    .addIndentedLine("Swing: " + (sequencer.isSwingEnabled() ? "Enabled" : "Disabled"), 1)
                    .addIndentedLine("Swing Amount: " + sequencer.getSwingPercentage(), 1);

            // 2. Check pattern data
            log.addSection("2. Pattern Data");
            int totalActiveSteps = 0;
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                int activeSteps = 0;
                for (int j = 0; j < sequencer.getPatternLength(i); j++) {
                    if (sequencer.isStepActive(i, j)) {
                        activeSteps++;
                        totalActiveSteps++;
                    }
                }
                log.addIndentedLine("Drum " + i + ": " + activeSteps + " active steps (Length: " +
                        sequencer.getPatternLength(i) + ")", 1);
            }
            log.addIndentedLine("Total active steps: " + totalActiveSteps, 1);

            if (totalActiveSteps == 0) {
                log.addWarning("No active steps found in patterns");
            }

            // 3. Check players and instruments
            log.addSection("3. Player/Instrument Check");
            int playersWithInstruments = 0;
            int openDevices = 0;

            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                Player player = sequencer.getPlayer(i);
                if (player != null) {
                    log.addIndentedLine("Drum " + i + " - Player: " + player.getName() +
                            " (Type: " + player.getClass().getSimpleName() + ")", 1);
                    log.addIndentedLine("Root Note: " + player.getRootNote() +
                            ", Channel: " + player.getChannel(), 2);

                    InstrumentWrapper instrument = player.getInstrument();
                    if (instrument != null) {
                        playersWithInstruments++;
                        log.addIndentedLine("Instrument: " + instrument.getName() +
                                " (ID: " + instrument.getId() + ")", 2);
                        log.addIndentedLine("Device Name: " + instrument.getDeviceName() +
                                ", Channel: " + instrument.getChannel(), 2);

                        MidiDevice device = instrument.getDevice();
                        if (device != null) {
                            log.addIndentedLine("Device: " + device.getDeviceInfo().getName() +
                                    " (Open: " + device.isOpen() + ")", 2);
                            if (device.isOpen()) {
                                openDevices++;
                            }

                            Receiver receiver = null;
                            try {
                                receiver = instrument.getReceiver();
                                log.addIndentedLine("Receiver: " + (receiver != null ? "OK" : "NULL"), 2);
                            } catch (Exception e) {
                                log.addIndentedLine("Receiver: ERROR - " + e.getMessage(), 2);
                            }
                        } else {
                            log.addIndentedLine("Device: NULL", 2);
                        }
                    } else {
                        log.addIndentedLine("Instrument: NULL", 2);
                    }
                } else {
                    log.addIndentedLine("Drum " + i + " - Player: NULL", 1);
                }
            }

            log.addIndentedLine("Players with instruments: " + playersWithInstruments +
                    " out of " + SequencerConstants.DRUM_PAD_COUNT, 1);
            log.addIndentedLine("Open MIDI devices: " + openDevices, 1);

            if (playersWithInstruments == 0) {
                log.addError("No players have instruments assigned");
            }

            if (openDevices == 0) {
                log.addError("No open MIDI devices found");
            }

            log.addLine("\nDrumSequencer diagnostics completed.");

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test pattern generation and manipulation
     */
    public DiagnosticLogBuilder testPatternOperations() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Pattern Operations Test");

        try {
            // Get the active DrumSequencer
            DrumSequencer sequencer = getActiveDrumSequencer();
            if (sequencer == null) {
                log.addError("No active DrumSequencer found");
                return log;
            }

            // Save current state to restore later
            boolean wasPlaying = sequencer.isPlaying();
            if (wasPlaying) {
                sequencer.stop();
                log.addLine("Stopped sequencer for testing");
            }

            // Test 1: Create a test pattern
            log.addSection("1. Creating test pattern");

            // Select first available drum
            int testDrum = 0;
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                if (sequencer.getPlayer(i) != null) {
                    testDrum = i;
                    break;
                }
            }

            log.addIndentedLine("Using drum " + testDrum + " for pattern test", 1);

            // Save original pattern to restore later
            boolean[] originalPattern = new boolean[16];
            for (int i = 0; i < 16; i++) {
                originalPattern[i] = sequencer.isStepActive(testDrum, i);
            }

            // Clear pattern first by toggling any active steps to inactive
            for (int i = 0; i < 16; i++) {
                if (sequencer.isStepActive(testDrum, i)) {
                    sequencer.toggleStep(testDrum, i);
                }
            }

            // Create a simple test pattern (quarter notes)
            // Since we just cleared the pattern, all steps are inactive
            // so toggling will activate them
            for (int i = 0; i < 16; i += 4) {
                sequencer.toggleStep(testDrum, i);
            }

            log.addIndentedLine("Created test pattern with quarter notes", 1);

            // Verify pattern was set correctly
            boolean patternCorrect = true;
            for (int i = 0; i < 16; i++) {
                boolean shouldBeActive = i % 4 == 0;
                if (sequencer.isStepActive(testDrum, i) != shouldBeActive) {
                    patternCorrect = false;
                    break;
                }
            }

            if (patternCorrect) {
                log.addIndentedLine("Pattern verification successful", 1);
            } else {
                log.addError("Pattern verification failed");
            }

            // Test 2: Change pattern length
            log.addSection("2. Testing pattern length");

            int originalLength = sequencer.getPatternLength(testDrum);
            log.addIndentedLine("Original pattern length: " + originalLength, 1);

            int newLength = 8;
            sequencer.setPatternLength(testDrum, newLength);
            log.addIndentedLine("Set new pattern length: " + newLength, 1);

            int verifiedLength = sequencer.getPatternLength(testDrum);
            if (verifiedLength == newLength) {
                log.addIndentedLine("Pattern length changed successfully", 1);
            } else {
                log.addError("Pattern length verification failed: expected=" +
                        newLength + ", actual=" + verifiedLength);
            }

            // Test 3: Trigger pattern playback
            log.addSection("3. Testing pattern playback");

            // Only briefly start and stop to avoid long test
            log.addIndentedLine("Starting sequencer", 1);
            sequencer.start();

            // Wait just long enough to verify it started
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }

            if (sequencer.isPlaying()) {
                log.addIndentedLine("Sequencer started successfully", 1);
            } else {
                log.addError("Sequencer failed to start");
            }

            log.addIndentedLine("Stopping sequencer", 1);
            sequencer.stop();

            if (!sequencer.isPlaying()) {
                log.addIndentedLine("Sequencer stopped successfully", 1);
            } else {
                log.addError("Sequencer failed to stop");
            }

            // Restore original pattern
            log.addSection("4. Restoring original pattern");

            // Clear test pattern first
            for (int i = 0; i < 16; i++) {
                if (sequencer.isStepActive(testDrum, i)) {
                    sequencer.toggleStep(testDrum, i);
                }
            }

            // Restore original active steps
            for (int i = 0; i < 16; i++) {
                if (originalPattern[i] && !sequencer.isStepActive(testDrum, i)) {
                    sequencer.toggleStep(testDrum, i);
                }
            }

            sequencer.setPatternLength(testDrum, originalLength);
            log.addIndentedLine("Original pattern and length restored", 1);

            // Restore playing state if needed
            if (wasPlaying) {
                sequencer.start();
                log.addIndentedLine("Restored original playing state", 1);
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Helper method to get the active DrumSequencer
     */
    private DrumSequencer getActiveDrumSequencer() {
        try {
            return DrumSequencerManager.getInstance().getActiveSequencer();
        } catch (Exception e) {
            logger.error("Error getting active sequencer: {}", e.getMessage(), e);
            return null;
        }
    }
}