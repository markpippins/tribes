package com.angrysurfer.core.service;

import com.angrysurfer.core.model.preset.SynthData;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.Instrument;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoundbankLoader {
    private static final Logger logger = LoggerFactory.getLogger(SoundbankLoader.class);
    private final Map<Long, SynthData> synthDataMap = new HashMap<>();
    private final LinkedHashMap<String, Soundbank> soundbanks = new LinkedHashMap<>();
    private final Map<String, List<Integer>> availableBanksMap = new HashMap<>();

    public void initializeSynthData() {
        try {
            // Clear existing data first
            synthDataMap.clear();

            // We need a synthesizer instance
            Synthesizer synthesizer = SynthProvider.getSynthesizer();

            if (synthesizer != null) {
                // Create entry for the default soundbank
                Soundbank defaultSoundbank = synthesizer.getDefaultSoundbank();
                if (defaultSoundbank != null) {
                    String sbName = SequencerConstants.DEFAULT_SOUNDBANK;
                    SynthData synthData = new SynthData(sbName);

                    // Add all instruments from default soundbank
                    for (Instrument instrument : defaultSoundbank.getInstruments()) {
                        synthData.addInstrument(instrument);
                    }

                    // Store in map with the synthesizer ID
                    long synthId = System.identityHashCode(synthesizer);
                    synthDataMap.put(synthId, synthData);

                    // Also add to soundbanks collection
                    soundbanks.put(sbName, defaultSoundbank);

                    // Cache available banks
                    availableBanksMap.put(sbName, synthData.getAvailableBanks());

                    logger.info("Initialized synthesizer data with {} instruments",
                            synthData.getInstruments().size());
                } else {
                    logger.warn("No default soundbank available in synthesizer");
                }
            } else {
                logger.warn("No synthesizer available for initialization");
            }
        } catch (Exception e) {
            logger.error("Error initializing synth data: " + e.getMessage(), e);
        }
    }

    public void loadDefaultSoundbank() {
        Synthesizer synthesizer = SynthProvider.getSynthesizer();
        if (synthesizer == null) {
            logger.error("Failed to initialize synthesizer to load soundbanks");
            return;
        }

        // Delegate to SoundbankManager
        SoundbankManager.getInstance().initializeSoundbanks();
    }

    public String loadSoundbank(File file) {
        return SoundbankManager.getInstance().loadSoundbank(file);
    }
}
