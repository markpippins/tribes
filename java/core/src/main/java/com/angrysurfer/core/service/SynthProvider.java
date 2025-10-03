package com.angrysurfer.core.service;

import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

public class SynthProvider {
    private static final Logger logger = LoggerFactory.getLogger(SynthProvider.class);
    private static Synthesizer synthesizer;

    public static synchronized Synthesizer getSynthesizer() {
        if (synthesizer == null || !synthesizer.isOpen()) {
            initializeSynthesizer();
        }
        return synthesizer;
    }

    private static void initializeSynthesizer() {
        try {
            // Try to find Gervill synthesizer first
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;

            for (MidiDevice.Info info : infos) {
                if (info.getName().contains(SequencerConstants.GERVILL)) {
                    gervillInfo = info;
                    break;
                }
            }

            if (gervillInfo != null) {
                synthesizer = (Synthesizer) MidiSystem.getMidiDevice(gervillInfo);
            }

            // If Gervill not found, get default synthesizer
            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                logger.info("Synthesizer initialized: {}", synthesizer.getDeviceInfo().getName());
            }
        } catch (Exception e) {
            logger.error("Error initializing synthesizer: " + e.getMessage(), e);
        }
    }
}
