package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.SequencerService;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import java.util.List;

/**
 * Helper class for MelodicSequencer diagnostics
 */
public class MelodicSequencerDiagnostics {
    
    /**
     * Runs comprehensive diagnostics on the MelodicSequencer
     */
    public DiagnosticLogBuilder runMelodicSequencerDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MelodicSequencer Diagnostics");
        
        try {
            // Get active MelodicSequencer instances from the manager
            List<MelodicSequencer> sequencers = SequencerService.getInstance().getAllSequencers();
            
            if (sequencers == null || sequencers.isEmpty()) {
                log.addError("No MelodicSequencer instances found");
                return log;
            }
            
            log.addLine("Found " + sequencers.size() + " melodic sequencer(s)");
            
            // Test each sequencer
            int sequencerIndex = 0;
            for (MelodicSequencer sequencer : sequencers) {
                sequencerIndex++;
                log.addSection(sequencerIndex + ". Sequencer ID: " + sequencer.getId());
                
                // 1. Check sequencer configuration
                checkSequencerConfiguration(sequencer, log);
                
                // 2. Check pattern data
                checkPatternData(sequencer, log);
                
                // 3. Check player and instrument
                checkPlayerAndInstrument(sequencer, log);
                
                // 4. Test note trigger
                testNoteTrigger(sequencer, log);
            }
            
            log.addLine("\nMelodicSequencer diagnostics completed.");
            
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
    
    /**
     * Check sequencer configuration
     */
    private void checkSequencerConfiguration(MelodicSequencer sequencer, DiagnosticLogBuilder log) {
        log.addSubSection("Configuration");
        
        try {
            MelodicSequenceData data = sequencer.getSequenceData();
            
            log.addIndentedLine("Playing: " + sequencer.isPlaying(), 1)
               .addIndentedLine("Pattern Length: " + data.getPatternLength(), 1)
               .addIndentedLine("Direction: " + data.getDirection(), 1)
               .addIndentedLine("Timing Division: " + data.getTimingDivision(), 1)
               .addIndentedLine("Looping: " + data.isLooping(), 1)
               .addIndentedLine("Swing Enabled: " + data.isSwingEnabled(), 1);
            
            if (data.isSwingEnabled()) {
                log.addIndentedLine("Swing Percentage: " + data.getSwingPercentage() + "%", 2);
            }
            
            log.addIndentedLine("Quantize Enabled: " + data.isQuantizeEnabled(), 1);
            if (data.isQuantizeEnabled()) {
                log.addIndentedLine("Root Note: " + data.getRootNote(), 2);
                log.addIndentedLine("Scale: " + data.getScale(), 2);
                log.addIndentedLine("Octave Shift: " + data.getOctaveShift(), 2);
            }
            
            // Check for any configuration issues
            if (data.getPatternLength() <= 0 || data.getPatternLength() > 64) {
                log.addWarning("Unusual pattern length: " + data.getPatternLength());
            }
            
            if (data.getTimingDivision() == null) {
                log.addWarning("Timing division is null");
            }
            
            if (data.getDirection() == null) {
                log.addWarning("Direction is null");
            }
            
        } catch (Exception e) {
            log.addError("Error checking configuration: " + e.getMessage());
        }
    }
    
    /**
     * Check pattern data
     */
    private void checkPatternData(MelodicSequencer sequencer, DiagnosticLogBuilder log) {
        log.addSubSection("Pattern Data");
        
        try {
            MelodicSequenceData data = sequencer.getSequenceData();
            
            // Count active steps
            int activeSteps = 0;
            int totalSteps = data.getPatternLength();
            
            for (int i = 0; i < totalSteps; i++) {
                if (data.isStepActive(i)) {
                    activeSteps++;
                }
            }
            
            log.addIndentedLine("Active Steps: " + activeSteps + " of " + totalSteps, 1);
            double percentActive = (totalSteps > 0) ? (double)activeSteps / totalSteps * 100 : 0;
            log.addIndentedLine("Pattern Density: " + String.format("%.1f", percentActive) + "%", 1);
            
            if (activeSteps == 0) {
                log.addWarning("Pattern has no active steps");
            }
            
            // Report on velocity range
            int minVel = 127, maxVel = 0, sumVel = 0;
            for (int i = 0; i < totalSteps; i++) {
                if (data.isStepActive(i)) {
                    int vel = data.getVelocityValue(i);
                    minVel = Math.min(minVel, vel);
                    maxVel = Math.max(maxVel, vel);
                    sumVel += vel;
                }
            }
            
            if (activeSteps > 0) {
                log.addIndentedLine("Velocity Range: " + minVel + " to " + maxVel, 1);
                log.addIndentedLine("Average Velocity: " + (sumVel / activeSteps), 1);
            }
            
            // Report on note range
            int minNote = 127, maxNote = 0;
            for (int i = 0; i < totalSteps; i++) {
                if (data.isStepActive(i)) {
                    int note = data.getNoteValue(i);
                    minNote = Math.min(minNote, note);
                    maxNote = Math.max(maxNote, note);
                }
            }
            
            if (activeSteps > 0) {
                log.addIndentedLine("Note Range: " + minNote + " to " + maxNote + 
                                 " (" + getNoteRange(minNote, maxNote) + " semitones)", 1);
                
                String minNoteName = midiNoteToName(minNote);
                String maxNoteName = midiNoteToName(maxNote);
                log.addIndentedLine("Note Names: " + minNoteName + " to " + maxNoteName, 1);
            }
            
            // Check probability values
            boolean hasVariableProbability = false;
            for (int i = 0; i < totalSteps; i++) {
                if (data.isStepActive(i) && data.getProbabilityValue(i) < 100) {
                    hasVariableProbability = true;
                    break;
                }
            }
            
            log.addIndentedLine("Variable Probability: " + (hasVariableProbability ? "Yes" : "No"), 1);
            
            // Check gate values
            int minGate = 100, maxGate = 0;
            for (int i = 0; i < totalSteps; i++) {
                if (data.isStepActive(i)) {
                    int gate = data.getGateValue(i);
                    minGate = Math.min(minGate, gate);
                    maxGate = Math.max(maxGate, gate);
                }
            }
            
            if (activeSteps > 0) {
                log.addIndentedLine("Gate Range: " + minGate + " to " + maxGate, 1);
            }
            
            // Check for harmonic tilt
            List<Integer> tiltValues = sequencer.getHarmonicTiltValues();
            boolean hasTilt = false;
            
            if (tiltValues != null && !tiltValues.isEmpty()) {
                for (Integer tilt : tiltValues) {
                    if (tilt != 0) {
                        hasTilt = true;
                        break;
                    }
                }
                
                log.addIndentedLine("Harmonic Tilt: " + (hasTilt ? "Active" : "Inactive"), 1);
                if (hasTilt) {
                    log.addIndentedLine("Tilt Values: " + tiltValues, 2);
                }
            } else {
                log.addWarning("No harmonic tilt values found");
            }
            
            // Check for pattern ID
            if (data.getId() != null) {
                log.addIndentedLine("Pattern ID: " + data.getId(), 1);
            } else {
                log.addWarning("Pattern has no ID");
            }
            
        } catch (Exception e) {
            log.addError("Error checking pattern data: " + e.getMessage());
        }
    }
    
    /**
     * Check player and instrument settings
     */
    private void checkPlayerAndInstrument(MelodicSequencer sequencer, DiagnosticLogBuilder log) {
        log.addSubSection("Player and Instrument");
        
        try {
            Player player = sequencer.getPlayer();
            
            if (player == null) {
                log.addError("No player assigned to sequencer");
                return;
            }
            
            log.addIndentedLine("Player ID: " + player.getId(), 1);
            log.addIndentedLine("Player Name: " + player.getName(), 1);
            log.addIndentedLine("Player Type: " + player.getClass().getSimpleName(), 1);
            log.addIndentedLine("Channel: " + player.getChannel(), 1);
            log.addIndentedLine("Level: " + player.getLevel(), 1);
            
            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                log.addError("No instrument assigned to player");
                return;
            }
            
            log.addIndentedLine("Instrument ID: " + instrument.getId(), 1);
            log.addIndentedLine("Instrument Name: " + instrument.getName(), 1);
            log.addIndentedLine("Soundbank: " + instrument.getSoundbankName(), 1);
            log.addIndentedLine("Bank Index: " + instrument.getBankIndex(), 1);
            log.addIndentedLine("Preset: " + instrument.getPreset(), 1);
            log.addIndentedLine("Device Name: " + instrument.getDeviceName(), 1);
            
            MidiDevice device = instrument.getDevice();
            if (device != null) {
                log.addIndentedLine("Device: " + device.getDeviceInfo().getName(), 1);
                log.addIndentedLine("Device Open: " + device.isOpen(), 2);
                
                if (!device.isOpen()) {
                    log.addWarning("MIDI device is not open");
                }
            } else {
                log.addError("No MIDI device associated with instrument");
            }
            
            Receiver receiver = instrument.getReceiver();
            if (receiver != null) {
                log.addIndentedLine("Receiver: Available", 1);
            } else {
                log.addError("No receiver available for instrument");
            }
            
        } catch (Exception e) {
            log.addError("Error checking player and instrument: " + e.getMessage());
        }
    }
    
