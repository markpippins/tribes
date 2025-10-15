package com.angrysurfer.core.sequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scale {

    static final Logger logger = LoggerFactory.getLogger(Scale.class);

    public static String[] SCALE_NOTES = {
        "C", "C♯/D♭", "D", "D♯/E♭", "E",
        "F", "F♯/G♭", "G", "G♯/A♭", "A", "A♯/B♭", "B"
    };

    // Scale name constants
    public static final String SCALE_CHROMATIC = "Chromatic";
    public static final String SCALE_MAJOR = "Major";
    public static final String SCALE_NATURAL_MINOR = "Natural Minor";
    public static final String SCALE_HARMONIC_MINOR = "Harmonic Minor";
    public static final String SCALE_MELODIC_MINOR = "Melodic Minor";
    public static final String SCALE_MAJOR_PENTATONIC = "Major Pentatonic";
    public static final String SCALE_MINOR_PENTATONIC = "Minor Pentatonic";
    public static final String SCALE_DOUBLE_HARMONIC_MINOR = "Double Harmonic Minor";
    public static final String SCALE_DOUBLE_HARMONIC_MAJOR = "Double Harmonic Major";
    public static final String SCALE_BLUES = "Blues";
    public static final String SCALE_WHOLE_TONE = "Whole Tone";
    public static final String SCALE_DIMINISHED_WHOLE_HALF = "Diminished (Whole-Half)";
    public static final String SCALE_DIMINISHED_HALF_WHOLE = "Diminished (Half-Whole)";
    public static final String SCALE_AUGMENTED = "Augmented";
    public static final String SCALE_IONIAN = "Ionian (Major)";
    public static final String SCALE_DORIAN = "Dorian";
    public static final String SCALE_PHRYGIAN = "Phrygian";
    public static final String SCALE_LYDIAN = "Lydian";
    public static final String SCALE_MIXOLYDIAN = "Mixolydian";
    public static final String SCALE_AEOLIAN = "Aeolian (Natural Minor)";
    public static final String SCALE_LOCRIAN = "Locrian";
    public static final String SCALE_HUNGARIAN_MINOR = "Hungarian Minor";
    public static final String SCALE_SPANISH_GYPSY = "Spanish Gypsy";
    public static final String SCALE_PERSIAN = "Persian";
    public static final String SCALE_HIRAJOSHI = "Hirajoshi";
    public static final String SCALE_IN_SCALE = "In Scale";
    public static final String SCALE_ARABIAN = "Arabian";
    public static final String SCALE_GYPSY_MAJOR = "Gypsy Major";
    public static final String SCALE_GYPSY_MINOR = "Gypsy Minor";
    public static final String SCALE_ARABIC = "Arabic";
    public static final String SCALE_HIJAZ = "Hijaz";
    public static final String SCALE_HIJAZ_KAR = "Hijaz Kar";
    public static final String SCALE_PHRYGIAN_DOMINANT = "Phrygian Dominant";
    public static final String SCALE_NAHAWAND = "Nahawand";
    public static final String SCALE_NAHAWAND_MURASSAA = "Nahawand Murassaa";
    public static final String SCALE_NIKRIZ = "Nikriz";
    public static final String SCALE_SABA = "Saba";
    public static final String SCALE_SIKAH = "Sikah";
    public static final String SCALE_SIKAH_BALADI = "Sikah Baladi";
    public static final String SCALE_RAST = "Rast";
    public static final String SCALE_BAYATI = "Bayati";
    public static final String SCALE_HUZAM = "Huzam";
    public static final String SCALE_KURD = "Kurd";
    public static final String SCALE_SABA_ZAMZAM = "Saba Zamzam";
    public static final String SCALE_SUZNAK = "Suznak";
    public static final String SCALE_NAWA_ATHAR = "Nawa Athar";
    public static final String SCALE_ATHAR_KURD = "Athar Kurd";
    public static final String SCALE_HIJAZ_KAR_KURD = "Hijaz Kar Kurd";
    public static final String SCALE_KURDISH = "Kurdish";
    public static final String SCALE_RAGA_TODI = "Raga Todi";
    public static final String SCALE_ORIENTAL = "Oriental";
    public static final String SCALE_JEWISH = "Jewish";
    public static final String SCALE_UKRAINIAN_DORIAN = "Ukrainian Dorian";
    public static final String SCALE_SUPER_LOCRIAN = "Super Locrian";
    public static final String SCALE_LYDIAN_AUGMENTED = "Lydian Augmented";
    public static final String SCALE_MIXOLYDIAN_FLAT_6 = "Mixolydian b6";
    public static final String SCALE_LOCRIAN_NATURAL_6 = "Locrian Natural 6";
    public static final String SCALE_HALF_WHOLE_DIMINISHED = "Half-Whole Diminished";
    public static final String SCALE_WHOLE_HALF_DIMINISHED = "Whole-Half Diminished";
    public static final String SCALE_MAJOR_BLUES = "Major Blues";
    public static final String SCALE_MINOR_BLUES = "Minor Blues";
    public static final String SCALE_TRITONE = "Tritone";
    public static final String SCALE_SPANISH_PHRYGIAN = "Spanish Phrygian";
    public static final String SCALE_DORIAN_FLAT_2 = "Dorian b2";
    public static final String SCALE_LYDIAN_SHARP_2 = "Lydian #2";
    public static final String SCALE_SUPER_LOCRIAN_FLAT_FLAT_7 = "Super Locrian bb7";
    public static final String SCALE_AUGMENTED_LYDIAN = "Augmented Lydian";
    public static final String SCALE_AUGMENTED_IONIAN = "Augmented Ionian";
    public static final String SCALE_AUGMENTED_DORIAN = "Augmented Dorian";
    public static final String SCALE_AUGMENTED_PHRYGIAN = "Augmented Phrygian";
    public static final String SCALE_AUGMENTED_MIXOLYDIAN = "Augmented Mixolydian";
    public static final String SCALE_AUGMENTED_AEOLIAN = "Augmented Aeolian";
    public static final String SCALE_AUGMENTED_LOCRIAN = "Augmented Locrian";
    public static final String SCALE_AUGMENTED_HARMONIC_MINOR = "Augmented Harmonic Minor";
    public static final String SCALE_AUGMENTED_MELODIC_MINOR = "Augmented Melodic Minor";
    public static final String SCALE_AUGMENTED_DORIAN_FLAT_2 = "Augmented Dorian b2";
    public static final String SCALE_AUGMENTED_MIXOLYDIAN_FLAT_6 = "Augmented Mixolydian b6";
    public static final String SCALE_AUGMENTED_LOCRIAN_FLAT_7 = "Augmented Locrian b7";
    public static final String SCALE_BEBOP = "Bebop";
    public static final String SCALE_BEBOP_MAJOR = "Bebop Major";
    public static final String SCALE_BEBOP_MINOR = "Bebop Minor";
    public static final String SCALE_BEBOP_DOMINANT = "Bebop Dominant";
    public static final String SCALE_BEBOP_DORIAN = "Bebop Dorian";
    public static final String SCALE_BEBOP_PHRYGIAN = "Bebop Phrygian";
    public static final String SCALE_BEBOP_LYDIAN = "Bebop Lydian";
    public static final String SCALE_BEBOP_LOCRIAN = "Bebop Locrian";
    public static final String SCALE_BEBOP_HARMONIC_MINOR = "Bebop Harmonic Minor";
    public static final String SCALE_BEBOP_MELODIC_MINOR = "Bebop Melodic Minor";
    public static final String SCALE_BEBOP_DORIAN_FLAT_2 = "Bebop Dorian b2";
    public static final String SCALE_BEBOP_MIXOLYDIAN_FLAT_6 = "Bebop Mixolydian b6";
    public static final String SCALE_BEBOP_LOCRIAN_FLAT_7 = "Bebop Locrian b7";
    public static final String SCALE_AUGMENTED_HEXATONIC = "Augmented Hexatonic";
    public static final String SCALE_PROMETHEUS = "Prometheus";
    public static final String SCALE_RAGA_MEGHA = "Raga Megha";
    public static final String SCALE_BALINESE = "Balinese";
    public static final String SCALE_JAVANESE = "Javanese";
    public static final String SCALE_JAPANESE = "Japanese";
    public static final String SCALE_ROMANIAN_MINOR = "Romanian Minor";
    public static final String SCALE_NEAPOLITAN_MAJOR = "Neapolitan Major";
    public static final String SCALE_NEAPOLITAN_MINOR = "Neapolitan Minor";
    public static final String SCALE_ENIGMATIC = "Enigmatic";
    public static final String SCALE_EIGHT_TONE_SPANISH = "Eight-Tone Spanish";
    public static final String SCALE_LEADING_WHOLE_TONE = "Leading Whole Tone";
    public static final String SCALE_LYDIAN_MINOR = "Lydian Minor";
    public static final String SCALE_ACOUSTIC = "Acoustic";
    public static final String SCALE_LOCRIAN_MAJOR = "Locrian Major";
    public static final String SCALE_IONIAN_AUGMENTED = "Ionian Augmented";
    public static final String SCALE_DORIAN_SHARP_4 = "Dorian #4";
    public static final String SCALE_AEOLIAN_FLAT_5 = "Aeolian b5";
    public static final String SCALE_ALTERED_DOMINANT = "Altered Dominant";
    public static final String SCALE_LOCRIAN_SHARP_6 = "Locrian #6";
    public static final String SCALE_IONIAN_SHARP_5 = "Ionian #5";
    public static final String SCALE_LYDIAN_FLAT_3 = "Lydian b3";
    public static final String SCALE_MIXOLYDIAN_FLAT_9 = "Mixolydian b9";
    public static final String SCALE_EGYPTIAN = "Egyptian";
    public static final String SCALE_IWATO = "Iwato";
    public static final String SCALE_MAN_GONG = "Man Gong";
    public static final String SCALE_QUARTER_TONE = "Quarter-Tone Scale";
    public static final String SCALE_ARABIAN_MAQAM_RAST = "Arabian Maqam Rast";
    public static final String SCALE_ARABIAN_MAQAM_BAYATI = "Arabian Maqam Bayati";
    public static final String SCALE_SLENDRO = "Slendro";
    public static final String SCALE_ISTRIAN = "Istrian";
    public static final String SCALE_BALINESE_PELOG = "Balinese Pelog";

    // Define scale patterns as offsets
    public static Map<String, int[]> SCALE_PATTERNS = new HashMap<>();

    static {
        // Major and Minor Scales
        SCALE_PATTERNS.put(SCALE_MAJOR, new int[]{0, 2, 4, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_NATURAL_MINOR, new int[]{0, 2, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_HARMONIC_MINOR, new int[]{0, 2, 3, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_MELODIC_MINOR, new int[]{0, 2, 3, 5, 7, 9, 11});

        // Pentatonic Scales
        SCALE_PATTERNS.put(SCALE_MAJOR_PENTATONIC, new int[]{0, 2, 4, 7, 9});
        SCALE_PATTERNS.put(SCALE_MINOR_PENTATONIC, new int[]{0, 3, 5, 7, 10});

        // Double Harmonic Scales
        SCALE_PATTERNS.put(SCALE_DOUBLE_HARMONIC_MINOR, new int[]{0, 1, 4, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_DOUBLE_HARMONIC_MAJOR, new int[]{0, 1, 4, 5, 7, 9, 11});

        // Blues Scales
        SCALE_PATTERNS.put(SCALE_BLUES, new int[]{0, 3, 5, 6, 7, 10});

        // Whole Tone Scale
        SCALE_PATTERNS.put(SCALE_WHOLE_TONE, new int[]{0, 2, 4, 6, 8, 10});

        // Diminished Scales
        SCALE_PATTERNS.put(SCALE_DIMINISHED_WHOLE_HALF, new int[]{0, 2, 3, 5, 6, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_DIMINISHED_HALF_WHOLE, new int[]{0, 1, 3, 4, 6, 7, 9, 10});

        // Augmented Scale
        SCALE_PATTERNS.put(SCALE_AUGMENTED, new int[]{0, 3, 4, 7, 8, 11});

        // Modes (Church Modes)
        SCALE_PATTERNS.put(SCALE_IONIAN, new int[]{0, 2, 4, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_DORIAN, new int[]{0, 2, 3, 5, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_PHRYGIAN, new int[]{0, 1, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_LYDIAN, new int[]{0, 2, 4, 6, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_MIXOLYDIAN, new int[]{0, 2, 4, 5, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_AEOLIAN, new int[]{0, 2, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_LOCRIAN, new int[]{0, 1, 3, 5, 6, 8, 10});

        // Exotic Scales
        SCALE_PATTERNS.put(SCALE_HUNGARIAN_MINOR, new int[]{0, 2, 3, 6, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_SPANISH_GYPSY, new int[]{0, 1, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_PERSIAN, new int[]{0, 1, 4, 5, 6, 8, 11});
        SCALE_PATTERNS.put(SCALE_HIRAJOSHI, new int[]{0, 2, 3, 7, 8});
        SCALE_PATTERNS.put(SCALE_IN_SCALE, new int[]{0, 1, 5, 7, 8});
        SCALE_PATTERNS.put(SCALE_ARABIAN, new int[]{0, 2, 4, 5, 6, 8, 10});

        // Gypsy Scales
        SCALE_PATTERNS.put(SCALE_GYPSY_MAJOR, new int[]{0, 1, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_GYPSY_MINOR, new int[]{0, 2, 3, 6, 7, 8, 10});

        // Arabic Scales and Modes
        SCALE_PATTERNS.put(SCALE_ARABIC, new int[]{0, 2, 4, 5, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_HIJAZ, new int[]{0, 1, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_HIJAZ_KAR, new int[]{0, 1, 4, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_PHRYGIAN_DOMINANT, new int[]{0, 1, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_NAHAWAND, new int[]{0, 2, 3, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_NAHAWAND_MURASSAA, new int[]{0, 2, 3, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_NIKRIZ, new int[]{0, 1, 4, 5, 6, 9, 10});
        SCALE_PATTERNS.put(SCALE_SABA, new int[]{0, 1, 4, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_SIKAH, new int[]{0, 3, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_SIKAH_BALADI, new int[]{0, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_RAST, new int[]{0, 2, 4, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_BAYATI, new int[]{0, 1, 4, 5, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_HUZAM, new int[]{0, 2, 3, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_KURD, new int[]{0, 2, 3, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_SABA_ZAMZAM, new int[]{0, 1, 4, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_SUZNAK, new int[]{0, 1, 4, 5, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_NAWA_ATHAR, new int[]{0, 1, 4, 6, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_ATHAR_KURD, new int[]{0, 2, 3, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_HIJAZ_KAR_KURD, new int[]{0, 1, 4, 5, 7, 8, 10});

        // Exotic and Related Scales
        SCALE_PATTERNS.put(SCALE_KURDISH, new int[]{0, 2, 3, 5, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_RAGA_TODI, new int[]{0, 1, 3, 6, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_ORIENTAL, new int[]{0, 1, 4, 5, 6, 9, 10});
        SCALE_PATTERNS.put(SCALE_JEWISH, new int[]{0, 1, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_UKRAINIAN_DORIAN, new int[]{0, 2, 3, 6, 7, 9, 10});

        // Missing Modes (Church Modes and Variations)
        SCALE_PATTERNS.put(SCALE_SUPER_LOCRIAN, new int[]{0, 1, 3, 4, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_LYDIAN_AUGMENTED, new int[]{0, 2, 4, 6, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_MIXOLYDIAN_FLAT_6, new int[]{0, 2, 4, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_LOCRIAN_NATURAL_6, new int[]{0, 1, 3, 5, 6, 9, 10});

        // Jazz and Blues Scales
        SCALE_PATTERNS.put(SCALE_HALF_WHOLE_DIMINISHED, new int[]{0, 1, 3, 4, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_WHOLE_HALF_DIMINISHED, new int[]{0, 2, 3, 5, 6, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_CHROMATIC, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_MAJOR_BLUES, new int[]{0, 2, 3, 4, 7, 9});
        SCALE_PATTERNS.put(SCALE_MINOR_BLUES, new int[]{0, 3, 5, 6, 7, 10});
        SCALE_PATTERNS.put(SCALE_TRITONE, new int[]{0, 1, 4, 6, 7, 10});
        SCALE_PATTERNS.put(SCALE_SPANISH_PHRYGIAN, new int[]{0, 1, 4, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_DORIAN_FLAT_2, new int[]{0, 1, 3, 5, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_LYDIAN_SHARP_2, new int[]{0, 3, 4, 6, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_SUPER_LOCRIAN_FLAT_FLAT_7, new int[]{0, 1, 3, 4, 6, 8, 9});

        // Augmented Scale Variations
        SCALE_PATTERNS.put(SCALE_AUGMENTED_LYDIAN, new int[]{0, 2, 4, 6, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_IONIAN, new int[]{0, 2, 4, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_DORIAN, new int[]{0, 2, 3, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_PHRYGIAN, new int[]{0, 1, 3, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_MIXOLYDIAN, new int[]{0, 2, 4, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_AEOLIAN, new int[]{0, 2, 3, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_LOCRIAN, new int[]{0, 1, 3, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_HARMONIC_MINOR, new int[]{0, 2, 3, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_MELODIC_MINOR, new int[]{0, 2, 3, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_DORIAN_FLAT_2, new int[]{0, 1, 3, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_MIXOLYDIAN_FLAT_6, new int[]{0, 2, 4, 5, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_AUGMENTED_LOCRIAN_FLAT_7, new int[]{0, 1, 3, 5, 8, 9, 11});

        // Bebop Scales
        SCALE_PATTERNS.put(SCALE_BEBOP, new int[]{0, 2, 4, 5, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_MAJOR, new int[]{0, 2, 4, 5, 7, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_MINOR, new int[]{0, 2, 3, 5, 7, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_BEBOP_DOMINANT, new int[]{0, 2, 4, 5, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_DORIAN, new int[]{0, 2, 3, 5, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_PHRYGIAN, new int[]{0, 1, 3, 5, 7, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_LYDIAN, new int[]{0, 2, 4, 6, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_LOCRIAN, new int[]{0, 1, 3, 5, 6, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_HARMONIC_MINOR, new int[]{0, 2, 3, 5, 7, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_MELODIC_MINOR, new int[]{0, 2, 3, 5, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_DORIAN_FLAT_2, new int[]{0, 1, 3, 5, 7, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_MIXOLYDIAN_FLAT_6, new int[]{0, 2, 4, 5, 7, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_BEBOP_LOCRIAN_FLAT_7, new int[]{0, 1, 3, 5, 6, 8, 10, 11});

        // Hexatonic Scales
        SCALE_PATTERNS.put(SCALE_AUGMENTED_HEXATONIC, new int[]{0, 3, 4, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_PROMETHEUS, new int[]{0, 2, 4, 6, 9, 10});
        SCALE_PATTERNS.put(SCALE_TRITONE, new int[]{0, 1, 4, 6, 7, 10});
        SCALE_PATTERNS.put(SCALE_RAGA_MEGHA, new int[]{0, 1, 3, 6, 7, 10});

        // Heptatonic (Exotic Scales)
        SCALE_PATTERNS.put(SCALE_BALINESE, new int[]{0, 1, 3, 7, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_JAVANESE, new int[]{0, 1, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_JAPANESE, new int[]{0, 1, 5, 7, 8, 9, 10});
        SCALE_PATTERNS.put(SCALE_ROMANIAN_MINOR, new int[]{0, 2, 3, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_NEAPOLITAN_MAJOR, new int[]{0, 1, 3, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_NEAPOLITAN_MINOR, new int[]{0, 1, 3, 5, 7, 8, 11});
        SCALE_PATTERNS.put(SCALE_ENIGMATIC, new int[]{0, 1, 4, 6, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_EIGHT_TONE_SPANISH, new int[]{0, 1, 3, 4, 5, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_LEADING_WHOLE_TONE, new int[]{0, 2, 4, 6, 8, 10, 11});
        SCALE_PATTERNS.put(SCALE_LYDIAN_MINOR, new int[]{0, 2, 4, 6, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_ACOUSTIC, new int[]{0, 2, 4, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_LOCRIAN_MAJOR, new int[]{0, 2, 4, 5, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_IONIAN_AUGMENTED, new int[]{0, 2, 4, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_DORIAN_SHARP_4, new int[]{0, 2, 3, 6, 7, 9, 10});
        SCALE_PATTERNS.put(SCALE_AEOLIAN_FLAT_5, new int[]{0, 2, 3, 5, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_ALTERED_DOMINANT, new int[]{0, 1, 3, 4, 6, 8, 10});
        SCALE_PATTERNS.put(SCALE_LOCRIAN_SHARP_6, new int[]{0, 1, 3, 5, 6, 9, 10});
        SCALE_PATTERNS.put(SCALE_IONIAN_SHARP_5, new int[]{0, 2, 4, 5, 8, 9, 11});
        SCALE_PATTERNS.put(SCALE_LYDIAN_FLAT_3, new int[]{0, 2, 3, 6, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_MIXOLYDIAN_FLAT_9, new int[]{0, 2, 4, 5, 7, 8, 10});

        // Pentatonic Variants
        SCALE_PATTERNS.put(SCALE_EGYPTIAN, new int[]{0, 2, 5, 7, 10});
        SCALE_PATTERNS.put(SCALE_IWATO, new int[]{0, 1, 5, 6, 10});
        SCALE_PATTERNS.put(SCALE_MAN_GONG, new int[]{0, 2, 3, 7, 9});

        // Microtonal and Other Exotic Scales
        SCALE_PATTERNS.put(SCALE_QUARTER_TONE, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});
        SCALE_PATTERNS.put(SCALE_ARABIAN_MAQAM_RAST, new int[]{0, 2, 4, 5, 7, 9, 11});
        SCALE_PATTERNS.put(SCALE_ARABIAN_MAQAM_BAYATI, new int[]{0, 2, 3, 5, 7, 8, 10});
        SCALE_PATTERNS.put(SCALE_SLENDRO, new int[]{0, 2, 4, 7, 9});
        SCALE_PATTERNS.put(SCALE_ISTRIAN, new int[]{0, 1, 3, 7, 9});
        SCALE_PATTERNS.put(SCALE_BALINESE_PELOG, new int[]{0, 1, 3, 7, 8});
    }

    /**
     * Get an array of all available scale names
     *
     * @return Array of scale names
     */
    public static String[] getScales() {
        // Convert the keyset to a sorted array for better UI presentation
        return SCALE_PATTERNS.keySet().stream()
                .sorted()
                .toArray(String[]::new);
    }

    /**
     * Get index for root note name
     *
     * @param rootNote The root note name (C, C#, D, etc.)
     * @return The index (0-11) of the root note
     */
    public static int getRootNoteIndex(String rootNote) {
        return switch (rootNote) {
            case "C" ->
                0;
            case "C#" ->
                1;
            case "D" ->
                2;
            case "D#" ->
                3;
            case "E" ->
                4;
            case "F" ->
                5;
            case "F#" ->
                6;
            case "G" ->
                7;
            case "G#" ->
                8;
            case "A" ->
                9;
            case "A#" ->
                10;
            case "B" ->
                11;
            default ->
                0; // Default to C
        };
    }

    /**
     * Get the Boolean array for a scale with specified root and scale name
     * @param rootNoteName The root note (C, C#, etc.)
     * @param scaleName The scale name (Major, Minor, etc.)
     * @return Boolean array of 12 scale notes
     * @throws IllegalArgumentException if root or scale name is invalid
     */
    public static Boolean[] getScale(String rootNoteName, String scaleName) {
        // Validate parameters to prevent exceptions
        if (rootNoteName == null || scaleName == null) {
            logger.warn("Null parameters passed to getScale: root={}, scale={}", rootNoteName, scaleName);
            return getDefaultScale(); // Return chromatic scale as fallback
        }

        // Normalize inputs to help with matching
        String normalizedRoot = rootNoteName.trim();
        String normalizedScale = scaleName.trim();
        
        // Check if root note is valid
        int rootOffset = getRootOffset(normalizedRoot);
        if (rootOffset < 0) {
            logger.warn("Invalid root note name: '{}'", normalizedRoot);
            return getDefaultScale(); // Return chromatic scale as fallback
        }
        
        // Check if scale name is valid 
        int[] pattern = getScalePattern(normalizedScale);
        if (pattern == null) {
            logger.warn("Invalid scale name: '{}'", normalizedScale);
            return getDefaultScale(); // Return chromatic scale as fallback
        }
        
        // Continue with existing scale creation logic...
        Boolean[] scale = new Boolean[12];
        Arrays.fill(scale, Boolean.FALSE);
        
        // Set scale notes based on pattern
        for (int i : pattern) {
            scale[(i + rootOffset) % 12] = Boolean.TRUE;
        }
        
        return scale;
    }

    /**
     * Get a default chromatic scale as fallback
     */
    private static Boolean[] getDefaultScale() {
        Boolean[] scale = new Boolean[12];
        Arrays.fill(scale, Boolean.TRUE); // All notes
        return scale;
    }

    // Find the index of the root note in SCALE_NOTES
    private static int findRootIndex(String key) {
        for (int i = 0; i < SCALE_NOTES.length; i++) {
            if (SCALE_NOTES[i].contains(key)) {
                return i;
            }
        }
        return -1; // Key not found
    }

    // For demonstration purposes
    public static void printScale(Boolean[] scale) {
        List<String> notes = new ArrayList<>();
        for (int i = 0; i < SCALE_NOTES.length; i++) {
            if (scale[i]) {
                notes.add(SCALE_NOTES[i]);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("{}", notes);
    }

    public static void main(String[] args) {
        // Example usage
        String key = "C";
        String scaleName = "Major";

        Boolean[] scale = getScale(key, scaleName);
    if (logger.isDebugEnabled()) logger.debug("Scale for key {} in {}:", key, scaleName);
        printScale(scale);

        // Try another scale
        key = "A";
        scaleName = "Natural Minor";

        scale = getScale(key, scaleName);
    if (logger.isDebugEnabled()) logger.debug("\nScale for key {} in {}:", key, scaleName);
        printScale(scale);
    }

    public static String getNoteNameWithOctave(int midiNote) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = midiNote / 12 - 1; // MIDI octaves start at -1
        int noteIndex = midiNote % 12;
        return noteNames[noteIndex] + octave;
    }

    public static int getRootOffset(String rootNote) {
        switch (rootNote) {
            case "C":
                return 0;
            case "C#":
            case "Db":
                return 1;
            case "D":
                return 2;
            case "D#":
            case "Eb":
                return 3;
            case "E":
                return 4;
            case "F":
                return 5;
            case "F#":
            case "Gb":
                return 6;
            case "G":
                return 7;
            case "G#":
            case "Ab":
                return 8;
            case "A":
                return 9;
            case "A#":
            case "Bb":
                return 10;
            case "B":
                return 11;
            default:
                return 0;
        }
    }

    /**
     * Returns the octave for a given MIDI note number.
     *
     * @param midiNote The MIDI note number (0-127)
     * @return The octave number (e.g., 4 for middle C)
     */
    public static int getOctave(int midiNote) {
        // MIDI octaves follow the convention where middle C (MIDI note 60) is C4
        return (midiNote / 12) - 1;
    }

    /**
     * Returns the MIDI note number for a given note name and octave.
     *
     * @param noteName The name of the note (e.g., "C", "C#", "Db", etc.)
     * @param octave The octave number
     * @return The corresponding MIDI note number
     */
    public static int getMidiNote(String noteName, int octave) {
        int noteOffset = getRootOffset(noteName);

        // Calculate the MIDI note number
        // The formula is: (octave + 1) * 12 + noteOffset
        return (octave + 1) * 12 + noteOffset;
    }

    /**
     * Get the scale pattern for a given scale name.
     *
     * @param scaleName The name of the scale
     * @return The scale pattern as an array of integers, or null if not found
     */
    static int[] getScalePattern(String scaleName) {
        return SCALE_PATTERNS.get(scaleName);
    }
}
