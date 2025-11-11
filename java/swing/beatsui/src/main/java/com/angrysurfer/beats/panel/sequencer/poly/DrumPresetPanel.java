package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.service.PlaybackService;
import com.angrysurfer.core.service.PlaybackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for selecting preset instruments for each drum in a drum sequencer
 */
public class DrumPresetPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumPresetPanel.class);
    private static final Color[] ROW_COLORS = {
            new Color(40, 40, 40),
            new Color(50, 50, 50)
    };
    private final DrumSequencer sequencer;
    private final Map<Integer, JComboBox<InstrumentWrapper>> presetCombos = new HashMap<>();
    private final Map<Integer, JLabel> drumLabels = new HashMap<>();

    /**
     * Creates a new drum preset panel
     *
     * @param sequencer The drum sequencer to configure
     */
    public DrumPresetPanel(DrumSequencer sequencer) {
        super(new BorderLayout(5, 5));
        this.sequencer = sequencer;

        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializeUI();
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        // Create title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel titleLabel = new JLabel("Drum Preset Selection");
        //titleLabel.setFont(UIHelper.BOLD_FONT);
        titlePanel.add(titleLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Create scrollable panel for drum presets
        JPanel presetsPanel = new JPanel();
        presetsPanel.setLayout(new BoxLayout(presetsPanel, BoxLayout.Y_AXIS));

        // Get available drum instruments
        List<InstrumentWrapper> drumInstruments = PlaybackService.getInstance().getInstrumentHelper().findAllInstruments()
                .stream().filter(i -> i.getChannel() == SequencerConstants.MIDI_DRUM_CHANNEL).toList();

        // For each drum pad, create a combo box for preset selection
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = sequencer.getPlayers()[i];

            // Create row panel with alternating background colors
            JPanel rowPanel = new JPanel(new BorderLayout(5, 0));
            rowPanel.setBackground(ROW_COLORS[i % ROW_COLORS.length]);
            rowPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

            // Create label for drum number and name
            JLabel drumLabel = new JLabel(String.format("Drum %d: %s", i + 1, player.getName()));
            drumLabel.setPreferredSize(new Dimension(150, UIHelper.CONTROL_HEIGHT));
            drumLabels.put(i, drumLabel);

            // Create combo box for preset selection
            JComboBox<InstrumentWrapper> presetCombo = new JComboBox<>();
            presetCombo.setRenderer(new InstrumentListCellRenderer());

            // Add instruments to combo box
            for (InstrumentWrapper instrument : drumInstruments) {
                presetCombo.addItem(instrument);
            }

            // Set selected instrument if player has one
            if (player.getInstrument() != null) {
                for (int j = 0; j < presetCombo.getItemCount(); j++) {
                    InstrumentWrapper item = presetCombo.getItemAt(j);
                    if (item.getId().equals(player.getInstrument().getId())) {
                        presetCombo.setSelectedIndex(j);
                        break;
                    }
                }
            }

            // Add action listener to update player's instrument
            final int drumIndex = i;
            presetCombo.addActionListener(e -> {
                SwingUtilities.invokeLater(() -> updateDrumInstrument(drumIndex, (InstrumentWrapper) presetCombo.getSelectedItem()));
            });

            presetCombos.put(i, presetCombo);

            // Add components to row
            rowPanel.add(drumLabel, BorderLayout.WEST);
            rowPanel.add(presetCombo, BorderLayout.CENTER);

            // Add row to panel
            presetsPanel.add(rowPanel);
        }

        // Add presets panel to CENTER
        add(presetsPanel, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 0));

        // Add reset button
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> resetToDefaults());

        // Add randomize button
        JButton randomizeButton = new JButton("Randomize");
        randomizeButton.addActionListener(e -> randomizePresets());

        // Add apply button
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applyChanges());

        buttonPanel.add(resetButton);
        buttonPanel.add(randomizeButton);
        buttonPanel.add(applyButton);

        JPanel buttonContainerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonContainerPanel.add(buttonPanel);
        add(buttonContainerPanel, BorderLayout.SOUTH);
    }

    /**
     * Update a drum player's instrument
     *
     * @param drumIndex  The drum index to update
     * @param instrument The instrument to set
     */
    private void updateDrumInstrument(int drumIndex, InstrumentWrapper instrument) {
        if (drumIndex < 0 || drumIndex >= SequencerConstants.DRUM_PAD_COUNT || instrument == null) {
            return;
        }

        try {
            Player player = sequencer.getPlayers()[drumIndex];

            // Make sure the instrument has the correct channel
            // This is the fix for the NPE - ensure a channel is set
            if (instrument.getChannel() == null) {
                // Channel 9 is standard for drums (MIDI channel 10)
                instrument.setChannel(SequencerConstants.MIDI_DRUM_CHANNEL);
                logger.info("Set missing channel for instrument {} to 9 (drum channel)",
                        instrument.getName());
            }

            player.setInstrument(instrument);

            // Update the player's name in the label
            drumLabels.get(drumIndex).setText(String.format("Drum %d: %s", drumIndex + 1, player.getName()));

            logger.info("Set drum {} instrument to {}", drumIndex + 1, instrument.getName());
        } catch (Exception e) {
            logger.error("Error updating drum instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Reset all drums to default instruments
     */
    private void resetToDefaults() {
        List<InstrumentWrapper> defaultInstruments = PlaybackService.getInstance().getDefaultDrumKit();

        for (int i = 0; i < Math.min(SequencerConstants.DRUM_PAD_COUNT, defaultInstruments.size()); i++) {
            JComboBox<InstrumentWrapper> combo = presetCombos.get(i);

            // Find the default instrument in the combo box
            for (int j = 0; j < combo.getItemCount(); j++) {
                InstrumentWrapper item = combo.getItemAt(j);
                if (item.getId().equals(defaultInstruments.get(i).getId())) {
                    combo.setSelectedIndex(j);
                    updateDrumInstrument(i, item);
                    break;
                }
            }
        }

        logger.info("Reset all drums to default instruments");
    }

    /**
     * Randomize all drum presets
     */
    private void randomizePresets() {
        List<InstrumentWrapper> drumInstruments = PlaybackService.getInstance().getDrumInstruments();
        if (drumInstruments.isEmpty()) {
            return;
        }

        java.util.Random random = new java.util.Random();

        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            // Select random instrument from list
            int randomIndex = random.nextInt(drumInstruments.size());
            InstrumentWrapper randomInstrument = drumInstruments.get(randomIndex);

            // Set the combo box selection
            JComboBox<InstrumentWrapper> combo = presetCombos.get(i);
            for (int j = 0; j < combo.getItemCount(); j++) {
                if (combo.getItemAt(j).getId().equals(randomInstrument.getId())) {
                    combo.setSelectedIndex(j);
                    updateDrumInstrument(i, randomInstrument);
                    break;
                }
            }
        }

        logger.info("Randomized all drum presets");
    }

    /**
     * Apply all instrument changes to the sequencer
     */
    private void applyChanges() {
        for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
            Player player = sequencer.getPlayers()[i];
            PlaybackService.getInstance().savePlayer(player);
            PlaybackService.getInstance().applyPreset(player);
        }

        // Notify that player instruments were changed
        CommandBus.getInstance().publish(
                Commands.DRUM_INSTRUMENTS_UPDATED,
                this,
                sequencer
        );

        logger.info("Applied all drum preset changes");
    }

    /**
     * Get the updated drum sequencer
     *
     * @return The drum sequencer with updated instruments
     */
    public DrumSequencer getUpdatedSequencer() {
        return sequencer;
    }

    /**
     * Custom renderer for instrument combo boxes
     */
    private static class InstrumentListCellRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof InstrumentWrapper instrument) {
                setText(instrument.getName());
            }

            return this;
        }
    }
}
