package com.angrysurfer.core.sequencer;

public class SequencerConstants {

    public static final String GERVILL = "Gervill";
    public static final String MSWAVE = "Microsoft GS Wavetable Synth";

    public static final double DEFAULT_BEAT_OFFSET = 1.0;
    public static final int DEFAULT_LOOP_COUNT = 0;
    public static final int DEFAULT_PART_COUNT = 1;
    // Constants
    public static final int DRUM_PAD_COUNT = 16; // Number of drum pads
    // Default values for parameters
    public static final int DEFAULT_VELOCITY = 100; // Default note velocity
    public static final int DEFAULT_DECAY = 60; // Default note decay
    public static final int DEFAULT_PROBABILITY = 100; // Default step probability (%)
    // public static final int DEFAULT_TICKS_PER_BEAT = 24; // Default timing fallback
    public static final int DEFAULT_MASTER_TEMPO = 96; // Default master tempo
    public static final int DEFAULT_PAN = 64; // Default pan position (center)
    public static final int DEFAULT_CHORUS = 0; // Default chorus effect amount
    public static final int DEFAULT_REVERB = 0; // Default reverb effect amount
    // MIDI and music constants
    public static final int MIDI_DRUM_CHANNEL = 9; // Standard MIDI drum channel
    public static final int MIDI_DRUM_NOTE_OFFSET = 36; // First drum pad note number
    public static final int MAX_MIDI_VELOCITY = 127; // Maximum MIDI velocity
    // Swing parameters
    public static final int NO_SWING = 50; // Percentage value for no swing
    public static final int MAX_SWING = 99; // Maximum swing percentage
    public static final int MIN_SWING = 25; // Minimum swing percentage
    // Pattern generation parameters
    public static final int MAX_DENSITY = 10; // Maximum density for pattern generation
    public static final long MIN_NOTE_INTERVAL_MS = 1; // 1ms minimum between notes
    public static final int DEFAULT_NOTE = 60; // Middle C
    public static final int DEFAULT_GATE = 75;

    public static final int[] MELODIC_CHANNELS = {2, 3, 4, 5, 6, 7, 8, 10}; //, 11, 12, 13, 14, 15};
    public static final int MAX_BAR_COUNT = 64;
    public static final Integer[] ALL_CHANNELS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public static final String DEFAULT_SOUNDBANK = "Java Internal Soundbank";

    public static int DEFAULT_PPQ = 48; // Update the default PPQ to 48 to work with both sequencers
    public static int DEFAULT_BAR_COUNT = 4;
    public static int DEFAULT_BEATS_PER_BAR = 4;
    public static int DEFAULT_BEAT_DIVIDER = 16;
    public static float DEFAULT_BPM = 88;
    public static int DEFAULT_PART_LENGTH = 1;
    public static int DEFAULT_MAX_TRACKS = 16;
    public static long DEFAULT_SONG_LENGTH = Long.MAX_VALUE;
    public static int DEFAULT_SWING = 25;
}
