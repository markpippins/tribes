package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.DrumStepParametersEvent;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SequencerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel containing the drum sequencing grid buttons
 */
public class DrumSequencerGridButtonsPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerGridButtonsPanel.class);
    // UI constants
    private static final int DRUM_PAD_COUNT = SequencerConstants.DRUM_PAD_COUNT;
    private static final int GRID_BUTTON_SIZE = 24;
    // Core components
    private final List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private final DrumSequencerGridButton[][] gridButtons;
    private final DrumSequencer sequencer;
    private final DrumSequencerGridPanel parentPanel;
    private final DrumSequencerGridPanelContextHandler contextMenuHandler;
    // UI state
    private boolean isPlaying = false;
    private boolean debugMode = false;

    /**
     * Create a new DrumSequencerGridPanel
     *
     * @param sequencer   The drum sequencer
     * @param parentPanel The parent panel for callbacks
     */
    public DrumSequencerGridButtonsPanel(DrumSequencer sequencer, DrumSequencerGridPanel parentPanel) {
        this.sequencer = sequencer;
        this.parentPanel = parentPanel;

        // Create the context menu handler
        this.contextMenuHandler = new DrumSequencerGridPanelContextHandler(sequencer, parentPanel);

        // Use GridLayout for perfect grid alignment
        // REDUCED: from 2,2 to 1,1 - tighter grid spacing for more compact appearance
        setLayout(new GridLayout(DRUM_PAD_COUNT, sequencer.getDefaultPatternLength(), 4, 4));
        // REDUCED: from 5,5,5,5 to 2,2,2,2 - consistent with other panels
        // setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        // setBorder(BorderFactory.createLoweredBevelBorder());
        // Initialize grid buttons array
        gridButtons = new DrumSequencerGridButton[DRUM_PAD_COUNT][sequencer.getDefaultPatternLength()];
        // Create the grid buttons
        createGridButtons();
        // Visualizer gridSaver = new Visualizer(this, gridButtons);
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_STEP_UPDATED,
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                Commands.DRUM_STEP_EFFECTS_CHANGED,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.DRUM_SEQUENCE_LOADED,
                Commands.DRUM_SEQUENCE_UPDATED,
                Commands.DRUM_GRID_REFRESH_REQUESTED  // Add this line
        });
