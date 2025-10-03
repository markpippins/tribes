package com.angrysurfer.core.test;

import java.util.List;

import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SoundbankManager;

/**
 * Test class to verify soundbank functionality
 */
public class SoundbankChangeTest {

    private static final Logger logger = LoggerFactory.getLogger(SoundbankChangeTest.class);
    private static final int TEST_BANK = 0;
    private static final int TEST_PRESET = 0;
    private static final int TEST_CHANNEL = 0; // Use general MIDI channel 1

    public static void main(String[] args) {
        try {
            // Initialize managers
            logger.info("Initializing test environment...");
            InternalSynthManager synthManager = InternalSynthManager.getInstance();
            SoundbankManager soundbankManager = SoundbankManager.getInstance();
            PlayerManager playerManager = PlayerManager.getInstance();

            // Step 1: Ensure synth is initialized
            // synthManager.initializeSynthesizer();
            Synthesizer synth = synthManager.getSynthesizer();
            logger.info("Synthesizer initialized: {}", synth.getDeviceInfo().getName());

            // Step 2: List available soundbanks
            List<String> soundbanks = soundbankManager.getSoundbankNames();
            logger.info("Available soundbanks ({}): {}", soundbanks.size(), soundbanks);

            if (soundbanks.isEmpty()) {
                logger.error("No soundbanks available. Test cannot continue.");
                return;
            }

            // Step 3: Create test player
            Player testPlayer = new Note();
            testPlayer.setName("TestPlayer");
            // testPlayer.setChannel(TEST_CHANNEL);

            // Set up instrument
            InstrumentWrapper instrument = new InstrumentWrapper();
            instrument.setName("TestInstrument");
            instrument.setChannel(TEST_CHANNEL);
            instrument.setBankIndex(TEST_BANK);
            instrument.setPreset(TEST_PRESET);
            testPlayer.setInstrument(instrument);

            // Step 4: Test soundbank changes
            for (String soundbankName : soundbanks) {
                logger.info("Testing soundbank: {}", soundbankName);

                // Apply soundbank
                instrument.setSoundBank(soundbankName);
                boolean applied = soundbankManager.applySoundbank(instrument, soundbankName);
                logger.info("Applied soundbank {} to instrument: {}",
                        soundbankName, applied ? "SUCCESS" : "FAILED");

                // Verify current soundbank
                Soundbank currentSoundbank = synth.getDefaultSoundbank();
                String currentName = currentSoundbank != null ? currentSoundbank.getName() : "NONE";
                logger.info("Current active soundbank: {}", currentName);

                // Play test note
                logger.info("Playing test note with soundbank: {}", soundbankName);
                soundbankManager.playPreviewNote(testPlayer, 500);

                // Wait for note to finish
                Thread.sleep(1000);
            }

            logger.info("Test completed successfully");
        } catch (Exception e) {
            logger.error("Test failed: {}", e.getMessage(), e);
        }
    }
}
