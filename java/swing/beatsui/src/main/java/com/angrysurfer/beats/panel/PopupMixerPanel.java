package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.SequencerService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PopupMixerPanel extends JPanel {

    private final DrumSequencer drumSequencer;
    private List<MelodicSequencer> melodicSequencers;

    // Arrays to track drum mute and solo states
    private final boolean[] drumMuteStates;
    private final boolean[] drumSoloStates;
    private boolean drumSoloActive = false;

    // Arrays to track melodic mute and solo states
    private final boolean[] melodicMuteStates;
    private final boolean[] melodicSoloStates;
    private boolean melodicSoloActive = false;

    // UI components for drum channel strips
    private final JSlider[] drumVolumeSliders;
    private final JToggleButton[] drumMuteButtons;
    private final JToggleButton[] drumSoloButtons;

    // UI components for melodic channel strips
    private final JSlider[] melodicVolumeSliders;
    private final JToggleButton[] melodicMuteButtons;
    private final JToggleButton[] melodicSoloButtons;

    public PopupMixerPanel(DrumSequencer drumSequencer) {
        super(new BorderLayout());
        this.drumSequencer = drumSequencer;

        // Get melodic sequencers from manager
        this.melodicSequencers = SequencerService.getInstance().getAllMelodicSequencers();
        if (melodicSequencers == null) {
            melodicSequencers = List.of(); // Empty list if none available
        }

        // Initialize drum state arrays
        int drumCount = SequencerConstants.DRUM_PAD_COUNT;
        drumMuteStates = new boolean[drumCount];
        drumSoloStates = new boolean[drumCount];
        drumVolumeSliders = new JSlider[drumCount];
        drumMuteButtons = new JToggleButton[drumCount];
        drumSoloButtons = new JToggleButton[drumCount];

        // Initialize melodic state arrays
        int melodicCount = melodicSequencers.size();
        melodicMuteStates = new boolean[melodicCount];
        melodicSoloStates = new boolean[melodicCount];
        melodicVolumeSliders = new JSlider[melodicCount];
        melodicMuteButtons = new JToggleButton[melodicCount];
        melodicSoloButtons = new JToggleButton[melodicCount];

        initialize();
    }

    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a tabbed pane to separate drums and melodic sequencers
        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the drums tab
        tabbedPane.addTab("Drums", createDrumPanel());

        // Create the melodic tab
        tabbedPane.addTab("Melodics", createMelodicPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Add master controls at bottom
        add(createMasterControls(), BorderLayout.SOUTH);
    }

    private JPanel createDrumPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Main panel for channel strips
        JPanel channelStripsPanel = new JPanel(new GridLayout(1, SequencerConstants.DRUM_PAD_COUNT));

        // Create drum channel strips
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            channelStripsPanel.add(createDrumChannelStrip(i));
        }

        // Add scrolling if needed
        JScrollPane scrollPane = new JScrollPane(channelStripsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Add drum-specific controls
        JPanel drumControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton allMuteButton = new JButton("Mute All Drums");
        allMuteButton.addActionListener(e -> {
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                drumMuteButtons[i].setSelected(true);
                drumMuteStates[i] = true;
                drumSequencer.setVelocity(i, 0);
            }
            updateDrumChannelStates();
        });
        drumControls.add(allMuteButton);

        JButton clearDrumMuteButton = new JButton("Clear Drum Mutes");
        clearDrumMuteButton.addActionListener(e -> {
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                drumMuteButtons[i].setSelected(false);
                drumMuteStates[i] = false;
            }
            updateDrumChannelStates();
        });
        drumControls.add(clearDrumMuteButton);

        JButton clearDrumSoloButton = new JButton("Clear Drum Solos");
        clearDrumSoloButton.addActionListener(e -> {
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                drumSoloButtons[i].setSelected(false);
                drumSoloStates[i] = false;
            }
            drumSoloActive = false;
            updateDrumChannelStates();
        });
        drumControls.add(clearDrumSoloButton);

        mainPanel.add(drumControls, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createMelodicPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        if (melodicSequencers.isEmpty()) {
            mainPanel.add(new JLabel("No melodic sequencers available", SwingConstants.CENTER), BorderLayout.CENTER);
            return mainPanel;
        }

        // Main panel for channel strips
        JPanel channelStripsPanel = new JPanel(new GridLayout(1, melodicSequencers.size()));

        // Create melodic channel strips
        for (int i = 0; i < melodicSequencers.size(); i++) {
            channelStripsPanel.add(createMelodicChannelStrip(i));
        }

        mainPanel.add(channelStripsPanel, BorderLayout.CENTER);

        // Add melodic-specific controls
        JPanel melodicControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton allMuteButton = new JButton("Mute All Melodics");
        allMuteButton.addActionListener(e -> {
            for (int i = 0; i < melodicSequencers.size(); i++) {
                melodicMuteButtons[i].setSelected(true);
                melodicMuteStates[i] = true;
                melodicSequencers.get(i).getPlayer().setLevel(0);
            }
            updateMelodicChannelStates();
        });
        melodicControls.add(allMuteButton);

        JButton clearMelodicMuteButton = new JButton("Clear Melodic Mutes");
        clearMelodicMuteButton.addActionListener(e -> {
            for (int i = 0; i < melodicSequencers.size(); i++) {
                melodicMuteButtons[i].setSelected(false);
                melodicMuteStates[i] = false;
            }
            updateMelodicChannelStates();
        });
        melodicControls.add(clearMelodicMuteButton);

        JButton clearMelodicSoloButton = new JButton("Clear Melodic Solos");
        clearMelodicSoloButton.addActionListener(e -> {
            for (int i = 0; i < melodicSequencers.size(); i++) {
                melodicSoloButtons[i].setSelected(false);
                melodicSoloStates[i] = false;
            }
            melodicSoloActive = false;
            updateMelodicChannelStates();
        });
        melodicControls.add(clearMelodicSoloButton);

        mainPanel.add(melodicControls, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createDrumChannelStrip(int drumIndex) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Add drum name label
        String drumName = "Drum " + (drumIndex + 1);
        if (drumSequencer.getPlayer(drumIndex) != null) {
            drumName = drumSequencer.getPlayer(drumIndex).getName();
        }

        JLabel nameLabel = new JLabel(drumName);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        panel.add(nameLabel);

        panel.add(Box.createVerticalStrut(5));

        // Add volume slider
        JSlider volumeSlider = new JSlider(JSlider.VERTICAL, 0, 127, drumSequencer.getVelocity(drumIndex));
        drumVolumeSliders[drumIndex] = volumeSlider;
        volumeSlider.setPreferredSize(new Dimension(40, 150));
        volumeSlider.setMajorTickSpacing(32);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add change listener to update volume
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                drumSequencer.setVelocity(drumIndex, volumeSlider.getValue());
            }
        });

        // Create a panel to center the slider
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.add(volumeSlider);
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sliderPanel);

        // Add volume value label
        JLabel volumeLabel = new JLabel(String.valueOf(volumeSlider.getValue()));
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        volumeSlider.addChangeListener(e -> volumeLabel.setText(String.valueOf(volumeSlider.getValue())));
        panel.add(volumeLabel);

        panel.add(Box.createVerticalStrut(10));

        // Add mute and solo buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create mute button
        drumMuteButtons[drumIndex] = createDrumMuteButton(drumIndex);
        buttonPanel.add(drumMuteButtons[drumIndex]);

        // Create solo button
        drumSoloButtons[drumIndex] = createDrumSoloButton(drumIndex);
        buttonPanel.add(drumSoloButtons[drumIndex]);

        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createMelodicChannelStrip(int sequencerIndex) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        MelodicSequencer sequencer = melodicSequencers.get(sequencerIndex);

        // Add sequencer name label
        String seqName = sequencer.getPlayer().getName();
        if (seqName == null || seqName.isEmpty()) {
            seqName = "Melodic " + (sequencerIndex + 1);
        }

        JLabel nameLabel = new JLabel(seqName);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        panel.add(nameLabel);

        // Add channel info
        JLabel channelLabel = new JLabel("Ch " + sequencer.getPlayer().getChannel());
        channelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(channelLabel);

        panel.add(Box.createVerticalStrut(5));

        // Add volume slider
        int currentLevel = sequencer.getPlayer().getLevel().intValue();
        JSlider volumeSlider = new JSlider(JSlider.VERTICAL, 0, 127, currentLevel);
        melodicVolumeSliders[sequencerIndex] = volumeSlider;
        volumeSlider.setPreferredSize(new Dimension(40, 150));
        volumeSlider.setMajorTickSpacing(32);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add change listener to update volume
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                sequencer.getPlayer().setLevel(volumeSlider.getValue());
            }
        });

        // Create a panel to center the slider
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.add(volumeSlider);
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sliderPanel);

        // Add volume value label
        JLabel volumeLabel = new JLabel(String.valueOf(volumeSlider.getValue()));
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        volumeSlider.addChangeListener(e -> volumeLabel.setText(String.valueOf(volumeSlider.getValue())));
        panel.add(volumeLabel);

        panel.add(Box.createVerticalStrut(10));

        // Add mute and solo buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create mute button
        melodicMuteButtons[sequencerIndex] = createMelodicMuteButton(sequencerIndex);
        buttonPanel.add(melodicMuteButtons[sequencerIndex]);

        // Create solo button
        melodicSoloButtons[sequencerIndex] = createMelodicSoloButton(sequencerIndex);
        buttonPanel.add(melodicSoloButtons[sequencerIndex]);

        panel.add(buttonPanel);

        return panel;
    }

    private JToggleButton createDrumMuteButton(int drumIndex) {
        JToggleButton muteButton = new JToggleButton("🔇");
        muteButton.setToolTipText("Mute Drum " + (drumIndex + 1));
        muteButton.setPreferredSize(new Dimension(30, 30));
        muteButton.setMargin(new Insets(2, 2, 2, 2));
        muteButton.setBackground(UIHelper.charcoalGray);
        muteButton.setForeground(Color.WHITE);

        // Store the original velocity to restore it when unmuting
        final int[] originalVelocity = {drumSequencer.getVelocity(drumIndex)};

        muteButton.addActionListener(e -> {
            boolean muted = muteButton.isSelected();
            drumMuteStates[drumIndex] = muted;

            if (muted) {
                // Save current velocity before muting
                originalVelocity[0] = drumSequencer.getVelocity(drumIndex);
                drumSequencer.setVelocity(drumIndex, 0);
                muteButton.setBackground(UIHelper.mutedRed);
            } else {
                // Restore original velocity
                drumSequencer.setVelocity(drumIndex, originalVelocity[0]);
                muteButton.setBackground(UIHelper.charcoalGray);
            }

            // Update the slider to match
            drumVolumeSliders[drumIndex].setValue(drumSequencer.getVelocity(drumIndex));

            // If this channel is unmuted but solo is active on other channels, 
            // we need to check if it should actually be audible
            updateDrumChannelStates();
        });

        return muteButton;
    }

    private JToggleButton createDrumSoloButton(int drumIndex) {
        JToggleButton soloButton = new JToggleButton("🎙️");
        soloButton.setToolTipText("Solo Drum " + (drumIndex + 1));
        soloButton.setPreferredSize(new Dimension(30, 30));
        soloButton.setMargin(new Insets(2, 2, 2, 2));
        soloButton.setBackground(UIHelper.charcoalGray);
        soloButton.setForeground(Color.WHITE);

        // Store original velocities for all channels to restore when un-soloing
        final int[][] originalVelocities = {new int[SequencerConstants.DRUM_PAD_COUNT]};

        soloButton.addActionListener(e -> {
            boolean soloed = soloButton.isSelected();
            drumSoloStates[drumIndex] = soloed;

            if (soloed) {
                // First time a solo is activated, save all channel volumes
                if (!drumSoloActive) {
                    for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                        originalVelocities[0][i] = drumSequencer.getVelocity(i);
                    }
                    drumSoloActive = true;
                }

                soloButton.setBackground(UIHelper.warmMustard);
            } else {
                soloButton.setBackground(UIHelper.charcoalGray);

                // Check if any other solos are active
                boolean anySoloActive = false;
                for (boolean state : drumSoloStates) {
                    if (state) {
                        anySoloActive = true;
                        break;
                    }
                }

                // If no more solos are active, reset the solo state
                drumSoloActive = anySoloActive;
            }

            // Update all channel states based on solo/mute configuration
            updateDrumChannelStates();
        });

        return soloButton;
    }

    private JToggleButton createMelodicMuteButton(int sequencerIndex) {
        MelodicSequencer sequencer = melodicSequencers.get(sequencerIndex);

        JToggleButton muteButton = new JToggleButton("🔇");
        muteButton.setToolTipText("Mute " + sequencer.getPlayer().getName());
        muteButton.setPreferredSize(new Dimension(30, 30));
        muteButton.setMargin(new Insets(2, 2, 2, 2));
        muteButton.setBackground(UIHelper.charcoalGray);
        muteButton.setForeground(Color.WHITE);

        // Store the original level to restore it when unmuting
        final int[] originalLevel = {sequencer.getPlayer().getLevel().intValue()};

        muteButton.addActionListener(e -> {
            boolean muted = muteButton.isSelected();
            melodicMuteStates[sequencerIndex] = muted;

            if (muted) {
                // Save current level before muting
                originalLevel[0] = sequencer.getPlayer().getLevel().intValue();
                sequencer.getPlayer().setLevel(0);
                muteButton.setBackground(UIHelper.mutedRed);
            } else {
                // Restore original level
                sequencer.getPlayer().setLevel(originalLevel[0]);
                muteButton.setBackground(UIHelper.charcoalGray);
            }

            // Update the slider to match
            melodicVolumeSliders[sequencerIndex].setValue(sequencer.getPlayer().getLevel().intValue());

            // Update channel states
            updateMelodicChannelStates();
        });

        return muteButton;
    }

    private JToggleButton createMelodicSoloButton(int sequencerIndex) {
        MelodicSequencer sequencer = melodicSequencers.get(sequencerIndex);

        JToggleButton soloButton = new JToggleButton("🎙️");
        soloButton.setToolTipText("Solo " + sequencer.getPlayer().getName());
        soloButton.setPreferredSize(new Dimension(30, 30));
        soloButton.setMargin(new Insets(2, 2, 2, 2));
        soloButton.setBackground(UIHelper.charcoalGray);
        soloButton.setForeground(Color.WHITE);

        // Store original levels for all sequencers to restore when un-soloing
        final int[][] originalLevels = {new int[melodicSequencers.size()]};

        soloButton.addActionListener(e -> {
            boolean soloed = soloButton.isSelected();
            melodicSoloStates[sequencerIndex] = soloed;

            if (soloed) {
                // First time a solo is activated, save all levels
                if (!melodicSoloActive) {
                    for (int i = 0; i < melodicSequencers.size(); i++) {
                        originalLevels[0][i] = melodicSequencers.get(i).getPlayer().getLevel().intValue();
                    }
                    melodicSoloActive = true;
                }

                soloButton.setBackground(UIHelper.warmMustard);
            } else {
                soloButton.setBackground(UIHelper.charcoalGray);

                // Check if any other solos are active
                boolean anySoloActive = false;
                for (boolean state : melodicSoloStates) {
                    if (state) {
                        anySoloActive = true;
                        break;
                    }
                }

                // If no more solos are active, reset the solo state
                melodicSoloActive = anySoloActive;
            }

            // Update all channel states based on solo/mute configuration
            updateMelodicChannelStates();
        });

        return soloButton;
    }

    /**
     * Updates all drum channels based on mute and solo states
     */
    private void updateDrumChannelStates() {
        // When any solo is active, only soloed channels play
        if (drumSoloActive) {
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                if (drumSoloStates[i]) {
                    // Solo trumps mute
                    if (!drumMuteStates[i]) {
                        // Channel is soloed and not muted - ensure it's at previous volume
                        int currentVol = drumSequencer.getVelocity(i);
                        if (currentVol == 0) {
                            // If it was at 0, set to default value
                            drumSequencer.setVelocity(i, 100);
                            drumVolumeSliders[i].setValue(100);
                        }
                    }
                } else {
                    // Channel not soloed - should be silent
                    drumSequencer.setVelocity(i, 0);
                    // Don't update slider position - we're temporarily silencing
                }
            }
        } else {
            // No solos active - respect only mute state
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                if (drumMuteStates[i]) {
                    // Muted - should be silent
                    drumSequencer.setVelocity(i, 0);
                } else {
                    // Not muted - restore to slider position
                    drumSequencer.setVelocity(i, drumVolumeSliders[i].getValue());
                }
            }
        }
    }

    /**
     * Updates all melodic channels based on mute and solo states
     */
    private void updateMelodicChannelStates() {
        // When any solo is active, only soloed channels play
        if (melodicSoloActive) {
            for (int i = 0; i < melodicSequencers.size(); i++) {
                MelodicSequencer sequencer = melodicSequencers.get(i);

                if (melodicSoloStates[i]) {
                    // Solo trumps mute
                    if (!melodicMuteStates[i]) {
                        // Sequencer is soloed and not muted - ensure it's at previous level
                        int currentLevel = sequencer.getPlayer().getLevel().intValue();
                        if (currentLevel == 0) {
                            // If it was at 0, set to default value
                            sequencer.getPlayer().setLevel(100);
                            melodicVolumeSliders[i].setValue(100);
                        }
                    }
                } else {
                    // Sequencer not soloed - should be silent
                    sequencer.getPlayer().setLevel(0);
                    // Don't update slider position - we're temporarily silencing
                }
            }
        } else {
            // No solos active - respect only mute state
            for (int i = 0; i < melodicSequencers.size(); i++) {
                MelodicSequencer sequencer = melodicSequencers.get(i);

                if (melodicMuteStates[i]) {
                    // Muted - should be silent
                    sequencer.getPlayer().setLevel(0);
                } else {
                    // Not muted - restore to slider position
                    sequencer.getPlayer().setLevel(melodicVolumeSliders[i].getValue());
                }
            }
        }
    }

    private JPanel createMasterControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // Add master volume controls if needed

        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            Container container = SwingUtilities.getAncestorOfClass(Window.class, this);
            if (container instanceof Window) {
                ((Window) container).dispose();
            }
        });
        panel.add(closeButton);

        return panel;
    }
}
