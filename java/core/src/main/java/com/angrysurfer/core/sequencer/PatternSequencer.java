package com.angrysurfer.core.sequencer;

import com.angrysurfer.core.service.SequencerService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PatternSequencer {
    private static final Logger logger = LoggerFactory.getLogger(PatternSequencer.class);
    final List<MelodicSequencer> melodicSequencers = new ArrayList<>();
    // Track pattern lists
    final List<PatternSlot> drumPatternSlots = new ArrayList<>();
    final Map<Integer, List<PatternSlot>> melodicPatternSlots = new HashMap<>();
    private final Map<String, Boolean> activeSequencers = new HashMap<>();
    // Sequencers
    private DrumSequencer drumSequencer;

    public PatternSequencer() {
        // Initialize sequencers
        initializeSequencers();
    }

    private void initializeSequencers() {
        // Get drum sequencer from service
        drumSequencer = SequencerService.getInstance().getDrumSequencer(0);

        // Get melodic sequencers
        List<MelodicSequencer> allMelodicSequencers = SequencerService.getInstance().getAllMelodicSequencers();
        for (MelodicSequencer sequencer : allMelodicSequencers) {
            if (sequencer != null) {
                melodicSequencers.add(sequencer);
                // Initialize empty pattern slot list for this sequencer
                melodicPatternSlots.put(sequencer.getId(), new ArrayList<>());
            }
        }
    }

    /**
     * Initialize all sequencers for song mode - disable looping
     */
    public void initializeSequencersForSongMode() {
        // Disable looping in all sequencers when starting song mode
        if (drumSequencer != null) {
            drumSequencer.setLooping(false);
            logger.debug("Initialized drum sequencer: looping disabled");
        }

        // Initialize melodic sequencers
        for (MelodicSequencer sequencer : melodicSequencers) {
            sequencer.getSequenceData().setLooping(true);
            logger.debug("Initialized melodic sequencer {}: looping disabled", sequencer.getId());
        }

        // Clear active tracking
        activeSequencers.clear();
    }

    /**
     * Handle a bar update from the timing system
     *
     * @param bar The current bar (1-based)
     */
    public void handleBarUpdate(int bar) {
        // The currentBar from timing updates is 1-based (starts at 1)
        // But our pattern positions are 0-based (start at 0)
        int zeroBasedCurrentBar = bar - 1;
        logger.debug("Handling bar update: bar={}, zeroBasedBar={}", bar, zeroBasedCurrentBar);

        // Check if we're entering or exiting pattern slots for each sequencer
        updateSequencerLoopingState(zeroBasedCurrentBar);

        // Look ahead to the next bar (still using 0-based index)
        int nextBar = zeroBasedCurrentBar + 1;
        logger.debug("Looking ahead to next bar: {}", nextBar + 1);

        // Check for drum pattern change
        PatternSlot nextDrumSlot = findSlotAtPosition(drumPatternSlots, nextBar);
        if (nextDrumSlot != null && drumSequencer != null) {
            drumSequencer.setNextPatternId(nextDrumSlot.getPatternId());
            logger.debug("Queuing drum pattern {} for bar {}",
                    nextDrumSlot.getPatternId(), nextBar + 1);
        }

        // Check for melodic pattern changes
        for (MelodicSequencer sequencer : melodicSequencers) {
            List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
            if (slots != null) {
                PatternSlot nextSlot = findSlotAtPosition(slots, nextBar);
                if (nextSlot != null) {
                    sequencer.setNextPatternId(nextSlot.getPatternId());
                    logger.debug("Queuing melodic pattern {} for sequencer {} at bar {}",
                            nextSlot.getPatternId(), sequencer.getId(), nextBar + 1);
                }
            }
        }
    }

    /**
     * Update the looping state of all sequencers based on pattern slots
     */
    private void updateSequencerLoopingState(int currentBar) {
        // Handle drum sequencer
        updateSequencerLooping("drum", drumSequencer, drumPatternSlots, currentBar);

        // Handle melodic sequencers
        for (MelodicSequencer sequencer : melodicSequencers) {
            List<PatternSlot> slots = melodicPatternSlots.get(sequencer.getId());
            if (slots != null) {
                updateSequencerLooping("melodic-" + sequencer.getId(), sequencer, slots, currentBar);
            }
        }
    }

    /**
     * Update looping state for a specific sequencer
     */
    private void updateSequencerLooping(String sequencerId, Object sequencer,
                                        List<PatternSlot> slots, int currentBar) {
        // Check if we're in a pattern slot
        PatternSlot currentSlot = findSlotAtPosition(slots, currentBar);
        boolean wasActive = activeSequencers.getOrDefault(sequencerId, false);

        if (currentSlot != null) {
            // We're in a pattern slot
            if (!wasActive) {
                // Just entered a pattern slot - enable looping
                setSequencerLooping(sequencer, true);
                activeSequencers.put(sequencerId, true);
                logger.debug("Enabling looping for {} at bar {} (pattern {})",
                        sequencerId, currentBar + 1, currentSlot.getPatternId());
            }

            // Check if we're at the last bar of this slot
            int slotEndPos = currentSlot.getPosition() + currentSlot.getLength() - 1;
            if (currentBar == slotEndPos) {
                // Check if there's another slot immediately after this one
                PatternSlot nextSlot = findSlotAtPosition(slots, currentBar + 1);
                if (nextSlot == null) {
                    // No slot follows - disable looping at the end of this bar
                    setSequencerLooping(sequencer, false);
                    activeSequencers.put(sequencerId, false);
                    logger.debug("Disabling looping for {} after bar {} (end of pattern {})",
                            sequencerId, currentBar + 1, currentSlot.getPatternId());
                }
            }
        } else if (wasActive) {
            // We just exited a pattern slot without entering a new one - disable looping
            setSequencerLooping(sequencer, false);
            activeSequencers.put(sequencerId, false);
            logger.debug("Disabling looping for {} at bar {} (exited pattern)",
                    sequencerId, currentBar + 1);
        }
    }

    /**
     * Helper method to set looping state based on sequencer type
     */
    private void setSequencerLooping(Object sequencer, boolean looping) {
        if (sequencer instanceof DrumSequencer) {
            ((DrumSequencer) sequencer).setLooping(looping);
        } else if (sequencer instanceof MelodicSequencer) {
            ((MelodicSequencer) sequencer).getSequenceData().setLooping(looping);
        }
    }

    /**
     * Find a pattern slot at a specific position
     */
    private PatternSlot findSlotAtPosition(List<PatternSlot> slots, int position) {
        for (PatternSlot slot : slots) {
            if (position >= slot.getPosition() && position < slot.getPosition() + slot.getLength()) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Clear all queued next patterns from sequencers and reset looping state
     */
    public void clearAllQueuedPatterns() {
        // Clear drum sequencer
        if (drumSequencer != null) {
            drumSequencer.setNextPatternId(null);
            drumSequencer.setLooping(true); // Reset to normal behavior when song mode is off
        }

        // Clear all melodic sequencers
        for (MelodicSequencer sequencer : melodicSequencers) {
            if (sequencer != null) {
                sequencer.setNextPatternId(null);
                sequencer.getSequenceData().setLooping(true); // Reset to normal behavior when song mode is off
            }
        }

        // Clear active tracking
        activeSequencers.clear();

        logger.info("Cleared all queued patterns and reset sequencer looping states");
    }
}