//        TimingBus.getInstance().register(this, new String[]{Commands.DRUM_STEP_UPDATED,
//                Commands.DRUM_STEP_PARAMETERS_CHANGED, Commands.DRUM_STEP_EFFECTS_CHANGED});

    }

    /**
     * Create all grid buttons
     */
    private void createGridButtons() {
        // Create grid buttons
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                DrumSequencerGridButton button = createStepButton(drumIndex, step);

                // IMPORTANT: Set initial state based on sequencer
                boolean isInPattern = step < sequencer.getPatternLength(drumIndex);
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Configure button
                button.setEnabled(isInPattern);
                button.setSelected(isActive);
                button.setVisible(true);

                // Add to panel and tracking list
                add(button);
                triggerButtons.add(button);

                // Also store in the 2D array for direct access by coordinates
                gridButtons[drumIndex][step] = button;
            }
        }
    }

    /**
     * Create step button with proper behavior
     */
    private DrumSequencerGridButton createStepButton(int drumIndex, int step) {
        DrumSequencerGridButton button = new DrumSequencerGridButton();

        // Make button square with constant size
        button.setPreferredSize(new Dimension(GRID_BUTTON_SIZE, GRID_BUTTON_SIZE));

        // Add debug info if needed
        if (debugMode) {
            button.setText(String.format("%d,%d", drumIndex, step));
            button.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 8));
        }

        button.setStepParameters(sequencer.isStepActive(drumIndex, step),
                sequencer.isStepAccented(drumIndex, step),
                sequencer.getStepVelocity(drumIndex, step),
                sequencer.getStepDecay(drumIndex, step),
                sequencer.getStepProbability(drumIndex, step),
                sequencer.getStepNudge(drumIndex, step)
        );


        // Set effects parameters
        button.setEffectsParameters(
                sequencer.getStepPan(drumIndex, step),
                sequencer.getStepChorus(drumIndex, step),
                sequencer.getStepReverb(drumIndex, step)
        );

        // Set step index for debugging/visualization
        button.setStepIndex(step);

        // Add action listener to toggle step state
        button.addActionListener(e -> {
            sequencer.toggleStep(drumIndex, step);
            button.setSelected(sequencer.isStepActive(drumIndex, step));

            // IMPORTANT: Update parameters after toggle to ensure defaults are applied
            button.setStepParameters(sequencer.isStepActive(drumIndex, step),
                    sequencer.isStepAccented(drumIndex, step),
                    sequencer.getStepVelocity(drumIndex, step),
                    sequencer.getStepDecay(drumIndex, step),
                    sequencer.getStepProbability(drumIndex, step),
                    sequencer.getStepNudge(drumIndex, step)
            );
        });

        // Add right-click context menu
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    contextMenuHandler.showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    contextMenuHandler.showContextMenu(e.getComponent(), e.getX(), e.getY(), drumIndex, step);
                }
            }
        });

        return button;
    }

    /**
     * Update appearance of an entire drum row
     */
    public void updateRowAppearance(int drumIndex, boolean isSelected) {
        int patternLength = sequencer.getPatternLength(drumIndex);

        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Keep all buttons visible
                button.setVisible(true);

                // Style based on whether step is active and in pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = sequencer.isStepActive(drumIndex, step);

                // Update button appearance using the button's own functionality
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Add subtle highlighting to the selected row
                if (isSelected) {
                    // Highlight the selected row's border
                    button.setBorder(BorderFactory.createLineBorder(
                            UIHelper.dustyAmber, 1));
                } else {
                    // Normal border for other rows
                    button.setBorder(BorderFactory.createLineBorder(
                            Color.DARK_GRAY, 1));
                }
            }
        }
    }

    /**
     * Update all step buttons for a specific drum
     */
    public void updateStepButtonsForDrum(int drumIndex) {
        if (drumIndex < 0 || drumIndex >= DRUM_PAD_COUNT || triggerButtons.isEmpty()) {
            // Invalid drum index or buttons not initialized yet
            logger.warn("Cannot update step buttons: invalid drum index {} or buttons not initialized", drumIndex);
            return;
        }

        // Get pattern length for this drum
        int patternLength = sequencer.getPatternLength(drumIndex);
        logger.debug("Updating step buttons for drum {} with pattern length {}", drumIndex, patternLength);

        // Update all buttons for this row
        for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
            int buttonIndex = (drumIndex * sequencer.getDefaultPatternLength()) + step;

            // Safety check
            if (buttonIndex >= 0 && buttonIndex < triggerButtons.size()) {
                DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                // Make button visible regardless of pattern length - CRITICAL FIX
                button.setVisible(true);

                // Update button state based on pattern
                boolean isInPattern = step < patternLength;
                boolean isActive = isInPattern && sequencer.isStepActive(drumIndex, step);

                // Update button state
                button.setEnabled(isInPattern);
                button.setSelected(isActive);

                // Update all parameter values
                button.setStepParameters(sequencer.isStepActive(drumIndex, step),
                        sequencer.isStepAccented(drumIndex, step),
                        sequencer.getStepVelocity(drumIndex, step),
                        sequencer.getStepDecay(drumIndex, step),
                        sequencer.getStepProbability(drumIndex, step),
                        sequencer.getStepNudge(drumIndex, step)
                );


                // Set effects parameters
                button.setEffectsParameters(
                        sequencer.getStepPan(drumIndex, step),
                        sequencer.getStepChorus(drumIndex, step),
                        sequencer.getStepReverb(drumIndex, step)
                );

                // Style based on whether step is active and in pattern
                if (!isInPattern) {
                    button.setBackground(UIHelper.charcoalGray);
                } else {
                    if (isActive) {
                        button.setBackground(UIHelper.agedOffWhite);
                    } else {
                        button.setBackground(UIHelper.slateGray);
                    }
                }

                // Always repaint
                button.repaint();
            }
        }
    }

    /**
     * Update step highlighting during playback with position-based colors
     */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Ensure we're on the EDT for UI updates
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateStepHighlighting(drumIndex, oldStep, newStep));
            return;
        }

        // Use the 2D array for more direct access to buttons by coordinates
        // This is more reliable than calculating indices in the flattened list
        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            // Un-highlight old step if valid
            if (oldStep >= 0 && oldStep < sequencer.getDefaultPatternLength()) {
                DrumSequencerGridButton oldButton = gridButtons[drumIndex][oldStep];
                if (oldButton != null) {
                    oldButton.setHighlighted(false);
                    oldButton.repaint();
                }
            }

            // Highlight new step if valid and we're playing
            if (isPlaying && newStep >= 0 && newStep < sequencer.getDefaultPatternLength()) {
                DrumSequencerGridButton newButton = gridButtons[drumIndex][newStep];
                if (newButton != null) {
                    // Choose color based on step position in the pattern
                    Color highlightColor;
                    if (newStep < 16) {
                        highlightColor = UIHelper.fadedOrange;
                    } else if (newStep < 32) {
                        highlightColor = UIHelper.coolBlue;
                    } else if (newStep < 48) {
                        highlightColor = UIHelper.deepNavy;
                    } else {
                        highlightColor = UIHelper.mutedOlive;
                    }

                    newButton.setHighlighted(true);
                    newButton.setHighlightColor(highlightColor);
                    newButton.repaint();
                }
            }
        }
    }

    /**
     * Special version of step highlighting for backward playback to prevent trails
     */
    public void updateBackwardStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Ensure we're on the EDT for UI updates
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateBackwardStepHighlighting(drumIndex, oldStep, newStep));
            return;
        }

        if (drumIndex >= 0 && drumIndex < DRUM_PAD_COUNT) {
            // For backward direction, first clear ALL highlights in this row
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                DrumSequencerGridButton button = gridButtons[drumIndex][step];
                if (button != null) {
                    button.setHighlighted(false);
                }
            }

            // Then highlight ONLY the new step
            if (isPlaying && newStep >= 0 && newStep < sequencer.getDefaultPatternLength()) {
                DrumSequencerGridButton newButton = gridButtons[drumIndex][newStep];
                if (newButton != null) {
                    // Choose color based on step position in the pattern
                    Color highlightColor;
                    if (newStep < 16) {
                        highlightColor = UIHelper.fadedOrange;
                    } else if (newStep < 32) {
                        highlightColor = UIHelper.coolBlue;
                    } else if (newStep < 48) {
                        highlightColor = UIHelper.deepNavy;
                    } else {
                        highlightColor = UIHelper.mutedOlive;
                    }

                    newButton.setHighlighted(true);
                    newButton.setHighlightColor(highlightColor);
                }
            }

            // Repaint the entire row to avoid partial update issues
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                if (gridButtons[drumIndex][step] != null) {
                    gridButtons[drumIndex][step].repaint();
                }
            }
        }
    }

    /**
     * Clear all step highlighting across all drum rows
     */
    public void clearAllStepHighlighting() {
        for (DrumSequencerGridButton button : triggerButtons) {
            button.clearTemporaryState();
            button.repaint();
        }
    }

    /**
     * Set playing state to control step highlighting
     */
    public void setPlayingState(boolean isPlaying) {
        this.isPlaying = isPlaying;

        if (!isPlaying) {
            clearAllStepHighlighting();
        }
    }

    /**
     * Refresh the entire grid UI to match the current sequencer state
     */
    public void refreshGridUI() {
        if (triggerButtons == null || triggerButtons.isEmpty()) {
            logger.warn("Cannot refresh grid UI - triggerButtons list is empty");
            return;
        }

        logger.info("Refreshing entire grid UI for sequence {}", sequencer.getSequenceData().getId());

        // Ensure we refresh ALL drums and ALL steps
        for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
            for (int step = 0; step < sequencer.getDefaultPatternLength(); step++) {
                int buttonIndex = drumIndex * sequencer.getDefaultPatternLength() + step;

                if (buttonIndex < triggerButtons.size()) {
                    DrumSequencerGridButton button = triggerButtons.get(buttonIndex);

                    if (button != null) {
                        // Get the current state from the sequencer
                        boolean active = sequencer.isStepActive(drumIndex, step);

                        // Force update button state without triggering events
                        button.setToggled(active);
                        button.setHighlighted(false); // Clear any highlighting
                        button.repaint(); // Force immediate repaint
                    }
                }
            }

            // Update the drum row's appearance
            updateRowAppearance(drumIndex, drumIndex == SequencerService.getInstance().getSelectedPadIndex());
        }

        // Update parameter visualizations for all buttons
        for (int drumIndex = 0; drumIndex < gridButtons.length; drumIndex++) {
            DrumSequencerGridButton[] row = gridButtons[drumIndex];
            for (int stepIndex = 0; stepIndex < row.length; stepIndex++) {
                DrumSequencerGridButton button = row[stepIndex];

                // Set step parameters for visualization
                button.setStepParameters(sequencer.isStepActive(drumIndex, stepIndex),
                        sequencer.isStepAccented(drumIndex, stepIndex),
                        sequencer.getStepVelocity(drumIndex, stepIndex),
                        sequencer.getStepDecay(drumIndex, stepIndex),
                        sequencer.getStepProbability(drumIndex, stepIndex),
                        sequencer.getStepNudge(drumIndex, stepIndex)
                );


                // Set effects parameters
                button.setEffectsParameters(
                        sequencer.getStepPan(drumIndex, stepIndex),
                        sequencer.getStepChorus(drumIndex, stepIndex),
                        sequencer.getStepReverb(drumIndex, stepIndex)
                );

                // Set visual modes based on current view
                button.setShowEffects(false); // Default off, toggle with a view button
                button.setStepIndex(stepIndex); // For debugging
            }
        }

        // Ensure proper visual refresh
        revalidate();
        repaint();
    }

    /**
     * Toggle debug mode to show grid indices
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;

        // Show indices on buttons in debug mode
        if (triggerButtons != null) {
            for (int i = 0; i < triggerButtons.size(); i++) {
                DrumSequencerGridButton button = triggerButtons.get(i);
                int drumIndex = i / sequencer.getDefaultPatternLength();
                int stepIndex = i % sequencer.getDefaultPatternLength();

                if (debugMode) {
                    button.setText(drumIndex + "," + stepIndex);
                    button.setForeground(Color.YELLOW);
                } else {
                    button.setText("");
                }
            }
        }
    }

    /**
     * Subscribe to parameter change events from DrumParamsSequencerPanel
     */
    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }


        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE:
                handleTimingUpdate(action);
                break;

            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED:
                SwingUtilities.invokeLater(this::refreshGridUI);

            case Commands.DRUM_STEP_UPDATED:
                if (action.getData() instanceof DrumStepUpdateEvent event) {
                    updateStepHighlighting(event.getDrumIndex(), event.getOldStep(), event.getNewStep());
                }
                break;

            // Existing cases for parameters and effects
            case Commands.DRUM_STEP_PARAMETERS_CHANGED:
                if (action.getData() instanceof DrumStepParametersEvent event) {
                    updateStepButtonParameters(event);
                }

            case Commands.DRUM_STEP_EFFECTS_CHANGED:
                if (action.getData() instanceof Object[] data && data.length >= 5) {
                    int drumIndex = (Integer) data[0];
                    int stepIndex = (Integer) data[1];
                    int pan = (Integer) data[2];
                    int chorus = (Integer) data[3];
                    int reverb = (Integer) data[4];

                    // Update the button effects visuals
                    updateStepButtonEffects(drumIndex, stepIndex, pan, chorus, reverb);
                }
                break;

            // Handle transport commands to update playing state
            case Commands.TRANSPORT_START:
                setPlayingState(true);
                break;

            case Commands.TRANSPORT_STOP:
                setPlayingState(false);
                break;

            // Add this case to the switch statement in onAction
            case Commands.DRUM_GRID_REFRESH_REQUESTED:
                // Use existing method to refresh the entire grid UI
                refreshGridUI();
                break;
        }
    }

    private void handleTimingUpdate(Command action) {

        if (isPlaying && action.getData() instanceof TimingUpdate update) {
            // Calculate current absolute tick position
            long tickCount = update.tickCount();

            // Get timing settings
            int ticksPerBeat = 96; // Default PPQN
            int stepsPerBeat = 4;  // 16th notes in 4/4
            int ticksPerStep = ticksPerBeat / stepsPerBeat;

            // Calculate the absolute step number (not wrapped)
            int absoluteStep = (int) (tickCount / ticksPerStep);

            // Update each drum row independently, using its own length for wrapping
            for (int drumIndex = 0; drumIndex < DRUM_PAD_COUNT; drumIndex++) {
                // Get this drum's pattern length
                int drumPatternLength = sequencer.getPatternLength(drumIndex);

                // Skip if the pattern has no length
                if (drumPatternLength <= 0) {
                    continue;
                }

                // Get the direction for this drum
                Direction direction = sequencer.getSequenceData().getDirections()[drumIndex];

                // Calculate current step position based on direction mode
                int drumCurrentStep;
                int drumPreviousStep;

                if (direction == Direction.BOUNCE) {
                    // For bounce mode, we need to calculate if we're in forward or backward phase
                    // First calculate total cycle length (forward + backward - overlap at ends)
                    int cycleLength = (drumPatternLength * 2) - 2;

                    // Calculate position within cycle
                    int positionInCycle = absoluteStep % cycleLength;

                    // Determine whether we're in forward or backward phase
                    if (positionInCycle < drumPatternLength) {
                        // Forward phase
                        drumCurrentStep = positionInCycle;
                    } else {
                        // Backward phase: map position from end back to start
                        drumCurrentStep = cycleLength - positionInCycle;
                    }

                    // Calculate previous step based on direction in the cycle
                    if (positionInCycle == 0) {
                        // FIXED: At start of forward phase, set previous to -1 instead of 0
                        // This ensures no visual artifact from previous step
                        drumPreviousStep = -1;
                    } else if (positionInCycle == drumPatternLength - 1) {
                        // At end of forward phase, previous is one step back
                        drumPreviousStep = drumPatternLength - 2;
                    } else if (positionInCycle == drumPatternLength) {
                        // At start of backward phase, previous is last step
                        drumPreviousStep = drumPatternLength - 1;
                    } else if (positionInCycle == cycleLength - 1) {
                        // At end of backward phase, previous is second step
                        drumPreviousStep = 1;
                    } else if (positionInCycle < drumPatternLength) {
                        // In forward phase
                        drumPreviousStep = drumCurrentStep - 1;
                    } else {
                        // In backward phase
                        drumPreviousStep = drumCurrentStep + 1;
                    }

                    // Update highlighting using the regular method - bounce mode works well with it
                    updateStepHighlighting(drumIndex, drumPreviousStep, drumCurrentStep);
                } else if (direction == Direction.BACKWARD) {
                    // For backward mode, we count from the end back to the start
                    drumCurrentStep = (drumPatternLength - 1) - (absoluteStep % drumPatternLength);

                    // Fix for the previous step calculation in backward mode
                    if (drumCurrentStep == 0) {
                        // If we're at the first step, previous was the last step
                        drumPreviousStep = drumPatternLength - 1;
                    } else {
                        // Otherwise, previous step is one step forward (not backward)
                        drumPreviousStep = drumCurrentStep - 1;
                    }

                    // **** THIS IS THE CRITICAL CHANGE ****
                    // Use the special backward highlighting method instead of the regular one
                    updateBackwardStepHighlighting(drumIndex, drumPreviousStep, drumCurrentStep);
                } else {
                    // Default forward mode (and bounce) continue to use the regular method
                    drumCurrentStep = absoluteStep % drumPatternLength;
                    drumPreviousStep = (drumCurrentStep == 0)
                            ? drumPatternLength - 1 : drumCurrentStep - 1;
                    updateStepHighlighting(drumIndex, drumPreviousStep, drumCurrentStep);
                }
            }
        }
    }

    /**
     * Update a step button's parameter visualizations
     */
    private void updateStepButtonParameters(DrumStepParametersEvent event) {
        if (event.getDrumIndex() >= 0 && event.getDrumIndex() < gridButtons.length) {
            DrumSequencerGridButton[] buttons = gridButtons[event.getDrumIndex()];
            if (event.getStepIndex() >= 0 && event.getStepIndex() < buttons.length) {
                DrumSequencerGridButton button = buttons[event.getStepIndex()];
                button.setStepParameters(event.isActive(), event.isAccented(), event.getVelocity(), event.getDecay(), event.getProbability(), event.getNudge());
                button.repaint();
            }
        }
    }

    /**
     * Update a step button's effects visualizations
     */
    private void updateStepButtonEffects(int drumIndex, int stepIndex, int pan, int chorus, int reverb) {
        if (drumIndex >= 0 && drumIndex < gridButtons.length) {
            DrumSequencerGridButton[] row = gridButtons[drumIndex];
            if (stepIndex >= 0 && stepIndex < row.length) {
                DrumSequencerGridButton button = row[stepIndex];
                button.setEffectsParameters(pan, chorus, reverb);
            }
        }
    }
}
