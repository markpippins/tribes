// package com.angrysurfer.beats.test;

// import com.angrysurfer.core.model.InstrumentWrapper;
// import com.angrysurfer.core.model.Note;
// import com.angrysurfer.core.model.Player;
// import com.angrysurfer.core.sequencer.SequencerConstants;
// import com.angrysurfer.core.service.DeviceManager;
// import com.angrysurfer.core.service.InternalSynthManager;
// import com.angrysurfer.core.service.SoundbankManager;
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestInstance;

// import javax.sound.midi.MidiDevice;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;

// /**
//  * Test class to verify proper functionality of the refactored Soundbank system
//  */
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// public class SoundbankTest {

//     private InstrumentWrapper testInstrument;
//     private Player testPlayer;

//     @BeforeAll
//     void setup() {
//         System.out.println("Setting up test environment...");

//         try {
//             // Initialize the synthesizer
//             // InternalSynthManager.getInstance().initializeSynthesizer();

//             // Initialize soundbanks
//             SoundbankManager.getInstance().initializeSoundbanks();


//             MidiDevice device = DeviceManager.getMidiDevice(SequencerConstants.GERVILL);
//             // Create a test instrument
//             testInstrument = InternalSynthManager.getInstance().createInternalInstrument("Gervill", 0, device);

//             // Create a test player
//             testPlayer = new Note();
//             testPlayer.setId(999L);
//             testPlayer.setName("Test Player");
//             testInstrument.setChannel(1);
//             testPlayer.setInstrument(testInstrument);

//             // Initialize instrument state
//             InternalSynthManager.getInstance().initializeInstrumentState(testInstrument);

//             System.out.println("Test environment initialized successfully");
//         } catch (Exception e) {
//             System.err.println("Failed to initialize test environment: " + e.getMessage());
//             e.printStackTrace();
//             fail("Test setup failed");
//         }
//     }

//     @AfterAll
//     void cleanup() {
//         // Clean up resources
//         System.out.println("Cleaning up test resources...");
//     }

//     @Test
//     void testSoundbankAvailability() {
//         List<String> soundbankNames = SoundbankManager.getInstance().getSoundbankNames();

//         // We should have at least one soundbank available
//         assertFalse(soundbankNames.isEmpty(), "No soundbanks available");
//         System.out.println("Available soundbanks: " + soundbankNames);
//     }

//     @Test
//     void testSoundbankApplication() {
//         List<String> soundbankNames = SoundbankManager.getInstance().getSoundbankNames();
//         if (soundbankNames.isEmpty()) {
//             fail("No soundbanks available for testing");
//         }

//         // Get the first available soundbank
//         String soundbankName = soundbankNames.get(0);
//         System.out.println("Testing application of soundbank: " + soundbankName);

//         // Apply the soundbank
//         boolean applied = SoundbankManager.getInstance().applySoundbank(testInstrument, soundbankName);

//         // Verify it was applied
//         assertTrue(applied, "Failed to apply soundbank: " + soundbankName);
//         assertEquals(soundbankName, testInstrument.getSoundBank(),
//                 "Instrument does not have the correct soundbank name set");

//         // Verify banks are available for this soundbank
//         List<Integer> banks = SoundbankManager.getInstance().getAvailableBanksByName(soundbankName);
//         assertNotNull(banks, "Banks list should not be null");
//         System.out.println("Available banks for " + soundbankName + ": " + banks);

//         // If banks are available, test setting a bank and preset
//         if (!banks.isEmpty()) {
//             Integer bank = banks.get(0);
//             testInstrument.setBankIndex(bank);

//             // Apply the bank change
//             boolean presetApplied = InternalSynthManager.getInstance().updateInstrumentPreset(
//                     testInstrument, bank, testInstrument.getPreset());

//             assertTrue(presetApplied, "Failed to apply bank change");
//             assertEquals(bank, testInstrument.getBankIndex(), "Bank index not correctly set");

//             System.out.println("Successfully applied bank " + bank + " and preset " +
//                     testInstrument.getPreset() + " for soundbank " + soundbankName);
//         }
//     }

//     @Test
//     void testPlayerInstrumentPresetApplication() {
//         // Ensure our test player is properly set up
//         assertNotNull(testPlayer.getInstrument(), "Test player has no instrument");

//         // Test applying presets through PlayerManager
//         boolean applied = SoundbankManager.getInstance().applyInstrumentPreset(testPlayer);

//         // This should succeed even if we don't change anything
//         assertTrue(applied, "Failed to apply instrument preset");

//         // Now try with a specific preset change
//         if (!SoundbankManager.getInstance().getSoundbankNames().isEmpty()) {
//             String soundbank = SoundbankManager.getInstance().getSoundbankNames().get(0);
//             testPlayer.getInstrument().setSoundBank(soundbank);

//             // Try to update preset
//             testPlayer.getInstrument().setPreset(0);
//             applied = SoundbankManager.getInstance().applyInstrumentPreset(testPlayer);

//             assertTrue(applied, "Failed to apply instrument preset after change");

//             System.out.println("Successfully applied preset change to player " +
//                     testPlayer.getName() + " with soundbank " + soundbank);
//         }
//     }
// }
