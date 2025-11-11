package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.SoundbankService;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Diagnostic helper for SoundbankManager
 */
public class SoundbankManagerDiagnostics {

    /**
     * Run comprehensive diagnostics on SoundbankService
     */
    public DiagnosticLogBuilder testSoundbankManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Soundbank Service Diagnostics");

        try {
            SoundbankService service = SoundbankService.getInstance();
            log.addLine("SoundbankService instance obtained: " + (service != null));

            if (service == null) {
                log.addError("Failed to get SoundbankService instance");
                return log;
            }

            // Check synthesizer status
            log.addSection("Synthesizer Status");
            Synthesizer synth = MidiService.getInstance().getSynthesizer();
            if (synth == null) {
                log.addError("No synthesizer available");
            } else {
                log.addLine("Synthesizer: " + synth.getDeviceInfo().getName() + " (Version: " + synth.getDeviceInfo().getVersion() + ")");
                log.addLine("Synthesizer is open: " + synth.isOpen());

                if (!synth.isOpen()) {
                    log.addWarning("Synthesizer is not open - this will prevent soundbank loading");
                    log.addLine("Attempting to open synthesizer...");
                    try {
                        synth.open();
                        log.addLine("Successfully opened synthesizer: " + synth.isOpen());
                    } catch (Exception e) {
                        log.addError("Failed to open synthesizer: " + e.getMessage());
                    }
                }

                // Get latency info
                log.addLine("Synthesizer latency: " + synth.getLatency() + " microseconds");
                log.addLine("Maximum polyphony: " + synth.getMaxPolyphony() + " voices");
            }

            // Check available soundbanks
            log.addSection("Available Soundbanks");
            List<String> sbNames = manager.getSoundbankNames();
            log.addLine("Number of soundbanks: " + sbNames.size());

            if (sbNames.isEmpty()) {
                log.addWarning("No soundbanks available - this will prevent preset loading");
                log.addLine("Attempting to initialize soundbanks...");
                boolean success = manager.initializeSoundbanks();
                log.addLine("Soundbank initialization " + (success ? "successful" : "failed"));

                // Check again
                sbNames = manager.getSoundbankNames();
                log.addLine("Number of soundbanks after initialization attempt: " + sbNames.size());
            }

            // Display soundbank details
            if (!sbNames.isEmpty()) {
                for (String sbName : sbNames) {
                    log.addIndentedLine("Soundbank: " + sbName, 1);

                    // Get available banks for this soundbank
                    List<Integer> banks = manager.getAvailableBanksByName(sbName);
                    log.addIndentedLine("Available banks: " + banks.size(), 2);

                    // Only show first 5 banks to avoid overwhelming output
                    int bankLimit = Math.min(banks.size(), 5);
                    for (int i = 0; i < bankLimit; i++) {
                        Integer bank = banks.get(i);
                        List<String> presets = manager.getPresetNames(sbName, bank);
                        log.addIndentedLine("Bank " + bank + ": " + presets.size() + " presets", 3);

                        // Only show first few presets
                        int presetLimit = Math.min(presets.size(), 5);
                        for (int j = 0; j < presetLimit; j++) {
                            log.addIndentedLine("Preset " + j + ": " + presets.get(j), 4);
                        }
                        if (presets.size() > presetLimit) {
                            log.addIndentedLine("... plus " + (presets.size() - presetLimit) + " more presets", 4);
                        }
                    }
                    if (banks.size() > bankLimit) {
                        log.addIndentedLine("... plus " + (banks.size() - bankLimit) + " more banks", 3);
                    }
                }
            }

            // Test loading a specific instrument by program/bank
            log.addSection("Instrument Loading Test");
            if (!sbNames.isEmpty()) {
                String testSoundbank = sbNames.get(0);
                List<Integer> banks = manager.getAvailableBanksByName(testSoundbank);

                if (!banks.isEmpty()) {
                    Integer testBank = banks.get(0);
                    List<String> presets = manager.getPresetNames(testSoundbank, testBank);

                    if (!presets.isEmpty()) {
                        int testPreset = 0;
                        log.addLine("Testing instrument load: Soundbank=" + testSoundbank +
                                ", Bank=" + testBank + ", Preset=" + testPreset);

                        try {
                            // Test with a specific MIDI channel
                            int testChannel = 0; // Channel 1 (non-drum)

                            // Fixed: Use getInstrumentInfo instead of getInstrument
                            Patch patch = new Patch(testBank, testPreset);
                            Instrument instrument = null;

                            // Get the soundbank first
                            Soundbank soundbank = manager.getSoundbank(testSoundbank);
                            if (soundbank != null) {
                                // Look for instrument with matching patch
                                for (Instrument inst : soundbank.getInstruments()) {
                                    if (inst.getPatch().getBank() == testBank &&
                                            inst.getPatch().getProgram() == testPreset) {
                                        instrument = inst;
                                        break;
                                    }
                                }
                            }

                            if (instrument != null) {
                                log.addLine("Successfully found instrument: " + instrument.getName());

                                // Try loading into synthesizer
                                // Fixed: Use synth.loadInstrument instead of manager.loadInstrument
                                boolean loadResult = synth.loadInstrument(instrument);
                                log.addLine("Loaded instrument into synthesizer: " + loadResult);

                                // Try to play a test note
                                try {
                                    Receiver receiver = synth.getReceiver();
                                    // Program change
                                    ShortMessage pc = new ShortMessage();
                                    pc.setMessage(ShortMessage.PROGRAM_CHANGE, testChannel, testPreset, 0);
                                    receiver.send(pc, -1);

                                    // Bank select (if needed)
                                    if (testBank > 0) {
                                        ShortMessage bankMsb = new ShortMessage();
                                        bankMsb.setMessage(ShortMessage.CONTROL_CHANGE, testChannel, 0, testBank >> 7);
                                        receiver.send(bankMsb, -1);

                                        ShortMessage bankLsb = new ShortMessage();
                                        bankLsb.setMessage(ShortMessage.CONTROL_CHANGE, testChannel, 32, testBank & MidiControlMessageEnum.POLY_MODE_ON);
                                        receiver.send(bankLsb, -1);
                                    }

                                    // Note on (middle C, moderate velocity)
                                    ShortMessage noteOn = new ShortMessage();
                                    noteOn.setMessage(ShortMessage.NOTE_ON, testChannel, 60, 64);
                                    receiver.send(noteOn, -1);

                                    // Sleep briefly
                                    Thread.sleep(500);

                                    // Note off
                                    ShortMessage noteOff = new ShortMessage();
                                    noteOff.setMessage(ShortMessage.NOTE_OFF, testChannel, 60, 0);
                                    receiver.send(noteOff, -1);

                                    log.addLine("Test note played successfully");
                                } catch (Exception e) {
                                    log.addWarning("Could not play test note: " + e.getMessage());
                                }
                            } else {
                                log.addWarning("Instrument not found for test parameters");
                            }
                        } catch (Exception e) {
                            log.addError("Error loading test instrument: " + e.getMessage());
                        }
                    } else {
                        log.addWarning("No presets available in bank " + testBank);
                    }
                } else {
                    log.addWarning("No banks available in soundbank " + testSoundbank);
                }
            }

            // Test drum channel (9) preset loading
            log.addSection("Drum Channel Preset Test");
            try {
                // Default to GM drums (channel 9, bank 0, program 0)
                log.addLine("Testing drum instrument loading (Channel 9)");

                if (synth != null && synth.isOpen()) {
                    MidiChannel drumChannel = synth.getChannels()[SequencerConstants.MIDI_DRUM_CHANNEL];
                    if (drumChannel != null) {
                        // Set GM drums
                        drumChannel.programChange(0);

                        // Test a few key drum notes
                        int[] drumNotes = {35, 38, 42, 46}; // Bass drum, snare, hi-hat, etc.
                        String[] drumNames = {"Bass Drum", "Snare", "Closed Hi-Hat", "Open Hi-Hat"};

                        for (int i = 0; i < drumNotes.length; i++) {
                            try {
                                log.addLine("Testing drum sound: " + drumNames[i] + " (note " + drumNotes[i] + ")");

                                // Play note
                                drumChannel.noteOn(drumNotes[i], 64);
                                Thread.sleep(200);
                                drumChannel.noteOff(drumNotes[i]);

                                if (i < drumNotes.length - 1) Thread.sleep(100);
                            } catch (Exception e) {
                                log.addWarning("Error playing drum note: " + e.getMessage());
                            }
                        }
                    } else {
                        log.addWarning("Drum channel (9) not available");
                    }
                } else {
                    log.addWarning("Cannot test drum sounds - synthesizer not open");
                }
            } catch (Exception e) {
                log.addError("Error testing drum channel: " + e.getMessage());
            }

            // Test SoundbankManager's instrument search functionality
            log.addSection("Instrument Search Test");
            try {
                // Fixed: Implement a search for instruments by name
                String searchTerm = "piano";
                log.addLine("Searching for instruments containing '" + searchTerm + "'...");

                // Create a list to store results
                List<Instrument> results = new ArrayList<>();

                // Search through all soundbanks and their instruments
                for (String soundbankName : sbNames) {
                    Soundbank sb = manager.getSoundbank(soundbankName);
                    if (sb != null) {
                        for (Instrument inst : sb.getInstruments()) {
                            if (inst.getName().toLowerCase().contains(searchTerm.toLowerCase())) {
                                results.add(inst);
                            }
                        }
                    }
                }

                log.addLine("Found " + results.size() + " matching instruments");

                int resultLimit = Math.min(results.size(), 10);
                for (int i = 0; i < resultLimit; i++) {
                    Instrument inst = results.get(i);
                    log.addIndentedLine(inst.getName() + " (Bank: " + inst.getPatch().getBank() +
                            ", Program: " + inst.getPatch().getProgram() + ")", 1);
                }

                if (results.size() > resultLimit) {
                    log.addIndentedLine("... plus " + (results.size() - resultLimit) + " more results", 1);
                }
            } catch (Exception e) {
                log.addWarning("Error searching for instruments: " + e.getMessage());
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test if soundbank presets are properly loaded and verify their consistency
     */
    public DiagnosticLogBuilder verifyPresetConsistency() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Soundbank Preset Verification");

        try {
            SoundbankManager manager = SoundbankService.getInstance();
            if (manager == null) {
                log.addError("Failed to get SoundbankManager instance");
                return log;
            }

            log.addSection("Preset Consistency Check");

            List<String> sbNames = manager.getSoundbankNames();
            int totalBanks = 0;
            int totalPresets = 0;
            List<String> problematicBanks = new ArrayList<>();

            for (String sbName : sbNames) {
                List<Integer> banks = manager.getAvailableBanksByName(sbName);
                totalBanks += banks.size();

                for (Integer bank : banks) {
                    List<String> presets = manager.getPresetNames(sbName, bank);
                    totalPresets += presets.size();

                    // Check for problematic presets (null, empty, or duplicated)
                    boolean foundEmptyName = false;
                    boolean foundDuplicateName = false;
                    Set<String> uniqueNames = new java.util.HashSet<>();

                    for (String preset : presets) {
                        if (preset == null || preset.isEmpty()) {
                            foundEmptyName = true;
                        }
                        if (!uniqueNames.add(preset)) {
                            foundDuplicateName = true;
                        }
                    }

                    if (foundEmptyName || foundDuplicateName) {
                        problematicBanks.add(sbName + " (Bank " + bank + ")");
                    }
                }
            }

            log.addLine("Total soundbanks: " + sbNames.size());
            log.addLine("Total banks: " + totalBanks);
            log.addLine("Total presets: " + totalPresets);

            if (!problematicBanks.isEmpty()) {
                log.addWarning("Found issues in the following banks:");
                for (String bank : problematicBanks) {
                    log.addIndentedLine(bank, 1);
                }
            } else {
                log.addLine("No issues found in preset consistency check");
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }
}
