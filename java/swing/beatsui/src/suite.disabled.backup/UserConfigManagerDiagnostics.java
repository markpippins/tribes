package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.UserConfigManager;

import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diagnostic helper for UserConfigManager
 */
public class UserConfigManagerDiagnostics {

    /**
     * Run diagnostics on UserConfigManager
     */
    public DiagnosticLogBuilder testUserConfigManager() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("UserConfigManager Diagnostics");

        try {
            log.addLine("UserConfigManager instance obtained: " + (UserConfigManager.getInstance() != null));

            // Get current configuration
            UserConfig config = UserConfigManager.getInstance().getCurrentConfig();
            log.addLine("Current configuration: " + (config != null ? "Available" : "Null"));

            if (config != null) {
                testConfig(config, log);
            } else {
                log.addError("No user configuration available");
                return log;
            }

            // Test instrument retrieval
            log.addSection("Instrument Retrieval Test");
            List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();
            log.addLine("Retrieved " + instruments.size() + " instruments");

            if (!instruments.isEmpty()) {
                InstrumentWrapper firstInstrument = instruments.get(0);
                log.addLine("Testing instrument lookup by ID: " + firstInstrument.getId());

                InstrumentWrapper found = UserConfigManager.getInstance().findInstrumentById(firstInstrument.getId());
                log.addLine("Lookup result: " + (found != null ? "Found" : "Not found"));

                if (found != null) {
                    boolean sameInstance = found == firstInstrument;
                    log.addLine("Same instance: " + sameInstance);

                    if (!sameInstance) {
                        boolean equalContent = found.getId().equals(firstInstrument.getId()) &&
                                found.getName().equals(firstInstrument.getName());
                        log.addLine("Equal content: " + equalContent);
                    }
                }

                // Test name lookup
                if (firstInstrument.getName() != null) {
                    String nameFragment = firstInstrument.getName().substring(0,
                            Math.min(3, firstInstrument.getName().length()));

                    log.addLine("Testing instrument lookup by name fragment: \"" + nameFragment + "\"");
                    List<InstrumentWrapper> foundByName = UserConfigManager.getInstance()
                            .findInstrumentsByName(nameFragment);
                    log.addLine("Found " + foundByName.size() + " instruments containing \"" + nameFragment + "\"");
                }
            }

            // Test command bus registration
            log.addSection("Event System Registration");
            CommandBus commandBus = CommandBus.getInstance();

            // Sound and dialog test
            log.addSection("Sound Verification Test");

            try {
                log.addLine("Testing MIDI sound output");

                // Get InternalSynthManager
                MidiService synthManager = MidiService.getInstance();
                if (synthManager == null) {
                    log.addError("InternalSynthManager not available");
                } else {
                    // Play a piano note (middle C) on channel 0
                    log.addLine("Playing piano note (middle C) on channel 0");
                    synthManager.playNote(60, 100, 500, 0);
                    Thread.sleep(700);

                    // Play a drum note (bass drum) on channel 9
                    log.addLine("Playing bass drum on channel 9");
                    synthManager.playNote(36, 100, 500, 9);
                    Thread.sleep(700);

                    // Ask the user if they heard the sounds
                    boolean soundsHeard = showYesNoDialog(
                            "Sound Test",
                            "Did you hear the two test sounds?\n\n" +
                                    "• Piano note (middle C)\n" +
                                    "• Bass drum sound\n\n" +
                                    "Click Yes if you heard the sounds, No if you didn't.");

                    log.addLine("User reported hearing sounds: " + (soundsHeard ? "Yes" : "No"));

                    if (!soundsHeard) {
                        log.addWarning("User reported not hearing the sounds. Check audio setup and MIDI devices.");

                        // Additional diagnostics if sounds weren't heard
                        log.addIndentedLine("MIDI System Information:", 1);
                        log.addIndentedLine("Java Sound Synthesizer available: " +
                                (synthManager.getSynthesizer() != null), 2);

                        if (synthManager.getSynthesizer() != null) {
                            log.addIndentedLine("Synthesizer open: " +
                                    synthManager.getSynthesizer().isOpen(), 2);
                            log.addIndentedLine("Latency: " +
                                    synthManager.getSynthesizer().getLatency() + " microseconds", 2);
                        }
                    }
                }
            } catch (Exception e) {
                log.addError("Error during sound test: " + e.getMessage());
            }

            // Ask if user wants to run the comprehensive player sound test
            boolean runComprehensiveTest = showYesNoDialog(
                    "Comprehensive Sound Test",
                    "Would you like to run a comprehensive test of all default players?\n\n" +
                            "This will test each default player individually with:\n" +
                            "• All drum sounds in sequence\n" +
                            "• Musical phrases on all melodic instruments\n\n" +
                            "If there are no default players, you'll be given the option to create them.");

            if (runComprehensiveTest) {
                log.addSection("Running Comprehensive Default Player Test");
                log.addLine("Starting comprehensive sound test of all default players...");

                DiagnosticLogBuilder playerTestLog = testDefaultPlayersSounds();

                // Merge the player test log into the main log
                log.addLine("Comprehensive test completed");
                log.addLine("Results from comprehensive test:");
                log.addIndentedLine("------------------------------", 1);

                for (String line : playerTestLog.getErrors()) {
                    log.addIndentedLine(line, 1);
                }
                log.addIndentedLine("------------------------------", 1);
                for (String line : playerTestLog.getWarnings()) {
                    log.addIndentedLine(line, 1);
                }
                log.addIndentedLine("------------------------------", 1);
            } else {
                log.addLine("User opted to skip comprehensive player sound test");
            }

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Test all default players by playing a test note with each one
     */
    public DiagnosticLogBuilder testDefaultPlayersSounds() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Default Players Sound Test");

        try {
            UserConfig config = UserConfigManager.getInstance().getCurrentConfig();
            if (config != null || config.getHasDefaults())
                UserConfigManager.getInstance().populateDefaults();

            // if (config == null || config.getPlayers() == null ||
            // config.getPlayers().isEmpty()) {
            // log.addError("No configuration or players available for testing");
            // return log;
            // }

            // Filter for default players only
            List<Player> defaultPlayers = new ArrayList<>();
            defaultPlayers.addAll(config.getDefaultNotes().stream()
                    .filter(p -> p.getIsDefault())
                    .collect(Collectors.toList()));

            defaultPlayers.addAll(config.getDefaultStrikes().stream()
                    .filter(p -> p.getIsDefault())
                    .collect(Collectors.toList()));

            log.addLine("Found " + defaultPlayers.size() + " default players to test");

            if (defaultPlayers.isEmpty()) {
                log.addWarning("No default players found. Creating them first...");

                // Ask user if they want to create default players
                boolean createDefaults = showYesNoDialog(
                        "No Default Players",
                        "No default players found in the configuration.\n\n" +
                                "Would you like to create the default players now?");

                if (createDefaults) {
                    log.addLine("Creating default instruments and players...");
                    boolean success = UserConfigManager.getInstance().populateDefaults();
                    log.addLine("Default population result: " + (success ? "Success" : "Failed"));

                    if (success) {
                        // Get the updated players list
                        config = UserConfigManager.getInstance().getCurrentConfig();
                        defaultPlayers = new ArrayList<>();
                        defaultPlayers.addAll(config.getDefaultNotes().stream()
                                .filter(p -> p.getIsDefault())
                                .collect(Collectors.toList()));

                        defaultPlayers.addAll(config.getDefaultStrikes().stream()
                                .filter(p -> p.getIsDefault())
                                .collect(Collectors.toList()));

                        log.addLine("Created " + defaultPlayers.size() + " default players");
                    } else {
                        log.addError("Failed to create default players");
                        return log;
                    }
                } else {
                    log.addLine("User chose not to create default players");
                    return log;
                }
            }

            // Separate drum and melodic players
            List<Player> drumPlayers = defaultPlayers.stream()
                    .filter(p -> p.isDrumPlayer())
                    .collect(Collectors.toList());

            List<Player> melodicPlayers = defaultPlayers.stream()
                    .filter(p -> p.isMelodicPlayer())
                    .collect(Collectors.toList());

            log.addLine("Default drum players: " + drumPlayers.size());
            log.addLine("Default melodic players: " + melodicPlayers.size());

            // Start by playing a sequence of drum sounds
            if (!drumPlayers.isEmpty()) {
                log.addSection("Testing Drum Players");
                log.addLine("Playing a sequence of drum sounds...");

                for (Player drumPlayer : drumPlayers) {
                    playTestNote(drumPlayer, log);
                    Thread.sleep(50); // Small gap between drum hits
                }

                // Ask user if they heard the drum sounds
                boolean drumSoundsHeard = showYesNoDialog(
                        "Drum Sounds Test",
                        "Did you hear a sequence of " + drumPlayers.size() + " drum sounds?\n\n" +
                                "Click Yes if you heard the sounds, No if you didn't.");

                log.addLine("User reported hearing drum sounds: " + (drumSoundsHeard ? "Yes" : "No"));

                if (!drumSoundsHeard) {
                    log.addWarning("User reported not hearing drum sounds");
                }
            }

            // Now test melodic players with a little musical phrase for each
            if (!melodicPlayers.isEmpty()) {
                log.addSection("Testing Melodic Players");
                log.addLine("Playing melodic instruments one by one...");

                for (int i = 0; i < melodicPlayers.size(); i++) {
                    Player melodicPlayer = melodicPlayers.get(i);
                    log.addIndentedLine("Playing " + melodicPlayer.getName(), 1);

                    // Play a simple musical phrase
                    if (melodicPlayer instanceof Note) {
                        playMelodicPhrase((Note) melodicPlayer, log);
                    } else {
                        playTestNote(melodicPlayer, log);
                    }

                    // Pause between instruments
                    Thread.sleep(500);

                    // Ask for confirmation periodically to avoid too many dialogs
                    if ((i + 1) % 4 == 0 || i == melodicPlayers.size() - 1) {
                        int start = i - ((i + 1) % 4) + 1;
                        int count = Math.min(4, i - start + 1);

                        boolean melodicSoundsHeard = showYesNoDialog(
                                "Melodic Sounds Test",
                                "Did you hear the " + count + " melodic instrument" +
                                        (count > 1 ? "s" : "") + " just played?\n\n" +
                                        "Click Yes if you heard them, No if you didn't.");

                        log.addLine("User reported hearing melodic instruments " +
                                (start + 1) + "-" + (i + 1) + ": " +
                                (melodicSoundsHeard ? "Yes" : "No"));

                        if (!melodicSoundsHeard) {
                            log.addWarning("User reported not hearing some melodic instruments");
                        }
                    }
                }
            }

            // Final summary
            log.addSection("Sound Test Summary");
            log.addLine("Completed testing " + defaultPlayers.size() + " default players");
            log.addLine("Check the warnings in this log if any sounds were not heard");

        } catch (Exception e) {
            log.addException(e);
        }

        return log;
    }