    /**
     * Test note triggering
     */
    private void testNoteTrigger(MelodicSequencer sequencer, DiagnosticLogBuilder log) {
        log.addSubSection("Note Trigger Test");
        
        try {
            // Only run test if sequencer is properly configured
            Player player = sequencer.getPlayer();
            
            if (player == null || player.getInstrument() == null || 
                player.getInstrument().getDevice() == null || 
                !player.getInstrument().getDevice().isOpen()) {
                
                log.addWarning("Skipping note trigger test - sequencer not properly configured");
                return;
            }
            
            // Find the first active step
            MelodicSequenceData data = sequencer.getSequenceData();
            int testStep = -1;
            
            for (int i = 0; i < data.getPatternLength(); i++) {
                if (data.isStepActive(i)) {
                    testStep = i;
                    break;
                }
            }
            
            if (testStep == -1) {
                // No active steps, create a temporary one
                testStep = 0;
                log.addIndentedLine("No active steps found, using step 0 for testing", 1);
            } else {
                log.addIndentedLine("Using active step " + testStep + " for testing", 1);
            }
            
            // Save playing state
            boolean wasPlaying = sequencer.isPlaying();
            if (wasPlaying) {
                sequencer.stop();
                log.addIndentedLine("Stopped sequencer for testing", 1);
            }
            
            // Attempt to trigger note
            try {
                log.addIndentedLine("Triggering note at step " + testStep, 1);
                log.addIndentedLine("Note: " + data.getNoteValue(testStep) + 
                                 " (" + midiNoteToName(data.getNoteValue(testStep)) + ")", 2);
                log.addIndentedLine("Velocity: " + data.getVelocityValue(testStep), 2);
                
                sequencer.triggerNote(testStep);
                
                // Small delay to let note play
                Thread.sleep(300);
                
                log.addIndentedLine("Note trigger completed", 1);
            } catch (Exception e) {
                log.addError("Error triggering note: " + e.getMessage());
            }
            
            // Restore playing state
            if (wasPlaying) {
                sequencer.start();
                log.addIndentedLine("Restored original playing state", 1);
            }
            
        } catch (Exception e) {
            log.addError("Error in note trigger test: " + e.getMessage());
        }
    }
    
