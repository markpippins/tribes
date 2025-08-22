package com.angrysurfer.beats.diagnostic.suite;

import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

/**
 * Helper class for UserConfig diagnostics using direct model objects
 */
public class UserConfigDiagnostics {

    private final RedisService redisService;
    private final UserConfigHelper configHelper;
    private final InstrumentManager instrumentManager;
    private final PlayerManager playerManager;
    private final SessionManager sessionManager;

    /**
     * Constructor
     */
    public UserConfigDiagnostics() {
        this.redisService = RedisService.getInstance();
        this.configHelper = redisService.getUserConfigHelper();
        this.instrumentManager = InstrumentManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }

    /**
     * Run diagnostic tests for user configuration
     */
//    public DiagnosticLogBuilder testUserConfigDiagnostics() {
//        DiagnosticLogBuilder log = new DiagnosticLogBuilder("User Configuration Diagnostics");
//
//        try {
//            // Test loading existing config
//            log.addSection("Test: UserConfig operations");
//
//            // First, check existing config
//            UserConfig existingConfig = configHelper.loadConfigFromRedis();
//            if (existingConfig != null) {
//                log.addIndentedLine("Found existing UserConfig in Redis", 1);
//                log.addIndentedLine("User ID: " + existingConfig.getUserId(), 1);
//                log.addIndentedLine("Default device: " + existingConfig.getDefaultDevice(), 1);
//            } else {
//                log.addIndentedLine("No existing UserConfig found in Redis", 1);
//            }
//
//            // Test instrument management
//            testInstruments(log);
//
//            // Test player management
//            testPlayers(log);
//
//            // Test integrated instrument-player workflow
//            testIntegratedWorkflow(log);
//
//        } catch (Exception e) {
//            log.addException(e);
//        }
//
//        return log;
//    }
//
//    /**
//     * Test instrument operations using InstrumentWrapper directly
//     */
//    private void testInstruments(DiagnosticLogBuilder log) {
//        log.addSection("Test: Instrument Operations");
//
//        try {
//            // Count existing instruments
//            List<InstrumentWrapper> existingInstruments = instrumentManager.getInstruments();
//            log.addIndentedLine("Current instrument count: " + existingInstruments.size(), 1);
//
//            // Create test instrument
//            String testName = "Test-Instrument-" + System.currentTimeMillis();
//            InstrumentWrapper testInstrument = createTestInstrument(testName);
//
//            if (testInstrument == null) {
//                log.addError("Failed to create test instrument");
//                return;
//            }
//
//            log.addIndentedLine("Created test instrument: " + testName, 1);
//
//            // Save instrument
//            boolean saved = instrumentManager.saveInstrument(testInstrument);
//            log.addIndentedLine("Saved instrument: " + saved, 1);
//
//            // Retrieve instrument
//            InstrumentWrapper retrieved = instrumentManager.getInstrumentByName(testName);
//            if (retrieved != null) {
//                log.addIndentedLine("Successfully retrieved instrument by name", 1);
//                log.addIndentedLine("ID: " + retrieved.getId(), 2);
//                log.addIndentedLine("Channel: " + retrieved.getChannel(), 2);
//                log.addIndentedLine("Internal: " + retrieved.getInternal(), 2);
//            } else {
//                log.addError("Failed to retrieve instrument by name");
//            }
//
//            // Test program change
//            if (retrieved != null) {
//                int oldPreset = retrieved.getPreset() != null ? retrieved.getPreset() : 0;
//                int newPreset = (oldPreset + 1) % 128;
//
//                log.addIndentedLine("Testing program change from " + oldPreset + " to " + newPreset, 1);
//                retrieved.setPreset(newPreset);
//                instrumentManager.saveInstrument(retrieved);
//
//                // Verify change persisted
//                InstrumentWrapper afterChange = instrumentManager.getInstrumentByName(testName);
//                if (afterChange != null && afterChange.getPreset() == newPreset) {
//                    log.addIndentedLine("Program change successfully saved", 2);
//                } else {
//                    log.addError("Program change not saved correctly");
//                }
//            }
//
//            // Clean up - remove test instrument if needed
//            // This is commented out to avoid unintended side effects in the real system
//            // instrumentManager.removeInstrument(testInstrument);
//
//        } catch (Exception e) {
//            log.addError("Error during instrument test: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Helper method to create a test instrument
//     */
//    private InstrumentWrapper createTestInstrument(String name) {
//        try {
//            // Create a basic InstrumentWrapper with Java Sound Synthesizer
//            InstrumentWrapper instrument = new InstrumentWrapper();
//            instrument.setName(name);
//            instrument.setChannel(0);
//            instrument.setPreset(0);
//            instrument.setSoundBank("Java Internal Soundbank");
//            instrument.setDeviceName("Test Device");
//
//            // Try to get a real device or create with null device
//            try {
//                MidiDevice device = MidiSystem.getSynthesizer();
//                if (!device.isOpen()) {
//                    device.open();
//                }
//                Receiver receiver = device.getReceiver();
//                instrument.setReceiver(receiver);
//            } catch (MidiUnavailableException e) {
//                // Just create without a device
//                instrument.setInternal(true);
//            }
//
//            return instrument;
//        } catch (Exception e) {
//            return null;
//        }
//    }

