package com.angrysurfer.beats.panel.player;

import com.angrysurfer.beats.widget.ChannelCombo;
import com.angrysurfer.beats.widget.InstrumentCombo;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.preset.DrumItem;
import com.angrysurfer.core.model.preset.PresetItem;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.MidiService;
import com.angrysurfer.core.service.SoundbankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streamlined panel for editing basic player properties
 */
public class PlayerEditBasicPropertiesPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditBasicPropertiesPanel.class);
    private static final int PREVIEW_DURATION_MS = 500;
    // Service references
    private final SoundbankService soundbankManager = SoundbankService.getInstance();
    private final MidiService synthManager = MidiService.getInstance();

    // State tracking
    private final AtomicBoolean initializing = new AtomicBoolean(false);
    // Player reference
    private Player player;
    // UI Components
    private JTextField nameField;
    private InstrumentCombo instrumentCombo;
    private ChannelCombo channelCombo;
    private JPanel presetControlPanel;
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private JComboBox<PresetItem> presetCombo;
    private JComboBox<DrumItem> drumCombo;
    private JButton previewButton;
    private JButton loadSoundbankButton;
    private boolean isDrumChannel = false;
    private boolean isInternalSynth = false;

    /**
     * Create a new panel for editing basic player properties
     */
    public PlayerEditBasicPropertiesPanel(Player player) {
        super(new GridBagLayout());
        this.player = player;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Basic Properties"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Initialize state
        updatePlayerState();

        // Set up UI
        initComponents();
        layoutComponents();
        updateControls();
    }

    /**
     * Update player state flags
     */
    private void updatePlayerState() {
        if (player != null) {
            isDrumChannel = player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL;
            isInternalSynth = player.getInstrument() != null &&
                    synthManager.isInternalSynth(player.getInstrument());
        }
    }

    /**
     * Initialize UI components
     */
    private void initComponents() {
        initializing.set(true);
        try {
            // Basic fields
            nameField = new JTextField(player.getName(), 20);

            // Specialized controls
            instrumentCombo = new InstrumentCombo();
            instrumentCombo.setCurrentPlayer(player);
            instrumentCombo.addActionListener(e -> {
                if (!initializing.get() && player != null) {
                    // Update internal/drum state and refresh UI
                    updatePlayerState();
                    SwingUtilities.invokeLater(this::updateControls);
                }
            });

            channelCombo = new ChannelCombo();
            channelCombo.setCurrentPlayer(player);
            channelCombo.addActionListener(e -> {
                if (!initializing.get() && player != null) {
                    // Check if we switched to/from drum channel
                    boolean wasDrumChannel = isDrumChannel;
                    isDrumChannel = player.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL;

                    if (wasDrumChannel != isDrumChannel) {
                        SwingUtilities.invokeLater(this::updateControls);
                    }
                }
            });

            // Container for preset controls that will change based on state
            presetControlPanel = new JPanel(new BorderLayout());

            // Sound controls
            soundbankCombo = new JComboBox<>();
            soundbankCombo.addActionListener(e -> {
                if (!initializing.get() && soundbankCombo.getSelectedItem() != null) {
                    onSoundbankChanged();
                }
            });

            bankCombo = new JComboBox<>();
            bankCombo.addActionListener(e -> {
                if (!initializing.get() && bankCombo.getSelectedItem() != null) {
                    onBankChanged();
                }
            });

            presetCombo = new JComboBox<>();
            presetCombo.addActionListener(e -> {
                if (!initializing.get() && presetCombo.getSelectedItem() != null) {
                    onPresetChanged();
                }
            });

            // Drum kit control
            drumCombo = new JComboBox<>();
            drumCombo.addActionListener(e -> {
                if (!initializing.get() && drumCombo.getSelectedItem() instanceof DrumItem) {
                    onDrumChanged();
                }
            });

            // Action buttons
            previewButton = new JButton("Preview");
            previewButton.addActionListener(e -> playPreview());

            loadSoundbankButton = new JButton("Load...");
            loadSoundbankButton.addActionListener(e -> loadSoundbankFile());
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Set up the components layout
     */
    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Name + Instrument + Channel row
        JPanel topRow = new JPanel(new GridBagLayout());
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.insets = new Insets(2, 5, 2, 5);
        topGbc.fill = GridBagConstraints.HORIZONTAL;

        // Name field
        topGbc.gridx = 0;
        topGbc.gridy = 0;
        topGbc.weightx = 0;
        topRow.add(new JLabel("Name:"), topGbc);

        topGbc.gridx = 1;
        topGbc.weightx = 0.3;
        nameField.setPreferredSize(new Dimension(100, nameField.getPreferredSize().height));
        topRow.add(nameField, topGbc);

        // Instrument field
        topGbc.gridx = 2;
        topGbc.weightx = 0;
        topRow.add(new JLabel("Instrument:"), topGbc);

        topGbc.gridx = 3;
        topGbc.weightx = 0.5;
        instrumentCombo.setPreferredSize(new Dimension(150, instrumentCombo.getPreferredSize().height));
        topRow.add(instrumentCombo, topGbc);

        // Channel field
        topGbc.gridx = 4;
        topGbc.weightx = 0;
        topRow.add(new JLabel("Channel:"), topGbc);

        topGbc.gridx = 5;
        topGbc.weightx = 0.1;
        channelCombo.setPreferredSize(new Dimension(60, channelCombo.getPreferredSize().height));
        topRow.add(channelCombo, topGbc);

        // Add top row to main panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        add(topRow, gbc);

        // Soundbank + Bank row (for internal synth)
        JPanel soundbankRow = new JPanel(new GridBagLayout());
        GridBagConstraints sbGbc = new GridBagConstraints();
        sbGbc.insets = new Insets(2, 5, 2, 5);
        sbGbc.fill = GridBagConstraints.HORIZONTAL;

        sbGbc.gridx = 0;
        sbGbc.gridy = 0;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Soundbank:"), sbGbc);

        sbGbc.gridx = 1;
        sbGbc.weightx = 1.0;
        soundbankRow.add(soundbankCombo, sbGbc);

        sbGbc.gridx = 2;
        sbGbc.weightx = 0.0;
        soundbankRow.add(new JLabel("Bank:"), sbGbc);

        sbGbc.gridx = 3;
        sbGbc.weightx = 0.0;
        bankCombo.setPreferredSize(new Dimension(60, bankCombo.getPreferredSize().height));
        soundbankRow.add(bankCombo, sbGbc);

        sbGbc.gridx = 4;
        sbGbc.weightx = 0.0;
        soundbankRow.add(loadSoundbankButton, sbGbc);

        // Add soundbank row to main panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        add(soundbankRow, gbc);

        // Preset row
        JPanel presetRow = new JPanel(new GridBagLayout());
        GridBagConstraints prGbc = new GridBagConstraints();
        prGbc.insets = new Insets(2, 5, 2, 5);
        prGbc.fill = GridBagConstraints.HORIZONTAL;

        prGbc.gridx = 0;
        prGbc.gridy = 0;
        prGbc.weightx = 0.0;
        presetRow.add(new JLabel("Preset:"), prGbc);

        prGbc.gridx = 1;
        prGbc.weightx = 1.0;
        prGbc.fill = GridBagConstraints.HORIZONTAL;
        presetRow.add(presetControlPanel, prGbc);

        prGbc.gridx = 2;
        prGbc.weightx = 0.0;
        prGbc.fill = GridBagConstraints.NONE;
        presetRow.add(previewButton, prGbc);

        // Add preset row to main panel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        add(presetRow, gbc);

        // Empty space (for expansion)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gbc);
    }

    /**
     * Update controls based on current player state (drum vs melodic, internal vs external)
     */
    private void updateControls() {
        initializing.set(true);
        try {
            // Clear preset control panel
            presetControlPanel.removeAll();

            // First determine if we need soundbank controls
            boolean showSoundbankControls = isInternalSynth && !isDrumChannel;

            // Update visibility
            soundbankCombo.setEnabled(showSoundbankControls);
            bankCombo.setEnabled(showSoundbankControls);
            loadSoundbankButton.setEnabled(showSoundbankControls);

            if (isDrumChannel) {
                // For drum channel, show drum selector
                presetControlPanel.add(drumCombo, BorderLayout.CENTER);

                // Populate drums
                populateDrumCombo();
            } else {
                // For melodic channel, show preset combo
                presetControlPanel.add(presetCombo, BorderLayout.CENTER);

                if (isInternalSynth) {
                    // For internal synth, populate soundbank, bank, preset
                    refreshSoundControls();
                } else {
                    // For external instrument, just populate preset combo with GM
                    populateStandardPresets();
                }
            }

            // Update layout
            revalidate();
            repaint();
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Refresh all sound controls using SoundbankManager
     */
    private void refreshSoundControls() {
        if (player != null && player.getInstrument() != null) {
            soundbankManager.updateInstrumentUIComponents(
                    player.getInstrument(),
                    soundbankCombo,
                    bankCombo,
                    presetCombo);
        }
    }

    /**
     * Populate the preset combo with GM presets for external instruments
     */
    private void populateStandardPresets() {
        presetCombo.removeAllItems();

        List<String> gmPresets = soundbankManager.getGeneralMIDIPresetNames();
        for (int i = 0; i < gmPresets.size(); i++) {
            presetCombo.addItem(new PresetItem(i, gmPresets.get(i)));
        }

        // Try to select current preset
        if (player != null && player.getInstrument() != null &&
                player.getInstrument().getPreset() != null) {
            for (int i = 0; i < presetCombo.getItemCount(); i++) {
                PresetItem item = presetCombo.getItemAt(i);
                if (item.getNumber() == player.getInstrument().getPreset()) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Populate the drum combo with GM drum sounds
     */
    private void populateDrumCombo() {
        drumCombo.removeAllItems();

        List<DrumItem> drums = soundbankManager.getDrumItems();
        for (DrumItem drum : drums) {
            drumCombo.addItem(drum);
        }

        // Try to select current drum sound
        if (player != null && player.getRootNote() != null) {
            for (int i = 0; i < drumCombo.getItemCount(); i++) {
                DrumItem item = drumCombo.getItemAt(i);
                if (item.getNoteNumber() == player.getRootNote()) {
                    drumCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    /**
     * Handle soundbank selection change
     */
    private void onSoundbankChanged() {
        if (player != null && player.getInstrument() != null &&
                soundbankCombo.getSelectedItem() != null) {

            String soundbank = soundbankCombo.getSelectedItem().toString();
            player.getInstrument().setSoundbankName(soundbank);

            // Update available banks for this soundbank
            initializing.set(true);
            try {
                bankCombo.removeAllItems();
                List<Integer> banks = soundbankManager.getAvailableBanksByName(soundbank);
                for (Integer bank : banks) {
                    bankCombo.addItem(bank);
                }

                // Select first bank and update presets
                if (bankCombo.getItemCount() > 0) {
                    bankCombo.setSelectedIndex(0);
                    onBankChanged();
                }
            } finally {
                initializing.set(false);
            }

            // Apply changes
            applyChanges();
        }
    }

    /**
     * Handle bank selection change
     */
    private void onBankChanged() {
        if (player != null && player.getInstrument() != null &&
                bankCombo.getSelectedItem() != null) {

            Integer bank = (Integer) bankCombo.getSelectedItem();
            player.getInstrument().setBankIndex(bank);

            // Update available presets for this bank
            initializing.set(true);
            try {
                presetCombo.removeAllItems();
                String soundbank = soundbankCombo.getSelectedItem() != null ?
                        soundbankCombo.getSelectedItem().toString() : "Java Internal Soundbank";

                List<String> presetNames = soundbankManager.getPresetNames(soundbank, bank);

                // Fall back to GM presets if none found
                if (presetNames == null || presetNames.isEmpty()) {
                    presetNames = soundbankManager.getGeneralMIDIPresetNames();
                }

                // Add presets to combo
                for (int i = 0; i < presetNames.size(); i++) {
                    String name = presetNames.get(i);
                    if (name != null && !name.isEmpty()) {
                        presetCombo.addItem(new PresetItem(i, name));
                    }
                }

                // Select first preset
                if (presetCombo.getItemCount() > 0) {
                    presetCombo.setSelectedIndex(0);
                }
            } finally {
                initializing.set(false);
            }

            // Apply changes
            applyChanges();
        }
    }

    /**
     * Handle preset selection change
     */
    private void onPresetChanged() {
        if (player != null && player.getInstrument() != null &&
                presetCombo.getSelectedItem() instanceof PresetItem item) {
            player.getInstrument().setPreset(item.getNumber());
            // Apply changes
            applyChanges();
        }
    }

    /**
     * Handle drum selection change
     */
    private void onDrumChanged() {
        if (player != null && drumCombo.getSelectedItem() instanceof DrumItem drum) {
            player.setRootNote(drum.getNoteNumber());
            player.setName(MidiService.getInstance().getDrumName(player.getRootNote()));
            // Apply changes
            applyChanges();
        }
    }

    /**
     * Apply all changes to the player and update system state
     */
    void applyChanges() {
        if (player == null || initializing.get()) {
            return;
        }

        try {
            // Apply name change
            player.setName(nameField.getText());

            // No need to apply instrument or channel changes - these components
            // handle that automatically

            // For internal synth, apply the current preset
            if (isInternalSynth && player.getInstrument() != null) {
                soundbankManager.updatePlayerSound(
                        player,
                        player.getInstrument().getSoundbankName(),
                        player.getInstrument().getBankIndex(),
                        player.getInstrument().getPreset()
                );
            }

            // Publish player update event
            CommandBus.getInstance().publish(
                    Commands.PLAYER_UPDATE_EVENT,
                    this,
                    new PlayerUpdateEvent(this, player)
            );
        } catch (Exception e) {
            logger.error("Error applying changes: {}", e.getMessage(), e);
        }
    }

    /**
     * Update the panel from a player instance
     */
    public void updateFromPlayer(Player newPlayer) {
        if (newPlayer == null) {
            return;
        }

        // Update player reference
        this.player = newPlayer;

        // Update state
        updatePlayerState();

        // Update UI components
        initializing.set(true);
        try {
            // Basic fields
            nameField.setText(player.getName());

            // Specialized controls update
            instrumentCombo.setCurrentPlayer(player);
            channelCombo.setCurrentPlayer(player);

            // Update all controls based on state
            updateControls();
        } finally {
            initializing.set(false);
        }
    }

    /**
     * Play a preview of the current sound
     */
    private void playPreview() {
        if (player == null) {
            return;
        }

        soundbankManager.playPreviewNote(player, PREVIEW_DURATION_MS);
    }

    /**
     * Load a new soundbank file
     */
    private void loadSoundbankFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".sf2") ||
                        f.getName().toLowerCase().endsWith(".dls");
            }

            @Override
            public String getDescription() {
                return "Soundbank Files (*.sf2, *.dls)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            // Load soundbank through SoundbankManager
            CommandBus.getInstance().publish(Commands.LOAD_SOUNDBANK, this, file);

            // Wait for soundbank loaded event
            CommandBus.getInstance().registerOneTime(action -> {
                if (Commands.SOUNDBANK_LOADED.equals(action.getCommand()) &&
                        action.getData() instanceof String) {

                    SwingUtilities.invokeLater(this::refreshSoundControls);
                }
            }, new String[]{Commands.SOUNDBANK_LOADED});
        }
    }
}
