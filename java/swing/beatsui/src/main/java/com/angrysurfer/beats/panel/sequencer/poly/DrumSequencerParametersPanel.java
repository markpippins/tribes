package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing sequence parameters controls for a drum sequencer
 */
public class DrumSequencerParametersPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequencerParametersPanel.class);
    // Reference to the sequencer
    private final DrumSequencer sequencer;
    // UI components
    private JSpinner lastStepSpinner;
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton loopToggleButton;
    // Flag to prevent recursive events
    private boolean updatingControls = false;

    /**
     * Creates a new Sequence Parameters panel
     */    public DrumSequencerParametersPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;

        UIHelper.setWidgetPanelBorder(this, "Sequence Parameters");
        
        // Add mouse wheel listener to the entire panel
        addMouseWheelListener(this::handleMouseWheelEvent);

        initializeComponents();
    }

    /**
     * Initialize all UI components
     */
    private void initializeComponents() {
        // Change from FlowLayout to BoxLayout for better control
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // Create a panel to hold the main controls with FlowLayout
        // REDUCED: from 10,5 to 2,1
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));

        // Last Step spinner
        // REDUCED: from 5,0 to 2,0
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-sequencer.getMaxSteps()
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(
                sequencer.getDefaultPatternLength(), 1, sequencer.getMaxPatternLength(), 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        lastStepSpinner.setToolTipText("Set the last step of the pattern (1-" + sequencer.getMaxPatternLength() + ")");
        lastStepSpinner.addChangeListener(e -> {
            if (updatingControls) return;

            int lastStep = (Integer) lastStepSpinner.getValue();
            int selectedPadIndex = sequencer.getSelectedPadIndex();

            logger.info("Setting last step to {} for drum {}", lastStep, selectedPadIndex);

            // Set pattern length directly in the sequencer
            sequencer.setPatternLength(selectedPadIndex, lastStep);

            // Publish command to update UI
            CommandBus.getInstance().publish(
                    Commands.DRUM_STEP_BUTTONS_UPDATE_REQUESTED,
                    this,
                    selectedPadIndex
            );
        });
        lastStepPanel.add(lastStepSpinner);

        // Direction combo
        // REDUCED: from 5,0 to 2,0
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH + 15, UIHelper.CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
        directionCombo.addActionListener(e -> {
            if (updatingControls) return;

            int selectedIndex = directionCombo.getSelectedIndex();
            Direction direction = Direction.FORWARD; // Default

            switch (selectedIndex) {
                case 0 -> direction = Direction.FORWARD;
                case 1 -> direction = Direction.BACKWARD;
                case 2 -> direction = Direction.BOUNCE;
                case 3 -> direction = Direction.RANDOM;
            }

            int selectedPadIndex = sequencer.getSelectedPadIndex();
            logger.info("Setting direction to {} for drum {}", direction, selectedPadIndex);

            // Set direction directly in the sequencer
            sequencer.setDirection(selectedPadIndex, direction);
        });
        directionPanel.add(directionCombo);

        // Timing division combo
        // REDUCED: from 5,0 to 2,0
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH + 15, UIHelper.CONTROL_HEIGHT));
        timingCombo.addActionListener(e -> {
            if (updatingControls) return;

            TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
            if (division != null) {
                int selectedPadIndex = sequencer.getSelectedPadIndex();
                logger.info("Setting timing to {} for drum {}", division, selectedPadIndex);

                // Set timing directly in the sequencer
                sequencer.setTimingDivision(selectedPadIndex, division);
            }
        });
        timingPanel.add(timingCombo);

        // Loop checkbox
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(24, 24));
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2));
        loopToggleButton.addActionListener(e -> {
            if (updatingControls) return;

            boolean loop = loopToggleButton.isSelected();
            int selectedPadIndex = sequencer.getSelectedPadIndex();
            logger.info("Setting loop to {} for drum {}", loop, selectedPadIndex);

            // Set looping directly in the sequencer
            sequencer.setLooping(selectedPadIndex, loop);
        });

        // Create rotation panel for push/pull buttons
        // REDUCED: from 5,0 to 2,0
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        // Push forward button
        JButton pushForwardButton = new JButton("⟶");
        pushForwardButton.setToolTipText("Push pattern forward (right)");
        pushForwardButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, 24));
        pushForwardButton.setMargin(new Insets(2, 2, 2, 2));
        pushForwardButton.addActionListener(e -> {
            sequencer.pushForward();

            // Publish command to refresh grid UI
            CommandBus.getInstance().publish(
                    Commands.DRUM_GRID_REFRESH_REQUESTED,
                    this,
                    null
            );
        });

        // Pull backward button
        JButton pullBackwardButton = new JButton("⟵");
        pullBackwardButton.setToolTipText("Pull pattern backward (left)");
        pullBackwardButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, 24));
        pullBackwardButton.setMargin(new Insets(2, 2, 2, 2));
        pullBackwardButton.addActionListener(e -> {
            sequencer.pullBackward();

            // Publish command to refresh grid UI
            CommandBus.getInstance().publish(
                    Commands.DRUM_GRID_REFRESH_REQUESTED,
                    this,
                    null
            );
        });

        // Add buttons to rotation panel
        rotationPanel.add(pullBackwardButton);
        rotationPanel.add(pushForwardButton);

        // Add components to the controls panel in desired order
        controlsPanel.add(timingPanel);
        controlsPanel.add(directionPanel);
        controlsPanel.add(loopToggleButton);
        controlsPanel.add(lastStepPanel);
        controlsPanel.add(rotationPanel);

        // Create the clear button
        JButton clearPatternButton = new JButton("🗑️");
        clearPatternButton.setToolTipText("Clear the pattern for this drum");
        clearPatternButton.setPreferredSize(new Dimension(24, 24));
        clearPatternButton.setMargin(new Insets(2, 2, 2, 2));
        clearPatternButton.addActionListener(e -> {
            int selectedPadIndex = sequencer.getSelectedPadIndex();

            // Publish command to clear row
            CommandBus.getInstance().publish(
                    Commands.DRUM_PATTERN_CLEAR_REQUESTED,
                    this,
                    selectedPadIndex
            );
        });

        // Add the main controls panel to the left
        add(controlsPanel);

        // Add horizontal glue to push clear button to right
        add(Box.createHorizontalGlue());

        // Create right-side panel for clear button with some padding
        // REDUCED: from 10,5 to 2,1
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 1));
        rightPanel.add(clearPatternButton);

        // Add right panel
        add(rightPanel);
    }

    /**
     * Update controls to match the selected drum's parameters
     */
    public void updateControls(int selectedDrumIndex) {
        // Set flag to prevent event recursion
        updatingControls = true;

        try {
            // Get values for the selected drum
            int length = sequencer.getPatternLength(selectedDrumIndex);
            Direction dir = sequencer.getDirection(selectedDrumIndex);
            TimingDivision timing = sequencer.getTimingDivision(selectedDrumIndex);
            boolean isLooping = sequencer.isLooping(selectedDrumIndex);

            // Update UI components
            lastStepSpinner.setValue(length);

            switch (dir) {
                case FORWARD -> directionCombo.setSelectedIndex(0);
                case BACKWARD -> directionCombo.setSelectedIndex(1);
                case BOUNCE -> directionCombo.setSelectedIndex(2);
                case RANDOM -> directionCombo.setSelectedIndex(3);
            }

            timingCombo.setSelectedItem(timing);
            loopToggleButton.setSelected(isLooping);

            // Update last step spinner's maximum value
            SpinnerNumberModel model = (SpinnerNumberModel) lastStepSpinner.getModel();
            model.setMaximum(sequencer.getMaxPatternLength());

            // Repaint components
            lastStepSpinner.repaint();
            directionCombo.repaint();
            timingCombo.repaint();
            loopToggleButton.repaint();
        } finally {
            // Clear flag after update
            updatingControls = false;
        }
    }

    /**
     * Handles mouse wheel events for the panel's components
     *
     * @param e The mouse wheel event
     */
    private void handleMouseWheelEvent(java.awt.event.MouseWheelEvent e) {
        // Skip if we're in the middle of updating controls
        if (updatingControls) {
            return;
        }
        
        // Determine scroll direction (-1 for up, 1 for down)
        int scrollDirection = e.getWheelRotation() > 0 ? -1 : 1;
        
        // Get the current focused component or use lastStepSpinner as default
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        
        // Adjust lastStepSpinner if focused or if no specific component has focus
        if (focusOwner == lastStepSpinner || !(focusOwner instanceof JComponent)) {
            // Get current value
            int currentValue = (Integer) lastStepSpinner.getValue();
            int newValue = currentValue + scrollDirection;
            
            // Get spinner model to check bounds
            SpinnerNumberModel model = (SpinnerNumberModel) lastStepSpinner.getModel();
            int min = (Integer) model.getMinimum();
            int max = (Integer) model.getMaximum();
            
            // Ensure value within bounds
            newValue = Math.max(min, Math.min(newValue, max));
            
            // Update if value changed
            if (newValue != currentValue) {
                lastStepSpinner.setValue(newValue);
                // The change listener will handle passing the value to the sequencer
            }
        }
        // Handle direction combo if focused (future enhancement)
        else if (focusOwner == directionCombo) {
            int index = directionCombo.getSelectedIndex();
            int newIndex = index + scrollDirection;
            newIndex = Math.max(0, Math.min(newIndex, directionCombo.getItemCount() - 1));
            
            if (newIndex != index) {
                directionCombo.setSelectedIndex(newIndex);
                // The action listener will handle passing the direction to the sequencer
            }
        }
        // Handle timing combo if focused (future enhancement)
        else if (focusOwner == timingCombo) {
            int index = timingCombo.getSelectedIndex();
            int newIndex = index + scrollDirection;
            newIndex = Math.max(0, Math.min(newIndex, timingCombo.getItemCount() - 1));
            
            if (newIndex != index) {
                timingCombo.setSelectedIndex(newIndex);
                // The action listener will handle passing the timing to the sequencer
            }
        }
    }
}
