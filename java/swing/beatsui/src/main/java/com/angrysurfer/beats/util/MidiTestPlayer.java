package com.angrysurfer.beats.util;

import com.angrysurfer.core.api.midi.MidiControlMessageEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import java.io.File;

/**
 * Self-contained class for reliable MIDI testing and preview
 */
public class MidiTestPlayer {
    private static final Logger logger = LoggerFactory.getLogger(MidiTestPlayer.class);
    private static MidiTestPlayer instance;

    private Synthesizer synth;

    private MidiTestPlayer() {
        try {
            // Create a dedicated synth for testing
            synth = MidiSystem.getSynthesizer();
            synth.open();
            logger.info("Test player initialized with {} channels", synth.getChannels().length);
        } catch (Exception e) {
            logger.error("Error initializing MidiTestPlayer: {}", e.getMessage());
        }
    }

    public static synchronized MidiTestPlayer getInstance() {
        if (instance == null) {
            instance = new MidiTestPlayer();
        }
        return instance;
    }

    /**
     * Load a soundbank file into the test synthesizer
     */
    public boolean loadSoundbank(File file) {
        try {
            Soundbank sb = MidiSystem.getSoundbank(file);
            if (sb != null) {
                synth.loadAllInstruments(sb);
                logger.info("Loaded soundbank: {}", sb.getName());
                return true;
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Play a test note with specified bank/program
     */
    public boolean playTestNote(int channel, int bank, int program, int note, int velocity) {
        try {
            if (!synth.isOpen()) {
                synth.open();
            }

            MidiChannel[] channels = synth.getChannels();
            if (channels == null || channel >= channels.length || channels[channel] == null) {
                logger.error("Invalid channel: {}", channel);
                return false;
            }

            // Set bank and program
            channels[channel].controlChange(0, bank >> 7); // Bank MSB
            channels[channel].controlChange(32, bank & MidiControlMessageEnum.POLY_MODE_ON); // Bank LSB
            channels[channel].programChange(program);

            // Play note
            channels[channel].noteOn(note, velocity);
            logger.info("Playing note {} on channel {} (bank={}, program={})",
                    note, channel, bank, program);

            // Schedule note off
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    channels[channel].noteOff(note);
                } catch (Exception e) {
                    // Ignore
                }
            }).start();

            return true;
        } catch (Exception e) {
            logger.error("Error playing test note: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        try {
            if (synth != null && synth.isOpen()) {
                synth.close();
            }
        } catch (Exception e) {
            logger.error("Error cleaning up MidiTestPlayer: {}", e.getMessage());
        }
    }
}
