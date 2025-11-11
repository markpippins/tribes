package com.angrysurfer.beats.panel.session;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.TimingUpdate;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Panel that displays session timing information (left side of toolbar)
 */
public class SessionDisplayPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(SessionDisplayPanel.class);

    private final Map<String, JTextField> fields = new HashMap<>();


    private Session currentSession;

    // Timing fields
    private JTextField sessionField;
    private JTextField tickField;
    private JTextField beatField;
    private JTextField barField;
    private JTextField partField;

    private JTextField tickCountField;
    private JTextField beatCountField;
    private JTextField barCountField;
    private JTextField partCountField;

    private boolean inverseDisplay = true;
    private Color inverseFontColor = Color.GREEN;
    private Color inverseBackgroundColor = Color.BLACK;

    public SessionDisplayPanel() {
        super(new BorderLayout());
        setup();
        setupTimingListener();
        setupCommandListener();
    }

    private void setup() {
        // Create top and bottom panels
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] labels = {"Session", "Tick", "Beat", "Bar", "Part"};
        for (String label : labels) {
            JPanel fieldPanel = createFieldPanel(label);
            JTextField field = (JTextField) fieldPanel.getComponent(1);

            // Store field references
            switch (label) {
                case "Session" -> sessionField = field;
                case "Tick" -> tickField = field;
                case "Beat" -> beatField = field;
                case "Bar" -> barField = field;
                case "Part" -> partField = field;
            }


            panel.add(fieldPanel);
        }

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 4, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        String[] labels = {"Players", "Ticks", "Beats", "Bars", "Parts"};
        for (String label : labels) {
            JPanel fieldPanel = createFieldPanel(label);
            JTextField field = (JTextField) fieldPanel.getComponent(1);

            // Store field references
            switch (label) {
                case "Ticks" -> tickCountField = field;
                case "Beats" -> beatCountField = field;
                case "Bars" -> barCountField = field;
                case "Parts" -> partCountField = field;
            }

            panel.add(fieldPanel);
        }

        return panel;
    }

    private JPanel createFieldPanel(String label) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 2));
        fieldPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Create label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(Color.GRAY);
        labelPanel.add(nameLabel);

        // Create text field
        JTextField field = UIHelper.createDisplayField("0");

        // Apply inverse display mode if enabled
        if (inverseDisplay) {
            applyInverseDisplay(field);
        }

        fields.put(label, field);

        fieldPanel.add(labelPanel, BorderLayout.NORTH);
        fieldPanel.add(field, BorderLayout.CENTER);

        return fieldPanel;
    }

    /**
     * Apply inverse display mode (green text on black background) to a text field
     *
     * @param field The text field to modify
     */
    private void applyInverseDisplay(JTextField field) {
        field.setForeground(Color.GREEN);
        field.setBackground(inverseBackgroundColor);
        field.setCaretColor(inverseFontColor);
    }

    /**
     * Apply inverse display mode to all fields
     *
     * @param inverse Whether to use inverse mode (true) or normal mode (false)
     */
    public void setInverseDisplay(boolean inverse) {
        this.inverseDisplay = inverse;

        // Update all fields in the map
        for (JTextField field : fields.values()) {
            if (inverse) {
                applyInverseDisplay(field);
            } else {
                // Reset to default look and feel
                field.setForeground(UIManager.getColor("TextField.foreground"));
                field.setBackground(UIManager.getColor("TextField.background"));
                field.setCaretColor(UIManager.getColor("TextField.caretForeground"));
            }
        }
        repaint();
    }

    /**
     * Set the color scheme for inverse display
     *
     * @param fontColor       The text color
     * @param backgroundColor The background color
     */
    public void setInverseColors(Color fontColor, Color backgroundColor) {
        this.inverseFontColor = fontColor;
        this.inverseBackgroundColor = backgroundColor;

        // Update colors if inverse display is active
        if (inverseDisplay) {
            setInverseDisplay(true);
        }
    }

    public void setSession(Session session) {
        this.currentSession = session;

        if (session == null) {
            resetDisplayCounters();
            return;
        }

        // Update session ID
        sessionField.setText(String.valueOf(session.getId()));

        // Update player count
        fields.get("Players").setText(String.valueOf(session.getPlayers().size()));

        // Always sync with the session's current values
        syncWithSession();
    }

    /**
     * Sync display fields with current session state
     */
    private void syncWithSession() {
        if (currentSession == null) {
            resetDisplayCounters();
            return;
        }

        // Get current position values directly from session (0-based internally)
        tickField.setText(String.valueOf(currentSession.getTick()));
        beatField.setText(String.valueOf(currentSession.getBeat()));
        barField.setText(String.valueOf(currentSession.getBar()));
        partField.setText(String.valueOf(currentSession.getPart()));

        tickCountField.setText(String.valueOf(currentSession.getTickCount()));
        beatCountField.setText(String.valueOf(currentSession.getBeatCount()));
        barCountField.setText(String.valueOf(currentSession.getBarCount()));
        partCountField.setText(String.valueOf(currentSession.getPartCount()));

        // Update player count
        fields.get("Players").setText(String.valueOf(currentSession.getPlayers().size()));

        repaint();
    }

    /**
     * Listen for timing events to update display in real-time
     */
    private void setupTimingListener() {
        TimingBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null || currentSession == null)
                    return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        case Commands.TIMING_UPDATE -> {
                            if (action.getData() instanceof TimingUpdate(
                                    Long tick, Double beat, Integer bar, Integer part, Long tickCount,
                                    Integer beatCount, Integer barCount, Integer partCount
                            )) {
                                // Update all timing fields at once from the TimingUpdate record

                                // Update position values if present
                                if (tick != null) {
                                    tickField.setText(String.valueOf(tick));
                                    tickField.invalidate();
                                    tickField.repaint();
                                }

                                if (beat != null) {
                                    beatField.setText(String.valueOf(beat));
                                    beatField.invalidate();
                                    beatField.repaint();
                                }

                                if (bar != null) {
                                    barField.setText(String.valueOf(bar));
                                    barField.invalidate();
                                    barField.repaint();
                                }

                                if (part != null) {
                                    partField.setText(String.valueOf(part));
                                    partField.invalidate();
                                    partField.repaint();
                                }

                                // Update count values if present
                                if (tickCount != null) {
                                    tickCountField.setText(String.valueOf(tickCount));
                                    tickCountField.invalidate();
                                    tickCountField.repaint();
                                }

                                if (beatCount != null) {
                                    beatCountField.setText(String.valueOf(beatCount));
                                    beatCountField.invalidate();
                                    beatCountField.repaint();
                                }

                                if (barCount != null) {
                                    barCountField.setText(String.valueOf(barCount));
                                    barCountField.invalidate();
                                    barCountField.repaint();
                                }

                                if (partCount != null) {
                                    partCountField.setText(String.valueOf(partCount));
                                    partCountField.invalidate();
                                    partCountField.repaint();
                                }
                            }
                        }

                        // Handle reset separately
                        case Commands.TIMING_RESET -> {
                            resetDisplayCounters();
                        }

                        case Commands.SESSION_STOPPED,
                             Commands.TRANSPORT_STOP -> {
                            // Session has stopped, reset all counters
                            resetDisplayCounters();
                        }
                    }
                });
            }
        });
    }

    /**
     * Listen for session and transport commands
     */
    private void setupCommandListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                SwingUtilities.invokeLater(() -> {
                    switch (action.getCommand()) {
                        // Session state changes - full reset and sync
                        case Commands.SESSION_CREATED, Commands.SESSION_SELECTED, Commands.SESSION_LOADED -> {
                            if (action.getData() instanceof Session session) {
                                // Complete reset and sync with new session
                                currentSession = session;
                                resetDisplayCounters();
                                syncWithSession();
                            }
                        }

                        // Session updates - just sync values
                        case Commands.SESSION_UPDATED -> {
                            if (action.getData() instanceof Session session) {
                                currentSession = session;
                                syncWithSession();
                            }
                        }

                        // Transport state changes
                        case Commands.TRANSPORT_STOP -> {
                            // Commands.TRANSPORT_RESET -> {
                            // Reset counters and sync with stopped session
                            resetDisplayCounters();
                            if (currentSession != null) {
                                syncWithSession();
                            }
                        }

                        case Commands.TRANSPORT_START -> {
                            // Get active session and sync before playing
                            currentSession = SessionManager.getInstance().getActiveSession();
                            if (currentSession != null) {
                                resetDisplayCounters(); // Start from zero
                                syncWithSession();
                            }
                        }

                        // Make sure we catch session stopping
                        case Commands.SESSION_STOPPED -> {
                            resetDisplayCounters();
                            if (currentSession != null) {
                                syncWithSession();
                            }
                        }
                    }
                });
            }
        }, new String[] {
        Commands.SESSION_CREATED,
        Commands.SESSION_SELECTED,
        Commands.SESSION_LOADED,
        Commands.SESSION_UPDATED,
        Commands.TRANSPORT_STOP,
        Commands.TRANSPORT_START,
        Commands.SESSION_STOPPED
    });
    }

    /**
     * Reset the display counters (for when session stops/resets)
     */
    private void resetDisplayCounters() {
        // Reset position displays to 0 (not 1)
        tickField.setText("0");
        beatField.setText("0");
        barField.setText("0");
        partField.setText("0");

        // Reset counters too if no session
        if (currentSession == null) {
            tickCountField.setText("0");
            beatCountField.setText("0");
            barCountField.setText("0");
            partCountField.setText("0");
            fields.get("Players").setText("0");
            sessionField.setText("0");
        } else {
            // Otherwise show session's configuration
            syncWithSession();
        }

        repaint();
    }
}