    /**
     * Play a test note with a player using its instrument directly
     */
    private void playTestNote(Player player, DiagnosticLogBuilder log) {
        try {
            if (player.getInstrument() == null || player.getInstrument().getReceiver() == null) {
                log.addWarning("Player has no instrument or receiver: " + player.getName());
                return;
            }

            int noteNumber = player.getRootNote() != null ? player.getRootNote() : 60;
            int channel = player.getDefaultChannel() != null ? player.getDefaultChannel()
                    : (player.isDrumPlayer() ? 9 : 0);
            int velocity = 100;

            // For drum players, use root note which should be a valid drum sound
            if (player.isDrumPlayer()) {
                log.addIndentedLine("Playing drum sound: " + player.getName() +
                        " (note: " + noteNumber + ", channel: " + channel + ")", 1);
            } else {
                log.addIndentedLine("Playing note: " + player.getName() +
                        " (note: " + noteNumber + ", channel: " + channel + ")", 1);
            }

            // TEST BOTH METHODS:
            // 1. First use the instrument's receiver directly to validate instrument
            // connection
            Receiver receiver = player.getInstrument().getReceiver();
            log.addIndentedLine("Sending message to receiver: " + receiver.getClass().getSimpleName(), 1);

            // Send note on message directly to instrument
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
            receiver.send(noteOn, -1);

            // Wait for half of the note duration
            Thread.sleep(player.isDrumPlayer() ? 100 : 250);

            // Send note off message
            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(ShortMessage.NOTE_OFF, channel, noteNumber, 0);
            receiver.send(noteOff, -1);

            // Small pause between the two testing methods
            Thread.sleep(100);

            // 2. Now test the player's own note-playing methods
            log.addIndentedLine("Testing player.noteOn() method", 1);
            player.noteOn(noteNumber, velocity);

            // Wait for the remaining duration
            Thread.sleep(player.isDrumPlayer() ? 100 : 250);

            // Turn off using player method
            player.allNotesOff();

            // Log success
            log.addIndentedLine("Test completed for player: " + player.getName(), 1);

        } catch (Exception e) {
            log.addWarning("Error playing test note for " + player.getName() + ": " + e.getMessage());
            log.addIndentedLine("Stack trace: " + e.getClass().getSimpleName() + " at " +
                    (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "unknown"), 2);
        }
    }

