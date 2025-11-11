package com.angrysurfer.beats.panel.sequencer;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.MuteButton;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MuteSequencerPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MuteSequencerPanel.class);

    private static final int STEP_COUNT = 16;
    private static final long serialVersionUID = 1L;

    private final List<MuteButton> muteButtons = new ArrayList<>(STEP_COUNT);

    // Reference to sequencer
    // Either MelodicSequencer or DrumSequencer
    private final Object sequencer;

    private int currentStep = 0;
    // The current player being edited
    private Player currentPlayer;

    /**
     * Create a mute sequencer panel for melodic sequencer
     */
    public MuteSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        this.currentPlayer = sequencer.getPlayer();
        initialize();
    }

    /**
     * Create a mute sequencer panel for drum sequencer
     */
    public MuteSequencerPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        this.currentPlayer = sequencer.getPlayers()[sequencer.getSelectedPadIndex()];
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(2, 2));
        UIHelper.setWidgetPanelBorder(this, "Mutes");

        // Correct the height settings
        setPreferredSize(new Dimension(800, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); // Match preferred height

        JPanel buttonPanel = new JPanel(new GridLayout(1, STEP_COUNT, 2, 0));
        buttonPanel.setBackground(getBackground());

        // Create the 16 mute buttons
        for (int i = 0; i < STEP_COUNT; i++) {
            final int index = i;

            // Create the mute button
            MuteButton button = new MuteButton();
            button.setToolTipText("Mute for step " + (i + 1));
            muteButtons.add(button);

            // Add action listener
            button.addActionListener(e -> setMuteForStep(index, button.isSelected()));

            // Create container with padding
            JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            container.add(button);
            buttonPanel.add(container);
        }

        add(buttonPanel, BorderLayout.CENTER);        // Register with command bus for events
        CommandBus.getInstance().register(this, new String[]{
                Commands.TIMING_UPDATE,
                Commands.DRUM_PAD_SELECTED,
                Commands.PLAYER_SELECTION_EVENT,
                Commands.TRANSPORT_STOP,
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
            case Commands.TIMING_UPDATE -> handleTimingUpdate(action);
            case Commands.TRANSPORT_STOP -> resetHighlighting();
            case Commands.DRUM_PAD_SELECTED,
                 Commands.SEQUENCER_SYNC_MESSAGE,
                 Commands.PLAYER_SELECTION_EVENT,
                 Commands.MELODIC_SEQUENCE_LOADED,
                 Commands.MELODIC_SEQUENCE_CREATED,
                 Commands.MELODIC_SEQUENCE_UPDATED -> SwingUtilities.invokeLater(this::synchronizeWithSequencer);
        }
    }

    private void handleTimingUpdate(Command action) {
        if (action.getData() instanceof TimingUpdate update) {
            // Check for bar updates instead of step/tick updates
            if (update.bar() != null) {
                int bar = update.bar() - 1; // Convert to 0-based index

                // Only highlight if bar changed
                if (bar != currentStep) {
                    // Update highlight
                    highlightStep(bar);

                    // Update current bar/step reference
                    currentStep = bar;
                }
            }
        }
    }

    private void highlightStep(int step) {
        // Update all buttons
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < muteButtons.size(); i++) {
                muteButtons.get(i).setHighlighted(i == step);
                muteButtons.get(i).repaint();
            }
        });
    }

    private void resetHighlighting() {
        SwingUtilities.invokeLater(() -> {
            for (MuteButton button : muteButtons) {
                button.setHighlighted(false);
                button.repaint();
            }
        });
    }

    /**
     * Set whether a step should be muted
     */
    public void setMuteForStep(int step, boolean muted) {
        if (currentPlayer == null) {
            return;
        }

        if (sequencer instanceof MelodicSequencer melodicSequencer) {
            List<Integer> muteValues = new ArrayList<>(melodicSequencer.getMuteValues());

            // Ensure list is large enough
            while (muteValues.size() <= step) {
                muteValues.add(0);
            }

            // Update the mute value at this position
            muteValues.set(step, muted ? 1 : 0);            // Save back to sequencer
            melodicSequencer.setMuteValues(muteValues);

            // We don't need to apply mute directly here, as that will happen
            // when the sequencer processes the next bar update        
        } else if (sequencer instanceof DrumSequencer drumSequencer) {
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
                    for (MuteButton button : muteButtons) {
                        sequenceData.setBarMuteValue(padIndex, bar, button.isSelected());
                        bar++;
                    }

                    logger.debug("Set mute for drum {} step {} to {}", padIndex, step, muted);
                } catch (Exception e) {
                    logger.error("Error setting mute for drum {} step {}: {}",
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
                    int bar = 0;
                    for (int i = 0; i < muteButtons.size(); i++) {
                        boolean isMuted = sequenceData.getBarMuteValue(padIndex, i);
                        muteButtons.get(i).setSelected(isMuted);
                        muteButtons.get(i).repaint();
                    }
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

        // Update UI to reflect loaded pattern
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < muteButtons.size(); i++) {
                boolean isMuted = muteValues != null && i < muteValues.size() && muteValues.get(i) > 0;
                muteButtons.get(i).setSelected(isMuted);
                muteButtons.get(i).repaint();
            }
        });
    }
}
