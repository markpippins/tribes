package com.angrysurfer.core.sequencer;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class MelodicSequenceData {
    // Constants
    public static final int MAX_STEPS = 16;
    public static final int MAX_BARS = 16;
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceData.class);
    // Id and metadata
    private Long id = 0L;
    private Long playerId;

    private String name = "New Pattern";
    private Long createdAt = System.currentTimeMillis();
    private Long updatedAt = System.currentTimeMillis();

    // Sequencer settings
    private Integer swing = 50;
    private Boolean swingEnabled = false;
    private Integer patternLength = 16;
    private Boolean looping = true;
    private Direction direction = Direction.FORWARD;
    private Integer shuffleAmount = 0;
    private Boolean quantizeEnabled = true;
    private Integer rootNote = 0; // C
    private String scale = "Major";
    private Integer octaveShift = 0;
    private TimingDivision timingDivision = TimingDivision.SIXTEENTH;
    private Integer sequencerId; // Add this field to store the sequencer ID

    private Integer followNoteSequencerId = -1;
    private Integer followTiltSequencerId = -1;

    // Sound-related settings
    private String soundbankName = SequencerConstants.DEFAULT_SOUNDBANK;
    private Integer bankIndex = 0;
    private Integer preset = 0;
    private String deviceName;
    private Long instrumentId;
    private String instrumentName;

    // Pattern data
    private boolean[] activeSteps = new boolean[MAX_STEPS];
    private int[] noteValues = new int[MAX_STEPS];
    private int[] velocityValues = new int[MAX_STEPS];
    private int[] gateValues = new int[MAX_STEPS];
    private int[] probabilityValues = new int[MAX_STEPS];
    private int[] nudgeValues = new int[MAX_STEPS];
    private int[] tiltValues = new int[MAX_BARS];
    private int[] muteValues = new int[MAX_BARS];
    private int[] lengthModifierValues = new int[MAX_BARS];
    private int[] muteBarsValues = new int[MAX_BARS];

    // private int[] mutesBarCountValues = new int[MAX_BARS];

    /**
     * Default constructor
     */
    public MelodicSequenceData() {
        initializeArrays();
    }

    /**
     * Initialize arrays with default values
     */
    private void initializeArrays() {
        // Initialize all arrays with default values
        Arrays.fill(activeSteps, false);
        Arrays.fill(noteValues, SequencerConstants.DEFAULT_NOTE);
        Arrays.fill(velocityValues, SequencerConstants.DEFAULT_VELOCITY);
        Arrays.fill(gateValues, SequencerConstants.DEFAULT_GATE);
        Arrays.fill(probabilityValues, SequencerConstants.DEFAULT_PROBABILITY);
        Arrays.fill(nudgeValues, 0);
        Arrays.fill(tiltValues, 0);
        Arrays.fill(muteValues, 0); // Initialize mute values to 0 (unmuted)
        Arrays.fill(lengthModifierValues, 0); // Initialize mute values to 0 (unmuted)
        Arrays.fill(muteBarsValues, 1); // Initialize mute values to 0 (unmuted)
        // Arrays.fill(mutesBarCountValues, 0); // Initialize mute values to 0 (unmuted)
    }

    /**
     * Get maximum steps in pattern
     */
    public int getMaxSteps() {
        return MAX_STEPS;
    }

    /**
     * Check if step is active
     */
    public boolean isStepActive(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return activeSteps[step];
        }
        return false;
    }

    /**
     * Set step active state
     */
    public void setStepActive(int step, boolean active) {
        if (step >= 0 && step < MAX_STEPS) {
            activeSteps[step] = active;
        } else {
            logger.warn("Attempted to set active state for invalid step: {}", step);
        }
    }

    /**
     * Get note value for step
     */
    public int getNoteValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return noteValues[step];
        }
        return SequencerConstants.DEFAULT_NOTE;
    }

    /**
     * Set note value for step
     */
    public void setNoteValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            // Ensure valid MIDI note range (0-127)
            value = Math.max(0, Math.min(127, value));
            noteValues[step] = value;
        } else {
            logger.warn("Attempted to set note value for invalid step: {}", step);
        }
    }

    /**
     * Get velocity value for step
     */
    public int getVelocityValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return velocityValues[step];
        }
        return SequencerConstants.DEFAULT_VELOCITY;
    }

    /**
     * Set velocity value for step
     */
    public void setVelocityValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            // Ensure valid MIDI velocity range (0-127)
            value = Math.max(0, Math.min(127, value));
            velocityValues[step] = value;
        } else {
            logger.warn("Attempted to set velocity value for invalid step: {}", step);
        }
    }

    /**
     * Get gate value for step
     */
    public int getGateValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return gateValues[step];
        }
        return SequencerConstants.DEFAULT_GATE;
    }

    /**
     * Set gate value for step
     */
    public void setGateValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            // Ensure valid gate range (0-100)
            value = Math.max(0, Math.min(100, value));
            gateValues[step] = value;
        } else {
            logger.warn("Attempted to set gate value for invalid step: {}", step);
        }
    }

    /**
     * Get probability value for step
     */
    public int getProbabilityValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return probabilityValues[step];
        }
        return SequencerConstants.DEFAULT_PROBABILITY;
    }

    /**
     * Set probability value for step
     */
    public void setProbabilityValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            // Ensure valid probability range (0-100)
            value = Math.max(0, Math.min(100, value));
            probabilityValues[step] = value;
        } else {
            logger.warn("Attempted to set probability value for invalid step: {}", step);
        }
    }

    /**
     * Get nudge value for step
     */
    public int getNudgeValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return nudgeValues[step];
        }
        return 0;
    }

    /**
     * Set nudge value for step
     */
    public void setNudgeValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            nudgeValues[step] = value;
        } else {
            logger.warn("Attempted to set nudge value for invalid step: {}", step);
        }
    }

    /**
     * Get tilt value for step
     */
    public int getTiltValue(int step) {
        if (step >= 0 && step < MAX_STEPS) {
            return tiltValues[step];
        }
        return 0;
    }

    /**
     * Set tilt value for step
     */
    public void setTiltValue(int step, int value) {
        if (step >= 0 && step < MAX_STEPS) {
            // Tilt values are typically small integers (-12 to +12)
            tiltValues[step] = value;
        } else {
            logger.warn("Attempted to set tilt value for invalid step: {}", step);
        }
    }

    /**
     * Get raw harmonic tilt values array
     */
    public int[] getHarmonicTiltValuesRaw() {
        return tiltValues;
    }

    /**
     * Get harmonic tilt values as a list
     *
     * @return List of harmonic tilt values
     */
    public List<Integer> getHarmonicTiltValues() {
        return Arrays.stream(tiltValues).boxed().collect(Collectors.toList());
    }

    /**
     * Set harmonic tilt values from array
     */
    public void setHarmonicTiltValues(int[] values) {
        if (values != null) {
            int copyLength = Math.min(values.length, tiltValues.length);
            System.arraycopy(values, 0, tiltValues, 0, copyLength);
        } else {
            logger.warn("Attempted to set null tilt values array");
        }
    }

    /**
     * Convert array to List of Boolean
     */
    public List<Boolean> getActiveSteps() {
        List<Boolean> result = new ArrayList<>(MAX_STEPS);
        for (boolean b : activeSteps) {
            result.add(b);
        }
        return result;
    }

    /**
     * Get pattern length
     */
    public int getPatternLength() {
        return patternLength;
    }

    /**
     * Set pattern length
     */
    public void setPatternLength(int length) {
        if (length > 0 && length <= MAX_STEPS) {
            patternLength = length;
        } else {
            logger.warn("Invalid pattern length: {}, must be between 1 and {}", length, MAX_STEPS);
        }
    }

    /**
     * Get swing percentage
     */
    public int getSwingPercentage() {
        return swing;
    }

    /**
     * Set swing percentage
     */
    public void setSwingPercentage(int percentage) {
        if (percentage >= 50 && percentage <= 99) {
            swing = percentage;
        } else {
            logger.warn("Invalid swing percentage: {}, must be between 50 and 99", percentage);
        }
    }

    /**
     * Check if swing is enabled
     */
    public boolean isSwingEnabled() {
        return swingEnabled;
    }

    /**
     * Set swing enabled flag
     */
    public void setSwingEnabled(boolean enabled) {
        swingEnabled = enabled;
    }

    /**
     * Check if looping is enabled
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Set looping flag
     */
    public void setLooping(boolean loop) {
        looping = loop;
    }

    /**
     * Check if quantization is enabled
     */
    public boolean isQuantizeEnabled() {
        return quantizeEnabled;
    }

    /**
     * Set quantization flag
     */
    public void setQuantizeEnabled(boolean enabled) {
        quantizeEnabled = enabled;
    }


    /**
     * Set the root note from a string value
     *
     * @param rootNoteStr The root note as a string (e.g., "C", "F#")
     */
    public void setRootNoteFromString(String rootNoteStr) {
        if (rootNoteStr == null || rootNoteStr.isEmpty()) {
            this.rootNote = 0; // Default to C
            return;
        }

        // Map from note name to integer value
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

        for (int i = 0; i < noteNames.length; i++) {
            if (noteNames[i].equalsIgnoreCase(rootNoteStr)) {
                this.rootNote = i;
                return;
            }
        }

        // Also check for flat notes
        String[] flatNoteNames = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

        for (int i = 0; i < flatNoteNames.length; i++) {
            if (flatNoteNames[i].equalsIgnoreCase(rootNoteStr)) {
                this.rootNote = i;
                return;
            }
        }

        // If not found, default to C
        logger.warn("Unknown root note: '{}', defaulting to C", rootNoteStr);
        this.rootNote = 0;
    }

    /**
     * Get the root note as a string
     *
     * @return The root note name
     */
    public String getRootNoteAsString() {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int index = (rootNote != null) ? (rootNote % 12) : 0;
        if (index < 0)
            index += 12;
        return noteNames[index];
    }

    /**
     * Create array of scale notes
     */
    public Boolean[] createScaleArray(Integer rootNote, String scaleName) {
        Boolean[] scaleNotes = new Boolean[12];
        Arrays.fill(scaleNotes, Boolean.FALSE);

        // Use Integer directly, no parsing needed
        int rootNoteInt = (rootNote != null) ? rootNote : 0;

        // Define scale patterns (semitone intervals)
        int[] intervals = switch (scaleName) {
            case "Major" -> new int[]{0, 2, 4, 5, 7, 9, 11};
            case "Minor" -> new int[]{0, 2, 3, 5, 7, 8, 10};
            case "Harmonic Minor" -> new int[]{0, 2, 3, 5, 7, 8, 11};
            case "Melodic Minor" -> new int[]{0, 2, 3, 5, 7, 9, 11};
            case "Dorian" -> new int[]{0, 2, 3, 5, 7, 9, 10};
            case "Phrygian" -> new int[]{0, 1, 3, 5, 7, 8, 10};
            case "Lydian" -> new int[]{0, 2, 4, 6, 7, 9, 11};
            case "Mixolydian" -> new int[]{0, 2, 4, 5, 7, 9, 10};
            case "Locrian" -> new int[]{0, 1, 3, 5, 6, 8, 10};
            case "Pentatonic Major" -> new int[]{0, 2, 4, 7, 9};
            case "Pentatonic Minor" -> new int[]{0, 3, 5, 7, 10};
            case "Blues" -> new int[]{0, 3, 5, 6, 7, 10};
            case "Chromatic" -> new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
            default -> {
                logger.warn("Unknown scale: {}, defaulting to Major", scaleName);
                yield new int[]{0, 2, 4, 5, 7, 9, 11};
            }
        };

        // Mark scale notes as true
        for (int interval : intervals) {
            int note = (rootNoteInt + interval) % 12;
            scaleNotes[note] = Boolean.TRUE;
        }

        return scaleNotes;
    }

    /**
     * Clear the pattern
     */
    public void clearPattern() {
        Arrays.fill(activeSteps, false);
        Arrays.fill(noteValues, SequencerConstants.DEFAULT_NOTE);
        Arrays.fill(velocityValues, SequencerConstants.DEFAULT_VELOCITY);
        Arrays.fill(gateValues, SequencerConstants.DEFAULT_GATE);
        Arrays.fill(probabilityValues, SequencerConstants.DEFAULT_PROBABILITY);
        Arrays.fill(nudgeValues, 0);
    }

    /**
     * Rotate pattern right
     */
    public void rotatePatternRight() {
        // Rotate active steps
        boolean lastStep = activeSteps[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            activeSteps[i] = activeSteps[i - 1];
        }
        activeSteps[0] = lastStep;

        // Rotate note values
        int lastNote = noteValues[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            noteValues[i] = noteValues[i - 1];
        }
        noteValues[0] = lastNote;

        // Rotate velocity values
        int lastVelocity = velocityValues[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            velocityValues[i] = velocityValues[i - 1];
        }
        velocityValues[0] = lastVelocity;

        // Rotate gate values
        int lastGate = gateValues[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            gateValues[i] = gateValues[i - 1];
        }
        gateValues[0] = lastGate;

        // Rotate probability values
        int lastProb = probabilityValues[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            probabilityValues[i] = probabilityValues[i - 1];
        }
        probabilityValues[0] = lastProb;

        // Rotate nudge values
        int lastNudge = nudgeValues[MAX_STEPS - 1];
        for (int i = MAX_STEPS - 1; i > 0; i--) {
            nudgeValues[i] = nudgeValues[i - 1];
        }
        nudgeValues[0] = lastNudge;
    }

    /**
     * Rotate pattern left
     */
    public void rotatePatternLeft() {
        // Rotate active steps
        boolean firstStep = activeSteps[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            activeSteps[i] = activeSteps[i + 1];
        }
        activeSteps[MAX_STEPS - 1] = firstStep;

        // Rotate note values
        int firstNote = noteValues[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            noteValues[i] = noteValues[i + 1];
        }
        noteValues[MAX_STEPS - 1] = firstNote;

        // Rotate velocity values
        int firstVelocity = velocityValues[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            velocityValues[i] = velocityValues[i + 1];
        }
        velocityValues[MAX_STEPS - 1] = firstVelocity;

        // Rotate gate values
        int firstGate = gateValues[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            gateValues[i] = gateValues[i + 1];
        }
        gateValues[MAX_STEPS - 1] = firstGate;

        // Rotate probability values
        int firstProb = probabilityValues[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            probabilityValues[i] = probabilityValues[i + 1];
        }
        probabilityValues[MAX_STEPS - 1] = firstProb;

        // Rotate nudge values
        int firstNudge = nudgeValues[0];
        for (int i = 0; i < MAX_STEPS - 1; i++) {
            nudgeValues[i] = nudgeValues[i + 1];
        }
        nudgeValues[MAX_STEPS - 1] = firstNudge;
    }

    /**
     * Set step data
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        if (stepIndex >= 0 && stepIndex < MAX_STEPS) {
            setStepActive(stepIndex, active);
            setNoteValue(stepIndex, note);
            setVelocityValue(stepIndex, velocity);
            setGateValue(stepIndex, gate);
        }
    }

    /**
     * Set step data with probability and nudge
     */
    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability,
                            int nudge) {
        setStepData(stepIndex, active, note, velocity, gate);
        if (stepIndex >= 0 && stepIndex < MAX_STEPS) {
            setProbabilityValue(stepIndex, probability);
            setNudgeValue(stepIndex, nudge);
        }
    }

    /**
     * Get mute value for bar
     */
    public int getMuteValue(int bar) {
        if (bar >= 0 && bar < MAX_BARS) {
            return muteValues[bar];
        }
        return 0;
    }

    /**
     * Get length modifier value for bar
     */
    public int getLengthModifierValue(int bar) {
        if (bar >= 0 && bar < MAX_BARS) {
            return lengthModifierValues[bar];
        }
        return 0;
    }


    /**
     * Set mute value for step
     */
    public void setMuteValue(int step, int value) {
        if (step >= 0 && step < MAX_BARS) {
            // Mute values are typically 0 (unmuted) or 1 (muted)
            muteValues[step] = value;
        } else {
            logger.warn("Attempted to set mute value for invalid step: {}", step);
        }
    }

    /**
     * Get mute value for step
     */
    public int getMutesBarsValue(int step) {
        if (step >= 0 && step < MAX_BARS) {
            return muteBarsValues[step];
        }
        return 0;
    }

    /**
     * Set mute value for step
     */
    public void setMutesBarsValue(int step, int value) {
        if (step >= 0 && step < MAX_BARS) {
            // Mute values are typically 0 (unmuted) or 1 (muted)
            muteBarsValues[step] = value;
        } else {
            logger.warn("Attempted to set mute value for invalid step: {}", step);
        }
    }

    /**
     * Get mute value for step
     */
//    public int getMutesBarsCountValue(int step) {
//        if (step >= 0 && step < MAX_BARS) {
//            return mutesBarCountValues[step];
//        }
//        return 0;
//    }

    /**
     * Set mute value for step
     */
//    public void setMutesBarsCountValue(int step, int value) {
//        if (step >= 0 && step < MAX_BARS) {
//            // Mute values are typically 0 (unmuted) or 1 (muted)
//            mutesBarCountValues[step] = value;
//        } else {
//            logger.warn("Attempted to set mute value for invalid step: {}", step);
//        }
//    }

    /**
     * Get raw mute values array
     */
    public int[] getMuteValuesRaw() {
        return muteValues;
    }

    /**
     * Get mute values as a list
     *
     * @return List of mute values
     */
    public List<Integer> getMuteValues() {
        return Arrays.stream(muteValues).boxed().collect(Collectors.toList());
    }

    /**
     * Set mute values from array
     */
    public void setMuteValues(int[] values) {
        if (values != null) {
            int copyLength = Math.min(values.length, muteValues.length);
            System.arraycopy(values, 0, muteValues, 0, copyLength);
        } else {
            logger.warn("Attempted to set null mute values array");
        }
    }
}