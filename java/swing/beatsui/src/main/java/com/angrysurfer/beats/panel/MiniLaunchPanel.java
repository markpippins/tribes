package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MiniLaunchPanel extends LivePanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(MiniLaunchPanel.class);

    private static final int BUTTON_SIZE = 38;
    private static final int GRID_ROWS = 2;
    private static final int GRID_COLS = 4;
    private static final int GRID_GAP = 5;

    // Add label mapping array
    private static final int[] PAD_LABELS = {
            40, 41, 42, 43, // top row
            36, 37, 38, 39 // bottom row
    };

    // Keep track of last preset sent to each channel
    private final Map<Integer, Integer> lastSentPresets = new HashMap<>();

    public MiniLaunchPanel() {
        super();
        setLayout(new BorderLayout());
        CommandBus.getInstance().register(this, new String[]{Commands.CHANGE_THEME,
                Commands.MINI_NOTE_SELECTED});
        setup();
    }

    @Override
    public void onAction(com.angrysurfer.core.api.Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.CHANGE_THEME -> SwingUtilities.invokeLater(this::repaint);

            case Commands.MINI_NOTE_SELECTED -> {
                if (action.getData() instanceof Integer midiNote) {
                    sendNoteToActivePlayer(midiNote);
                }
            }
        }
    }

    @Override
    public void handlePlayerActivated() {

    }

    @Override
    public void handlePlayerUpdated() {

    }

    private void setup() {
        setLayout(new BorderLayout());
        JPanel gridPanel = createGridPanel();
        add(gridPanel, BorderLayout.CENTER);

        // Set preferred size based on content
        int width = GRID_COLS * (BUTTON_SIZE + GRID_GAP) + GRID_GAP;
        int height = GRID_ROWS * (BUTTON_SIZE + GRID_GAP) + GRID_GAP;
        setPreferredSize(new Dimension(width, height));
    }

    private JPanel createGridPanel() {
        JPanel gridPanel = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, GRID_GAP, GRID_GAP));
        gridPanel.setBorder(new EmptyBorder(GRID_GAP, GRID_GAP, GRID_GAP, GRID_GAP));
        gridPanel.setOpaque(false);

        for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
            int midiNote = PAD_LABELS[i];
            PadButton padButton = createPadButton(midiNote);
            gridPanel.add(padButton);
        }

        return gridPanel;
    }

    private PadButton createPadButton(int midiNote) {
        PadButton button = new PadButton(midiNote);
        Color baseColor = UIHelper.mutedRed;
        Color flashColor = new Color(
                Math.min(baseColor.getRed() + 100, 255),
                Math.min(baseColor.getGreen() + 100, 255),
                Math.min(baseColor.getBlue() + 100, 255));

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Fill background
                g2d.setColor(button.isFlashing() ? flashColor : baseColor);
                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Draw border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Draw highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                // Draw text
                String text = button.getText();
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text,
                        (w - textWidth) / 2,
                        ((h + textHeight) / 2) - fm.getDescent());

                g2d.dispose();
            }
        });

        // Update action listener to directly send note
        button.addActionListener(e -> {
            // Send note directly
            sendNoteToActivePlayer(midiNote);

            // Also publish event for other components that might need it
            CommandBus.getInstance().publish(Commands.MINI_NOTE_SELECTED, this, midiNote);

            // Visual feedback
            button.setFlashing(true);
            Timer timer = new Timer(100, evt -> {
                button.setFlashing(false);
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("MIDI Note " + midiNote);

        return button;
    }

    /**
     * Sends a MIDI note to the active player without triggering heavy updates
     *
     * @param midiNote The MIDI note to send
     * @return true if note was successfully sent, false otherwise
     */
    public boolean sendNoteToActivePlayer(int midiNote) {

        if (getPlayer() == null) {
            logger.debug("No active player to receive MIDI note: {}", midiNote);
            return false;
        }

        if (SessionManager.getInstance().isRecording()) {
            CommandBus.getInstance().publish(Commands.NEW_VALUE_NOTE, this, midiNote);
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, getPlayer());
        }


        try {
            // Use the player's instrument, channel, and a reasonable velocity
            InstrumentWrapper instrument = getPlayer().getInstrument();
            if (instrument == null) {
                logger.debug("Active player has no instrument");
                return false;
            }

            int channel = getPlayer().getChannel();

            // Only send program change if preset has changed for this channel
            if (getPlayer().getPreset() != null) {
                Integer lastPreset = lastSentPresets.get(channel);
                Integer currentPreset = getPlayer().getPreset();

                // Send program change only if the preset has changed for this channel
                if (lastPreset == null || !lastPreset.equals(currentPreset)) {
                    try {
                        logger.debug("Sending program change: channel={}, preset={}",
                                channel, currentPreset);
                        instrument.programChange(currentPreset, 0);

                        // Remember this preset for this channel
                        lastSentPresets.put(channel, currentPreset);
                    } catch (Exception e) {
                        logger.warn("Failed to send program change: {}", e.getMessage());
                    }
                }
            }

            // Calculate velocity from player settings
            int velocity = (int) Math.round((getPlayer().getMinVelocity() + getPlayer().getMaxVelocity()) / 2.0);

            // Just update the note in memory temporarily - don't save to Redis
            getPlayer().setRootNote(midiNote);

            // Send the note to the device
            logger.debug("Sending note: note={}, channel={}, velocity={}", midiNote, channel, velocity);
            instrument.noteOn(midiNote, velocity);

            // Schedule note-off after a reasonable duration
            long duration = 250; // milliseconds
            new java.util.Timer(true).schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            try {
                                instrument.noteOff(midiNote, 0);
                            } catch (Exception e) {
                                logger.debug("Error sending note-off: {}", e.getMessage());
                            }
                        }
                    },
                    duration);

            return true;

        } catch (Exception e) {
            logger.warn("Error sending MIDI note: {}", e.getMessage());
            return false;
        }
    }

    private static final class PadButton extends JButton {
        private boolean isFlashing;

        PadButton(int midiNote) {
            super(String.valueOf(midiNote));
            this.isFlashing = false;
        }

        public boolean isFlashing() {
            return isFlashing;
        }

        public void setFlashing(boolean flashing) {
            isFlashing = flashing;
            repaint();
        }
    }
}
