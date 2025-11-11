package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.SequencerService;
import com.angrysurfer.core.service.PlaybackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Panel that handles all mute buttons for both drum and melodic sequencers
 */
public class GlobalMuteButtonsPanel extends JPanel implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(GlobalMuteButtonsPanel.class);
    // Button colors
    private static final Color DRUM_UNMUTED_COLOR = new Color(128, 0, 128); // Purple
    private static final Color MELODIC_UNMUTED_COLOR = new Color(0, 0, 200); // Blue
    private static final Color MUTED_COLOR = new Color(255, 0, 0); // Bright red
    private static final Dimension buttonSize = new Dimension(18, 18); // Square button
    // Button lists
    private final List<JToggleButton> drumMuteButtons = new ArrayList<>();
    private final List<JToggleButton> melodicMuteButtons = new ArrayList<>();
    // Track active notes for visual effect duration
    private final Map<Integer, ScheduledExecutorService> activeNoteTimers = new HashMap<>();
    // Sequencer references
    private DrumSequencer drumSequencer;
    private List<MelodicSequencer> melodicSequencers;
    // Activity indicator
    private JPanel activityIndicator;
    private long lastActivityTime = 0;

    /**
     * Create a new mute buttons panel (without sequencers initially)
     */
    public GlobalMuteButtonsPanel() {
        initializeUI();
        registerWithCommandBus();
        // Add debug message
        logger.info("MuteButtonsPanel created and registered with CommandBus");
        setBackground(Color.ORANGE);
    }

    /**
     * Create a new mute buttons panel with sequencers
     */
    public GlobalMuteButtonsPanel(DrumSequencer drumSequencer, List<MelodicSequencer> melodicSequencers) {
        this.drumSequencer = drumSequencer;
        this.melodicSequencers = melodicSequencers;
        initializeUI();
        registerWithCommandBus();
    }

    private void registerWithCommandBus() {
        CommandBus.getInstance().register(this, new String[]{Commands.DRUM_NOTE_TRIGGERED, Commands.MELODIC_NOTE_TRIGGERED});
        TimingBus.getInstance().register(this);
        logger.info("MuteButtonsPanel registered with CommandBus, listening for note events");
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        // Always print info about received events
        logger.info("MuteButtonsPanel received command: {}, data type: {}",
                action.getCommand(),
                action.getData() != null ? action.getData().getClass().getSimpleName() : "null");

        // Flash activity indicator
        lastActivityTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> {
            activityIndicator.setBackground(Color.GREEN);
            activityIndicator.repaint();
        });

        // Create timer to reset indicator
        javax.swing.Timer timer = new javax.swing.Timer(100, e -> {
            if (System.currentTimeMillis() - lastActivityTime > 90) {
                activityIndicator.setBackground(Color.DARK_GRAY);
                activityIndicator.repaint();
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        timer.setRepeats(false);
        timer.start();

        switch (action.getCommand()) {
            case Commands.DRUM_NOTE_TRIGGERED -> {
                if (action.getData() instanceof Integer) {
                    int drumIndex = (Integer) action.getData();
                    logger.debug("Received DRUM_NOTE_TRIGGERED for drum index: {}", drumIndex);

                    if (drumIndex >= 0 && drumIndex < drumMuteButtons.size()) {
                        // Get velocity from sequencer (fallback to 100 if not available)
                        int velocity = drumSequencer != null ? drumSequencer.getVelocity(drumIndex) : 100;

                        // Always use a fairly strong velocity for visibility
                        velocity = Math.max(velocity, 80);

                        // Flash the button
                        logger.debug("Flashing drum button at index {} with velocity {}", drumIndex, velocity);
                        flashButton(drumMuteButtons.get(drumIndex), true, velocity, 150);
                    } else {
                        logger.warn("Drum index out of range: {}, buttons size: {}",
                                drumIndex, drumMuteButtons.size());
                    }
                }
            }

            case Commands.MELODIC_NOTE_TRIGGERED -> {
                if (action.getData() instanceof NoteEvent noteEvent && action.getSender() instanceof MelodicSequencer source) {

                    // Find the index of the sequencer that triggered the note
                    if (melodicSequencers != null) {
                        for (int i = 0; i < melodicSequencers.size(); i++) {
                            if (melodicSequencers.get(i) == source) {
                                // Use a more conservative duration calculation
                                // Ensure it's not too long or too short
                                int durationMs = 150; // Fixed reasonable duration

                                try {
                                    if (noteEvent.getDurationMs() > 0) {
                                        // Cap at 500ms to prevent sticking
                                        durationMs = Math.min(500, noteEvent.getDurationMs() / 4);
                                    }
                                } catch (Exception ex) {
                                    // If any error occurs, use the default
                                    logger.warn("Error calculating note duration", ex);
                                }

                                // Ensure minimum duration
                                durationMs = Math.max(100, durationMs);

                                // Try to use note velocity for visual intensity
                                int velocity = noteEvent.getVelocity();

                                // Ensure we have a valid index
                                if (i < melodicMuteButtons.size()) {
                                    logger.debug("Flashing melodic button {} with velocity {} for {}ms",
                                            i, velocity, durationMs);
                                    flashButton(melodicMuteButtons.get(i), false, velocity, durationMs);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Flash a button for the duration of a note
     */
    private void flashButton(JToggleButton button, boolean isDrum, int velocity, int durationMs) {
        // Safety check
        if (button == null)
            return;

        // Ensure reasonable duration limits
        durationMs = Math.max(100, Math.min(500, durationMs));

        // Generate hash code based on button identity
        final int buttonId = System.identityHashCode(button);

        // Cancel any existing timer for this button
        ScheduledExecutorService existingTimer = activeNoteTimers.get(buttonId);
        if (existingTimer != null) {
            existingTimer.shutdownNow();
            activeNoteTimers.remove(buttonId);
        }

        // Get button current state
        final boolean isMuted = button.isSelected();

        // Store original look
        final Color originalBg = button.getBackground();
        final Color originalFg = button.getForeground();
        final javax.swing.border.Border originalBorder = button.getBorder();

        // Use VERY dramatic flash color - bright yellow or white regardless of original
        // color
        final Color flashColor = new Color(255, 255, 100); // Bright yellow

        // Update UI on EDT with dramatic changes
        SwingUtilities.invokeLater(() -> {
            try {
                // Change background to bright yellow
                button.setBackground(flashColor);

                // Set black text for contrast
                button.setForeground(Color.BLACK);

                // Add thick border
                button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));

                // Force immediate repaint
                button.repaint();
            } catch (Exception ex) {
                logger.error("Error updating button appearance", ex);
            }
        });

        // Create timer to revert color after duration
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        activeNoteTimers.put(buttonId, timer);

        // Also create a swing timer as a backup in case the executor fails
        final javax.swing.Timer backupTimer = new javax.swing.Timer(durationMs + 100, e -> {
            resetButtonAppearance(button, originalBg, originalFg, originalBorder, buttonId);
            ((javax.swing.Timer) e.getSource()).stop();
        });
        backupTimer.setRepeats(false);
        backupTimer.start();

        // Schedule task to revert to original appearance
        timer.schedule(() -> {
            try {
                resetButtonAppearance(button, originalBg, originalFg, originalBorder, buttonId);
                // Stop the backup timer since we succeeded
                SwingUtilities.invokeLater(() -> backupTimer.stop());
            } catch (Exception ex) {
                logger.error("Error resetting button appearance", ex);
            } finally {
                activeNoteTimers.remove(buttonId);
                timer.shutdown();
            }
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper method to reset button appearance
     */
    private void resetButtonAppearance(JToggleButton button, Color originalBg, Color originalFg,
                                       javax.swing.border.Border originalBorder, int buttonId) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Restore original appearance
                button.setBackground(originalBg);
                button.setForeground(originalFg);
                button.setBorder(originalBorder);

                // Force repaint
                button.repaint();
            } catch (Exception ex) {
                logger.error("Error in resetButtonAppearance", ex);
            }
        });
    }

    /**
     * Safety method to reset all buttons
     */
    public void resetAllButtons() {
        // Force clean all active timers
        for (ScheduledExecutorService timer : new ArrayList<>(activeNoteTimers.values())) {
            timer.shutdownNow();
        }
        activeNoteTimers.clear();

        // Reset all drum buttons
        for (JToggleButton button : drumMuteButtons) {
            boolean isMuted = button.isSelected();
            button.setBackground(isMuted ? MUTED_COLOR : DRUM_UNMUTED_COLOR);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        }

        // Reset all melodic buttons
        for (JToggleButton button : melodicMuteButtons) {
            boolean isMuted = button.isSelected();
            button.setBackground(isMuted ? MUTED_COLOR : MELODIC_UNMUTED_COLOR);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        }
    }

    private Color getVelocityColor(int velocity, boolean isDrum) {
        // Normalize velocity to 0.0-1.0 range
        float normalizedVelocity = Math.min(1.0f, Math.max(0.0f, velocity / 127.0f));

        // Base color (purple for drums, blue for melodic)
        Color baseColor = isDrum ? DRUM_UNMUTED_COLOR : MELODIC_UNMUTED_COLOR;

        // Create spectrum: from base color to bright yellow as velocity increases
        float[] baseHSB = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(),
                baseColor.getBlue(), null);

        // As velocity increases: increase brightness and shift hue toward yellow
        float hue = baseHSB[0] * (1.0f - normalizedVelocity * 0.5f); // Shift toward yellow
        float saturation = baseHSB[1];
        float brightness = Math.min(1.0f, baseHSB[2] + normalizedVelocity * 0.5f); // Brighter

        return Color.getHSBColor(hue, saturation, brightness);
    }

    private void initializeUI() {
        // Initialize activityIndicator FIRST
        activityIndicator = new JPanel();
        activityIndicator.setPreferredSize(buttonSize);
        activityIndicator.setBackground(Color.DARK_GRAY);
        activityIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Then set up the panel layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        // Now add components after everything is initialized
        add(Box.createVerticalGlue());
        add(createButtonsPanel());
        add(Box.createVerticalGlue());

        setPreferredSize(new Dimension(800, 24)); // Set preferred size for the panel
        setMaximumSize(new Dimension(800, 24)); // Set preferred size for the panel
    }

    private JPanel createButtonsPanel() {
        // Change alignment from RIGHT to CENTER for horizontal centering
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2)); // Even padding

        // Create drum mute buttons
        for (int i = 0; i < 16; i++) {
            JToggleButton muteButton = createMuteButton(i, true);
            buttonPanel.add(muteButton);
            drumMuteButtons.add(muteButton);
        }

        JToggleButton muteDrumsButton = createMuteAllButton(true);
        muteDrumsButton.addActionListener(e -> {
            boolean isMuted = muteDrumsButton.isSelected();
            if (isMuted) {
                muteAllDrums();
            } else {
                unmuteAllDrums();
            }
        });
        muteDrumsButton.setToolTipText("Mute All Drums");

        buttonPanel.add(muteDrumsButton);

        // Add a more visible separator
        buttonPanel.add(Box.createHorizontalStrut(12));

        // Create melodic sequencer mute buttons
        for (int i = 0; i < SequencerService.getInstance().getSequencerCount(); i++) {
            JToggleButton muteButton = createMuteButton(i, false);
            buttonPanel.add(muteButton);
            melodicMuteButtons.add(muteButton);
        }

        JToggleButton muteMelodicsButton = createMuteAllButton(false);
        muteMelodicsButton.addActionListener(e -> {
            boolean isMuted = muteMelodicsButton.isSelected();
            if (isMuted) {
                muteAllMelodic();
            } else {
                unmuteAllMelodic();
            }
        });
        muteMelodicsButton.setToolTipText("Mute All Melodics");
        buttonPanel.add(muteMelodicsButton);

        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(activityIndicator);

        return buttonPanel;
    }

    private JToggleButton createMuteButton(int index, boolean isDrum) {
        JToggleButton muteButton = new JToggleButton();

        // Increase button size for better visibility
        muteButton.setPreferredSize(buttonSize);
        muteButton.setMinimumSize(buttonSize);
        muteButton.setMaximumSize(buttonSize);

        // Force button properties to ensure visibility
        muteButton.setOpaque(true);
        muteButton.setBorderPainted(true);
        muteButton.setContentAreaFilled(true);
        muteButton.setFocusPainted(false);

        // Add rounded corners using client properties (works with FlatLaf and some
        // other L&Fs)
        muteButton.putClientProperty("JButton.buttonType", "roundRect");

        // More aggressive rounding if needed
        muteButton.putClientProperty("JButton.arc", 10);

        // Make sure it stays square
        muteButton.putClientProperty("JButton.squareSize", true);

        // Add a number to each button for easier identification
        muteButton.setText(String.valueOf(index + 1));
        muteButton.setFont(new Font("Arial", Font.BOLD, 9));
        muteButton.setToolTipText("Mute " + (isDrum ? "Drum " : "Synth ") + (index + 1));

        // Use extremely saturated colors
        Color defaultColor = isDrum ? new Color(180, 0, 180) : // Very bright purple for drums
                new Color(0, 0, 220); // Very bright blue for melodic

        // Set initial appearance
        muteButton.setBackground(defaultColor);
        muteButton.setForeground(Color.WHITE);
        muteButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        muteButton.addActionListener(e -> {
            boolean isMuted = muteButton.isSelected();

            // Update color based on mute state - use VERY bright red for muted
            muteButton.setBackground(isMuted ? MUTED_COLOR : defaultColor);
            muteButton.setForeground(Color.WHITE);

            if (isDrum) {
                toggleDrumMute(index, isMuted);
            } else {
                toggleMelodicMute(index, isMuted);
            }
        });

        return muteButton;
    }

    private JToggleButton createMuteAllButton(boolean isDrum) {
        JToggleButton muteButton = new JToggleButton();

        // Increase button size for better visibility
        muteButton.setPreferredSize(buttonSize);
        muteButton.setMinimumSize(buttonSize);
        muteButton.setMaximumSize(buttonSize);

        // Force button properties to ensure visibility
        muteButton.setOpaque(true);
        muteButton.setBorderPainted(true);
        muteButton.setContentAreaFilled(true);
        muteButton.setFocusPainted(false);

        // Add rounded corners using client properties (works with FlatLaf and some
        // other L&Fs)
        muteButton.putClientProperty("JButton.buttonType", "roundRect");

        // More aggressive rounding if needed
        muteButton.putClientProperty("JButton.arc", 10);

        // Make sure it stays square
        muteButton.putClientProperty("JButton.squareSize", true);

        // Add a number to each button for easier identification
        muteButton.setFont(new Font("Arial", Font.BOLD, 9));
        muteButton.setToolTipText("Mute All" + (isDrum ? "Drums " : "Synths "));

        // Use extremely saturated colors
        Color defaultColor = isDrum ? new Color(180, 0, 180) : // Very bright purple for drums
                new Color(0, 0, 220); // Very bright blue for melodic

        // Set initial appearance
        muteButton.setBackground(defaultColor.brighter().brighter());
        muteButton.setForeground(Color.WHITE);
        muteButton.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

        muteButton.addActionListener(e -> {
            boolean isMuted = muteButton.isSelected();

            // Update color based on mute state - use VERY bright red for muted
            muteButton.setBackground(isMuted ? MUTED_COLOR : defaultColor);
            muteButton.setForeground(Color.WHITE);

            // if (isDrum) {
            //     toggleDrumMute(index, isMuted);
            // } else {
            //     toggleMelodicMute(index, isMuted);
            // }
        });

        return muteButton;
    }

    private void toggleDrumMute(int drumIndex, boolean muted) {
        logger.info("{}muting drum {}", muted ? "" : "Un", drumIndex + 1);
        if (drumSequencer != null) {
            drumSequencer.setVelocity(drumIndex, muted ? 0 : 100);
        }
    }

    private void toggleMelodicMute(int seqIndex, boolean muted) {
        logger.info("{}muting melodic sequencer {}", muted ? "" : "Un", seqIndex + 1);
        if (melodicSequencers != null && seqIndex < melodicSequencers.size()) {
            MelodicSequencer sequencer = melodicSequencers.get(seqIndex);
            if (sequencer != null && sequencer.getPlayer() != null) {
                // Set the player level
                sequencer.getPlayer().setLevel(muted ? 0 : 100);

                // Log the new level to help diagnose issues
                logger.debug("Set melodic sequencer {} player level to {}",
                        seqIndex, sequencer.getPlayer().getLevel());

                // Ensure changes are persisted
                PlaybackService.getInstance().savePlayer(sequencer.getPlayer());

                // Publish an event so other components can be notified
                CommandBus.getInstance().publish(
                        Commands.PLAYER_LEVEL_CHANGED,
                        this,
                        Map.of("playerId", sequencer.getPlayer().getId(), "level", muted ? 0 : 100)
                );
            } else {
                logger.warn("Cannot mute melodic sequencer {} - sequencer or player is null", seqIndex);
            }
        } else {
            logger.warn("Cannot mute melodic sequencer {} - index out of range", seqIndex);
        }
    }

    /**
     * Set the drum sequencer to control
     */
    public void setDrumSequencer(DrumSequencer sequencer) {
        this.drumSequencer = sequencer;
    }

    /**
     * Set the melodic sequencers to control
     */
    public void setMelodicSequencers(List<MelodicSequencer> sequencers) {
        this.melodicSequencers = sequencers;
    }

    /**
     * Check if a drum is muted
     */
    public boolean isDrumMuted(int index) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            return drumMuteButtons.get(index).isSelected();
        }
        return false;
    }

    /**
     * Check if a melodic sequencer is muted
     */
    public boolean isMelodicMuted(int index) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            return melodicMuteButtons.get(index).isSelected();
        }
        return false;
    }

    /**
     * Set mute state for a drum
     */
    public void setDrumMuted(int index, boolean muted) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            JToggleButton button = drumMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted ? MUTED_COLOR : DRUM_UNMUTED_COLOR;
                button.setBackground(color);
                // Update sequencer
                toggleDrumMute(index, muted);
            }
        }
    }

    /**
     * Set mute state for a melodic sequencer
     */
    public void setMelodicMuted(int index, boolean muted) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            JToggleButton button = melodicMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted ? MUTED_COLOR : MELODIC_UNMUTED_COLOR;
                button.setBackground(color);
                // Update sequencer
                toggleMelodicMute(index, muted);
            }
        }
    }

    /**
     * Mute all drums
     */
    public void muteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, true);
        }
    }

    /**
     * Unmute all drums
     */
    public void unmuteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, false);
        }
    }

    /**
     * Mute all melodic sequencers
     */
    public void muteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, true);
        }
    }

    /**
     * Unmute all melodic sequencers
     */
    public void unmuteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, false);
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        // Clean up timers when panel is removed
        for (ScheduledExecutorService timer : activeNoteTimers.values()) {
            timer.shutdownNow();
        }
        activeNoteTimers.clear();

        // Unregister from command bus
        CommandBus.getInstance().unregister(this);
        TimingBus.getInstance().unregister(this);
    }
}
