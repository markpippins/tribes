package com.angrysurfer.beats.panel.internalsynth;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SoundbankManager;

import lombok.Getter;
import lombok.Setter;

/**
 * Panel for controlling a MIDI synthesizer with tabs for oscillator, envelope,
 * filter and modulation parameters.
 */
@Getter
@Setter
public class InternalSynthControlPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(InternalSynthControlPanel.class);

    // Remove synthesizer field - now managed by InternalSynthManager
    private JComboBox<PresetItem> presetCombo;
    private int midiChannel = 15; // Default to channel 16 (zero-indexed as 15)
    private JComboBox<String> channelCombo; // Add this field
    private JComboBox<String> soundbankCombo;
    private JComboBox<Integer> bankCombo;
    private InternalSynthOscillatorPanel[] oscillatorPanels;
    private InternalSynthLFOPanel lfoPanel;
    private InternalSynthFilterPanel filterPanel;
    private InternalSynthEnvelopePanel envelopePanel;
    private InternalSynthEffectsPanel effectsPanel;
    private InternalSynthMixerPanel mixerPanel;

    // Track the currently selected soundbank name
    private String currentSoundbankName = null;

    /**
     * Create a new SynthControlPanel with its own synthesizer
     */
    public InternalSynthControlPanel() {
        super(new BorderLayout(5, 5));

        // No longer initializing synthesizer here - using singleton instead
        // Just ensure it's available
        InternalSynthManager.getInstance().getSynthesizer();

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setupUI();
    }

    /**
     * Get the synthesizer from InternalSynthManager
     */
    public Synthesizer getSynthesizer() {
        return InternalSynthManager.getInstance().getSynthesizer();
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Create preset selection at top
        JPanel soundPanel = createSoundPanel();
        add(soundPanel, BorderLayout.NORTH);

        // Main panel with tabs
        JTabbedPane paramTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        paramTabs.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        paramTabs.addTab("Parameters", createParamsPanel());
        add(paramTabs, BorderLayout.CENTER);

        // Create piano panel
        InternalSynthPianoPanel pianoPanel = new InternalSynthPianoPanel();

        // Add to a container panel to center it horizontally
        JPanel pianoContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pianoContainer.add(pianoPanel);

        // Add to the bottom of the main panel
        add(pianoContainer, BorderLayout.SOUTH);
    }

    /**
     * Set a MIDI CC value on the synth
     *
     * @param ccNumber The CC number to set
     * @param value    The value to set (0-127)
     */
    protected void setControlChange(int ccNumber, int value) {
        setControlChange(midiChannel, ccNumber, value);
    }

    /**
     * Set a MIDI CC value on the synth with specified channel
     *
     * @param channel  MIDI channel (0-15)
     * @param ccNumber The CC number to set
     * @param value    The value to set (0-127)
     */
    protected void setControlChange(int channel, int ccNumber, int value) {
        // Delegate to InternalSynthManager
        InternalSynthManager.getInstance().setControlChange(channel, ccNumber, value);
    }

    /**
     * Inner class to represent preset items in the combo box
     */
    private static class PresetItem {

        private final int number;
        private final String name;

        public PresetItem(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Create a panel with soundbank and preset selectors
     */
    private JPanel createSoundPanel() {
        JPanel soundPanel = new JPanel(new BorderLayout(5, 5));
        soundPanel.setBorder(BorderFactory.createTitledBorder("Sounds"));

        // Create a single row panel for all controls
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));

        // 1. Soundbank selector section
        JPanel soundbankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        soundbankSection.add(new JLabel("Soundbank:"));
        soundbankCombo = new JComboBox<>();
        soundbankCombo.setPrototypeDisplayValue("SoundFont 2.0 (Default)XXXXXX");
        soundbankSection.add(soundbankCombo);

        // Load soundbank button
        JButton loadSoundbankBtn = new JButton("Load...");
        loadSoundbankBtn.addActionListener(e -> loadSoundbankFile());
        soundbankSection.add(loadSoundbankBtn);

        // 2. Bank selector section
        JPanel bankSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bankSection.add(new JLabel("Bank:"));
        bankCombo = new JComboBox<>();
        bankCombo.setPreferredSize(new Dimension(60, 25));
        bankSection.add(bankCombo);

        // 3. MIDI Channel selector section - ADD THIS NEW SECTION
        JPanel channelSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        channelSection.add(new JLabel("MIDI Ch:"));
        channelCombo = new JComboBox<>();
        channelCombo.setPreferredSize(new Dimension(60, 25));
        // Add all 16 MIDI channels (displayed as 1-16 but stored as 0-15)
        for (int i = 1; i <= 16; i++) {
            channelCombo.addItem(Integer.toString(i));
        }
        // Set default to channel 16
        channelCombo.setSelectedIndex(15);
        channelSection.add(channelCombo);
        
        // Add listener to update the channel
        channelCombo.addActionListener(e -> {
            int selectedIndex = channelCombo.getSelectedIndex();
            updateMidiChannel(selectedIndex);
        });

        // 4. Preset selector section
        JPanel presetSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetSection.add(new JLabel("Preset:"));
        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(new Dimension(250, 25));
        presetSection.add(presetCombo);

        // Add all sections to the row
        controlsRow.add(soundbankSection);
        controlsRow.add(bankSection);
        controlsRow.add(channelSection); // Add the new channel section
        controlsRow.add(presetSection);

        // Add the controls row to the main panel
        soundPanel.add(controlsRow, BorderLayout.NORTH);

        // Initialize default soundbank
        initializeSoundbanks();

        // Add listeners
        soundbankCombo.addActionListener(e -> {
            if (soundbankCombo.getSelectedItem() != null) {
                String selectedName = (String) soundbankCombo.getSelectedItem();
                selectSoundbank(selectedName);
            }
        });

        bankCombo.addActionListener(e -> {
            if (bankCombo.getSelectedIndex() >= 0) {
                populatePresetComboForBank((Integer) bankCombo.getSelectedItem());
            }
        });

        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                int bank = bankCombo.getSelectedIndex() >= 0 ? (Integer) bankCombo.getSelectedItem() : 0;
                setProgramChange(bank, item.getNumber());
                logger.info("Selected preset: {} (Bank {}, Program {})", item.getName(), bank, item.getNumber());
            }
        });

        return soundPanel;
    }

    /**
     * Update the MIDI channel and reinitialize controllers
     */
    private void updateMidiChannel(int newChannel) {
        // Store previous channel to handle cleanup
        int oldChannel = midiChannel;
        
        // Update the channel
        midiChannel = newChannel;
        logger.info("Changed MIDI channel to {} (was {})", newChannel + 1, oldChannel + 1);
        
        // Send all notes off to the previous channel
        if (getSynthesizer() != null && getSynthesizer().isOpen()) {
            try {
                getSynthesizer().getChannels()[oldChannel].allNotesOff();
                getSynthesizer().getChannels()[oldChannel].allSoundOff();
            } catch (Exception ex) {
                logger.error("Error cleaning up old channel: {}", ex.getMessage());
            }
        }

        // Update oscillator panels with new channel
        for (InternalSynthOscillatorPanel panel : oscillatorPanels) {
            panel.setMidiChannel(midiChannel);
        }
        
        // Update other panels with new channel
        if (lfoPanel != null) lfoPanel.setMidiChannel(midiChannel);
        if (filterPanel != null) filterPanel .setMidiChannel(midiChannel);
        if (envelopePanel != null) envelopePanel.setMidiChannel(midiChannel);
        if (effectsPanel != null) effectsPanel.setMidiChannel(midiChannel);
        if (mixerPanel != null) mixerPanel.setMidiChannel(midiChannel);
        
        // Reinitialize controllers on the new channel
        reinitializeControllers();
        
        // If needed, re-select the current program on the new channel
        if (presetCombo.getSelectedItem() instanceof PresetItem) {
            PresetItem item = (PresetItem) presetCombo.getSelectedItem();
            int bank = bankCombo.getSelectedIndex() >= 0 ? (Integer) bankCombo.getSelectedItem() : 0;
            setProgramChange(bank, item.getNumber());
        }
    }

    /**
     * Initialize available soundbanks
     */
    private void initializeSoundbanks() {
        try {
            // Use the manager instance to initialize soundbanks
            InternalSynthManager manager = InternalSynthManager.getInstance();
            manager.loadDefaultSoundbank();

            // Update UI with data from the manager
            soundbankCombo.removeAllItems();
            List<String> names = manager.getSoundbankNames();
            for (String name : names) {
                soundbankCombo.addItem(name);
            }

            // Temporarily remove the action listener to prevent double-triggering
            ActionListener[] listeners = soundbankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                soundbankCombo.removeActionListener(listener);
            }

            // Select the first soundbank
            if (soundbankCombo.getItemCount() > 0) {
                soundbankCombo.setSelectedIndex(0);
                String selectedName = (String) soundbankCombo.getSelectedItem();
                currentSoundbankName = selectedName;

                // Explicitly populate banks for this soundbank
                populateBanksCombo(selectedName);
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                soundbankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error initializing soundbanks: {}", e.getMessage());
        }
    }

    /**
     * Select a soundbank and load it into the synthesizer
     */
    private void selectSoundbank(String soundbankName) {
        try {
            // Store the selected soundbank name
            currentSoundbankName = soundbankName;

            // Get the soundbank by name
            Soundbank soundbank = SoundbankManager.getInstance().getSoundbank(soundbankName);

            // Load the soundbank into the synthesizer if it's not null
            if (soundbank != null && getSynthesizer() != null && getSynthesizer().isOpen()) {
                // Unload any current instruments
                getSynthesizer().unloadAllInstruments(getSynthesizer().getDefaultSoundbank());

                // Load the new soundbank
                boolean loaded = getSynthesizer().loadAllInstruments(soundbank);
                if (loaded) {
                    logger.info("Loaded soundbank: {}", soundbankName);
                } else {
                    logger.error("Failed to load soundbank: {}", soundbankName);
                }
            }

            // Populate banks for this soundbank
            populateBanksCombo(soundbankName);
        } catch (Exception e) {
            logger.error("Error selecting soundbank: {}", e.getMessage());
        }
    }

    /**
     * Populate the bank combo box with available banks for a specific soundbank
     */
    private void populateBanksCombo(String soundbankName) {
        try {
            bankCombo.removeAllItems();

            // Get banks by soundbank name
            List<Integer> banks = InternalSynthManager.getInstance()
                    .getAvailableBanksByName(soundbankName);

            // Add banks to combo box
            for (Integer bank : banks) {
                bankCombo.addItem(bank);
            }

            // Temporarily remove the action listener
            ActionListener[] listeners = bankCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                bankCombo.removeActionListener(listener);
            }

            // Select the first bank
            if (bankCombo.getItemCount() > 0) {
                bankCombo.setSelectedIndex(0);

                // Explicitly populate presets for this bank
                if (bankCombo.getSelectedItem() instanceof Integer bank) {
                    populatePresetComboForBank(bank);
                }
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                bankCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error populating banks: {}", e.getMessage());
        }
    }

    /**
     * Populate the preset combo box with presets from the selected bank
     */
    private void populatePresetComboForBank(int bank) {
        try {
            presetCombo.removeAllItems();

            // Get preset names from InternalSynthManager for the current soundbank
            InternalSynthManager manager = InternalSynthManager.getInstance();
            List<String> presetNames = manager.getPresetNames(currentSoundbankName, bank);

            logger.info("Retrieved {} preset names for bank {}", presetNames.size(), bank);

            // Add all presets to the combo box with format: "0: Acoustic Grand Piano"
            for (int i = 0; i < Math.min(128, presetNames.size()); i++) {
                String presetName = presetNames.get(i);

                // Use generic name if the specific name is empty
                if (presetName == null || presetName.isEmpty()) {
                    presetName = "Program " + i;
                }

                // Add the preset to the combo box
                presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
            }

            // Temporarily remove the action listener
            ActionListener[] listeners = presetCombo.getActionListeners();
            for (ActionListener listener : listeners) {
                presetCombo.removeActionListener(listener);
            }

            // Select the first preset by default
            if (presetCombo.getItemCount() > 0) {
                presetCombo.setSelectedIndex(0);

                // Explicitly set the program change
                if (presetCombo.getSelectedItem() instanceof PresetItem item) {
                    setProgramChange(bank, item.getNumber());
                }
            }

            // Restore the action listeners
            for (ActionListener listener : listeners) {
                presetCombo.addActionListener(listener);
            }
        } catch (Exception e) {
            logger.error("Error populating presets: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank file from disk
     */
    private void loadSoundbankFile() {
        try {
            // Use DialogManager approach but implement directly here
            File soundbankFile = showSoundbankFileChooser();

            if (soundbankFile != null && soundbankFile.exists()) {
                logger.info("Loading soundbank file: {}", soundbankFile.getAbsolutePath());

                // Fix type mismatch: loadSoundbankFile returns a String (name), not a Soundbank
                String soundbankName = SoundbankManager.getInstance().loadSoundbankFile(soundbankFile);

                if (soundbankName != null) {
                    // Update UI with the new soundbank list
                    List<String> names = InternalSynthManager.getInstance().getSoundbankNames();
                    soundbankCombo.removeAllItems();
                    for (String name : names) {
                        soundbankCombo.addItem(name);
                    }

                    // Find and select the newly added soundbank by name
                    for (int i = 0; i < soundbankCombo.getItemCount(); i++) {
                        if (soundbankCombo.getItemAt(i).equals(soundbankName)) {
                            soundbankCombo.setSelectedIndex(i);
                            return;
                        }
                    }
                    
                    // Fallback - select the last added soundbank
                    soundbankCombo.setSelectedIndex(soundbankCombo.getItemCount() - 1);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank: {}", e.getMessage());
        }
    }

    /**
     * Show a file chooser dialog for selecting soundbank files
     */
    private File showSoundbankFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Soundbank File");

        // Add filters for soundbank formats
        FileNameExtensionFilter sfFilter = new FileNameExtensionFilter(
                "SoundFont Files (*.sf2, *.dls)", "sf2", "dls");
        fileChooser.addChoosableFileFilter(sfFilter);
        fileChooser.setFileFilter(sfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        // Show open dialog
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }

        return null;
    }

    /**
     * Send program change to the synthesizer with bank selection
     */
    private void setProgramChange(int bank, int program) {
        if (getSynthesizer() != null && getSynthesizer().isOpen()) {
            try {
                // Handle program changes directly since InternalSynthManager no longer has
                // sendProgramChange
                MidiChannel channel = getSynthesizer().getChannels()[midiChannel];
                if (channel != null) {
                    // Send bank select MSB (CC 0)
                    channel.controlChange(0, 0);

                    // Send bank select LSB (CC 32)
                    channel.controlChange(32, bank);

                    // Send program change
                    channel.programChange(program);

                    logger.info("Sent program change: channel={}, bank={}, program={}",
                            midiChannel, bank, program);
                }

                // Reset controls and controllers after the change
                resetControlsToDefault();
                reinitializeControllers();
            } catch (Exception e) {
                logger.error("Error changing program: {}", e.getMessage());
            }
        }
    }

    /**
     * Reset UI controls to default values after preset change
     */
    private void resetControlsToDefault() {
        logger.info("Resetting UI controls to default values");

        // Reset all oscillator panels
        for (InternalSynthOscillatorPanel panel : oscillatorPanels) {
            panel.resetToDefaults();
        }

        // Reset all specialized panels
        if (lfoPanel != null) {
            lfoPanel.resetToDefaults();
        }
        if (filterPanel != null) {
            filterPanel.resetToDefaults();
        }
        if (envelopePanel != null) {
            envelopePanel.resetToDefaults();
        }
        if (effectsPanel != null) {
            effectsPanel.resetToDefaults();
        }
        if (mixerPanel != null) {
            mixerPanel.resetToDefaults();
        }

        // Reset all sliders (except those in specialized panels)
        UIHelper.findComponentsByType(this, JSlider.class, component -> {
            JSlider slider = (JSlider) component;
            // Skip slider if it's owned by one of the specialized panels
            if (slider.getParent() != null
                    && (UIHelper.isChildOf(slider, lfoPanel)
                            || UIHelper.isChildOf(slider, filterPanel)
                            || UIHelper.isChildOf(slider, envelopePanel)
                            || UIHelper.isChildOf(slider, effectsPanel))) {
                return;
            }

            int defaultValue = (slider.getMaximum() - slider.getMinimum()) / 2 + slider.getMinimum();
            slider.setValue(defaultValue);
                logger.debug("Reset slider to {}", defaultValue);
        });

        // Reset all dials to appropriate values (using the utility method)
        UIHelper.findComponentsByType(this, Dial.class, component -> {
            Dial dial = (Dial) component;
            String name = dial.getName();
            int value = 64; // Default to middle

            // Set specific defaults based on control name
            if (name != null) {
                if (name.contains("volume")) {
                    // Set oscillators 2-3 to zero, first one to full
                    if (name.equals("volumeDial0")) {
                        value = 100; // Oscillator 1 on full volume
                    } else {
                        value = 0; // Other oscillators muted
                    }
                } else if (name.contains("tune") || name.contains("detune")) {
                    value = 64; // Middle for tuning
                } else if (name.contains("master")) {
                    value = 100; // Master volume full
                }
                    logger.debug("Reset {} to {}", name, value);
            }

            dial.setValue(value);
        });

        // After resetting UI, force MIDI controller resets for all channels
        try {
            for (int ch = 0; ch < 3; ch++) {
                // Set volume for the oscillators - only first one on
                int baseCCForOsc = ch * 20 + 20;
                int volume = (ch == 0) ? 100 : 0;
                setControlChange(midiChannel, baseCCForOsc + 4, volume);

                // Reset all other controllers
                setControlChange(midiChannel, baseCCForOsc, 0); // Waveform
                setControlChange(midiChannel, baseCCForOsc + 1, 64); // Octave
                setControlChange(midiChannel, baseCCForOsc + 2, 64); // Tune
                setControlChange(midiChannel, baseCCForOsc + 3, 64); // Brightness
            }

            // Set master volume
            setControlChange(midiChannel, 7, 100);
        } catch (Exception e) {
                logger.error("Error resetting controllers", e);
        }
    }

    private JScrollPane createParamsPanel() {
        // Main panel with vertical layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // ROW 1: Oscillator panels 1 & 2 in horizontal layout
        JPanel row1Panel = new JPanel();
        row1Panel.setLayout(new BoxLayout(row1Panel, BoxLayout.X_AXIS));
        row1Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // row1Panel.setPreferredSize(new Dimension(getPreferredSize().width, 120));
        // row1Panel.setMaximumSize(new Dimension(getMaximumSize().width, 120));

        // Create three oscillator panels
        InternalSynthOscillatorPanel osc1Panel = new InternalSynthOscillatorPanel(getSynthesizer(), midiChannel, 0);
        InternalSynthOscillatorPanel osc2Panel = new InternalSynthOscillatorPanel(getSynthesizer(), midiChannel, 1);
        InternalSynthOscillatorPanel osc3Panel = new InternalSynthOscillatorPanel(getSynthesizer(), midiChannel, 2);

        // Store references to the oscillator panels
        oscillatorPanels = new InternalSynthOscillatorPanel[] { osc1Panel, osc2Panel, osc3Panel };

        // Add property change listener for osc1 waveform changes
        osc1Panel.addPropertyChangeListener("oscillator1WaveformChanged", evt -> {
            // Property change handling could go here
        });

        // Add oscillator panels 1 & 2 to row 1
        row1Panel.add(osc1Panel);
        row1Panel.add(Box.createHorizontalStrut(10));
        row1Panel.add(osc2Panel);
        row1Panel.add(Box.createHorizontalStrut(10));
        row1Panel.add(osc3Panel);

        // ROW 2: Oscillator 3 and Oscillator Mix panel in horizontal layout
        JPanel row2Panel = new JPanel();
        row2Panel.setLayout(new BoxLayout(row2Panel, BoxLayout.X_AXIS));
        row2Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // row2Panel.setPreferredSize(new Dimension(getPreferredSize().width, 120));
        // row2Panel.setMaximumSize(new Dimension(getMaximumSize().width, 120));

        // Create oscillator mix panel
        JPanel oscMixPanel = createOscillatorMixPanel();

        effectsPanel = new InternalSynthEffectsPanel(getSynthesizer(), midiChannel);

        // Add oscillator 3 and mix panel to row 2
        row2Panel.add(effectsPanel);
        row2Panel.add(Box.createHorizontalStrut(10));
        row2Panel.add(oscMixPanel);

        // ROW 3: Envelope, Filter, LFO and Effects panels
        JPanel row3Panel = new JPanel();
        row3Panel.setLayout(new BoxLayout(row3Panel, BoxLayout.X_AXIS));
        row3Panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create the missing panels
        envelopePanel = new InternalSynthEnvelopePanel(getSynthesizer(), midiChannel);
        filterPanel = new InternalSynthFilterPanel(getSynthesizer(), midiChannel);
        lfoPanel = new InternalSynthLFOPanel(getSynthesizer(), midiChannel);
        // effectsPanel = new InternalSynthEffectsPanel(synthesizer, midiChannel);

        // Add all panels to row 3 with spacing
        row3Panel.add(envelopePanel);
        row3Panel.add(Box.createHorizontalStrut(10));
        row3Panel.add(filterPanel);
        row3Panel.add(Box.createHorizontalStrut(10));
        row3Panel.add(lfoPanel);
        // row3Panel.add(Box.createHorizontalStrut(10));
        // row3Panel.add(effectsPanel);

        // Add all rows to main panel with vertical spacing
        mainPanel.add(row1Panel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(row3Panel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(row2Panel);

        // Add scroll pane for better UI experience
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }

    /**
     * Create a panel for oscillator mixing controls
     */
    private JPanel createOscillatorMixPanel() {
        JPanel mixPanel = new JPanel(new BorderLayout(0, 5));
        mixPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Oscillator Mix"));

        // Create dials section for the mix panel with FlowLayout
        JPanel dialsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // Balance control (Osc1-Osc2)
        JPanel balance12Panel = new JPanel();
        balance12Panel.setLayout(new BoxLayout(balance12Panel, BoxLayout.Y_AXIS));
        JLabel balanceLabel = new JLabel("1 <-> 2", JLabel.CENTER);
        balanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dial balanceDial = UIHelper.createLabeledDial("", "Balance between Osc 1 & 2", 64);
        balanceDial.setAlignmentX(Component.CENTER_ALIGNMENT);
        balance12Panel.add(balanceDial);
        balance12Panel.add(balanceLabel);

        // Balance control (mix-Osc3)
        JPanel balance3Panel = new JPanel();
        balance3Panel.setLayout(new BoxLayout(balance3Panel, BoxLayout.Y_AXIS));
        JLabel balance3Label = new JLabel("Mix <-> 3", JLabel.CENTER);
        balance3Label.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dial balance3Dial = UIHelper.createLabeledDial("", "Balance between Mix & Osc 3", 64);
        balance3Dial.setAlignmentX(Component.CENTER_ALIGNMENT);
        balance3Panel.add(balance3Dial);
        balance3Panel.add(balance3Label);

        // Master volume control
        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));
        JLabel masterLabel = new JLabel("Master", JLabel.CENTER);
        masterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dial masterDial = UIHelper.createLabeledDial("", "Master Volume", 100);
        masterDial.setAlignmentX(Component.CENTER_ALIGNMENT);
        masterPanel.add(masterDial);
        masterPanel.add(masterLabel);

        // Add dials to panel
        dialsPanel.add(balance12Panel);
        dialsPanel.add(balance3Panel);
        dialsPanel.add(masterPanel);

        // Add to main mix panel - CENTER position ensures vertical centering
        mixPanel.add(dialsPanel, BorderLayout.CENTER);

        // Add event handlers for the dials
        balanceDial.addChangeListener(e -> setControlChange(60, balanceDial.getValue()));
        balance3Dial.addChangeListener(e -> setControlChange(61, balance3Dial.getValue()));
        masterDial.addChangeListener(e -> setControlChange(7, masterDial.getValue()));

        return mixPanel;
    }

    /**
     * Reset controllers and synchronize controls after preset change
     */
    private void reinitializeControllers() {
        if (getSynthesizer() != null && getSynthesizer().isOpen()) {
            try {
                MidiChannel channel = getSynthesizer().getChannels()[midiChannel];

                // First reset all controllers and send all notes off
                channel.resetAllControllers();
                channel.allNotesOff();
                channel.allSoundOff();

                // Wait a tiny bit
                Thread.sleep(5);

                // Set basic channel parameters
                channel.controlChange(7, 100); // Volume
                channel.controlChange(10, 64); // Pan center
                channel.controlChange(11, 127); // Expression

                // Set all oscillator volumes to match UI state
                UIHelper.findComponentsByType(this, JCheckBox.class, component -> {
                    JCheckBox checkBox = (JCheckBox) component;
                    if (checkBox.getName() != null && checkBox.getName().contains("Toggle")) {
                        int oscIndex = Integer.parseInt(checkBox.getName().substring(3, 4));
                        int baseCCForOsc = oscIndex * 20 + 20;
                        boolean enabled = checkBox.isSelected();

                        // Find associated volume dial
                        UIHelper.findComponentsByType(this, Dial.class, dial -> {
                            if (dial.getName() != null && dial.getName().equals("volumeDial" + oscIndex)) {
                                int volume = enabled ? ((Dial) dial).getValue() : 0;
                                setControlChange(midiChannel, baseCCForOsc + 4, volume);
                            }
                        });
                    }
                });

                // Update all oscillator panels to send their control changes
                for (InternalSynthOscillatorPanel panel : oscillatorPanels) {
                    panel.updateSynthState();
                }

                // Update LFO panel state
                if (lfoPanel != null) {
                    lfoPanel.updateSynthState();
                }

                // Update filter panel state
                if (filterPanel != null) {
                    filterPanel.updateSynthState();
                }

                // Update envelope panel state
                if (envelopePanel != null) {
                    envelopePanel.updateSynthState();
                }

                // Update effects panel state
                if (effectsPanel != null) {
                    effectsPanel.updateSynthState();
                }

                // Update mixer panel state
                if (mixerPanel != null) {
                    mixerPanel.updateSynthState();
                }

                logger.info("Reinitialized controllers after preset change");
            } catch (Exception e) {
                logger.error("Error reinitializing controllers: {}", e.getMessage());
            }
        }
    }

    /**
     * Add a method to play test notes using the InternalSynthManager
     */
//    public void playTestNote(int note, int velocity, int preset) {
//        if (getSynthesizer() != null && getSynthesizer().isOpen()) {
//            // Use the manager but pass the synthesizer and soundbank name explicitly
//            InternalSynthManager.getInstance().playTestNote(
//                    getSynthesizer(), midiChannel, note, velocity, preset, currentSoundbankName);
//        }
//    }

    /**
     * Play a test note
     */
    public void playNote(int note, int velocity, int durationMs) {
        InternalSynthManager.getInstance().playNote(note, velocity, durationMs, midiChannel);
    }

}
