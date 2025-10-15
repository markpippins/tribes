package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ControlPanel extends LivePanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class.getName());
    private static final int BUTTON_SIZE = 30;
    private static final int PANEL_HEIGHT = 100; // Increased from 90 to 100px
    private static final String PLAYER_PANEL = "PLAYER_PANEL";
    private static final String SESSION_PANEL = "SESSION_PANEL";
    // Dials
    private Dial levelDial;
    private NoteSelectionDial noteSelectionDial;
    private Dial swingDial;
    private Dial probabilityDial;
    private Dial velocityMinDial;
    private Dial velocityMaxDial;
    private Dial randomDial;
    private Dial panDial;
    private Dial sparseDial;
    private JButton nextScaleButton;
    private JButton prevScaleButton;
    // Current active player
    private Player activePlayer;
    // Octave buttons
    private JButton octaveUpButton;
    private JButton octaveDownButton;
    // Helper flag to prevent feedback when programmatically updating controls
    private boolean listenersEnabled = true;

    public ControlPanel() {
        // Use BorderLayout as the main layout for better component positioning
        super();
        setLayout(new BorderLayout());

        // Set fixed height - updated to match new PANEL_HEIGHT
        setMinimumSize(new Dimension(getMinimumSize().width, PANEL_HEIGHT));
        setPreferredSize(new Dimension(getPreferredSize().width, PANEL_HEIGHT));
        setMaximumSize(new Dimension(Short.MAX_VALUE, PANEL_HEIGHT));

        // Create a wrapper panel for the left-side controls using FlowLayout
        JPanel leftControlsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftControlsWrapper.setOpaque(false); // Make transparent to show parent background

        // Create the MiniLaunchPanel that will be positioned on the right
        MiniLaunchPanel launchPanel = new MiniLaunchPanel();

        // Add all standard components to the left wrapper
        initComponents(leftControlsWrapper);

        // Create a vertical centering panel with BoxLayout for left controls
        JPanel centeringPanel = new JPanel();
        centeringPanel.setLayout(new BoxLayout(centeringPanel, BoxLayout.Y_AXIS));
        centeringPanel.setOpaque(false);

        // Add vertical glue at top for centering
        centeringPanel.add(Box.createVerticalGlue());
        // Add the controls wrapper
        centeringPanel.add(leftControlsWrapper);
        // Add vertical glue at bottom for centering
        centeringPanel.add(Box.createVerticalGlue());

        // Create a right panel with centered MiniLaunchPanel - ensure vertical centering
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        // Add equal glue above and below to ensure perfect vertical centering
        rightPanel.add(Box.createVerticalGlue());

        // Center the launch panel horizontally within its container
        JPanel launchCenterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        launchCenterPanel.setOpaque(false);
        launchCenterPanel.add(launchPanel);

        rightPanel.add(launchCenterPanel);
        rightPanel.add(Box.createVerticalGlue());

        // Add both panels to the main panel
        add(centeringPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        setupCommandBusListener();
    }

    @Override
    public void handlePlayerActivated() {

    }

    @Override
    public void handlePlayerUpdated() {

    }

    private void initComponents(JPanel controlsWrapper) {
        // Add vertical adjust panels
        JPanel presetPanel = UIHelper.createVerticalAdjustPanel("Preset", "↑", "↓", Commands.PRESET_UP,
                Commands.PRESET_DOWN);
        presetPanel.setName(PLAYER_PANEL + "_PRESET");
        controlsWrapper.add(presetPanel);

        JPanel offsetPanel = UIHelper.createVerticalAdjustPanel("Offset", "↑", "↓", Commands.TRANSPOSE_UP,
                Commands.TRANSPOSE_DOWN);
        offsetPanel.setName(SESSION_PANEL + "_OFFSET");
        controlsWrapper.add(offsetPanel);

        controlsWrapper.add(createScaleAdjustPanel());

        // Add octave panel
        JPanel octavePanel = createOctavePanel();
        octavePanel.setName(PLAYER_PANEL + "_OCTAVE");
        controlsWrapper.add(octavePanel);

        // Create and add dials
        createAndAddDials(controlsWrapper);

        // Initially disable dials
        disableDials();

        // Set up control change listeners
        setupControlChangeListeners();
    }

    /**
     * Create and add dials to the control panel
     */
    private void createAndAddDials(JPanel controlsWrapper) {
        // Create note selection dial with special sizing
        noteSelectionDial = new NoteSelectionDial();
        // Make slightly smaller to fit better in the panel - from 90x90 to 70x70
        var noteDialSize = new Dimension(70, 70);
        noteSelectionDial.setPreferredSize(noteDialSize);
        noteSelectionDial.setMinimumSize(noteDialSize);
        noteSelectionDial.setMaximumSize(noteDialSize);
        noteSelectionDial.setCommand(Commands.NEW_VALUE_NOTE);

        // Regular dials
        levelDial = createDial("level", 100, 0, 127, 1);
        levelDial.setCommand(Commands.NEW_VALUE_LEVEL);

        panDial = createDial("pan", 64, 0, 127, 1);
        panDial.setCommand(Commands.NEW_VALUE_PAN);
        panDial.setKnobColor(UIHelper.mutedRed);
        panDial.setGradientStartColor(panDial.getKnobColor().brighter());
        panDial.setGradientEndColor(panDial.getKnobColor().darker());

        velocityMinDial = createDial("minVelocity", 64, 0, 127, 1);
        velocityMinDial.setCommand(Commands.NEW_VALUE_VELOCITY_MIN);
        velocityMinDial.setKnobColor(UIHelper.warmGray);
        velocityMinDial.setGradientStartColor(velocityMinDial.getKnobColor().brighter());
        velocityMinDial.setGradientEndColor(velocityMinDial.getKnobColor().darker());

        velocityMaxDial = createDial("maxVelocity", 127, 0, 127, 1);
        velocityMaxDial.setCommand(Commands.NEW_VALUE_VELOCITY_MAX);
        velocityMaxDial.setKnobColor(UIHelper.warmGray);
        velocityMaxDial.setGradientStartColor(velocityMaxDial.getKnobColor().brighter());
        velocityMaxDial.setGradientEndColor(velocityMaxDial.getKnobColor().darker());

        swingDial = createDial("swing", 50, 0, 100, 1);
        swingDial.setCommand(Commands.NEW_VALUE_SWING);
        swingDial.setKnobColor(UIHelper.slateGray);
        swingDial.setGradientStartColor(swingDial.getKnobColor().brighter());
        swingDial.setGradientEndColor(swingDial.getKnobColor().darker());

        probabilityDial = createDial("probability", 100, 0, 100, 1);
        probabilityDial.setCommand(Commands.NEW_VALUE_PROBABILITY);
        probabilityDial.setKnobColor(UIHelper.deepNavy);
        probabilityDial.setGradientStartColor(probabilityDial.getKnobColor().brighter());
        probabilityDial.setGradientEndColor(probabilityDial.getKnobColor().darker());

        randomDial = createDial("random", 0, 0, 100, 1);
        randomDial.setCommand(Commands.NEW_VALUE_RANDOM);
        randomDial.setKnobColor(UIHelper.mutedOlive);
        randomDial.setGradientStartColor(randomDial.getKnobColor().brighter());
        randomDial.setGradientEndColor(randomDial.getKnobColor().darker());

        sparseDial = createDial("sparse", 0, 0, 100, 1);
        sparseDial.setCommand(Commands.NEW_VALUE_SPARSE);
        sparseDial.setKnobColor(UIHelper.deepTeal);
        sparseDial.setGradientStartColor(sparseDial.getKnobColor().brighter());
        sparseDial.setGradientEndColor(sparseDial.getKnobColor().darker());

        // Add note selection dial with special panel width (80px vs 60px)
        controlsWrapper.add(noteSelectionDial);

        // Add regular dials with standard panel width (60px)
        controlsWrapper.add(createLabeledControl("Level", levelDial));
        controlsWrapper.add(createLabeledControl("Pan", panDial));
        controlsWrapper.add(createLabeledControl("Min Vel", velocityMinDial));
        controlsWrapper.add(createLabeledControl("Max Vel", velocityMaxDial));
        controlsWrapper.add(createLabeledControl("Swing", swingDial));
        controlsWrapper.add(createLabeledControl("Probability", probabilityDial));
        controlsWrapper.add(createLabeledControl("Random", randomDial));
        controlsWrapper.add(createLabeledControl("Sparse", sparseDial));
    }

    /**
     * Create a labeled control panel with standard 60px width
     */
    private JPanel createLabeledControl(String label, Dial dial) {
        return createLabeledControl(label, dial, 60);
    }

    /**
     * Create a labeled control panel with custom width
     */
    private JPanel createLabeledControl(String label, Dial dial, int panelWidth) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Set consistent panel height but allow custom width
        panel.setPreferredSize(new Dimension(panelWidth, PANEL_HEIGHT - 10));
        panel.setMinimumSize(new Dimension(panelWidth, PANEL_HEIGHT - 10));
        panel.setMaximumSize(new Dimension(panelWidth, PANEL_HEIGHT - 10));

        // Add spacer at top if there's a label
        if (label != null) {
            JLabel l = new JLabel(label);
            l.setHorizontalAlignment(JLabel.CENTER);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(l);
        } else {
            // Add empty space if no label
            panel.add(Box.createVerticalStrut(15));
        }

        // Add flexible space
        panel.add(Box.createVerticalGlue());

        // Center the dial
        JPanel dialWrapper = new JPanel();
        dialWrapper.setOpaque(false);
        dialWrapper.setLayout(new BoxLayout(dialWrapper, BoxLayout.X_AXIS));
        dialWrapper.add(Box.createHorizontalGlue());
        dialWrapper.add(dial);
        dialWrapper.add(Box.createHorizontalGlue());
        dialWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(dialWrapper);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createOctavePanel() {
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);

        // Create up and down buttons
        JButton upButton = new JButton("↑");
        JButton downButton = new JButton("↓");
        upButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        downButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        // Add action listeners
        upButton.addActionListener(e -> {
            if (activePlayer != null) {
                int currentNote = activePlayer.getRootNote().intValue();
                int newNote = Math.min(127, currentNote + 12); // Don't exceed 127 (max MIDI note)

                // Only update if it actually changed
                if (newNote != currentNote) {
                    logger.info("Octave UP: Changing note from " + currentNote + " to " + newNote);

                    // Update player's note
                    activePlayer.setRootNote(newNote);

                    // Update the dials (without triggering listeners)
                    noteSelectionDial.setValue(newNote, false);

                    // Save the change and notify UI
                    getPlayer().setRootNote(newNote);

                    // Request row refresh in players panel
                    CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);

                    // Update button states
                    updateOctaveButtons(upButton, downButton, newNote);
                }
            }
        });

        downButton.addActionListener(e -> {
            if (activePlayer != null) {
                int currentNote = activePlayer.getRootNote().intValue();
                int newNote = Math.max(0, currentNote - 12); // Don't go below 0 (min MIDI note)

                // Only update if it actually changed
                if (newNote != currentNote) {
                    logger.info("Octave DOWN: Changing note from " + currentNote + " to " + newNote);

                    // Update player's note
                    activePlayer.setRootNote(newNote);

                    // Update the dials (without triggering listeners)
                    noteSelectionDial.setValue(newNote, false);

                    // Save the change and notify UI
                    getPlayer().setRootNote(newNote);

                    // Request row refresh in players panel
                    CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);

                    // Update button states
                    updateOctaveButtons(upButton, downButton, newNote);
                }
            }
        });

        // Disable buttons by default (until a player is selected)
        upButton.setEnabled(false);
        downButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);

        // Store the buttons for later access
        this.octaveUpButton = upButton;
        this.octaveDownButton = downButton;

        return navPanel;
    }

    private void updateOctaveButtons(JButton upButton, JButton downButton, int note) {
        // Disable up button if we're at max octave (note >= 116)
        upButton.setEnabled(note < 116);

        // Disable down button if we're at min octave (note < 12)
        downButton.setEnabled(note >= 12);
    }

    private JPanel createScaleAdjustPanel() {
        JPanel scalePanel = new JPanel(new BorderLayout(0, 2));
        scalePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        scalePanel.setName(SESSION_PANEL + "_SCALE");

        JLabel scaleLabel = new JLabel("Scale", JLabel.CENTER);

        // Create scale navigation buttons
        prevScaleButton = new JButton("↑");
        prevScaleButton.setActionCommand(Commands.PREV_SCALE_SELECTED);
        prevScaleButton.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.PREV_SCALE_SELECTED, this, Scale.SCALE_PATTERNS.keySet());
        });
        prevScaleButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        prevScaleButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        nextScaleButton = new JButton("↓");
        nextScaleButton.setActionCommand(Commands.NEXT_SCALE_SELECTED);
        nextScaleButton.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.NEXT_SCALE_SELECTED, this, Scale.SCALE_PATTERNS.keySet());
        });
        nextScaleButton.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        nextScaleButton.setMaximumSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));

        // Enable buttons by default
        prevScaleButton.setEnabled(true);
        nextScaleButton.setEnabled(true);

        // Layout buttons vertically
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        buttonPanel.add(prevScaleButton);
        buttonPanel.add(nextScaleButton);

        scalePanel.add(scaleLabel, BorderLayout.NORTH);
        scalePanel.add(buttonPanel, BorderLayout.CENTER);

        return scalePanel;
    }

    @Override
    public void onAction(Command action) {
        if (action.getSender() == ControlPanel.this) {
            return;
        }
        if (action.getCommand() == null)
            return;
        String cmd = action.getCommand();
        switch (cmd) {
            case Commands.FIRST_SCALE_SELECTED -> prevScaleButton.setEnabled(false);
            case Commands.LAST_SCALE_SELECTED -> nextScaleButton.setEnabled(false);
            case Commands.SCALE_SELECTED -> {
                prevScaleButton.setEnabled(true);
                nextScaleButton.setEnabled(true);
            }
            case Commands.PLAYER_SELECTION_EVENT -> {
                // Enable dials when player is activated
                enableDials();
            }
        }
    }

    private Dial createDial(String propertyName, long value, int min, int max, int majorTick) {
        Dial dial = new Dial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue((int) value);
        dial.setPreferredSize(new Dimension(50, 50));
        dial.setMinimumSize(new Dimension(50, 50));
        dial.setMaximumSize(new Dimension(50, 50));
        dial.setBackground(UIHelper.getDialColor(propertyName));
        // Store the property name in the dial
        dial.setName(propertyName);

        dial.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Dial sourceDial = (Dial) e.getSource();
                if (sourceDial.getCommand() != null) {
                    CommandBus.getInstance().publish(sourceDial.getCommand(),
                            getPlayer(), sourceDial.getValue());
                }
            }
        });
        return dial;
    }

    private void enableDials() {
        levelDial.setEnabled(true);
        noteSelectionDial.setEnabled(true);
        swingDial.setEnabled(true);
        probabilityDial.setEnabled(true);
        velocityMinDial.setEnabled(true);
        velocityMaxDial.setEnabled(true);
        randomDial.setEnabled(true);
        panDial.setEnabled(true);
        sparseDial.setEnabled(true);
    }

    private void disableDials() {
        levelDial.setEnabled(false);
        noteSelectionDial.setEnabled(false);
        swingDial.setEnabled(false);
        probabilityDial.setEnabled(false);
        velocityMinDial.setEnabled(false);
        velocityMaxDial.setEnabled(false);
        randomDial.setEnabled(false);
        panDial.setEnabled(false);
        sparseDial.setEnabled(false);
    }

    private void updateVerticalAdjustButtons(boolean enabled) {
        // Find and update all buttons in vertical adjust panels
        for (Component comp : getComponents()) {
            if (comp instanceof JPanel panel && (panel.getName() != null && panel.getName().contains("PLAYER_PANEL"))) {
                // Improved traversal to handle nested panels
                traverseAndEnableButtons(panel, enabled);
            }
        }
    }

    // New helper method to properly traverse component hierarchy
    private void traverseAndEnableButtons(Container container, boolean enabled) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton button) {
                // Enable button if we have an active player
                button.setEnabled(enabled);
                logger.debug("Button " + button.getText() + " enabled: " + enabled);
            } else if (comp instanceof Container innerContainer) {
                // Recursively search nested containers
                traverseAndEnableButtons(innerContainer, enabled);
            }
        }
    }

    private void setupCommandBusListener() {
        // Register this panel as a listener for all needed commands
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_SELECTION_EVENT,
                Commands.FIRST_SCALE_SELECTED,
                Commands.LAST_SCALE_SELECTED,
                Commands.SCALE_SELECTED
        });
    }

    private void setupControlChangeListeners() {
        // For level dial
        levelDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = levelDial.getValue();
            logger.info("Updating player level to: " + value);

            // Update player
            activePlayer.setLevel(value);

            // Save the change and notify UI
            getPlayer().setLevel(value);

            // Request row refresh in players panel (important!)
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // For swing dial
        swingDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = swingDial.getValue();
            logger.info("Updating player swing to: " + value);

            // Update player
            activePlayer.setSwing(value);

            // Save the change and notify UI
            getPlayer().setSwing(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // For probability dial
        probabilityDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = probabilityDial.getValue();
            logger.info("Updating player probability to: " + value);

            // Update player and save
            getPlayer().setProbability(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Replace the velocityMinDial listener with this:
        velocityMinDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int minValue = velocityMinDial.getValue();
            int maxValue = velocityMaxDial.getValue();

            logger.info("Updating player min velocity to: " + minValue);

            // Ensure min velocity doesn't exceed max velocity
            if (minValue > maxValue) {
                // Update max velocity to match min velocity
                listenersEnabled = false; // Prevent feedback loop
                velocityMaxDial.setValue(minValue);
                activePlayer.setMaxVelocity(minValue);

                // Force immediate visual update of the max dial
                velocityMaxDial.repaint();

                listenersEnabled = true;

                // Log the adjustment
                logger.info("Auto-adjusted max velocity to match: " + minValue);
            }

            // Update player
            activePlayer.setMinVelocity(minValue);

            // Save the changes and notify UI
            getPlayer().setMinVelocity(minValue);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Replace the velocityMaxDial listener with this:
        velocityMaxDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int maxValue = velocityMaxDial.getValue();
            int minValue = velocityMinDial.getValue();

            logger.info("Updating player max velocity to: " + maxValue);

            // Ensure max velocity is not less than min velocity
            if (maxValue < minValue) {
                // Update min velocity to match max velocity
                listenersEnabled = false; // Prevent feedback loop
                velocityMinDial.setValue(maxValue);
                activePlayer.setMinVelocity(maxValue);

                // Force immediate visual update of the min dial
                velocityMinDial.repaint();

                listenersEnabled = true;

                // Log the adjustment
                logger.info("Auto-adjusted min velocity to match: " + maxValue);
            }

            // Update player
            activePlayer.setMaxVelocity(maxValue);

            // Save the changes and notify UI
            getPlayer().setMaxVelocity(maxValue);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add panDial listener
        panDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = panDial.getValue();
            logger.info("Updating player pan to: " + value);

            // Update player and save
            activePlayer.setPanPosition(value);
            getPlayer().setPan(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add randomDial listener
        randomDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = randomDial.getValue();
            logger.info("Updating player random to: " + value);

            // Update player and save
            activePlayer.setRandomDegree(value);
            getPlayer().setRandomDegree(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add sparseDial listener
        sparseDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = sparseDial.getValue();
            logger.info("Updating player sparse to: " + value);

            // Update player and save
            activePlayer.setSparse(value / 100.0); // Convert to 0-1.0 range
            getPlayer().setSparse(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });

        // Add note dial listener
        noteSelectionDial.addChangeListener(e -> {
            if (!listenersEnabled || activePlayer == null)
                return;

            int value = noteSelectionDial.getValue();
            logger.info("Updating player note to: " + value);

            // Update player and save
            activePlayer.setRootNote(value);
            getPlayer().setRootNote(value);

            // Request row refresh
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, activePlayer);
        });
    }

    public void updateDialsFromPlayer(Player player) {
        if (player == null) {
            logger.error("Attempted to update dials with null player");
            return;
        }

        try {
            logger.info("Setting dial values for player: " + player.getName());

            // Temporarily disable listeners
            listenersEnabled = false;

            // Get player values, handle potential nulls
            int level = player.getLevel() != null ? player.getLevel().intValue() : 64;
            int note = player.getRootNote() != null ? player.getRootNote().intValue() : 60;
            int swing = player.getSwing() != null ? player.getSwing().intValue() : 0;
            int minVelocity = player.getMinVelocity() != null ? player.getMinVelocity().intValue() : 64;
            int maxVelocity = player.getMaxVelocity() != null ? player.getMaxVelocity().intValue() : 127;
            int probability = player.getProbability() != null ? player.getProbability().intValue() : 100;
            int randomDegree = player.getRandomDegree() != null ? player.getRandomDegree().intValue() : 0;
            Double sparseValue = player.getSparse(); // Get the Double object first
            int sparse = sparseValue != null ? (int) (sparseValue * 100) : 0;
            int panPosition = player.getPanPosition() != null ? player.getPanPosition().intValue() : 64;

            // Update dials without triggering notifications (false parameter)
            levelDial.setValue(level, false);
            swingDial.setValue(swing, false);
            velocityMinDial.setValue(minVelocity, false);
            velocityMaxDial.setValue(maxVelocity, false);
            probabilityDial.setValue(probability, false);
            randomDial.setValue(randomDegree, false);
            sparseDial.setValue(sparse, false);
            panDial.setValue(panPosition, false);

            // Note dial might be custom
            if (noteSelectionDial != null) {
                noteSelectionDial.setValue(note, false);
            }

            // Update octave button states based on the current note
            if (octaveUpButton != null && octaveDownButton != null) {
                updateOctaveButtons(octaveUpButton, octaveDownButton, note);
            }

            // Enable dials now that they're updated
            enableDials();
            updateVerticalAdjustButtons(true);

            // Re-enable listeners
            listenersEnabled = true;

            logger.info("Successfully updated all dials for player: " + player.getName());
        } catch (Exception e) {
            logger.error("Error updating dials", e);

            // Make sure listeners are re-enabled even if there's an error
            listenersEnabled = true;
        }
    }
}