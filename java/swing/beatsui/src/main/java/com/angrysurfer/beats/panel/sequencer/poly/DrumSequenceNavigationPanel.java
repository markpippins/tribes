package com.angrysurfer.beats.panel.sequencer.poly;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.service.SequencerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel providing navigation controls for drum sequences
 */
public class DrumSequenceNavigationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceNavigationPanel.class);
    private final DrumSequencer sequencer;
    private final SequencerService manager;
    private JLabel sequenceIdLabel;
    private JButton firstButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton lastButton;
    private JButton saveButton;
    private JButton newButton; // Add new button field

    public DrumSequenceNavigationPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        this.manager = SequencerService.getInstance();

        initializeUI();
    }

    private void initializeUI() {
        // REDUCED: from 5,2 to 2,1
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));

        UIHelper.setWidgetPanelBorder(this, "Sequence");

        // Make ID label slightly smaller
        sequenceIdLabel = new JLabel(getFormattedIdText(), SwingConstants.CENTER);
        // REDUCED: from ID_LABEL_WIDTH to ID_LABEL_WIDTH - 10
        sequenceIdLabel.setPreferredSize(new Dimension(UIHelper.ID_LABEL_WIDTH - 5, UIHelper.CONTROL_HEIGHT - 2));
        sequenceIdLabel.setOpaque(true);
        sequenceIdLabel.setBackground(UIHelper.darkGray);
        sequenceIdLabel.setForeground(UIHelper.coolBlue);
        sequenceIdLabel.setFont(sequenceIdLabel.getFont().deriveFont(12f));

        // Create new sequence button with plus icon
        newButton = createButton("➕", "Create new sequence", e -> createNewSequence());

        // Create navigation buttons with icons
        firstButton = createButton("⏮", "First sequence", e -> loadFirstSequence());
        prevButton = createButton("◀", "Previous sequence", e -> loadPreviousSequence());
        nextButton = createButton("▶", "Next sequence", e -> loadNextSequence());
        lastButton = createButton("⏭", "Last sequence", e -> loadLastSequence());

        saveButton = createButton("💾", "Save current sequence", e -> saveCurrentSequence());

        // Add components to panel - add new button first
        add(sequenceIdLabel);
        add(firstButton);
        add(prevButton);
        add(nextButton);
        add(lastButton);
        add(saveButton);
        add(newButton); // Add new button here

        // Set initial button state
        updateButtonStates();
    }

    private JButton createButton(String text, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setFocusable(false);

        // Set consistent size and margins to match other panels
        button.setPreferredSize(new Dimension(24, 24));
        button.setMargin(new Insets(2, 2, 2, 2));

        return button;
    }

    /**
     * Update the ID display with current sequence ID
     */
    public void updateSequenceIdDisplay() {
        sequenceIdLabel.setText(getFormattedIdText());
        updateButtonStates();
    }

    private String getFormattedIdText() {
        return "Seq: " +
                (sequencer.getSequenceData().getId() == 0 ? "New" : sequencer.getSequenceData().getId());
    }

    /**
     * Enable/disable buttons based on current sequence position
     */
    private void updateButtonStates() {
        long currentId = sequencer.getSequenceData().getId();
        boolean hasSequences = manager.hasSequences();

        // Get first/last sequence IDs
        Long firstId = manager.getFirstSequenceId();
        Long lastId = manager.getLastSequenceId();

        // First/Previous buttons - enabled if we're not at the first sequence
        boolean isFirst = !hasSequences || (firstId != null && currentId <= firstId);

        // Next button should ALWAYS be enabled - this allows creating new sequences
        // even when at the last saved sequence
        // Last button - only enabled if we're not at the last sequence
        boolean isLast = !hasSequences || (lastId != null && currentId >= lastId);

        firstButton.setEnabled(hasSequences && !isFirst);
        prevButton.setEnabled((hasSequences || sequencer.getSequenceData().getId() < 0) && !isFirst);
        nextButton.setEnabled(sequencer.getSequenceData().getId() > 0); // Always enable the next button
        lastButton.setEnabled(hasSequences && !isLast);

        logger.debug("Button states: currentId={}, firstId={}, lastId={}, isFirst={}, isLast={}",
                currentId, firstId, lastId, isFirst, isLast);
    }

    /**
     * Load the sequence with the given ID
     */
    private void loadSequence(Long sequenceId) {
        if (sequenceId != null) {
            // Load the sequence
            manager.loadSequence(sequenceId, sequencer);

            // Reset the sequencer to ensure proper step indicator state
            sequencer.reset(sequencer.isPlaying());

            // Update UI
            updateSequenceIdDisplay();

            // Use consistent command for sequence loading notifications
            CommandBus.getInstance().publish(
                    Commands.PATTERN_LOADED,
                    this,
                    sequencer.getSequenceData().getId());
        }
    }

    /**
     * Load the first available sequence
     */
    private void loadFirstSequence() {
        Long firstId = manager.getFirstSequenceId();
        if (firstId != null) {
            loadSequence(firstId);
        }
    }

    /**
     * Load the previous sequence
     */
    private void loadPreviousSequence() {
        Long prevId = manager.getPreviousSequenceId(sequencer.getSequenceData().getId());
        if (prevId != null) {
            loadSequence(prevId);
        }
    }

    /**
     * Load the next available sequence
     */
    private void loadNextSequence() {
        // Get current sequence ID
        Long currentId = sequencer.getSequenceData().getId();

        // Find the next sequence ID
        Long nextId = manager.getNextSequenceId(currentId);

        // If there is a next sequence, load it
        if (nextId != null) {
            // Load the sequence into the sequencer
            if (manager.loadSequence(nextId, sequencer)) {
                logger.info("Loaded next drum sequence: {}", nextId);
                updateSequenceIdDisplay();

                // Notify other components that sequence has been updated
                CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_UPDATED, this, nextId);
            }
        } else {
            // We're at the last sequence - don't create a new one automatically
            logger.info("Already at last sequence - use New button to create a new sequence");
        }

        // Update button states based on new position
        updateButtonStates();
    }

    /**
     * Load the last available sequence
     */
    private void loadLastSequence() {
        Long lastId = manager.getLastSequenceId();
        if (lastId != null) {
            loadSequence(lastId);
        }
    }

    /**
     * Save the current sequence
     */
    private void saveCurrentSequence() {
        // Save the sequence
        manager.saveSequence(sequencer);

        // After saving, get the latest sequence IDs from the database
        manager.refreshSequenceList();

        // Update display and button states
        updateSequenceIdDisplay();

        // Force update of button states
        updateButtonStates();

        CommandBus.getInstance().publish(
                Commands.DRUM_SEQUENCE_SAVED,
                this,
                sequencer.getSequenceData().getId());

        logger.info("Saved drum sequence: {}", sequencer.getSequenceData().getId());
    }

    /**
     * Create a new sequence and apply it to the sequencer
     */
    private void createNewSequence() {
        try {
            // Create a new sequence with an assigned ID right away
            DrumSequenceData newSequence = manager.createNewSequenceData();

            if (newSequence != null) {
                // Apply the new sequence to the sequencer
                Long newId = newSequence.getId();
                RedisService.getInstance().applyDrumSequenceToSequencer(newSequence, sequencer);

                logger.info("Created new drum sequence with ID: {}", newId);

                // Update UI
                updateSequenceIdDisplay();

                // Notify other components
                CommandBus.getInstance().publish(
                        Commands.DRUM_SEQUENCE_UPDATED,
                        this,
                        newId);

                // Update button states
                updateButtonStates();
            } else {
                logger.error("Failed to create new sequence - returned null");
            }
        } catch (Exception e) {
            logger.error("Error creating new drum sequence", e);
        }
    }
}
