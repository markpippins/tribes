package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.NumberedTickDial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel containing detailed player controls for performance, modulation, and
 * ratcheting. This panel is used inside PlayerEditPanel to manage the middle
 * section of controls.
 */
public class PlayerEditDetailPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEditDetailPanel.class);
    private static final int SLIDER_HEIGHT = 80;
    private static final int PANEL_HEIGHT = 125;
    // Performance controls
    private final JSlider levelSlider;
    private final JSlider velocityMinSlider;
    private final JSlider velocityMaxSlider;
    private final Dial panDial; // Changed from JSlider to Dial
    private final JButton prevButton;
    private final JButton nextButton;
    private final NoteSelectionDial noteDial;
    // Modulation controls
    private final JSlider swingSlider;
    private final JSlider probabilitySlider;
    private final JSlider randomSlider;
    private final JSlider sparseSlider;
    // Reference to player being edited
    private Player player;
    // Ratchet controls
    // Replace the existing slider components with NumberedTickDials
    private NumberedTickDial countDial;
    private NumberedTickDial intervalDial;

    private boolean changing = false;

    /**
     * Creates a new PlayerEditDetailPanel for the given player
     *
     * @param player The player being edited (can be null initially)
     */
    public PlayerEditDetailPanel(Player player) {
        super(new BorderLayout());
        this.player = player;

        // Initialize performance controls
        levelSlider = createSlider("Level", player.getLevel(), 0, 100);
        velocityMinSlider = createSlider("Min Velocity", player.getMinVelocity(), 0, 127);
        velocityMaxSlider = createSlider("Max Velocity", player.getMaxVelocity(), 0, 127);
        panDial = new Dial();
        panDial.setMinimum(0);
        panDial.setMaximum(127);
        panDial.setValue(player.getPanPosition().intValue(), false);
        panDial.setPreferredSize(new Dimension(50, 50));
        panDial.setMinimumSize(new Dimension(50, 50));
        panDial.setMaximumSize(new Dimension(50, 50));

        prevButton = new JButton("▲");
        nextButton = new JButton("▼");
        noteDial = new NoteSelectionDial();

        // Initialize modulation controls
        swingSlider = createSlider("Swing", player.getSwing(), 0, 100);
        probabilitySlider = createSlider("Probability", player.getProbability(), 0, 100);
        randomSlider = createSlider("Random", player.getRandomDegree(), 0, 100);
        sparseSlider = createSlider("Sparse", (int) (player.getSparse() * 100), 0, 100);


        // Set up the UI components
        setupLayout();
        setupActionListeners();
    }

    /**
     * Sets up the overall panel layout with Ratchet panel on the right side
     */
    private void setupLayout() {
        // Use BorderLayout for the main container to have left and right sections
        setLayout(new BorderLayout(10, 5));

        // Create left panel to stack Performance and Modulation panels vertically
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Create and add performance panel to left stack
        JPanel performancePanel = createPerformancePanel();
        leftPanel.add(performancePanel);

        // Create and add modulation panel to left stack
        JPanel modulationPanel = createModulationPanel();
        leftPanel.add(modulationPanel);

        // Create ratchet panel for right side
        JPanel ratchetPanel = createRatchetPanel();

        // Add panels to main layout
        add(leftPanel, BorderLayout.CENTER);
        add(ratchetPanel, BorderLayout.EAST);
    }

    /**
     * Creates the performance panel with reordered controls: Octave, Note,
     * Level, Pan - with vertical centering and fixed height
     */
    private JPanel createPerformancePanel() {
        // Use GridBagLayout for better component positioning and centering
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Performance"));

        // Set fixed height for the panel
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, PANEL_HEIGHT));
        panel.setMinimumSize(new Dimension(0, PANEL_HEIGHT));

        // Create a flow sub-panel to hold the actual controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // 1. OCTAVE CONTROLS
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        prevButton.setPreferredSize(new Dimension(35, 35));
        nextButton.setPreferredSize(new Dimension(35, 35));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);
        controlsPanel.add(navPanel);

        // 2. NOTE DIAL
        Dimension dialSize = new Dimension(100, 100);
        noteDial.setPreferredSize(dialSize);
        noteDial.setMinimumSize(dialSize);
        noteDial.setMaximumSize(dialSize);
        noteDial.setCommand(Commands.NEW_VALUE_NOTE);
        noteDial.setValue(player.getRootNote().intValue());

        JPanel notePanel = new JPanel(new BorderLayout(5, 2));
        JLabel noteLabel = new JLabel("Note", JLabel.CENTER);
        notePanel.add(noteLabel, BorderLayout.NORTH);
        notePanel.add(noteDial, BorderLayout.CENTER);
        controlsPanel.add(notePanel);

        // 3. LEVEL SLIDER
        controlsPanel.add(createLabeledSlider("Level", levelSlider));

        // 4. PAN DIAL
        Dimension panDialSize = new Dimension(60, 60);
        panDial.setPreferredSize(panDialSize);
        panDial.setMinimumSize(panDialSize);
        panDial.setMaximumSize(panDialSize);

        JPanel panPanel = new JPanel(new BorderLayout(5, 2));
        JLabel panLabel = new JLabel("Pan", JLabel.CENTER);
        panPanel.add(panLabel, BorderLayout.NORTH);
        panPanel.add(panDial, BorderLayout.CENTER);
        controlsPanel.add(panPanel);

        // Add the controls panel to the main panel, centered vertically
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; // Center vertically and horizontally
        gbc.weighty = 1.0; // Take all vertical space
        panel.add(controlsPanel, gbc);

        return panel;
    }

    /**
     * Creates the modulation panel with velocity controls first, then swing,
     * probability, etc. - with vertical centering and fixed height
     */
    private JPanel createModulationPanel() {
        // Use GridBagLayout for better component positioning and centering
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Modulation"));

        // Set fixed height for the panel - SAME HEIGHT as performance panel
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, PANEL_HEIGHT));
        panel.setMinimumSize(new Dimension(0, PANEL_HEIGHT));

        // Create a flow sub-panel to hold the actual controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // 1. VELOCITY CONTROLS (added first)
        controlsPanel.add(createLabeledSlider("Min Vel", velocityMinSlider));
        controlsPanel.add(createLabeledSlider("Max Vel", velocityMaxSlider));

        // 2. OTHER MODULATION CONTROLS
        controlsPanel.add(createLabeledSlider("Swing", swingSlider));
        controlsPanel.add(createLabeledSlider("Probability", probabilitySlider));
        controlsPanel.add(createLabeledSlider("Random", randomSlider));
        controlsPanel.add(createLabeledSlider("Sparse", sparseSlider));

        // Add the controls panel to the main panel, centered vertically
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; // Center vertically and horizontally
        gbc.weighty = 1.0; // Take all vertical space
        panel.add(controlsPanel, gbc);

        return panel;
    }

    /**
     * Creates the ratchet panel with count and interval controls
     * Redesigned to be vertically oriented with titled borders
     */
    private JPanel createRatchetPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        // panel.setBorder(BorderFactory.createTitledBorder("Ratchet"));

        // Create panel for sliders with vertical layout
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new BoxLayout(slidersPanel, BoxLayout.Y_AXIS));

        // Set preferred width for ratchet panel
        panel.setPreferredSize(new Dimension(120, 400));

        // Count dial with titled border
        countDial = new NumberedTickDial(1, 16);
        countDial.setValue(4, false);
        countDial.setPreferredSize(new Dimension(80, 80));

        // Create panel with titled border for count dial
        JPanel countPanel = new JPanel(new BorderLayout(2, 5));
        countPanel.setBorder(BorderFactory.createTitledBorder("Ratchet Count"));
        countPanel.add(countDial, BorderLayout.CENTER);

        // Interval dial with titled border
        intervalDial = new NumberedTickDial(1, 16);
        intervalDial.setValue(4, false);
        intervalDial.setPreferredSize(new Dimension(80, 80));

        // Create panel with titled border for interval dial
        JPanel intervalPanel = new JPanel(new BorderLayout(2, 5));
        intervalPanel.setBorder(BorderFactory.createTitledBorder("Ratchet Interval"));
        intervalPanel.add(intervalDial, BorderLayout.CENTER);

        // Replace slider listeners with dial listeners
        countDial.addChangeListener(e -> {
            int value = countDial.getValue();
            player.setRatchetCount(value);
            CommandBus.getInstance().publish(Commands.NEW_VALUE_RATCHET_COUNT, this,
                    new Object[]{player.getId(), (long) value});
        });

        intervalDial.addChangeListener(e -> {
            int value = intervalDial.getValue();
            player.setRatchetInterval(value);
            CommandBus.getInstance().publish(Commands.NEW_VALUE_RATCHET_INTERVAL, this,
                    new Object[]{player.getId(), (long) value});
        });

        // Add spacing between components
        slidersPanel.add(Box.createVerticalStrut(20));
        slidersPanel.add(countPanel);
        slidersPanel.add(Box.createVerticalStrut(30));
        slidersPanel.add(intervalPanel);
        slidersPanel.add(Box.createVerticalStrut(20));

        // Center the sliders panel
        panel.add(slidersPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Sets up action listeners for buttons and sliders
     */
    private void setupActionListeners() {
        CommandBus commandBus = CommandBus.getInstance();

        // Octave navigation - updated to use NoteSelectionDial directly
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Move note up an octave
                int currentOctave = noteDial.getOctave();
                noteDial.setOctaveOnly(currentOctave + 1, true);

                // Log the change
                logger.debug("Octave up: {}", noteDial.getNoteWithOctave());
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Move note down an octave
                int currentOctave = noteDial.getOctave();
                noteDial.setOctaveOnly(currentOctave - 1, true);

                // Log the change
                logger.debug("Octave down: {}", noteDial.getNoteWithOctave());
            }
        });

        // Note dial changes
        noteDial.addChangeListener(e -> {
            if (player == null) return;

            int value = noteDial.getValue();
            player.setRootNote(value);
            if (player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL)
                player.setName(MidiService.getInstance().getDrumName(player.getRootNote()));

            // Create a player update event
            CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATE_EVENT,
                    this,
                    new PlayerUpdateEvent(this, player)
            );

            // Show the note name in logs
            logger.debug("Note changed: {} (MIDI: {})",
                    noteDial.getNoteWithOctave(), value);
        });

        // Pan dial change listener
        panDial.addChangeListener(e -> {
            if (player == null) return;

            int value = panDial.getValue();
            player.setPanPosition(value);

            // Create a player update event
            CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATE_EVENT,
                    this,
                    new PlayerUpdateEvent(this, player)
            );

            logger.debug("Pan changed: {}", value);
        });

        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getSender() != this && action.getCommand() == Commands.NEW_VALUE_NOTE) {
                    noteDial.setValue((Integer) action.getData(), false);
                }
            }
        }, new String[]{Commands.NEW_VALUE_NOTE});

        // Add velocity min slider change listener
        velocityMinSlider.addChangeListener(e -> {
            if (!velocityMinSlider.getValueIsAdjusting()) {
                int minVelocity = velocityMinSlider.getValue();
                int maxVelocity = velocityMaxSlider.getValue();

                // Ensure min value is never greater than max value
                if (minVelocity > maxVelocity) {
                    // Set max value equal to min value
                    velocityMaxSlider.setValue(minVelocity);
                    maxVelocity = minVelocity;
                }

                // Publish the new min velocity with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_VELOCITY_MIN, this,
                        new Object[]{player.getId(), (long) minVelocity});

                logger.debug("Min velocity changed: {} (max: {})", minVelocity, maxVelocity);
            }
        });

        // Add velocity max slider change listener
        velocityMaxSlider.addChangeListener(e -> {
            if (!velocityMaxSlider.getValueIsAdjusting()) {
                int minVelocity = velocityMinSlider.getValue();
                int maxVelocity = velocityMaxSlider.getValue();

                // Ensure max value is never less than min value
                if (maxVelocity < minVelocity) {
                    // Set min value equal to max value
                    velocityMinSlider.setValue(maxVelocity);
                    minVelocity = maxVelocity;
                }

                // Publish the new max velocity with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_VELOCITY_MAX, this,
                        new Object[]{player.getId(), (long) maxVelocity});

                logger.debug("Max velocity changed: {} (min: {})", maxVelocity, minVelocity);
            }
        });
    }

    /**
     * Updates the player object with current control values
     */
    public void applyChanges() {
        player.setLevel(levelSlider.getValue());
        player.setRootNote(noteDial.getValue());
        player.setMinVelocity(velocityMinSlider.getValue());
        player.setMaxVelocity(velocityMaxSlider.getValue());
        player.setPanPosition(panDial.getValue()); // Changed from panSlider to panDial

        player.setSwing(swingSlider.getValue());
        player.setProbability(probabilitySlider.getValue());
        player.setRandomDegree(randomSlider.getValue());
        player.setSparse(((double) sparseSlider.getValue()) / 100.0);

        player.setRatchetCount(countDial.getValue());
        player.setRatchetInterval(intervalDial.getValue());

        logger.debug("Updated player parameters: level={}, note={}, swing={}",
                player.getLevel(), player.getRootNote(), player.getSwing());
    }

    /**
     * Update this panel from the player object
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        // Store reference to the new player
        this.player = newPlayer;

        // Temporarily disable listeners to prevent feedback loops
        boolean wasChanging = changing;
        changing = true;

        try {
            // Update level slider
            if (levelSlider != null && player.getLevel() != null) {
                levelSlider.setValue(player.getLevel());
            }

            // Update note dial
            if (noteDial != null && player.getRootNote() != null) {
                noteDial.setValue(player.getRootNote());
            }

            // Update velocity sliders
            if (velocityMinSlider != null && player.getMinVelocity() != null) {
                velocityMinSlider.setValue(player.getMinVelocity());
            }

            if (velocityMaxSlider != null && player.getMaxVelocity() != null) {
                velocityMaxSlider.setValue(player.getMaxVelocity());
            }

            // Update pan dial
            if (panDial != null && player.getPanPosition() != null) {
                panDial.setValue(player.getPanPosition());
            }

            // Update swing slider
            if (swingSlider != null && player.getSwing() != null) {
                swingSlider.setValue(player.getSwing());
            }

            // Update probability slider
            if (probabilitySlider != null && player.getProbability() != null) {
                probabilitySlider.setValue(player.getProbability());
            }

            // Update random slider
            if (randomSlider != null && player.getRandomDegree() != null) {
                randomSlider.setValue(player.getRandomDegree());
            }

            // Update sparse slider
            if (sparseSlider != null && player.getSparse() > -1) {
                sparseSlider.setValue((int) (player.getSparse() * 100.0));
            }

            // Update ratchet dials
            if (countDial != null && player.getRatchetCount() != null) {
                countDial.setValue(player.getRatchetCount());
            }

            if (intervalDial != null && player.getRatchetInterval() != null) {
                intervalDial.setValue(player.getRatchetInterval());
            }

            logger.debug("Updated PlayerEditDetailPanel from player: {}", player.getName());
        } finally {
            // Re-enable listeners
            changing = wasChanging;
        }
    }

    /**
     * Creates a vertical slider with the given parameters
     */
    private JSlider createSlider(String name, Integer value, int min, int max) {
        // Handle null values safely
        int safeValue;
        if (value == null) {
            logger.error(name + " value is null, using default: " + min);
            safeValue = min;
        } else {
            // Clamp to valid range
            safeValue = Math.max(min, Math.min(max, value));

            // Debug logging
            if (safeValue != value) {
                logger.error(String.format("%s value %d out of range [%d-%d], clamped to %d",
                        name, value, min, max, safeValue));
            }
        }

        JSlider slider = new JSlider(SwingConstants.VERTICAL, min, max, safeValue);
        slider.setPreferredSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMinimumSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMaximumSize(new Dimension(20, SLIDER_HEIGHT));

        return slider;
    }

    /**
     * Creates a vertical slider with major tick marks
     */
    private JSlider createSlider(String name, int value, int min, int max, boolean setMajorTickSpacing) {
        JSlider slider = createSlider(name, value, min, max);
        if (setMajorTickSpacing) {
            slider.setMajorTickSpacing((max - min) / 4);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setSnapToTicks(true);
        }
        return slider;
    }

    /**
     * Creates a panel containing a labeled slider
     */
    private JPanel createLabeledSlider(String label, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JLabel labelComponent = new JLabel(label, JLabel.CENTER);
        labelComponent.setFont(new Font(labelComponent.getFont().getName(), Font.PLAIN, 11));
        panel.add(labelComponent, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }
}
