package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.Symbols;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.visualization.Visualizer;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.sequencer.DrumSequenceModifier;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.angrysurfer.core.service.SequencerService;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * A sequencer panel with X0X-style step sequencing capabilities. This is the UI
 * component for the DrumSequencer.
 */
@Getter
@Setter
public class DrumSequencerGridPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumParamsSequencerPanel.class);

    private DrumSequencerInfoPanel drumInfoPanel;
    private DrumSequenceNavigationPanel navigationPanel;
    private Visualizer visualizer;

    private DrumSequencer sequencer;

    private DrumSequencerParametersPanel sequenceParamsPanel;
    private JToggleButton loopToggleButton;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JButton generatePatternButton;
    private JButton clearPatternButton;
    private JSpinner densitySpinner;
    // Debug mode flag
    private boolean debugMode = false;

    // Add this field to the class
    private boolean isPlaying = false;

    // Add this field to the class
    private boolean isSelectingDrumPad = false;

    // Add this field to the class
    private boolean updatingUI = false;

    // Add this field to DrumSequencerPanel:
    private DrumSequencerSwingPanel swingPanel;

    // Add this field to DrumSequencerPanel:
    private DrumSelectorPanel drumSelectorPanel;

    // Add this field instead:
    private DrumSequencerGridButtonsPanel gridPanel;

    private JScrollPane scrollPane;

    // Add this field to the class:
    private DrumSequenceGeneratorPanel generatorPanel;

    /**
     * Create a new DrumSequencerPanel
     *
     * @param noteEventConsumer Callback for when a note should be played
     */
    public DrumSequencerGridPanel(Consumer<NoteEvent> noteEventConsumer) {
        super(new BorderLayout());

        // Create the sequencer
        sequencer = SequencerService.getInstance().newSequencer(
                noteEventConsumer,
                event -> gridPanel.updateStepHighlighting(event.getDrumIndex(), event.getOldStep(),
                        event.getNewStep()));


        // Initialize UI components
        initialize();

        // When the panel gains focus or becomes visible, request focus
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
//                selectDrumPad(SequencerService.getInstance().getSelectedPadIndex());
            }
        });

        // Request focus when the panel becomes visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
                if (SequencerService.getInstance().getSelectedPadIndex() == -1)
                    SwingUtilities.invokeLater(() -> selectDrumPad(0));
            }
        });


        // Add as listener
        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_STEP_UPDATED,
                Commands.DRUM_SEQUENCE_GRID_RECREATE_REQUESTED,
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                Commands.DRUM_STEP_EFFECTS_CHANGED,
                Commands.HIGHLIGHT_STEP,
                Commands.WINDOW_RESIZED
        });
        logger.info("DrumSequencerPanel registered as listener");

    }

    /**
     * Initialize the panel
     */
    private void initialize() {
        // Clear any existing components first to prevent duplication
        removeAll();

        setLayout(new BorderLayout(2, 2));
        UIHelper.setPanelBorder(this);

        JPanel topEastPanel = new JPanel(new BorderLayout(2, 2));
        JPanel topWestPanel = new JPanel(new BorderLayout(2, 2));

        JPanel topPanel = new JPanel(new BorderLayout(2, 2));

        DrumSequenceNavigationPanel navigationPanel = new DrumSequenceNavigationPanel(sequencer);

        topEastPanel.add(navigationPanel, BorderLayout.WEST);

        topWestPanel.add(new SoundParametersPanel(), BorderLayout.WEST);

        topPanel.add(topEastPanel, BorderLayout.EAST);
        topPanel.add(topWestPanel, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);

        JPanel drumPadsPanel = createDrumPadsPanel();
        add(drumPadsPanel, BorderLayout.WEST);

        // Wrap in scroll pane
        scrollPane = new JScrollPane(createSequenceGridPanel());
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout(2, 2));

        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);

        bottomPanel.add(sequenceParamsPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rightPanel.add(new DrumSequencerMaxLengthPanel(sequencer));

        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        DrumSequencerSwingPanel swingPanel = new DrumSequencerSwingPanel(sequencer);
        swingPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        rightPanel.add(swingPanel);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        // JPanel buttonPanel = new JPanel();
        //UIHelper.createSectionPanel("Presets");
        // UIHelper.setWidgetPanelBorder(buttonPanel, "Debug");
        // Create preset selection button
        // JButton presetButton = new JButton(Symbols.get(Symbols.LOAD));
        // presetButton.setToolTipText("Select preset instruments for each drum");
        // presetButton.setPreferredSize(new Dimension(24, 24));
        // presetButton.setMaximumSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        // presetButton.addActionListener(e -> {
        //     CommandBus.getInstance().publish(
        //             Commands.DRUM_PRESET_SELECTION_REQUEST,
        //             this,
        //             sequencer);
        // });

        // buttonPanel.add(presetButton);
        // buttonPanel.add(createRefreshButton());
        // Add the button to the bottom panel
        // eastPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Add this as a new method:
    private JButton createRefreshButton() {
        JButton refreshButton = new JButton(
                Symbols.get(Symbols.RESTART));
        refreshButton.setToolTipText("Refresh drum instruments (fixes sound issues)");
        refreshButton.setPreferredSize(new Dimension(com.angrysurfer.beats.util.UIHelper.SMALL_CONTROL_WIDTH,
                com.angrysurfer.beats.util.UIHelper.CONTROL_HEIGHT));
        refreshButton.setMaximumSize(new Dimension(com.angrysurfer.beats.util.UIHelper.SMALL_CONTROL_WIDTH,
                com.angrysurfer.beats.util.UIHelper.CONTROL_HEIGHT));

        refreshButton.addActionListener(e -> {
            if (sequencer != null) {
                logger.info("Refreshing all drum instruments");

                // Force instrument refresh for all drum players
                for (int i = 0; i < sequencer.getPlayers().length; i++) {
                    com.angrysurfer.core.model.Player player = sequencer.getPlayers()[i];
                    if (player != null && player.getInstrument() != null) {
                        // Apply preset through PlayerManager
                        com.angrysurfer.core.service.PlaybackService.getInstance().applyPreset(player);

                        // Log instrument details
                        logger.info("Refreshed drum {}: {} (bank={}, program={})",
                                i, player.getName(),
                                player.getInstrument().getBankIndex(),
                                player.getInstrument().getPreset());
                    }
                }

                // Check MIDI connections
                sequencer.ensureDeviceConnections();

                // Update UI
                com.angrysurfer.core.api.CommandBus.getInstance().publish(
                        com.angrysurfer.core.api.Commands.STATUS_UPDATE,
                        this,
                        new StatusUpdate(
                                "Drum Refresh", "Info", "Refreshed all drum instruments"));
            }
        });

        return refreshButton;
    }

    /**
     * Create the drum pads panel on the left side
     */
    private JPanel createDrumPadsPanel() {
        // Create and return the new drum selector panel
        drumSelectorPanel = new DrumSelectorPanel(sequencer, this);
        return drumSelectorPanel;
    }

    /**
     * Create the sequence grid panel with step buttons
     */
    private JPanel createSequenceGridPanel() {
        gridPanel = new DrumSequencerGridButtonsPanel(sequencer, this);
        return gridPanel;
    }

    /**
     * Handle selection of a drum pad - completely revised to fix display issues
     */
    public void selectDrumPad(int padIndex) {
        logger.info("DrumSequencerPanel: Selecting drum pad {}", padIndex);
        // Guard against recursive calls
        if (isSelectingDrumPad) {
            return;
        }

        isSelectingDrumPad = true;
        try {
            // Store the selected pad index
            SequencerService.getInstance().setSelectedPadIndex(padIndex);

            // Tell sequencer about the selection (without sending another event back)
            sequencer.setSelectedPadIndex(padIndex);

            // Update UI for all drum rows
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                gridPanel.updateRowAppearance(i, i == padIndex);
            }

            // Update the drum selector panel buttons
            if (drumSelectorPanel != null) {
                drumSelectorPanel.updateButtonSelection(padIndex);
            }

            // Update parameter controls for the selected drum
            updateParameterControls();

            // Update info panel
            if (drumInfoPanel != null) {
                drumInfoPanel.updateInfo(padIndex);
            }

            // Update sound parameters panel
            // updateSoundParametersPanel(padIndex);

            CommandBus.getInstance().publish(Commands.PLAYER_SELECTION_EVENT, this, sequencer.getPlayers()[padIndex]);

            // IMPORTANT: Notify other panels through the command bus
            CommandBus.getInstance().publish(
                    Commands.DRUM_PAD_SELECTED,
                    this, // Send 'this' as the source to prevent circular updates
                    new DrumPadSelectionEvent(-1, padIndex));
            logger.info("DrumSequencerPanel: Published selection event for pad {}", padIndex);
        } finally {
            isSelectingDrumPad = false;
        }
    }

    /**
     * Update the parameter controls to reflect the current selected drum
     */
    private void updateParameterControls() {
        // Check if we have a valid selection before updating
        if (SequencerService.getInstance().getSelectedPadIndex() < 0
                || SequencerService.getInstance().getSelectedPadIndex() >= SequencerConstants.DRUM_PAD_COUNT) {
            // logger.warn("Cannot update parameters - invalid drum index: {}",
            // selectedPadIndex);
            return;
        }

        // Use the new panel's method to update controls
        sequenceParamsPanel.updateControls(SequencerService.getInstance().getSelectedPadIndex());
    }

    /**
     * Enable debug mode to show grid indices
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;
        gridPanel.toggleDebugMode();
    }

    /**
     * Enable or disable debug mode to show grid indices
     *
     * @param enabled Whether debug mode should be enabled
     */
    public void toggleDebugMode(boolean enabled) {
        this.debugMode = enabled;

        // Delegate to the grid panel
        if (gridPanel != null) {
            gridPanel.toggleDebugMode();
        }
    }

    /**
     * Handle command bus messages
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_UPDATED -> {
                // Update the UI to reflect new sequence data
                SwingUtilities.invokeLater(() -> {
                    // Update the entire grid UI
                    gridPanel.refreshGridUI();

                    // Update the parameter controls for the selected drum
                    updateParameterControls();

                    // Update the drum info panel
                    if (drumInfoPanel != null) {
                        drumInfoPanel.updateInfo(SequencerService.getInstance().getSelectedPadIndex());
                    }

                    // Reset all step highlighting
                    gridPanel.clearAllStepHighlighting();
                });
            }

            case Commands.DRUM_PAD_SELECTED -> {
                if (action.getData() instanceof DrumPadSelectionEvent event) {
                    selectDrumPad(event.getNewSelection());
                }
            }

            case Commands.TRANSPORT_START -> {
                // Show step highlighting when playing starts
                isPlaying = true;
                if (gridPanel != null) {
                    gridPanel.setPlayingState(true);
                }
            }

            case Commands.TRANSPORT_STOP -> {
                // Hide step highlighting when playing stops
                isPlaying = false;
                if (gridPanel != null) {
                    gridPanel.setPlayingState(false);
                }
            }

            case Commands.MAX_LENGTH_SELECTED -> {
                if (action.getData() instanceof Integer maxLength) {
                    List<Integer> updatedDrums = DrumSequenceModifier.applyMaxPatternLength(sequencer, maxLength);

                    // Update UI for affected drums
                    for (int drumIndex : updatedDrums) {
                        gridPanel.updateStepButtonsForDrum(drumIndex);
                    }

                    // Update parameter controls if the selected drum was affected
                    if (updatedDrums.contains(SequencerService.getInstance().getSelectedPadIndex())) {
                        updateParameterControls();
                    }

                    // Show confirmation message
                    showPatternLengthUpdateMessage(updatedDrums.size());
                }
            }

            case Commands.EUCLIDEAN_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    boolean[] pattern = (boolean[]) result[1];

                    // Use the static method from DrumSequenceModifier
                    boolean success = DrumSequenceModifier.applyEuclideanPattern(sequencer, drumIndex, pattern);

                    // If successful, update the UI
                    if (success) {
                        gridPanel.updateStepButtonsForDrum(drumIndex);
                        updateParameterControls();
                    }
                }
            }

            case Commands.FILL_PATTERN_SELECTED -> {
                if (action.getData() instanceof Object[] result) {
                    int drumIndex = (Integer) result[0];
                    int startStep = (Integer) result[1];
                    String fillType = (String) result[2];

                    // Apply the fill pattern
                    int patternLength = sequencer.getPatternLength(drumIndex);

                    for (int i = startStep; i < patternLength; i++) {
                        boolean shouldActivate = false;

                        switch (fillType) {
                            case "all" -> shouldActivate = true;
                            case "everyOther" -> shouldActivate = ((i - startStep) % 2) == 0;
                            case "every4th" -> shouldActivate = ((i - startStep) % 4) == 0;
                            case "decay" -> {
                                shouldActivate = true;
                                sequencer.setVelocity(drumIndex,
                                        Math.max(SequencerConstants.DEFAULT_VELOCITY / 2, SequencerConstants.DEFAULT_VELOCITY - ((i - startStep) * 8)));
                            }
                        }

                        if (shouldActivate) {
                            sequencer.toggleStep(drumIndex, i);
                        }
                    }

                    // Update UI to reflect changes
                    gridPanel.updateStepButtonsForDrum(drumIndex);
                }
            }

            case Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Update step buttons for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        updateStepButtonsForDrum(drumIndex);
                    });
                }
            }

            case Commands.DRUM_GRID_REFRESH_REQUESTED -> {
                // Refresh the entire grid UI
                SwingUtilities.invokeLater(() -> {
                    refreshGridUI();
                });
            }

            case Commands.DRUM_PATTERN_CLEAR_REQUESTED -> {
                if (action.getData() instanceof Integer drumIndex) {
                    // Clear the pattern for the specified drum
                    SwingUtilities.invokeLater(() -> {
                        clearRow(drumIndex);
                    });
                }
            }
        }
    }

    /**
     * Clears all steps for a specific drum track
     *
     * @param drumIndex The index of the drum to clear
     */
    public void clearRow(int drumIndex) {
        boolean success = DrumSequenceModifier.clearDrumTrack(sequencer, drumIndex);
        if (success) {
            // Update UI after successful clear
            gridPanel.updateStepButtonsForDrum(drumIndex);

            // Update parameter controls if clearing the selected drum
            if (drumIndex == SequencerService.getInstance().getSelectedPadIndex()) {
                updateParameterControls();
            }
        }
    }

    /**
     * Shows a confirmation dialog for pattern length updates
     *
     * @param updatedCount The number of drum patterns that were modified
     */
    private void showPatternLengthUpdateMessage(int updatedCount) {
        SwingUtilities.invokeLater(() -> {
            if (updatedCount > 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Updated pattern length for " + updatedCount + " drums.",
                        "Pattern Length Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "No drum patterns were affected.",
                        "Pattern Length Check",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // If there's an updateSwingControls() method, update it too:
    private void updateSwingControls() {
        if (swingPanel != null) {
            swingPanel.updateControls();
        }
    }

    /**
     * Updates the UI for a specific drum's steps
     *
     * @param drumIndex The index of the drum to update
     */
    public void updateStepButtonsForDrum(int drumIndex) {
        if (gridPanel != null) {
            gridPanel.updateStepButtonsForDrum(drumIndex);
        }
    }

    /**
     * Refreshes the entire grid UI
     */
    public void refreshGridUI() {
        if (gridPanel != null) {
            gridPanel.refreshGridUI();
        }
    }

    /**
     * Update step highlighting based on current step position - delegates to grid
     * panel
     *
     * @param oldStep The previous step to un-highlight
     * @param newStep The new step to highlight
     */
    public void updateStepHighlighting(int drumIndex, int oldStep, int newStep) {
        // Delegate to the grid panel which has direct access to the buttons
        if (gridPanel != null) {
            gridPanel.updateStepHighlighting(drumIndex, oldStep, newStep);
        }
    }

    /**
     * Completely recreate the grid panel to reflect changes in max pattern length
     */
    public void recreateGridPanel() {

        // gridPanel.clearGridButtons();
        // gridPanel.createGridButtons();
        // gridPanel.refreshGridUI();
        // gridPanel.updateStepButtonsForDrum(selectedPadIndex);
        // Refresh layout
        // revalidate();
        // repaint();
    }
}
