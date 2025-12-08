package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.MainPanel;
import com.angrysurfer.beats.panel.player.SoundParametersPanel;
import com.angrysurfer.beats.panel.sequencer.MuteSequencerPanel;
import com.angrysurfer.beats.panel.sequencer.OffsetSequencerPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.AccentButton;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.event.DrumPadSelectionEvent;
import com.angrysurfer.core.event.DrumStepParametersEvent;
import com.angrysurfer.core.event.DrumStepUpdateEvent;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SequencerService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class DrumSequencerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerPanel.class);

    private static DrumStepParametersEvent reusableParamChangeEvent = new DrumStepParametersEvent();
    private final List<TriggerButton> selectorButtons = new ArrayList<>();
    private final List<AccentButton> accentButtons = new ArrayList<>();

    private DrumSequenceNavigationPanel navigationPanel;
    private DrumSequencerParametersPanel sequenceParamsPanel; // Changed from DrumParamsSequencerParametersPanel
    private DrumSequencerMaxLengthPanel maxLengthPanel; // New field
    private DrumSequenceGeneratorPanel generatorPanel; // New field
    private DrumSequencerSwingPanel swingPanel;
    private DrumButtonsPanel drumPadPanel;
    private MuteSequencerPanel muteSequencerPanel; // Add this field to PolyPanel

    private DrumSequencerGridPanelContextHandler contextMenuHandler; // Context menu handler

    private int selectedPadIndex = -1; // Default to no selection
    private boolean updatingControls = false;
    private boolean isHandlingSelection = false;
    private DrumSequencer sequencer;
    private Consumer<NoteEvent> noteEventConsumer;

    public DrumSequencerPanel() {
        super();
        setup();
    }

    public DrumSequencerPanel(LayoutManager layout) {
        super(layout);
        setup();
    }

    abstract String getKnobLabel(int index);

    protected DrumStepParametersEvent createDrumStepParametersEvent(DrumSequencer sequencer, int drumIndex, int stepIndex) {
        reusableParamChangeEvent.setDrumIndex(drumIndex);
        reusableParamChangeEvent.setStepIndex(stepIndex);
        reusableParamChangeEvent.setAccented(sequencer.isStepAccented(drumIndex, stepIndex));
        reusableParamChangeEvent.setDecay(sequencer.getStepDecay(drumIndex, stepIndex));
        reusableParamChangeEvent.setNudge(sequencer.getStepNudge(drumIndex, stepIndex));
        reusableParamChangeEvent.setProbability(sequencer.getStepProbability(drumIndex, stepIndex));
        reusableParamChangeEvent.setVelocity(sequencer.getStepVelocity(drumIndex, stepIndex));
        return reusableParamChangeEvent;
    }

    private void setup() {
        // Get the shared sequencer instance from SequencerService
        sequencer = SequencerService.getInstance().getDrumSequencer(0);

        // If no sequencer exists yet, create one
        if (sequencer == null) {
            logger.info("Creating new drum sequencer through manager");
            sequencer = SequencerService.getInstance().createDrumSequencer();
            // Double check we got a sequencer
            if (sequencer == null) {
                throw new IllegalStateException("Failed to create sequencer");
            }
        }

        createUI();

        // which will execute after all buttons are created
        CommandBus.getInstance().register(this, new String[]{
                Commands.DRUM_PAD_SELECTED,
                Commands.DRUM_STEP_SELECTED,
                Commands.DRUM_INSTRUMENTS_UPDATED,
                Commands.HIGHLIGHT_STEP,

                // Add these events to respond to DrumSequenceModifier operations
                Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED,
                Commands.PATTERN_UPDATED,
                Commands.DRUM_STEP_PARAMETERS_CHANGED,
                Commands.DRUM_STEP_EFFECTS_CHANGED,
                Commands.DRUM_GRID_REFRESH_REQUESTED,

                // Add these to respond to dialog results
                Commands.FILL_PATTERN_SELECTED,
                Commands.EUCLIDEAN_PATTERN_SELECTED,
                Commands.MAX_LENGTH_SELECTED,

                // Add mute events
                Commands.DRUM_TRACK_MUTE_CHANGED,
                Commands.DRUM_TRACK_MUTE_VALUES_CHANGED
        });

        logger.info("DrumParamsSequencerPanel registered for specific events");

        TimingBus.getInstance().register(this);
    }

    abstract JPanel createSequenceColumn(int i);


    abstract void updateControlsFromSequencer();


    private void refreshControls() {
        if (selectedPadIndex > -1) {
            refreshAccentButtonsForPad(selectedPadIndex);
            refreshTriggerButtonsForPad(selectedPadIndex);
            updateControlsFromSequencer();

            if (sequenceParamsPanel != null) {
                sequenceParamsPanel.updateControls(selectedPadIndex);
            }

            CommandBus.getInstance().publish(Commands.SEQUENCER_SYNC_MESSAGE, this, sequencer);
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {

            case Commands.CHANGE_THEME -> {
                // Handle theme change by recreating drum pad panel
                SwingUtilities.invokeLater(this::handleThemeChange);

            }

            case Commands.TIMING_UPDATE -> {
                // Only update if we have a drum selected and are playing
                if (selectedPadIndex >= 0 && sequencer.isPlaying() && action.getData() instanceof TimingUpdate) {
                    // Get the current sequencer state
                    int[] steps = sequencer.getSequenceData().getCurrentStep();

                    // Safety check for array bounds
                    if (selectedPadIndex < steps.length) {
                        int currentStep = steps[selectedPadIndex];
                        int previousStep = calculatePreviousStep(currentStep);

                        updateStep(previousStep, currentStep);
                    }
                }
            }

            case Commands.TRANSPORT_START, Commands.DRUM_SEQUENCE_LOADED, Commands.DRUM_SEQUENCE_CREATED,
                 Commands.PATTERN_UPDATED, Commands.DRUM_SEQUENCE_UPDATED -> refreshControls();

            case Commands.TRANSPORT_STOP -> {
                reset();
                refreshControls();
            }

            case Commands.DRUM_STEP_PARAMETERS_CHANGED -> {
                if (action.getData() instanceof DrumStepParametersEvent event
                        && event.getDrumIndex() == selectedPadIndex) {
                    refreshControls();
                }
            }

            case Commands.STEP_UPDATED, Commands.DRUM_STEP_UPDATED -> {
                // Handle step updates coming from sequencer
                if (action.getData() instanceof DrumStepUpdateEvent event
                        && event.getDrumIndex() == selectedPadIndex) {
                    updateStep(event.getOldStep(), event.getNewStep());
                }
            }

            case Commands.DRUM_PAD_SELECTED -> handleDrumPadSelected(action);

        }
    }

    private void handleDrumPadSelected(Command action) {
        // Only respond to events from other panels to avoid loops
        if (action.getData() instanceof DrumPadSelectionEvent event && action.getSender() != this) {
            int newSelection = event.getNewSelection();

            // Check if index is valid and different
            if (newSelection != selectedPadIndex
                    && newSelection >= 0
                    && newSelection < drumPadPanel.getButtonCount()) {

                // Skip heavy operations - just update necessary state
                selectedPadIndex = newSelection;

                // Update UI without triggering further events
                SwingUtilities.invokeLater(() -> {
                    drumPadPanel.selectDrumPadNoCallback(newSelection);
                    refreshControls();
                });
            }
        }
    }

    private void handleThemeChange() {
        // Remember which pad was selected
        int currentSelection = selectedPadIndex;

        // Find the centering panel that contains our drumPadPanel
        Container parent = drumPadPanel.getParent();

        if (parent != null) {
            // Remove the old panel
            parent.remove(drumPadPanel);

            // Create a new drum pad panel with updated theme colors
            drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

            // Add it back to the layout
            parent.add(drumPadPanel);

            // Update UI
            parent.revalidate();
            parent.repaint();

            // Restore selection state
            if (currentSelection >= 0) {
                drumPadPanel.selectDrumPad(currentSelection);
            }

            logger.info("DrumParamsSequencerPanel: Recreated drum pad panel after theme change");
        }
    }

    // Add helper method to set enabled state of all trigger buttons
    void setAccentButtonsEnabled(boolean enabled) {
        for (AccentButton button : accentButtons) {
            button.setEnabled(enabled);
            // When disabling, also clear toggle and highlight state
            if (!enabled) {
                button.setSelected(false);
                button.setHighlighted(false);
            }
            button.repaint();
        }
    }

    void setTriggerButtonsEnabled(boolean enabled) {
        for (TriggerButton button : selectorButtons) {
            button.setEnabled(enabled);
            // When disabling, also clear toggle and highlight state
            if (!enabled) {
                button.setSelected(false);
                button.setHighlighted(false);
            }

            button.repaint();
        }
    }

    protected JPanel createAccentPanel(int index) {
        AccentButton accentButton = new AccentButton(Integer.toString(index + 1));
        accentButton.setName("AccentButton-" + index);
        accentButton.setToolTipText("Step " + (index + 1));
        accentButton.setEnabled(true);
        accentButton.setPreferredSize(new Dimension(20, 20));
        accentButton.setMaximumSize(new Dimension(20, 20));
        accentButton.addActionListener(e -> toggleAccentForActivePad((index)));
        accentButtons.add(accentButton);

        // Center the button horizontally
        JPanel accentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        accentPanel.add(accentButton);
        return accentPanel;
    }

    int calculatePreviousStep(int currentStep) {
        if (currentStep <= 0) {
            return sequencer.getPatternLength(selectedPadIndex) - 1;
        }
        return currentStep - 1;
    }

    void updateStep(int oldStep, int newStep) {
        // First reset all buttons to ensure clean state
        for (TriggerButton button : selectorButtons) {
            button.setHighlighted(false);
        }

        for (AccentButton button : accentButtons) {
            button.setHighlighted(false);
        }

        // Then highlight only the current step
        if (newStep >= 0 && newStep < selectorButtons.size()) {
            TriggerButton newButton = selectorButtons.get(newStep);
            newButton.setHighlighted(true);
            newButton.repaint();

            // Highlight accent button too
            if (newStep < accentButtons.size()) {
                AccentButton accentButton = accentButtons.get(newStep);
                accentButton.setHighlighted(true);
                accentButton.repaint();
            }
        }
    }

    void createUI() {
        // Clear any existing components first to prevent duplication
        removeAll();

        // Create required panels BEFORE initialize() is called
        navigationPanel = new DrumSequenceNavigationPanel(sequencer);
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

        // Clear existing collections to avoid duplicates
        accentButtons.clear();
        selectorButtons.clear();

        // clearDials();
        // REDUCED: from 5,5 to 2,2
        setLayout(new BorderLayout(2, 2));
        UIHelper.setPanelBorder(this);

        // Create west panel to hold navigation
        JPanel westPanel = new JPanel(new BorderLayout(2, 2));

        // Create east panel for sound parameters
        JPanel eastPanel = new JPanel(new BorderLayout(2, 2));
        eastPanel.add(new SoundParametersPanel(), BorderLayout.NORTH);

        // Create top panel to hold west, center and east panels
        JPanel topPanel = new JPanel(new BorderLayout(2, 2));

        // Navigation panel goes NORTH-WEST
        UIHelper.addSafely(westPanel, navigationPanel, BorderLayout.NORTH);

        // Add panels to the top panel
        UIHelper.addSafely(topPanel, westPanel, BorderLayout.EAST);
        UIHelper.addSafely(topPanel, eastPanel, BorderLayout.WEST);

        // Add top panel to main layout
        UIHelper.addSafely(this, topPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 2, 0));
        // REDUCED: from 10,10,10,10 to 5,5,5,5
        sequencePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            UIHelper.addSafely(sequencePanel, columnPanel);
        }

        // Create a panel to hold both the sequence panel and drum buttons
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Add sequence panel directly to CENTER
        UIHelper.addSafely(centerPanel, sequencePanel, BorderLayout.CENTER);

        // Create drum pad panel with callback
        drumPadPanel = new DrumButtonsPanel(sequencer, this::handleDrumPadSelected);

        // Create a panel for the drum section (drum buttons only)
        JPanel drumSection = new JPanel(new BorderLayout(2, 2));

        // Create and add mute sequencer panel
        muteSequencerPanel = new MuteSequencerPanel(sequencer);
        drumSection.add(new OffsetSequencerPanel((sequencer)));
        drumSection.add(muteSequencerPanel, BorderLayout.NORTH);
        drumSection.add(drumPadPanel, BorderLayout.CENTER);

        UIHelper.addSafely(centerPanel, drumSection, BorderLayout.SOUTH);

        UIHelper.addSafely(this, centerPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout(2, 2));
        sequenceParamsPanel = new DrumSequencerParametersPanel(sequencer);
        statusPanel.add(sequenceParamsPanel, BorderLayout.WEST);
        statusPanel.add(sequenceParamsPanel, BorderLayout.WEST);

        maxLengthPanel = new DrumSequencerMaxLengthPanel(sequencer);
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        rightPanel.add(maxLengthPanel);
        rightPanel.add(maxLengthPanel);

        generatorPanel = new DrumSequenceGeneratorPanel(sequencer);
        rightPanel.add(generatorPanel);

        swingPanel = new DrumSequencerSwingPanel(sequencer);
        rightPanel.add(swingPanel);

        statusPanel.add(rightPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        bottomPanel.add(new OffsetSequencerPanel(sequencer), BorderLayout.NORTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add key listener for Escape key to return to DrumSequencer panel
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // Navigate to DrumSequencer tab
                    MainPanel mainPanel = findMainPanel();
                    if (mainPanel != null) {
                        // The index 0 is for "Drum" tab (DrumSequencerPanel)
                        mainPanel.setSelectedTab(0);
                    }
                }
            }
        });

        // Make the panel focusable to receive key events
        setFocusable(true);        // When the panel gains focus or becomes visible, request focus
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                requestFocusInWindow();
            }
        });

        // Request focus when the panel becomes visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                requestFocusInWindow();
            }
        });

        // Add mouse listener for right-click context menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (contextMenuHandler != null) {
                        // Show context menu at mouse position with selected pad
                        contextMenuHandler.showContextMenu(e.getComponent(), e.getX(), e.getY(), selectedPadIndex, -1);
                    }
                }
            }
        });

        // Initialize the context menu handler after the component hierarchy is established
        SwingUtilities.invokeLater(() -> {
            // Find the DrumSequencerPanel that this PolyPanel might be contained within
            Container parent = getParent();
            while (parent != null && !(parent instanceof DrumSequencerGridPanel)) {
                parent = parent.getParent();
            }

            // Initialize the context menu handler
            if (parent instanceof DrumSequencerGridPanel) {
                // If we found a DrumSequencerPanel parent, use it
                contextMenuHandler = new DrumSequencerGridPanelContextHandler(sequencer, (DrumSequencerGridPanel) parent);
                logger.info("Created context menu handler with DrumSequencerPanel parent");
            } else {
                // Otherwise create with a null parent (will have limited functionality)
                contextMenuHandler = new DrumSequencerGridPanelContextHandler(sequencer, null);
                logger.info("Created context menu handler with null parent");
            }
        });
    }

    /**
     * Initialize the context menu handler for right-click functionality.
     * This needs to be called after the component hierarchy is established.
     */
    private void initializeContextMenuHandler() {
        // Find the DrumSequencerPanel that this PolyPanel might be contained within
        Container parent = getParent();
        while (parent != null && !(parent instanceof DrumSequencerGridPanel)) {
            parent = parent.getParent();
        }

        // Initialize the context menu handler
        if (parent instanceof DrumSequencerGridPanel) {
            // If we found a DrumSequencerPanel parent, use it
            contextMenuHandler = new com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler(sequencer, (DrumSequencerGridPanel) parent);
            logger.info("Created context menu handler with DrumSequencerPanel parent");
        } else {
            // Otherwise create with a null parent (will have limited functionality)
            contextMenuHandler = new com.angrysurfer.beats.widget.DrumSequencerGridPanelContextHandler(sequencer, null);
            logger.info("Created context menu handler with null parent");
        }
    }

    // Replace the handleDrumPadSelected method with this version
    private void handleDrumPadSelected(int padIndex) {
        // Don't process if already selected or we're in the middle of handling a
        // selection
        if (padIndex == selectedPadIndex || isHandlingSelection) {
            Player player = sequencer.getPlayers()[padIndex];
            player.noteOn(player.getRootNote(), 100, 100);
            return;
        }

        try {
            // Set flag to prevent recursive calls
            isHandlingSelection = true;

            selectedPadIndex = padIndex;
            sequencer.setSelectedPadIndex(padIndex);

            // Get the player for this pad index
            if (padIndex >= 0 && padIndex < sequencer.getPlayers().length) {
                Player player = sequencer.getPlayers()[padIndex];

                if (player != null && player.getInstrument() != null) {
                    CommandBus.getInstance().publish(Commands.STATUS_UPDATE, this, new StatusUpdate("Selected pad: " + player.getName()));
                    CommandBus.getInstance().publish(Commands.PLAYER_SELECTION_EVENT, this, player);
                }
            }

            // Update UI in a specific order
            setAccentButtonsEnabled(true);
            setTriggerButtonsEnabled(true);
            refreshControls();

            // Publish drum pad event LAST and only if we're handling a direct user
            // selection
            CommandBus.getInstance().publish(Commands.DRUM_PAD_SELECTED, this, new DrumPadSelectionEvent(-1, padIndex));
        } finally {
            // Always clear the flag when done
            isHandlingSelection = false;
        }
    }

    void refreshAccentButtonsForPad(int padIndex) {
        // Handle no selection case
        if (padIndex < 0) {
            for (AccentButton button : accentButtons) {
                button.setSelected(false);
                button.setHighlighted(false);
                button.setEnabled(false);
                button.repaint();
            }
            return;
        }

        // A pad is selected, so enable all buttons
        setAccentButtonsEnabled(true);

        // Update each button's state
        for (int i = 0; i < accentButtons.size(); i++) {
            AccentButton button = accentButtons.get(i);

            // Set selected state based on pattern
            boolean isActive = sequencer.getSequenceData().isStepAccented(padIndex, i);
            button.setSelected(isActive);

            // Highlight current step if playing
            if (sequencer.isPlaying()) {
                int[] steps = sequencer.getSequenceData().getCurrentStep();
                if (padIndex < steps.length) {
                    button.setHighlighted(i == steps[padIndex]);
                }
            } else {
                button.setHighlighted(false);
            }

            // Force repaint
            button.repaint();
        }

    }

    void refreshTriggerButtonsForPad(int padIndex) {
        // Handle no selection case
        if (padIndex < 0) {
            for (TriggerButton button : selectorButtons) {
                button.setSelected(false);
                button.setHighlighted(false);
                button.setEnabled(false);
                button.repaint();
            }
            return;
        }

        // A pad is selected, so enable all buttons
        setTriggerButtonsEnabled(true);

        // Update each button's state
        for (int i = 0; i < selectorButtons.size(); i++) {
            TriggerButton button = selectorButtons.get(i);

            // Set selected state based on pattern
            boolean isActive = sequencer.isStepActive(padIndex, i);
            button.setSelected(isActive);

            // Highlight current step if playing
            if (sequencer.isPlaying()) {
                int[] steps = sequencer.getSequenceData().getCurrentStep();
                if (padIndex < steps.length) {
                    button.setHighlighted(i == steps[padIndex]);
                }
            } else {
                button.setHighlighted(false);
            }

            // Force repaint
            button.repaint();
        }

    }

    JPanel createOffsetPanel(int index) {
        JComboBox<Integer> combo = new JComboBox<>();
        //combo.setName("TriggerButton-" + index);
        //combo.setToolTipText("Step " + (index + 1));
        //combo.setEnabled(selectedPadIndex >= 0);
        //combo.addActionListener(e -> toggleStepForActivePad(index));

        ///selectorButtons.add(triggerButton);

        // Center the button horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(combo);
        return buttonPanel;
    }

    JPanel createTriggerPanel(int index) {
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));
        triggerButton.setEnabled(selectedPadIndex >= 0);
        triggerButton.addActionListener(e -> toggleStepForActivePad(index));
        triggerButton.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        selectorButtons.add(triggerButton);

        // Center the button horizontally
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel.add(triggerButton);
        return buttonPanel;
    }

    void toggleAccentForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0) {
            sequencer.toggleAccent(selectedPadIndex, stepIndex);
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, sequencer);
        }
    }

    void toggleStepForActivePad(int stepIndex) {
        if (selectedPadIndex >= 0) {
            // Toggle the step in the sequencer
            boolean wasActive = sequencer.isStepActive(selectedPadIndex, stepIndex);
            sequencer.toggleStep(selectedPadIndex, stepIndex);

            // Verify the toggle took effect
            boolean isNowActive = sequencer.isStepActive(selectedPadIndex, stepIndex);
            boolean hasAccent = sequencer.getSequenceData().isStepAccented(selectedPadIndex, stepIndex);
            if (hasAccent && wasActive)
                sequencer.toggleAccent(selectedPadIndex, stepIndex);

            if (wasActive == isNowActive) {
                logger.warn("Toggle step failed for pad {}, step {}", selectedPadIndex, stepIndex);
            }

            // Update the visual state of the button
            TriggerButton triggerButton = selectorButtons.get(stepIndex);
            triggerButton.setSelected(isNowActive);
            triggerButton.repaint();

            // Notify the system of the sequence update
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, sequencer);
        }
    }

    /**
     * Reset the sequencer state
     */
    public void reset() {
        // Clear all highlighting
        for (TriggerButton button : selectorButtons) {
            if (button != null) {
                button.setHighlighted(false);
                button.repaint();
            }
        }

        for (AccentButton button : accentButtons) {
            if (button != null) {
                button.setHighlighted(false);
                button.repaint();
            }
        }

    }

    /**
     * Get the maximum pattern length
     */
    public int getPatternLength() {
        return sequencer.getPatternLength(selectedPadIndex);
    }

    // Helper method to find the MainPanel ancestor
    MainPanel findMainPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof MainPanel) {
                return (MainPanel) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Create a dial with default row type (using the index as the row type)
     *
     * @param index        The dial index
     * @param minimum      The minimum value
     * @param maximum      The maximum value
     * @param defaultValue The default value
     * @return A configured Dial
     */
    Dial createDial(int index, int minimum, int maximum, int defaultValue) {
        // For backwards compatibility, use index as the rowType by default
        return createDial(index, minimum, maximum, defaultValue, index);
    }

    /**
     * Create a dial with explicit row type specification
     *
     * @param index        The dial index (typically the column index)
     * @param minimum      The minimum value
     * @param maximum      The maximum value
     * @param defaultValue The default value
     * @param rowType      The row type to use for knob coloring (0=velocity, 1=decay, etc.)
     * @return A configured Dial
     */
    Dial createDial(int index, int minimum, int maximum, int defaultValue, int rowType) {
        // Create basic dial
        Dial dial = new Dial();
        dial.setMinimum(minimum);
        dial.setMaximum(maximum);
        dial.setValue(defaultValue);

        dial.setKnobColor(UIHelper.getDialColor(getKnobLabel(rowType).toLowerCase())); // Set knob color
        dial.setName(getKnobLabel(rowType) + "-" + index);
        return dial;
    }


}
