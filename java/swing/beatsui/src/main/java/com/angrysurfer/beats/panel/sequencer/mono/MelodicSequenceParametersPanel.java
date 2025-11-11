package com.angrysurfer.beats.panel.sequencer.mono;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.Direction;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.sequencer.TimingDivision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing sequence parameters for melodic sequencers
 */
public class MelodicSequenceParametersPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceParametersPanel.class);

    // UI Controls
    private JComboBox<String> directionCombo;
    private JComboBox<TimingDivision> timingCombo;
    private JToggleButton loopToggleButton;
    private JSpinner lastStepSpinner;
    private JComboBox<Integer> followSequenceCombo;

    // Reference to sequencer
    private MelodicSequencer sequencer;

    // Flag to prevent event loops
    private boolean updatingUI = false;

    /**
     * Create a new sequence parameters panel
     *
     * @param sequencer The melodic sequencer to control
     */
    public MelodicSequenceParametersPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        initialize();
    }

    /**
     * Initialize the panel with all controls
     */
    private void initialize() {
        setLayout(new BorderLayout(0, 0)); // No gaps between components
        UIHelper.setWidgetPanelBorder(this, "Sequence Parameters");

        // Reduce spacing in the controls panel
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));

        // Add all controls to the left panel EXCEPT those now in the scale panel
        createLastStepControls(controlsPanel);
        createDirectionControls(controlsPanel);
        createTimingControls(controlsPanel);
        createLoopButton(controlsPanel);
        createRotationControls(controlsPanel);
        createFollowSequenceControls(controlsPanel);
        createFollowHarmonicTiltControls(controlsPanel);
        add(controlsPanel, BorderLayout.WEST);

        // Reduce spacing in the clear button panel
        JPanel clearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        JButton clearButton = createClearButton();
        clearPanel.add(clearButton);

        // Add the clear panel to the EAST position
        add(clearPanel, BorderLayout.EAST);
    }

    private void createFollowHarmonicTiltControls(JPanel controlsPanel) {
        JPanel followPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        JLabel label = new JLabel("Tilt:");

        followSequenceCombo = new JComboBox<>(getOtherSequencerIds());
        followSequenceCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        followSequenceCombo.setToolTipText("Set a master sequencer for Harmonic Tilt");

        // Add the custom renderer
        setupFollowSequenceComboRenderer(followSequenceCombo);

        followSequenceCombo.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.getSequenceData().setFollowTiltSequencerId(-1);
                Integer followId = (Integer) followSequenceCombo.getSelectedItem();
                if (followId > -1) {
                    logger.info("Setting a master sequencer for Harmonic Tilt");
                    sequencer.getSequenceData().setFollowTiltSequencerId(followId);
                }
                CommandBus.getInstance().publish(Commands.SEQUENCER_TILT_FOLLOW_EVENT, sequencer, followId);
            }
        });

        followPanel.add(label);
        followPanel.add(followSequenceCombo);

        controlsPanel.add(followPanel);
    }

    private void createFollowSequenceControls(JPanel parentPanel) {
        JPanel followPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        JLabel label = new JLabel("Note Source:");

        followSequenceCombo = new JComboBox<>(getOtherSequencerIds());
        followSequenceCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        followSequenceCombo.setToolTipText("Set a master sequencer for this one");

        // Add the custom renderer
        setupFollowSequenceComboRenderer(followSequenceCombo);

        followSequenceCombo.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.getSequenceData().setFollowNoteSequencerId(-1);
                Integer followId = (Integer) followSequenceCombo.getSelectedItem();
                if (followId > -1) {
                    logger.info("Setting a master sequencer for this one");
                    sequencer.getSequenceData().setFollowNoteSequencerId(followId);
                }
                CommandBus.getInstance().publish(Commands.SEQUENCER_NOTE_FOLLOW_EVENT, sequencer, followId);
            }
        });

        followPanel.add(label);
        followPanel.add(followSequenceCombo);

        parentPanel.add(followPanel);
    }

    /**
     * Create custom renderer for the follow sequence combo box
     */
    private void setupFollowSequenceComboRenderer(JComboBox<Integer> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {

                // Get the default renderer
                Component component = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                // Cast to label
                JLabel label = (JLabel) component;

                // Format the display based on the value
                if (value != null) {
                    int id = (Integer) value;
                    if (id == -1) {
                        label.setText("None");
                    } else {
                        // Make the sequencer ID 1-based for display
                        label.setText("Melo " + (id + 1));
                    }
                }

                return label;
            }
        });
    }

    /**
     * Get an array of all other sequencer IDs (plus a "none" option)
     *
     * @return Array of sequencer IDs, with -1 representing "none"
     */
    private Integer[] getOtherSequencerIds() {
        // Get the maximum number of melodic sequencers
        int totalSequencers = SequencerConstants.MELODIC_CHANNELS.length;

        // Create array with size totalSequencers + 1 to include the "none" option (-1)
        Integer[] ids = new Integer[totalSequencers + 1];

        // First option is always -1 (meaning "none" or "don't follow")
        ids[0] = -1;

        // Fill the rest with sequencer IDs, skipping our own
        int currentIndex = 1;
        for (int i = 0; i < totalSequencers; i++) {
            // Skip our own sequencer ID
            if (i != sequencer.getId()) {
                ids[currentIndex++] = i;
            }
        }

        // If array is now too long (because we skipped one), create a properly sized one
        if (currentIndex < ids.length) {
            Integer[] trimmed = new Integer[currentIndex];
            System.arraycopy(ids, 0, trimmed, 0, currentIndex);
            return trimmed;
        }

        return ids;
    }

    /**
     * Create last step spinner control
     */
    private void createLastStepControls(JPanel parentPanel) {
        JPanel lastStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0)); // Changed from 2,0 to 1,0
        lastStepPanel.add(new JLabel("Last Step:"));

        // Create spinner model with range 1-16, default 16
        SpinnerNumberModel lastStepModel = new SpinnerNumberModel(16, 1, 64, 1);
        lastStepSpinner = new JSpinner(lastStepModel);
        lastStepSpinner.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH - 5, UIHelper.CONTROL_HEIGHT)); // Reduced
        // width
        lastStepSpinner.setToolTipText("Set the last step for the pattern (1-16)");
        lastStepSpinner.addChangeListener(e -> {
            if (!updatingUI) {
                int lastStep = (Integer) lastStepSpinner.getValue();
                sequencer.getSequenceData().setPatternLength(lastStep);
            }
        });
        lastStepPanel.add(lastStepSpinner);

        parentPanel.add(lastStepPanel);
    }

    /**
     * Create direction combo control
     */
    private void createDirectionControls(JPanel parentPanel) {
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        directionCombo = new JComboBox<>(new String[]{"Forward", "Backward", "Bounce", "Random"});
        directionCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        directionCombo.setToolTipText("Set the playback direction of the pattern");
        directionCombo.addActionListener(e -> {
            if (!updatingUI) {
                int selectedIndex = directionCombo.getSelectedIndex();
                Direction direction = switch (selectedIndex) {
                    case 1 -> Direction.BACKWARD;
                    case 2 -> Direction.BOUNCE;
                    case 3 -> Direction.RANDOM;
                    default -> Direction.FORWARD;
                };
                sequencer.getSequenceData().setDirection(direction);
            }
        });
        directionPanel.add(directionCombo);

        parentPanel.add(directionPanel);
    }

    /**
     * Create timing division combo control
     */
    private void createTimingControls(JPanel parentPanel) {
        JPanel timingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        timingCombo = new JComboBox<>(TimingDivision.getValuesAlphabetically());
        timingCombo.setPreferredSize(new Dimension(UIHelper.LARGE_CONTROL_WIDTH, UIHelper.CONTROL_HEIGHT));
        timingCombo.setToolTipText("Set the timing division for this pattern");
        timingCombo.addActionListener(e -> {
            if (!updatingUI) {
                TimingDivision division = (TimingDivision) timingCombo.getSelectedItem();
                if (division != null) {
                    logger.info("Setting timing division to {}", division);
                    sequencer.getSequenceData().setTimingDivision(division);
                }
            }
        });
        timingPanel.add(timingCombo);

        parentPanel.add(timingPanel);
    }

    /**
     * Create loop toggle button
     */
    private void createLoopButton(JPanel parentPanel) {
        loopToggleButton = new JToggleButton("🔁", true); // Default to looping enabled
        loopToggleButton.setToolTipText("Loop this pattern");
        loopToggleButton.setPreferredSize(new Dimension(24, 24));
        loopToggleButton.setMargin(new Insets(2, 2, 2, 2)); // Reduce internal padding
        loopToggleButton.addActionListener(e -> {
            if (!updatingUI) {
                sequencer.getSequenceData().setLooping(loopToggleButton.isSelected());
                CommandBus.getInstance().publish(Commands.LOOPING_TOGGLE_EVENT, this, sequencer);
            }
        });

        parentPanel.add(loopToggleButton);
    }

    /**
     * Create rotation controls
     */
    private void createRotationControls(JPanel parentPanel) {
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0)); // REDUCED: from 5,0 to 2,0

        // Rotate Left button
        JButton rotateLeftButton = new JButton("⟵");
        rotateLeftButton.setToolTipText("Rotate pattern one step left");
        rotateLeftButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, 24));
        rotateLeftButton.setMargin(new Insets(2, 2, 2, 2));
        rotateLeftButton.addActionListener(e -> {
            sequencer.getSequenceData().rotatePatternLeft();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        // Rotate Right button
        JButton rotateRightButton = new JButton("⟶");
        rotateRightButton.setToolTipText("Rotate pattern one step right");
        rotateRightButton.setPreferredSize(new Dimension(UIHelper.SMALL_CONTROL_WIDTH, 24));
        rotateRightButton.setMargin(new Insets(2, 2, 2, 2));
        rotateRightButton.addActionListener(e -> {
            sequencer.getSequenceData().rotatePatternRight();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        // Add buttons to rotation panel
        rotationPanel.add(rotateLeftButton);
        rotationPanel.add(rotateRightButton);

        parentPanel.add(rotationPanel);
    }

    /**
     * Create clear button
     *
     * @return The created button
     */
    private JButton createClearButton() {
        JButton clearButton = new JButton("🗑️");
        clearButton.setToolTipText("Clear pattern");
        clearButton.setPreferredSize(new Dimension(24, 24));
        clearButton.setMargin(new Insets(2, 2, 2, 2));
        clearButton.addActionListener(e -> {
            sequencer.getSequenceData().clearPattern();
            updateUI(sequencer);
            // Notify that the pattern was updated
            CommandBus.getInstance().publish(
                    Commands.PATTERN_UPDATED,
                    sequencer,
                    null);
        });

        return clearButton;
    }

    /**
     * Update the panel UI to reflect sequencer state
     *
     * @param sequencer The sequencer to sync with
     */
    public void updateUI(MelodicSequencer sequencer) {
        if (sequencer == null) {
            return;
        }

        // Store the current sequencer reference
        this.sequencer = sequencer;

        // Set flag to prevent event loops
        updatingUI = true;

        try {
            loopToggleButton.setToolTipText(sequencer.getSequenceData().isLooping() ? "Looping on." : "Looping off.");

            // Update timing division
            TimingDivision timingDivision = sequencer.getSequenceData().getTimingDivision();
            if (timingDivision != null) {
                timingCombo.setSelectedItem(timingDivision);
            }

            // Update direction
            Direction direction = sequencer.getSequenceData().getDirection();
            if (direction != null) {
                switch (direction) {
                    case FORWARD -> directionCombo.setSelectedIndex(0);
                    case BACKWARD -> directionCombo.setSelectedIndex(1);
                    case BOUNCE -> directionCombo.setSelectedIndex(2);
                    case RANDOM -> directionCombo.setSelectedIndex(3);
                }
            }

            // Update loop state
            loopToggleButton.setSelected(sequencer.getSequenceData().isLooping());
            loopToggleButton.setToolTipText(sequencer.getSequenceData().isLooping() ? "Looping on." : "Looping off.");

            // Update last step
            lastStepSpinner.setValue(sequencer.getSequenceData().getPatternLength());

        } finally {
            // Reset flag after updates
            updatingUI = false;
        }
    }

    /**
     * Register for command bus events
     */
//    private void registerForEvents() {
//        // Register only for events we actually handle
//        CommandBus.getInstance().register(this, new String[] {
//            Commands.PATTERN_UPDATED,
//            Commands.MELODIC_SEQUENCE_UPDATED,
//            Commands.MELODIC_SEQUENCE_LOADED,
//            Commands.MELODIC_SEQUENCE_CREATED,
//            Commands.PLAYER_UPDATED,
//            Commands.PLAYER_ACTIVATED
//        });
//
//        logger.debug("MelodicSequenceParametersPanel registered for specific events");
//    }
}