    /**
     * Test player operations using Player subclasses directly
     */
//    private void testPlayers(DiagnosticLogBuilder log) {
//        log.addSection("Test: Player Operations");
//
//        try {
//            // Get active session
//            Session session = sessionManager.getActiveSession();
//            if (session == null) {
//                log.addError("No active session available for player testing");
//                return;
//            }
//
//            // Count existing players
//            int existingCount = session.getPlayers().size();
//            log.addIndentedLine("Current player count in session: " + existingCount, 1);
//
//            // Create test melodic player (Note)
//            String noteName = "Test-Note-" + System.currentTimeMillis();
//            log.addIndentedLine("Creating test melodic player: " + noteName, 1);
//
//            // Get a suitable instrument
//            InstrumentWrapper instrument = instrumentManager.getInstrumentCache().isEmpty() ?
//                    createTestInstrument("Test-Instrument-For-Player") :
//                    instrumentManager.getInstrumentCache().get(0);
//
//            if (instrument == null) {
//                log.addError("No instrument available for player test");
//                return;
//            }
//
//            // Create a Note (melodic player)
//            Note notePlayer = new Note(noteName, session, instrument, 60, new ArrayList<>());
//            notePlayer.setMelodicPlayer(true);
//            notePlayer.setRootNote(60);  // Middle C
//
//            // Save to session
//            session.getPlayers().add(notePlayer);
//            sessionManager.saveActiveSession();
//
//            log.addIndentedLine("Added melodic player to session", 1);
//
//            // Create test drum player (Strike)
//            String strikeName = "Test-Strike-" + System.currentTimeMillis();
//            log.addIndentedLine("Creating test drum player: " + strikeName, 1);
//
//            // Create a Strike (drum player)
//            Strike strikePlayer = new Strike(strikeName, session, instrument, 36, new ArrayList<>());
//            strikePlayer.setDrumPlayer(true);
//            strikePlayer.setRootNote(36);  // Kick drum
//
//            // Save to session
//            session.getPlayers().add(strikePlayer);
//            sessionManager.saveActiveSession();
//
//            log.addIndentedLine("Added drum player to session", 1);
//
//            // Verify player retrieval
//            Session verifySession = sessionManager.getActiveSession();
//            boolean noteFound = false;
//            boolean strikeFound = false;
//
//            for (Player player : verifySession.getPlayers()) {
//                if (player.getName().equals(noteName)) {
//                    noteFound = true;
//                    log.addIndentedLine("Found melodic player in session", 2);
//                    log.addIndentedLine("Root note: " + player.getRootNote(), 3);
//                }
//                if (player.getName().equals(strikeName)) {
//                    strikeFound = true;
//                    log.addIndentedLine("Found drum player in session", 2);
//                    log.addIndentedLine("Root note: " + player.getRootNote(), 3);
//                }
//            }
//
//            if (!noteFound) log.addError("Melodic player not found in session");
//            if (!strikeFound) log.addError("Drum player not found in session");
//
//            // Test player properties
//            if (strikeFound) {
//                // Find and modify our test strike player
//                for (Player player : verifySession.getPlayers()) {
//                    if (player.getName().equals(strikeName)) {
//                        // Test changing a property
//                        int oldVelocity = player.getMaxVelocity();
//                        int newVelocity = oldVelocity + 10;
//                        player.setMaxVelocity(newVelocity);
//
//                        log.addIndentedLine("Changed max velocity from " + oldVelocity + " to " + newVelocity, 2);
//
//                        // Save changes
//                        sessionManager.saveActiveSession();
//                        break;
//                    }
//                }
//
//                // Verify changes persisted
//                Session afterSession = sessionManager.getActiveSession();
//                for (Player player : afterSession.getPlayers()) {
//                    if (player.getName().equals(strikeName)) {
//                        log.addIndentedLine("Current velocity: " + player.getMaxVelocity(), 2);
//                        break;
//                    }
//                }
//            }
//
//            // Clean up - remove test players if needed
//            // This is commented out to avoid unintended side effects in the real system
//            /*
//            Session cleanupSession = sessionManager.getActiveSession();
//            cleanupSession.getPlayers().removeIf(p ->
//                p.getName().equals(noteName) || p.getName().equals(strikeName));
//            sessionManager.saveActiveSession();
//            */
//
//        } catch (Exception e) {
//            log.addError("Error during player test: " + e.getMessage());
//        }
//    }

