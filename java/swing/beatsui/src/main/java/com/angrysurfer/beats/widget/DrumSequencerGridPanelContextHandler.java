package com.angrysurfer.beats.widget;

import com.angrysurfer.beats.panel.sequencer.poly.DrumSequencerGridPanel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Handler for context menu operations on the drum sequencer grid
 */
public class DrumSequencerGridPanelContextHandler implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridPanelContextHandler.class);

    // References to required components
    private final DrumSequencer sequencer;
    private final DrumSequencerGridPanel parentPanel;

    /**
     * Create a new context menu handler
     *
     * @param sequencer   The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSequencerGridPanelContextHandler(DrumSequencer sequencer, DrumSequencerGridPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;

        CommandBus.getInstance().register(this, new String[]{
                Commands.FILL_PATTERN_SELECTED,
                Commands.EUCLIDEAN_PATTERN_SELECTED,
                Commands.MAX_LENGTH_SELECTED
        });

        logger.debug("DrumSequencerGridPanelContextHandler registered for commands");
    }

    /**
     * Applies a pattern that activates every Nth step
     *
     * @param sequencer    The drum sequencer to modify
     * @param drumIndex    The index of the drum to update
     * @param stepInterval The interval between active steps (2 = every other step)
     * @return True if pattern was applied successfully
     */
    public static boolean applyPatternEveryN(DrumSequencer sequencer, int drumIndex, int stepInterval) {
        try {
            int patternLength = sequencer.getPatternLength(drumIndex);

            // Clear existing pattern first - this also resets all parameters
            DrumSequenceModifier.clearDrumTrack(sequencer, drumIndex);

            // Set every Nth step
            for (int i = 0; i < patternLength; i += stepInterval) {
                // Toggle the step to make it active
                sequencer.toggleStep(drumIndex, i);

                // Always set parameters for activated steps
                sequencer.setStepVelocity(drumIndex, i, SequencerConstants.DEFAULT_VELOCITY);
                sequencer.setStepDecay(drumIndex, i, SequencerConstants.DEFAULT_DECAY);
                sequencer.setStepProbability(drumIndex, i, SequencerConstants.DEFAULT_PROBABILITY);

                // Also set effects parameters
                sequencer.setStepPan(drumIndex, i, 64); // Center
                sequencer.setStepChorus(drumIndex, i, 0);
                sequencer.setStepReverb(drumIndex, i, 0);
            }

            // Notify UI of changes
            DrumSequenceModifier.notifyPatternChanged(sequencer, drumIndex);

            logger.info("Applied 1/{} pattern to drum {}", stepInterval, drumIndex);
            return true;
        } catch (Exception e) {
            logger.error("Error applying pattern every {} steps to drum {}", stepInterval, drumIndex, e);
            return false;
        }
    }

    /**
     * Show context menu for drum grid
     */
    public void showContextMenu(Component component, int x, int y, int drumIndex, int step) {
        JPopupMenu menu = new JPopupMenu();

        // ----- Basic Pattern Operations -----
        JMenuItem clearRowItem = new JMenuItem("Clear");
        clearRowItem.addActionListener(e -> parentPanel.clearRow(drumIndex));

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setEnabled(false); // To be implemented
        //copyItem.addActionListener(e -> copyPattern(drumIndex));

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setEnabled(false); // To be implemented
        //pasteItem.addActionListener(e -> pastePattern(drumIndex));

        menu.add(clearRowItem);
        menu.add(copyItem);
        menu.add(pasteItem);
        menu.addSeparator();

        // ----- Pattern Length -----
        JMenuItem doublePatternItem = new JMenuItem("Double");
        doublePatternItem.addActionListener(e -> doublePattern(drumIndex));
        menu.add(doublePatternItem);
        menu.addSeparator();

        // ----- Direction Submenu -----
        JMenu directionMenu = new JMenu("Direction");


        JMenuItem forwardItem = new JMenuItem("Forward");
        forwardItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.pushPatternForward(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem backwardItem = new JMenuItem("Backward");
        backwardItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.pullPatternBackward(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem bounceItem = new JMenuItem("Bounce");
        bounceItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.createBouncePattern(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem randomDirItem = new JMenuItem("Random");
        randomDirItem.addActionListener(e -> {
            int density = 50; // Default density of 50%
            boolean success = DrumSequenceModifier.generateRandomPattern(sequencer, drumIndex, density);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        directionMenu.add(forwardItem);
        directionMenu.add(backwardItem);
        directionMenu.add(bounceItem);
        directionMenu.add(randomDirItem);
        menu.add(directionMenu);
        menu.addSeparator();

        // ----- Fill Submenu -----
        JMenu fillMenu = new JMenu("Fill");

        JMenuItem fillAllItem = new JMenuItem("All");
        fillAllItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyFillPattern(sequencer, drumIndex, 0, "all");
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });        JMenuItem fill2ndItem = new JMenuItem("8th Notes");
        fill2ndItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, 2);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fill3rdItem = new JMenuItem("Triplets");
        fill3rdItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, 3);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fill4thItem = new JMenuItem("Quarter Notes");
        fill4thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, 4);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fill6thItem = new JMenuItem("Dotted 8ths");
        fill6thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, 6);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fill8thItem = new JMenuItem("Half Notes");
        fill8thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, 8);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillEuclideanItem = new JMenuItem("Euclidean");
        fillEuclideanItem.addActionListener(e -> {
            // Use CommandBus for dialog creation - Euclidean for entire row
            Object[] params = new Object[]{sequencer, drumIndex};
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });

        fillMenu.add(fillAllItem);
        fillMenu.add(fill2ndItem);
        fillMenu.add(fill3rdItem);
        fillMenu.add(fill4thItem);
        fillMenu.add(fill6thItem);
        fillMenu.add(fill8thItem);
        fillMenu.addSeparator();
        fillMenu.add(fillEuclideanItem);
        menu.add(fillMenu);

        // ----- Fill From Here Submenu -----
        JMenu fillFromHereMenu = new JMenu("Fill From Here...");

        JMenuItem fillFromHereAllItem = new JMenuItem("All");
        
        fillFromHereAllItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyFillPattern(sequencer, drumIndex, step, "all");
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });        JMenuItem fillFromHere2ndItem = new JMenuItem("8th Notes");
        fillFromHere2ndItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryNFromStep(sequencer, drumIndex, step, 2);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillFromHere3rdItem = new JMenuItem("Triplets");
        fillFromHere3rdItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryNFromStep(sequencer, drumIndex, step, 3);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillFromHere4thItem = new JMenuItem("Quarter Notes");
        fillFromHere4thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryNFromStep(sequencer, drumIndex, step, 4);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillFromHere6thItem = new JMenuItem("Dotted 8ths");
        fillFromHere6thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryNFromStep(sequencer, drumIndex, step, 6);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillFromHere8thItem = new JMenuItem("Half Notes");
        fillFromHere8thItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.applyPatternEveryNFromStep(sequencer, drumIndex, step, 8);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem fillFromHereEuclideanItem = new JMenuItem("Euclidean");
        fillFromHereEuclideanItem.addActionListener(e -> {
            // Use CommandBus for dialog creation - Euclidean from specific step
            Object[] params = new Object[]{sequencer, drumIndex, step};
            CommandBus.getInstance().publish(Commands.SHOW_EUCLIDEAN_DIALOG, this, params);
        });

        fillFromHereMenu.add(fillFromHereAllItem);
        fillFromHereMenu.add(fillFromHere2ndItem);
        fillFromHereMenu.add(fillFromHere3rdItem);
        fillFromHereMenu.add(fillFromHere4thItem);
        fillFromHereMenu.add(fillFromHere6thItem);
        fillFromHereMenu.add(fillFromHere8thItem);
        fillFromHereMenu.addSeparator();
        fillFromHereMenu.add(fillFromHereEuclideanItem);
        menu.add(fillFromHereMenu);

        // ----- Fill Params Submenu -----
        JMenu fillParamsMenu = new JMenu("Fill Params");

        JMenu nudgeMenu = new JMenu("Nudge");
        JMenuItem randomizeNudgeItem = new JMenuItem("Randomize");
        randomizeNudgeItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.randomizeNudgeValues(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });
        nudgeMenu.add(randomizeNudgeItem);

        JMenu probabilityMenu = new JMenu("Probability");
        JMenuItem randomizeProbItem = new JMenuItem("Randomize");
        randomizeProbItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.randomizeProbabilities(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });
        probabilityMenu.add(randomizeProbItem);

        JMenu velocityMenu = new JMenu("Velocity");
        JMenuItem ascendingVelItem = new JMenuItem("Ascending");
        ascendingVelItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.setAscendingVelocities(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem descendingVelItem = new JMenuItem("Descending");
        descendingVelItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.setDescendingVelocities(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem randomizeVelItem = new JMenuItem("Randomize");
        randomizeVelItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.randomizeVelocities(sequencer, drumIndex);
            if (success) {
                // Update grid buttons
                parentPanel.updateStepButtonsForDrum(drumIndex);

                // Force refresh of any parameter panels
                CommandBus.getInstance().publish(
                        Commands.DRUM_GRID_REFRESH_REQUESTED,
                        this,
                        drumIndex
                );
            }
        });


        velocityMenu.add(ascendingVelItem);
        velocityMenu.add(descendingVelItem);
        velocityMenu.add(randomizeVelItem);

        fillParamsMenu.add(nudgeMenu);
        fillParamsMenu.add(probabilityMenu);
        fillParamsMenu.add(velocityMenu);
        menu.add(fillParamsMenu);
        menu.addSeparator();

        // ----- Push/Pull Operations -----
        JMenuItem pullBackItem = new JMenuItem("Pull Back");
        pullBackItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.pullPatternBackward(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        JMenuItem pushForwardItem = new JMenuItem("Push Forward");
        pushForwardItem.addActionListener(e -> {
            boolean success = DrumSequenceModifier.pushPatternForward(sequencer, drumIndex);
            if (success) {
                parentPanel.updateStepButtonsForDrum(drumIndex);
            }
        });

        menu.add(pullBackItem);
        menu.add(pushForwardItem);

        // Show menu at the requested position
        menu.show(component, x, y);
    }    /**
     * Shows a dialog to choose pattern type
     *
     * @param drumIndex The drum index to apply the pattern to
     */
    private void showPatternDialog(int drumIndex) {
        Object[] options = {"8th Notes", "Triplets", "Quarter Notes"};
        int choice = JOptionPane.showOptionDialog(
                parentPanel,
                "Choose pattern type:",
                "Pattern Generator",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice >= 0) {
            applyPatternEveryN(drumIndex, 2 + choice);
        }
    }

    /**
     * Apply a pattern that activates every Nth step
     *
     * @param drumIndex The drum index to apply the pattern to
     * @param n         The step interval
     */
    private void applyPatternEveryN(int drumIndex, int n) {
        boolean success = DrumSequenceModifier.applyPatternEveryN(sequencer, drumIndex, n);
        if (success) {
            parentPanel.updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Copy the current sequence to a new sequence
     *
     * @param drumIndex The drum index to copy
     */
    private void copyToNewSequence(int drumIndex) {
        // Implementation for copying sequence to a new one
        logger.info("Copying sequence for drum index {} to a new sequence.", drumIndex);
    }

    /**
     * Double the current pattern for the given drum index
     *
     * @param drumIndex The drum index to double the pattern for
     */
    private void doublePattern(int drumIndex) {
        try {
            int currentLength = sequencer.getPatternLength(drumIndex);
            int maxLength = sequencer.getMaxPatternLength();

            // Safety check - make sure doubling is possible
            if (currentLength * 2 > maxLength) {
                JOptionPane.showMessageDialog(parentPanel,
                        "Cannot double pattern - maximum length would be exceeded.",
                        "Double Pattern",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Copy the pattern
            boolean[] activeSteps = new boolean[currentLength];
            int[] velocities = new int[currentLength];
            int[] decays = new int[currentLength];
            int[] probabilities = new int[currentLength];
            int[] nudges = new int[currentLength];

            // Get current values
            for (int i = 0; i < currentLength; i++) {
                activeSteps[i] = sequencer.isStepActive(drumIndex, i);
                velocities[i] = sequencer.getStepVelocity(drumIndex, i);
                decays[i] = sequencer.getStepDecay(drumIndex, i);
                probabilities[i] = sequencer.getStepProbability(drumIndex, i);
                nudges[i] = sequencer.getStepNudge(drumIndex, i);
            }

            // Double the pattern length
            int newLength = currentLength * 2;
            sequencer.setPatternLength(drumIndex, newLength);

            // Copy the pattern to the second half
            for (int i = 0; i < currentLength; i++) {
                // Set first half (should already be set, but to be safe)
                sequencer.isStepActive(drumIndex, i);
                sequencer.setStepVelocity(drumIndex, i, velocities[i]);
                sequencer.setStepDecay(drumIndex, i, decays[i]);
                sequencer.setStepProbability(drumIndex, i, probabilities[i]);
                sequencer.setStepNudge(drumIndex, i, nudges[i]);

                // Copy to second half
                int destIndex = i + currentLength;
                if (activeSteps[i]) {
                    sequencer.toggleStep(drumIndex, destIndex); // Set active if original was active
                }
                sequencer.setStepVelocity(drumIndex, destIndex, velocities[i]);
                sequencer.setStepDecay(drumIndex, destIndex, decays[i]);
                sequencer.setStepProbability(drumIndex, destIndex, probabilities[i]);
                sequencer.setStepNudge(drumIndex, destIndex, nudges[i]);
            }

            // Update UI
            CommandBus.getInstance().publish(
                    Commands.DRUM_SEQUENCE_UPDATED,
                    this,
                    null
            );

            // Log the action
            logger.info("Doubled pattern for drum {} from length {} to {}",
                    drumIndex, currentLength, newLength);
        } catch (Exception ex) {
            logger.error("Error doubling pattern: {}", ex.getMessage(), ex);
            JOptionPane.showMessageDialog(parentPanel,
                    "Error doubling pattern: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handles the action for commands.
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.FILL_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    int startStep = (Integer) result[1];
                    String fillType = (String) result[2];

                    // Call DrumSequenceModifier's method directly
                    boolean success = DrumSequenceModifier.applyFillPattern(
                            sequencer, drumIndex, startStep, fillType);

                    if (success) {
                        // Force UI update
                        parentPanel.updateStepButtonsForDrum(drumIndex);
                        logger.debug("Applied fill pattern {} from step {} for drum {}",
                                fillType, startStep, drumIndex);
                    }
                }
            }

            case Commands.EUCLIDEAN_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    boolean[] pattern = (boolean[]) result[1];

                    // Call DrumSequenceModifier's method directly
                    boolean success = DrumSequenceModifier.applyEuclideanPattern(
                            sequencer, drumIndex, pattern);

                    if (success) {
                        // Force UI update directly through parent panel
                        parentPanel.updateStepButtonsForDrum(drumIndex);
                        logger.info("Successfully applied Euclidean pattern to drum {}", drumIndex);
                    } else {
                        logger.error("Failed to apply Euclidean pattern to drum {}", drumIndex);
                    }
                }
            }

            case Commands.MAX_LENGTH_SELECTED -> {
                if (action.getData() instanceof Integer maxLength) {
                    List<Integer> updatedDrums = DrumSequenceModifier.applyMaxPatternLength(
                            sequencer, maxLength);

                    // Update UI for all affected drums
                    for (int drumIndex : updatedDrums) {
                        parentPanel.updateStepButtonsForDrum(drumIndex);
                    }

                    logger.info("Applied max pattern length {} to {} drums",
                            maxLength, updatedDrums.size());
                }
            }
        }
    }
}