    /**
     * Play a short musical phrase with a melodic player
     */
    private void playMelodicPhrase(Note player, DiagnosticLogBuilder log) {
        try {
            if (player.getInstrument() == null || player.getInstrument().getReceiver() == null) {
                log.addWarning("Player has no instrument or receiver: " + player.getName());
                return;
            }

            int channel = player.getDefaultChannel() != null ? player.getDefaultChannel() : 0;
            int baseNote = player.getRootNote() != null ? player.getRootNote() : 60; // Middle C
            int velocity = 100;

            // Define a short musical phrase, centered around the root note
            // A little arpeggio in C major
            int[] notes = {baseNote, baseNote + 4, baseNote + 7, baseNote + 12,
                    baseNote + 7, baseNote + 4, baseNote};
            int[] durations = {200, 200, 200, 400, 200, 200, 400};

            Receiver receiver = player.getInstrument().getReceiver();

            // Play the phrase
            for (int i = 0; i < notes.length; i++) {
                // Note On
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, channel, notes[i], velocity);
                receiver.send(noteOn, -1);

                // Wait for note duration
                Thread.sleep(durations[i]);

                // Note Off
                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, channel, notes[i], 0);
                receiver.send(noteOff, -1);

                // Small gap between notes
                Thread.sleep(50);
            }

        } catch (Exception e) {
            log.addWarning("Error playing melodic phrase: " + e.getMessage());
        }
    }

    /**
     * Test configuration integrity
     */
    private void testConfig(UserConfig config, DiagnosticLogBuilder log) {
        log.addSection("Configuration Details");
        log.addLine("Config version: " + config.getConfigVersion());
        log.addLine(
                "Last updated: " + (config.getLastUpdated() != null ? config.getLastUpdated().toString() : "Not set"));

        // Test instrument configuration
        List<InstrumentWrapper> instruments = config.getInstruments();
        log.addLine("Instruments: " + (instruments != null ? instruments.size() : "None"));

        if (instruments != null && !instruments.isEmpty()) {
            // Count device types
            Map<String, Integer> deviceCount = new HashMap<>();
            int internalInstruments = 0;
            int missingNames = 0;
            int missingIds = 0;

            for (InstrumentWrapper instrument : instruments) {
                // Count by device type
                String deviceType = instrument.getDeviceName() != null ? instrument.getDeviceName() : "Unknown";
                deviceCount.put(deviceType, deviceCount.getOrDefault(deviceType, 0) + 1);

                // Count internal instruments
                if (deviceType.contains(SequencerConstants.GERVILL) || deviceType.contains("Internal")) {
                    internalInstruments++;
                }

                // Check for missing data
                if (instrument.getName() == null || instrument.getName().trim().isEmpty()) {
                    missingNames++;
                }

                if (instrument.getId() == null) {
                    missingIds++;
                }
            }

            log.addIndentedLine("Internal instruments: " + internalInstruments, 1);
            log.addIndentedLine("Instruments by device type:", 1);

            for (Map.Entry<String, Integer> entry : deviceCount.entrySet()) {
                log.addIndentedLine(entry.getKey() + ": " + entry.getValue(), 2);
            }

            if (missingNames > 0 || missingIds > 0) {
                log.addWarning("Found incomplete instrument records:");
                if (missingNames > 0)
                    log.addIndentedLine("Missing names: " + missingNames, 1);
                if (missingIds > 0)
                    log.addIndentedLine("Missing IDs: " + missingIds, 1);
            }

            // Check for duplicate instrument IDs
            Map<Long, List<String>> idToNames = new HashMap<>();

            for (InstrumentWrapper instrument : instruments) {
                if (instrument.getId() != null) {
                    idToNames.computeIfAbsent(instrument.getId(), id -> new ArrayList<>())
                            .add(instrument.getName() != null ? instrument.getName() : "Unknown");
                }
            }

            List<Long> duplicateIds = new ArrayList<>();
            for (Map.Entry<Long, List<String>> entry : idToNames.entrySet()) {
                if (entry.getValue().size() > 1) {
                    duplicateIds.add(entry.getKey());
                }
            }

            if (!duplicateIds.isEmpty()) {
                log.addWarning("Found " + duplicateIds.size() + " duplicate instrument IDs:");
                for (Long id : duplicateIds) {
                    log.addIndentedLine("ID " + id + ": " + String.join(", ", idToNames.get(id)), 1);
                }
            }
        }

        // Test players
        List<Player> players = new ArrayList<>();
        players.addAll(config.getDefaultNotes());
        players.addAll(config.getDefaultStrikes());

        log.addLine("Players: " + (players != null ? players.size() : "None"));

        if (players != null && !players.isEmpty()) {
            int drumPlayers = 0;
            int melodicPlayers = 0;
            int defaultPlayers = 0;

            for (Player player : players) {
                if (player.isDrumPlayer()) {
                    drumPlayers++;
                } else if (player.isMelodicPlayer()) {
                    melodicPlayers++;
                }

                if (player.getIsDefault()) {
                    defaultPlayers++;
                }
            }

            log.addIndentedLine("Drum players: " + drumPlayers, 1);
            log.addIndentedLine("Melodic players: " + melodicPlayers, 1);
            log.addIndentedLine("Default players: " + defaultPlayers, 1);
        }

        // Test Redis connectivity for user config
        log.addSection("Redis Connectivity Test");
        try {
            RedisService redisService = RedisService.getInstance();

            if (redisService != null && redisService.getUserConfigHelper() != null) {
                log.addLine("UserConfigHelper available: Yes");

                // Try loading config from Redis
                UserConfig redisConfig = redisService.getUserConfigHelper().loadConfigFromRedis();
                log.addLine("Config loaded from Redis: " + (redisConfig != null ? "Yes" : "No"));

                if (redisConfig != null) {
                    boolean sameConfig = redisConfig == config;
                    log.addLine("Same instance as current config: " + sameConfig);

                    if (!sameConfig) {
                        // Compare basic attributes
                        boolean sameVersion = redisConfig.getConfigVersion() == config.getConfigVersion();
                        boolean sameInstrumentCount = redisConfig.getInstruments() != null &&
                                config.getInstruments() != null &&
                                redisConfig.getInstruments().size() == config.getInstruments().size();

                        log.addLine("Same version: " + sameVersion);
                        log.addLine("Same instrument count: " + sameInstrumentCount);
                    }
                }
            } else {
                log.addWarning("UserConfigHelper not available, can't test Redis connectivity");
            }
        } catch (Exception e) {
            log.addError("Error testing Redis connectivity: " + e.getMessage());
        }
    }

    /**
     * Display a Yes/No confirmation dialog and return the result
     *
     * @param title   The dialog title
     * @param message The message to display
     * @return true if Yes was selected, false otherwise
     */
    private boolean showYesNoDialog(String title, String message) {
        int response = JOptionPane.showConfirmDialog(
                null,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        return response == JOptionPane.YES_OPTION;
    }
}
