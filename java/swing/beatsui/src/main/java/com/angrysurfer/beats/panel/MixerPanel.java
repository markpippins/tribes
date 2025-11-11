package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.SequencerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mixer panel that provides volume, pan, and effect controls for all sequencers
 */
public class MixerPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MixerPanel.class);

    // Constants
    private static final int CHANNEL_OFFSET = 1; // Mono channels start at 2

    // MIDI CC values
    private static final int CC_VOLUME = 7;
    private static final int CC_PAN = 10;
    private static final int CC_REVERB = 91;
    private static final int CC_CHORUS = 93;
    private static final int CC_DELAY = 94;

    // Track channels and names
    private final String[] trackNames = {
            "Poly Drum", "Poly FX",
            "Mono 1", "Mono 2", "Mono 3", "Mono 4",
            "Mono 5", "Mono 6", "Mono 7", "Mono 8",
            "Mono 9", "Mono 10", "Mono 11", "Mono 12",
            "Master"
    };

    private final int[] trackChannels = {
            SequencerConstants.MIDI_DRUM_CHANNEL, SequencerConstants.MIDI_DRUM_CHANNEL,
            CHANNEL_OFFSET, CHANNEL_OFFSET + 1, CHANNEL_OFFSET + 2, CHANNEL_OFFSET + 3,
            CHANNEL_OFFSET + 4, CHANNEL_OFFSET + 5, CHANNEL_OFFSET + 6, CHANNEL_OFFSET + 8,
            CHANNEL_OFFSET + 9, CHANNEL_OFFSET + 10, CHANNEL_OFFSET + 11, CHANNEL_OFFSET + 12,
            -1 // Master
    };

    // UI Components
    private final List<Dial> volumeDials = new ArrayList<>();
    private final List<Dial> panDials = new ArrayList<>();
    private final List<JToggleButton> muteButtons = new ArrayList<>();
    private final List<JToggleButton> soloButtons = new ArrayList<>();
    private final List<Dial> reverbDials = new ArrayList<>();
    private final List<Dial> chorusDials = new ArrayList<>();
    private final List<Dial> delayDials = new ArrayList<>();

    // Reference to synthesizer for sending MIDI
    private final Synthesizer synthesizer;
    private boolean updatingUI = false;

    // Solo state tracking
    private boolean soloActive = false;

    /**
     * Create a mixer panel with controls for all tracks
     *
     * @param synthesizer The synthesizer to control
     */
    public MixerPanel(Synthesizer synthesizer) {
        super(new BorderLayout());
        this.synthesizer = synthesizer;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setupUI();

        // CommandBus.getInstance().register(this);
    }

    /**
     * Set up the mixer UI components with vertical channel strips
     */
    private void setupUI() {
        // Clear all collections before rebuilding UI
        volumeDials.clear();
        panDials.clear();
        muteButtons.clear();
        soloButtons.clear();
        reverbDials.clear();
        chorusDials.clear();
        delayDials.clear();

        // Create main container with horizontal layout for channel strips
        JPanel mixerPanel = new JPanel();
        mixerPanel.setLayout(new BoxLayout(mixerPanel, BoxLayout.X_AXIS));

        // Create track labels panel (left side instead of header row)
        JPanel trackLabelsPanel = createTrackLabelsPanel();
        mixerPanel.add(trackLabelsPanel);

        // Add all mixer channels side by side (including drums, mono tracks and master)
        for (int i = 0; i < trackNames.length; i++) {
            JPanel channelStrip = createVerticalChannelStrip(i, trackNames[i], trackChannels[i]);
            mixerPanel.add(channelStrip);

            // Add separator except after the last channel
            // REDUCED: from 5 to 2
            if (i < trackNames.length - 1) {
                mixerPanel.add(Box.createHorizontalStrut(2));
            }
        }

        // Put mixer panel in scroll pane
        JScrollPane scrollPane = new JScrollPane(mixerPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Add mixer panel to main layout
        add(scrollPane, BorderLayout.CENTER);

        // Add global controls at bottom (master effects)
        JPanel globalControls = createGlobalControlsPanel();
        add(globalControls, BorderLayout.SOUTH);

        // Initialize all MIDI controls with current values
        initializeControlValues();
    }

    /**
     * Create track labels panel (left side)
     */
    private JPanel createTrackLabelsPanel() {
        JPanel labelsPanel = new JPanel(new GridLayout(7, 1, 0, 2));
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Add section labels
        JLabel controlLabel = new JLabel("Control", JLabel.LEFT);
        controlLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(controlLabel);

        JLabel muteLabel = new JLabel("Mute/Solo", JLabel.LEFT);
        muteLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(muteLabel);

        JLabel volumeLabel = new JLabel("Volume", JLabel.LEFT);
        volumeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(volumeLabel);

        JLabel panLabel = new JLabel("Pan", JLabel.LEFT);
        panLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(panLabel);

        JLabel reverbLabel = new JLabel("Reverb", JLabel.LEFT);
        reverbLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(reverbLabel);

        JLabel chorusLabel = new JLabel("Chorus", JLabel.LEFT);
        chorusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(chorusLabel);

        JLabel delayLabel = new JLabel("Delay", JLabel.LEFT);
        delayLabel.setFont(new Font("Arial", Font.BOLD, 12));
        labelsPanel.add(delayLabel);

        return labelsPanel;
    }

    /**
     * Create a vertical channel strip for a specific track
     */
    private JPanel createVerticalChannelStrip(int index, String name, int midiChannel) {
        JPanel panel = new JPanel(new GridLayout(7, 1, 0, 2));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        boolean isMaster = midiChannel == -1;

        // Track name with stylized border
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.deepNavy),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)));

        JLabel nameLabel = new JLabel(name, JLabel.CENTER);
        nameLabel.setPreferredSize(new Dimension(80, 20));
        nameLabel.setFont(new Font("Arial", Font.BOLD, isMaster ? 12 : 11));
        if (isMaster) {
            nameLabel.setForeground(UIHelper.mutedRed);
        }
        namePanel.add(nameLabel);
        panel.add(namePanel);

        // Mute/Solo buttons panel
        JPanel muteSoloPanel = new JPanel(new GridLayout(1, 2, 2, 0));

        // Create mute button
        JToggleButton muteButton = new JToggleButton("M");
        muteButton.setFont(new Font("Arial", Font.BOLD, 10));
        muteButton.setToolTipText("Mute " + name);
        muteButton.setFocusable(false);

        // Create solo button
        JToggleButton soloButton = new JToggleButton("S");
        soloButton.setFont(new Font("Arial", Font.BOLD, 10));
        soloButton.setToolTipText("Solo " + name);
        soloButton.setFocusable(false);
        soloButton.setEnabled(!isMaster); // Master can't be soloed

        // Add buttons to panel
        muteSoloPanel.add(muteButton);
        muteSoloPanel.add(soloButton);
        panel.add(wrapInCenteredPanel(muteSoloPanel));

        // Volume dial
        Dial volumeDial = new Dial();
        volumeDial.setPreferredSize(new Dimension(50, 50));
        volumeDial.setValue(100); // Default volume
        volumeDial.setToolTipText("Volume");
        volumeDial.setKnobColor(UIHelper.getDialColor("volume"));
        volumeDials.add(volumeDial);
        panel.add(wrapInCenteredPanel(volumeDial));

        // Pan dial
        Dial panDial = new Dial();
        panDial.setValue(64); // Center
        panDial.setEnabled(!isMaster); // Master has no pan
        panDial.setToolTipText("Pan");
        panDial.setPreferredSize(new Dimension(50, 50));
        panDial.setKnobColor(UIHelper.getDialColor("pan"));
        panDials.add(panDial);
        panel.add(wrapInCenteredPanel(panDial));

        // Reverb dial
        Dial reverbDial = new Dial();
        reverbDial.setValue(0);
        reverbDial.setToolTipText("Reverb Send");
        reverbDial.setPreferredSize(new Dimension(50, 50));
        reverbDial.setKnobColor(UIHelper.getDialColor("reverb"));
        reverbDials.add(reverbDial);
        panel.add(wrapInCenteredPanel(reverbDial));

        // Chorus dial
        Dial chorusDial = new Dial();
        chorusDial.setValue(0);
        chorusDial.setToolTipText("Chorus Send");
        chorusDial.setPreferredSize(new Dimension(50, 50));
        chorusDial.setKnobColor(UIHelper.getDialColor("chorus"));
        chorusDials.add(chorusDial);
        panel.add(wrapInCenteredPanel(chorusDial));

        // Delay dial
        Dial delayDial = new Dial();
        delayDial.setValue(0);
        delayDial.setToolTipText("Delay Send");
        delayDial.setPreferredSize(new Dimension(50, 50));
        delayDial.setKnobColor(UIHelper.getDialColor("delay"));
        delayDials.add(delayDial);
        panel.add(wrapInCenteredPanel(delayDial));

        // Store buttons for later reference
        muteButtons.add(muteButton);
        soloButtons.add(soloButton);

        // Add listeners for controls
        setupControlListeners(index, midiChannel, volumeDial, panDial,
                reverbDial, chorusDial, delayDial,
                muteButton, soloButton);

        return panel;
    }

    /**
     * Create global effects panel
     */
    private JPanel createGlobalControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(UIHelper.deepNavy),
                        "Master Effects",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 12),
                        UIHelper.deepNavy),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));

        // Add global effect dials: reverb time, delay time, delay feedback
        panel.add(createGlobalEffectControl("Reverb Decay", 103));
        panel.add(Box.createHorizontalStrut(5));
        panel.add(createGlobalEffectControl("Delay Time", 102));
        panel.add(Box.createHorizontalStrut(5));
        panel.add(createGlobalEffectControl("Delay Feedback", 104));

        // Add reset button at the end
        JButton resetButton = new JButton("Reset All");
        resetButton.addActionListener(e -> resetAllControls());
        panel.add(Box.createHorizontalGlue());
        panel.add(resetButton);

        return panel;
    }

    /**
     * Create a global effect control with label and dial
     */
    private JPanel createGlobalEffectControl(String name, int ccNumber) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create label
        JLabel label = new JLabel(name, SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font("Arial", Font.PLAIN, 11));

        // Create dial
        Dial dial = new Dial();
        dial.setValue(0);
        dial.setPreferredSize(new Dimension(40, 40));
        dial.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add listener to send MIDI CC
        dial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int value = dial.getValue();
            // Apply to all channels except master
            for (int i = 0; i < trackChannels.length - 1; i++) {
                sendMidiCC(trackChannels[i], ccNumber, value);
            }
        });

        panel.add(label);
        panel.add(dial);

        return panel;
    }

    /**
     * Setup listeners for all mixer controls
     */
    private void setupControlListeners(int index, int midiChannel,
                                       Dial volumeDial, Dial panDial,
                                       Dial reverbDial, Dial chorusDial, Dial delayDial,
                                       JToggleButton muteButton, JToggleButton soloButton) {

        // Volume dial listener
        volumeDial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int volume = volumeDial.getValue();
            if (midiChannel == -1) {
                // Master channel - apply to all channels
                for (int i = 0; i < trackChannels.length - 1; i++) {
                    if (!muteButtons.get(i).isSelected() &&
                            (!soloActive || soloButtons.get(i).isSelected())) {
                        sendMidiCC(trackChannels[i], CC_VOLUME, volume);
                    }
                }
            } else {
                // Regular channel
                if (!muteButton.isSelected() && (!soloActive || soloButton.isSelected())) {
                    sendMidiCC(midiChannel, CC_VOLUME, volume);
                }
            }
        });

        // Pan dial listener
        panDial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int pan = panDial.getValue();
            if (midiChannel >= 0) {
                sendMidiCC(midiChannel, CC_PAN, pan);
            }
        });

        // Reverb dial listener
        reverbDial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int reverb = reverbDial.getValue();
            if (midiChannel == -1) {
                // Master channel - apply to all
                for (int i = 0; i < trackChannels.length - 1; i++) {
                    sendMidiCC(trackChannels[i], CC_REVERB, reverb);
                }
            } else {
                sendMidiCC(midiChannel, CC_REVERB, reverb);
            }
        });

        // Chorus dial listener
        chorusDial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int chorus = chorusDial.getValue();
            if (midiChannel == -1) {
                // Master channel - apply to all
                for (int i = 0; i < trackChannels.length - 1; i++) {
                    sendMidiCC(trackChannels[i], CC_CHORUS, chorus);
                }
            } else {
                sendMidiCC(midiChannel, CC_CHORUS, chorus);
            }
        });

        // Delay dial listener
        delayDial.addChangeListener(e -> {
            if (updatingUI)
                return;

            int delay = delayDial.getValue();
            if (midiChannel == -1) {
                // Master channel - apply to all
                for (int i = 0; i < trackChannels.length - 1; i++) {
                    sendMidiCC(trackChannels[i], CC_DELAY, delay);
                }
            } else {
                sendMidiCC(midiChannel, CC_DELAY, delay);
            }
        });

        // Mute button listener
        muteButton.addActionListener(e -> {
            if (updatingUI)
                return;

            boolean muted = muteButton.isSelected();
            if (midiChannel == -1) {
                // Master mute - mute all tracks
                for (int i = 0; i < trackChannels.length - 1; i++) {
                    muteChannel(i, muted);
                }
            } else {
                muteChannel(index, muted);
            }
        });

        // Solo button listener
        soloButton.addActionListener(e -> {
            if (updatingUI)
                return;

            updateSoloState();
        });
    }

    /**
     * Helper method to mute a specific channel
     */
    private void muteChannel(int index, boolean muted) {
        int channel = trackChannels[index];
        if (channel < 0)
            return; // Skip master

        // Get the volume from the dial instead of slider
        int volume = muted ? 0 : volumeDials.get(index).getValue();
        sendMidiCC(channel, CC_VOLUME, volume);
    }

    /**
     * Update solo state for all channels
     */
    private void updateSoloState() {
        // Check if any solo button is active
        boolean anySolo = false;
        for (JToggleButton soloBtn : soloButtons) {
            if (soloBtn.isSelected()) {
                anySolo = true;
                break;
            }
        }

        soloActive = anySolo;

        // Update all channels
        for (int i = 0; i < trackChannels.length; i++) {
            int channel = trackChannels[i];
            if (channel < 0)
                continue; // Skip master

            boolean muted = muteButtons.get(i).isSelected();
            boolean soloed = soloButtons.get(i).isSelected();
            boolean effectivelyMuted = muted || (soloActive && !soloed);

            // Get the volume from the dial
            int volume = effectivelyMuted ? 0 : volumeDials.get(i).getValue();
            sendMidiCC(channel, CC_VOLUME, volume);
        }
    }

    /**
     * Send a MIDI CC message to a specific channel
     */
    private void sendMidiCC(int midiChannel, int cc, int value) {
        if (midiChannel < 0 || synthesizer == null)
            return;

        try {
            MidiChannel channel = synthesizer.getChannels()[midiChannel];
            if (channel != null) {
                channel.controlChange(cc, value);
            }
        } catch (Exception e) {
            logger.error("Error sending MIDI CC: {}", e.getMessage());
        }
    }

    /**
     * Initialize control values based on existing MIDI state
     */
    private void initializeControlValues() {
        if (synthesizer == null)
            return;

        updatingUI = true;
        try {
            // Set all channels to reasonable defaults
            for (int i = 0; i < trackChannels.length - 1; i++) {
                int channel = trackChannels[i];
                if (channel < 0)
                    continue;

                // Add defensive checks to ensure collections are properly populated
                if (i < volumeDials.size()) {
                    volumeDials.get(i).setValue(100);
                    sendMidiCC(channel, CC_VOLUME, 100);
                }

                if (i < panDials.size()) {
                    panDials.get(i).setValue(64);
                    sendMidiCC(channel, CC_PAN, 64);
                }

                if (i < reverbDials.size()) {
                    reverbDials.get(i).setValue(0);
                    sendMidiCC(channel, CC_REVERB, 0);
                }

                if (i < chorusDials.size()) {
                    chorusDials.get(i).setValue(0);
                    sendMidiCC(channel, CC_CHORUS, 0);
                }

                if (i < delayDials.size()) {
                    delayDials.get(i).setValue(0);
                    sendMidiCC(channel, CC_DELAY, 0);
                }
            }

            // Set master to default (with defensive check)
            if (volumeDials.size() > trackChannels.length - 1) {
                volumeDials.getLast().setValue(100);
            }

        } finally {
            updatingUI = false;
        }
    }

    /**
     * Reset all controls to default values
     */
    private void resetAllControls() {
        updatingUI = true;
        try {
            // Reset all channel strips
            for (int i = 0; i < trackChannels.length; i++) {
                if (i < volumeDials.size())
                    volumeDials.get(i).setValue(100);
                if (i < panDials.size())
                    panDials.get(i).setValue(64);
                if (i < reverbDials.size())
                    reverbDials.get(i).setValue(0);
                if (i < chorusDials.size())
                    chorusDials.get(i).setValue(0);
                if (i < delayDials.size())
                    delayDials.get(i).setValue(0);
                if (i < muteButtons.size())
                    muteButtons.get(i).setSelected(false);
                if (i < soloButtons.size())
                    soloButtons.get(i).setSelected(false);
            }

            soloActive = false;
        } finally {
            updatingUI = false;
        }

        // Apply changes to MIDI
        for (int i = 0; i < trackChannels.length - 1; i++) {
            int channel = trackChannels[i];
            if (channel >= 0) {
                sendMidiCC(channel, CC_VOLUME, 100);
                sendMidiCC(channel, CC_PAN, 64);
                sendMidiCC(channel, CC_REVERB, 0);
                sendMidiCC(channel, CC_CHORUS, 0);
                sendMidiCC(channel, CC_DELAY, 0);
            }
        }
    }

    /**
     * Helper method to wrap a component in a centered panel
     */
    private JPanel wrapInCenteredPanel(Component component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.add(component);
        return panel;
    }

    /**
     * Handle commands from the CommandBus
     */
    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START:
                // Nothing to do
                break;

            case Commands.TRANSPORT_STOP:
                // Nothing to do
                break;

            // Add any other relevant command handling
        }
    }
}
