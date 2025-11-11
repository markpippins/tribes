package com.angrysurfer.beats.panel.sequencer;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OffsetSequencerPanel extends JPanel implements IBusListener {
    static final Integer[] OFFSET_VALUES = {-12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    private static final Logger logger = LoggerFactory.getLogger(OffsetSequencerPanel.class);
    private static final int STEP_COUNT = 16;
    private static final long serialVersionUID = 1L;
    private final List<JComboBox<Integer>> offsetCombos = new ArrayList<>(STEP_COUNT);

    // Reference to sequencer
    private final Object sequencer; // Either MelodicSequencer or DrumSequencer
    // Original player levels before muting

    private int currentStep = 0;
    private Player currentPlayer;
    private boolean isSynchronizing = false;

    /**
     * Create a offset sequencer panel for melodic sequencer
     */
    public OffsetSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        this.currentPlayer = sequencer.getPlayer();

        initialize();
        synchronizeWithSequencer();
    }

    /**
     * Create a offset sequencer panel for drum sequencer
     */
    public OffsetSequencerPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        int selectedPadIndex = sequencer.getSelectedPadIndex();

        // Initialize for currently selected player if any
        if (selectedPadIndex >= 0 && selectedPadIndex < sequencer.getPlayers().length) {
            Player player = sequencer.getPlayers()[selectedPadIndex];
            if (player != null) {
                currentPlayer = player;
            }
        }

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(2, 2));
        UIHelper.setWidgetPanelBorder(this, "Offsets");

        // Correct the height settings
        setPreferredSize(new Dimension(800, 54));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 54)); // Match preferred height

        JPanel buttonPanel = new JPanel(new GridLayout(1, STEP_COUNT, 2, 0));
        buttonPanel.setBackground(getBackground());

        // Create the 16 offset buttons
        for (int i = 0; i < STEP_COUNT; i++) {
            final int index = i;

            // Create the offset button
            JComboBox<Integer> combo = new JComboBox<>(OFFSET_VALUES);
            combo.setToolTipText("Offset for step " + (i + 1));
            combo.setSelectedItem(0);
            combo.setPreferredSize(new Dimension(54, UIHelper.CONTROL_HEIGHT));
            combo.setMaximumSize(new Dimension(54, UIHelper.CONTROL_HEIGHT));
            offsetCombos.add(combo);

            // Add action listener
            combo.addActionListener(e -> setOffsetForBar(index, (Integer) combo.getSelectedItem()));

            // Create container with padding
            JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            container.add(combo);
            buttonPanel.add(container);
        }

        add(buttonPanel, BorderLayout.CENTER);        // Register with command bus for events
        CommandBus.getInstance().register(this, new String[]{
                Commands.TIMING_UPDATE,
                Commands.DRUM_PAD_SELECTED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.TRANSPORT_STOP,
                Commands.PLAYER_SELECTION_EVENT,
                // Add melodic sequence events
                Commands.MELODIC_SEQUENCE_LOADED,
                Commands.MELODIC_SEQUENCE_CREATED,
                Commands.MELODIC_SEQUENCE_UPDATED
        });
        TimingBus.getInstance().register(this);
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (action.getData() instanceof TimingUpdate update) {
                    handleTimingUpdate(update);
                }
            }
            case Commands.DRUM_PAD_SELECTED,
                 Commands.SEQUENCER_SYNC_MESSAGE,
                 Commands.PLAYER_SELECTION_EVENT,
                 Commands.MELODIC_SEQUENCE_LOADED,
                 Commands.MELODIC_SEQUENCE_CREATED,
                 Commands.MELODIC_SEQUENCE_UPDATED -> SwingUtilities.invokeLater(this::synchronizeWithSequencer);
        }
    }

    private void handleTimingUpdate(TimingUpdate update) {
        // Check for bar updates instead of step/tick updates
        if (update.bar() != null) {
            // Convert to 0-based index
            int bar = update.bar() - 1;
            if (bar != currentStep)
                currentStep = bar;
        }
    }

    /**
     * Update the player being edited
     */
    private void updatePlayer(Player player) {
        if (player == null || (currentPlayer != null && player.getId().equals(currentPlayer.getId()))) {
            return;
        }

        // Update to new player
        currentPlayer = player;

    }

    /**
     * Set whether a step should be offsetd
     */
    public void setOffsetForBar(int step, Integer offset) {
        if (currentPlayer == null || isSynchronizing) {
            return;
        }

        if (sequencer instanceof DrumSequencer drumSequencer) {
            int padIndex = drumSequencer.getSelectedPadIndex();

            if (padIndex >= 0) {
                try {
                    DrumSequenceData sequenceData = drumSequencer.getSequenceData();
                    if (sequenceData == null) {
                        logger.error("Sequence data is null in DrumSequencer");
                        return;
                    }

                    // Update the sequencer
                    int bar = 0;
                    for (JComboBox<Integer> combo : offsetCombos) {
                        sequenceData.setBarOffsetValue(padIndex, bar, (Integer) combo.getSelectedItem());
                        bar++;
                    }

                    logger.debug("Set offset for drum {} step {} to {}", padIndex, step, offset);
                } catch (Exception e) {
                    logger.error("Error setting offset for drum {} step {}: {}",
                            padIndex, step, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Synchronize this panel with the current sequencer's mute values
     */
    private void synchronizeWithSequencer() {

        if (sequencer instanceof MelodicSequencer melodicSequencer)
            syncWithMelodicSequencer(melodicSequencer);

        if (sequencer instanceof DrumSequencer drumSequencer)
            syncWithDrumSequencer(drumSequencer);
    }

    private void syncWithDrumSequencer(DrumSequencer drumSequencer) {
        int padIndex = drumSequencer.getSelectedPadIndex();
        if (padIndex >= 0) {
            try {
                DrumSequenceData sequenceData = drumSequencer.getSequenceData();
                if (sequenceData == null) {
                    logger.error("Sequence data is null in DrumSequencer");
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    isSynchronizing = true;
                    for (int bar = 0; bar < offsetCombos.size(); bar++) {
                        Integer offset = sequenceData.getBarOffsetValue(padIndex, bar);
                        offsetCombos.get(bar).setSelectedItem(offset);
                        offsetCombos.get(bar).repaint();
                    }
                    isSynchronizing = false;
                });
            } catch (Exception e) {
                logger.error("Error syncing mute panel with drum sequencer: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No drum pad selected, cannot sync mute panel");
        }
    }

    private void syncWithMelodicSequencer(MelodicSequencer melodicSequencer) {
        List<Integer> muteValues = melodicSequencer.getMuteValues();
        logger.debug("Syncing mute panel with melodic sequencer - found {} mute values",
                muteValues != null ? muteValues.size() : 0);

    }

}