    /**
     * Test pattern operations
     */
    public DiagnosticLogBuilder testPatternOperations() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Melodic Pattern Operations Test");
        
        try {
            // Get an active sequencer
            List<MelodicSequencer> sequencers = SequencerService.getInstance().getAllSequencers();
            
            if (sequencers == null || sequencers.isEmpty()) {
                log.addError("No MelodicSequencer instances found");
                return log;
            }
            
            MelodicSequencer sequencer = sequencers.get(0);
            log.addLine("Using sequencer with ID: " + sequencer.getId());
            
            // Save current state to restore later
            boolean wasPlaying = sequencer.isPlaying();
            if (wasPlaying) {
                sequencer.stop();
                log.addLine("Stopped sequencer for testing");
            }
            
            MelodicSequenceData data = sequencer.getSequenceData();
            
            // 1. Test pattern generation
            log.addSection("1. Pattern Generation");
            
            // Save original pattern
            boolean[] originalActive = new boolean[16];
            int[] originalNotes = new int[16];
            int[] originalVelocities = new int[16];
            
            for (int i = 0; i < 16; i++) {
                originalActive[i] = data.isStepActive(i);
                originalNotes[i] = data.getNoteValue(i);
                originalVelocities[i] = data.getVelocityValue(i);
            }
            
            // Generate a new pattern
            log.addIndentedLine("Generating new pattern", 1);
            boolean success = sequencer.generatePattern(2, 50);
            
            if (success) {
                log.addIndentedLine("Pattern generation successful", 1);
                
                // Check if pattern changed
                boolean patternChanged = false;
                for (int i = 0; i < 16; i++) {
                    if (originalActive[i] != data.isStepActive(i) ||
                        originalNotes[i] != data.getNoteValue(i) ||
                        originalVelocities[i] != data.getVelocityValue(i)) {
                        patternChanged = true;
                        break;
                    }
                }
                
                if (patternChanged) {
                    log.addIndentedLine("Pattern content verified to be changed", 1);
                    
                    // Count active steps
                    int activeSteps = 0;
                    for (int i = 0; i < 16; i++) {
                        if (data.isStepActive(i)) {
                            activeSteps++;
                        }
                    }
                    log.addIndentedLine("New pattern has " + activeSteps + " active steps", 1);
                } else {
                    log.addWarning("Pattern did not change after generation");
                }
            } else {
                log.addError("Pattern generation failed");
            }
            
            // 2. Test quantization
            log.addSection("2. Quantization");
            
            // Save original quantization setting
            boolean originalQuantize = data.isQuantizeEnabled();
            Integer originalRoot = data.getRootNote();
            String originalScale = data.getScale();
            
            log.addIndentedLine("Original quantization: " + originalQuantize, 1);
            if (originalQuantize) {
                log.addIndentedLine("Root: " + originalRoot + ", Scale: " + originalScale, 2);
            }
            
            // Toggle quantization
            data.setQuantizeEnabled(!originalQuantize);
            log.addIndentedLine("Toggled quantization to: " + data.isQuantizeEnabled(), 1);
            
            // Test another scale
            if (data.isQuantizeEnabled()) {
                String testScale = "Minor";
                data.setScale(testScale);
                log.addIndentedLine("Changed scale to: " + testScale, 1);
                
                // Quantize a note
                int testNote = 60; // Middle C
                int quantized = sequencer.quantizeNote(testNote);
                log.addIndentedLine("Quantized C4 (60) to: " + quantized + " (" + midiNoteToName(quantized) + ")", 1);
            }
            
            // 3. Test direction change
            log.addSection("3. Direction Change");
            
            // Save original direction
            Direction originalDirection = data.getDirection();
            log.addIndentedLine("Original direction: " + originalDirection, 1);
            
            // Change direction
            Direction newDirection = Direction.BACKWARD;
            data.setDirection(newDirection);
            log.addIndentedLine("Changed direction to: " + newDirection, 1);
            
            // 4. Test playback
            log.addSection("4. Playback Test");
            
            log.addIndentedLine("Starting sequencer", 1);
            sequencer.start();
            
            // Wait briefly to observe playback
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            log.addIndentedLine("Playback active: " + sequencer.isPlaying(), 1);
            log.addIndentedLine("Current step: " + sequencer.getCurrentStep(), 1);
            
            // Stop sequencer
            log.addIndentedLine("Stopping sequencer", 1);
            sequencer.stop();
            
            if (!sequencer.isPlaying()) {
                log.addIndentedLine("Sequencer stopped successfully", 1);
            } else {
                log.addError("Failed to stop sequencer");
            }
            
            // 5. Restore original state
            log.addSection("5. Restoring Original State");
            
            // Restore pattern
            for (int i = 0; i < 16; i++) {
                if (data.isStepActive(i) != originalActive[i]) {
                    // Need to toggle to original state
                    if (originalActive[i]) {
                        // Need to activate
                        if (!data.isStepActive(i)) {
                            // Toggle on for inactive steps that should be active
                            sequencer.setStepData(i, true, originalNotes[i], originalVelocities[i], 75);
                        }
                    } else {
                        // Need to deactivate
                        if (data.isStepActive(i)) {
                            sequencer.setStepData(i, false, originalNotes[i], originalVelocities[i], 75);
                        }
                    }
                }
            }
            
            // Restore other settings
            data.setQuantizeEnabled(originalQuantize);
            data.setRootNote(originalRoot);
            data.setScale(originalScale);
            data.setDirection(originalDirection);
            
            log.addIndentedLine("Original pattern and settings restored", 1);
            
            // Restore playing state
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
     * Calculate the range between two MIDI notes
     */
    private int getNoteRange(int minNote, int maxNote) {
        return maxNote - minNote;
    }
    
    /**
     * Convert MIDI note number to note name
     */
    private String midiNoteToName(int midiNote) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (midiNote / 12) - 1;
        int noteIndex = midiNote % 12;
        return noteNames[noteIndex] + octave;
    }
}