    /**
     * Test integrated workflow between instruments and players
     */
//    private void testIntegratedWorkflow(DiagnosticLogBuilder log) {
//        log.addSection("Test: Integrated Instrument-Player Workflow");
//
//        try {
//            // Generate unique names for test
//            String instName = "Test-Integrated-Inst-" + UUID.randomUUID().toString().substring(0, 8);
//            String playerName = "Test-Integrated-Player-" + UUID.randomUUID().toString().substring(0, 8);
//
//            // 1. Create and save test instrument
//            log.addIndentedLine("Step 1: Creating test instrument " + instName, 1);
//            InstrumentWrapper testInstrument = createTestInstrument(instName);
//            if (testInstrument == null) {
//                log.addError("Failed to create test instrument");
//                return;
//            }
//
//            instrumentManager.saveInstrument(testInstrument);
//            log.addIndentedLine("Instrument saved successfully", 2);
//
//            // 2. Create player that uses this instrument
//            log.addIndentedLine("Step 2: Creating test player " + playerName, 1);
//            Session session = sessionManager.getActiveSession();
//            if (session == null) {
//                log.addError("No active session available");
//                return;
//            }
//
//            // Use Strike player for test
//            Strike player = new Strike();
//            player.setName(playerName);
//            player.setSession(session);
//            player.setRootNote(42);  // Hi-hat
//            player.setLevel(90);
//            player.setInstrument(testInstrument);
//
//            session.getPlayers().add(player);
//            sessionManager.saveActiveSession();
//            log.addIndentedLine("Player created and added to session", 2);
//
//            // 3. Test changing instrument parameters
//            log.addIndentedLine("Step 3: Testing instrument parameter change", 1);
//            testInstrument.setPreset(10);  // Change to a different sound
//            instrumentManager.saveInstrument(testInstrument);
//            log.addIndentedLine("Changed instrument preset to 10", 2);
//
//            // 4. Verify player now uses updated instrument
//            log.addIndentedLine("Step 4: Verifying player uses updated instrument", 1);
//            Session verifySession = sessionManager.getActiveSession();
//            for (Player p : verifySession.getPlayers()) {
//                if (p.getName().equals(playerName)) {
//                    InstrumentWrapper playerInst = p.getInstrument();
//                    if (playerInst != null) {
//                        log.addIndentedLine("Player's instrument preset: " + playerInst.getPreset(), 2);
//                        if (playerInst.getPreset() == 10) {
//                            log.addIndentedLine("SUCCESS: Player has updated instrument settings", 2);
//                        } else {
//                            log.addError("Player does not have updated instrument settings");
//                        }
//                    } else {
//                        log.addError("Player has no instrument assigned");
//                    }
//                    break;
//                }
//            }
//
//            // 5. Test sending MIDI messages
//            log.addIndentedLine("Step 5: Testing MIDI message sending", 1);
//            try {
//                for (Player p : verifySession.getPlayers()) {
//                    if (p.getName().equals(playerName)) {
//                        if (p.getInstrument() != null && p.getInstrument().getReceiver() != null) {
//                            // Try to send a note
//                            p.getInstrument().noteOn(60, 100);
//                            log.addIndentedLine("Sent noteOn message successfully", 2);
//
//                            // Wait a moment and send note off
//                            Thread.sleep(100);
//                            p.getInstrument().noteOff(60, 0);
//                            log.addIndentedLine("Sent noteOff message successfully", 2);
//                        } else {
//                            log.addIndentedLine("Skipping MIDI test - no receiver available", 2);
//                        }
//                        break;
//                    }
//                }
//            } catch (Exception e) {
//                log.addIndentedLine("MIDI test exception: " + e.getMessage(), 2);
//            }
//
//            // Clean up - remove test instrument and player if needed
//            // This is commented out to avoid unintended side effects in the real system
//            /*
//            log.addIndentedLine("Step 6: Cleaning up test objects", 1);
//            Session cleanupSession = sessionManager.getActiveSession();
//            cleanupSession.getPlayers().removeIf(p -> p.getName().equals(playerName));
//            sessionManager.saveActiveSession();
//            instrumentManager.removeInstrumentByName(instName);
//            */
//
//        } catch (Exception e) {
//            log.addError("Error in integrated workflow test: " + e.getMessage());
//        }
//    }
}
