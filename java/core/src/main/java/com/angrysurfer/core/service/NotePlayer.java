package com.angrysurfer.core.service;

import com.angrysurfer.core.model.InstrumentWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotePlayer {
    private static final Logger logger = LoggerFactory.getLogger(NotePlayer.class);

    public void playNote(int note, int velocity, int durationMs, int channel) {
        InstrumentWrapper instrument = InstrumentManager.getInstance().findOrCreateInternalInstrument(channel);
        if (instrument != null) {
            instrument.playNote(note, velocity, durationMs);
        }
    }
}
