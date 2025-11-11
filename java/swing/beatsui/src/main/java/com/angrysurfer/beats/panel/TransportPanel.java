package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

/**
 * Panel containing transport controls (play, stop, record, etc.)
 */
public class TransportPanel extends JPanel {

    // Simple app startup time tracker
    private static final long APP_STARTUP_TIME = System.currentTimeMillis();
    private final boolean isPlaying;
    // Add a flag to track initial application load state
    private final boolean initialLoadCompleted = false;
    private final boolean ignoreNextSessionUpdate = true; // Flag to ignore the first update
    // Transport controls
    private JButton playButton;
    private JButton stopButton;
    private JButton recordButton;
    private JButton rewindButton;
    private JButton forwardButton;
    private JButton pauseButton;
    private boolean isRecording;
    // Add a timer to delay the auto-recording feature
    private boolean autoRecordingEnabled = false;
    private javax.swing.Timer autoRecordingEnableTimer;
    // Tracking variables for session navigation
    private String lastCommand = "";
    private long lastSessionNavTime = 0;
    private static final Logger logger = LoggerFactory.getLogger(TransportPanel.class);

    public TransportPanel() {
        // Change to BorderLayout to better control vertical alignment
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // Start with recording disabled
        isRecording = false;
        isPlaying = false;

        // Set up auto-recording timer
        autoRecordingEnableTimer = new javax.swing.Timer(3000, e -> {
            autoRecordingEnabled = true;
            autoRecordingEnableTimer.stop();
        });
        autoRecordingEnableTimer.setRepeats(false);
        autoRecordingEnableTimer.start();

        // Create the buttons panel
        JPanel buttonPanel = createTransportButtons();

        // Add to CENTER for vertical centering
        add(buttonPanel, BorderLayout.CENTER);

        setupCommandBusListener();

        // Set initial button states
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private static long getAppStartupTime() {
        return APP_STARTUP_TIME;
    }

    /**
     * Creates and configures transport control buttons
     *
     * @return JPanel containing all transport buttons
     */
    private JPanel createTransportButtons() {
        // Create panel to hold transport buttons with a vertical BoxLayout to center
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

        // Row panel for buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // Create all buttons first - ensure none are null
        rewindButton = createToolbarButton(Commands.TRANSPORT_REWIND, "⏮", "Previous Session");
        pauseButton = createToolbarButton(Commands.TRANSPORT_PAUSE, "⏸", "Pause");
        stopButton = createToolbarButton(Commands.TRANSPORT_STOP, "⏹", "Stop");
        recordButton = createToolbarButton(Commands.TRANSPORT_RECORD, "⏺", "Record");
        playButton = createToolbarButton(Commands.TRANSPORT_START, "▶", "Play");
        forwardButton = createToolbarButton(Commands.TRANSPORT_FORWARD, "⏭", "Next Session");

        // Add buttons to the button row - check each one first
        if (rewindButton != null) buttonRow.add(rewindButton);
        if (pauseButton != null) buttonRow.add(pauseButton);
        if (stopButton != null) buttonRow.add(stopButton);
        if (recordButton != null) buttonRow.add(recordButton);
        if (playButton != null) buttonRow.add(playButton);
        if (forwardButton != null) buttonRow.add(forwardButton);

        // Add padding at top to push content down to center
        buttonsPanel.add(Box.createVerticalGlue());
        // Add the button row
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(buttonRow);
        // Add padding at bottom to push content up to center
        buttonsPanel.add(Box.createVerticalGlue());

        // Only call these methods if buttons are non-null
        if (playButton != null) updatePlayButtonAppearance();
        if (recordButton != null) updateRecordButtonAppearance();

        return buttonsPanel;
    }

    private JButton createToolbarButton(String command, String text, String tooltip) {
        JButton button = new JButton(text);

        button.setToolTipText(tooltip);
        button.setEnabled(true);
        button.setActionCommand(command);
        button.addActionListener(e -> {
            CommandBus.getInstance().publish(command, button);
            updatePlayButtonAppearance();
            updateRecordButtonAppearance();
        });

        // Styling
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));

        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);
        button.setVerticalAlignment(SwingConstants.CENTER);

        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(button.getBackground().brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("Button.background"));
            }
        });

        return button;
    }

    private void toggleRecordingState() {
        boolean wasRecording = isRecording;
        isRecording = !isRecording;
        updateRecordButtonAppearance();

        if (wasRecording && !isRecording) {
            // We're turning recording off - save the session
            try {
                // Get the current session from SessionManager
                Session currentSession = SessionManager.getInstance().getActiveSession();
                if (currentSession != null) {
                    // Save the session (which includes players and rules)
                    SessionManager.getInstance().saveSession(currentSession);

                    // Show save confirmation
                    // CommandBus.getInstance().publish(Commands.SHOW_STATUS, this, "Session saved");
                }
            } catch (Exception ex) {
                // Log and show any errors during save
                logger.error("Error saving session: {}", ex.getMessage(), ex);
                // CommandBus.getInstance().publish(Commands.SHOW_ERROR, this, "Error saving session: " +
                // ex.getMessage());
            }
        }

        // Publish the appropriate command
        if (isRecording) {
            CommandBus.getInstance().publish(Commands.TRANSPORT_RECORD_START, this);
        } else {
            CommandBus.getInstance().publish(Commands.TRANSPORT_RECORD_STOP, this);
        }
    }

    private void updateRecordButtonAppearance() {
        if (isRecording) {
            recordButton.setBackground(Color.RED);
            recordButton.setForeground(Color.WHITE);
        } else {
            recordButton.setBackground(UIManager.getColor("Button.background"));
            recordButton.setForeground(UIManager.getColor("Button.foreground"));
        }
    }

    private void updatePlayButtonAppearance() {
        if (isPlaying) {
            playButton.setBackground(Color.GREEN);
            playButton.setForeground(Color.WHITE);
        } else {
            playButton.setBackground(UIManager.getColor("Button.background"));
            playButton.setForeground(UIManager.getColor("Button.foreground"));
        }
        playButton.invalidate();
        playButton.repaint();
    }

    /**
     * Update the enabled state of transport buttons based on session state
     */
    public void updateTransportState(Session session) {
        boolean hasActiveSession = Objects.nonNull(session);

        if (session == null) {
            rewindButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            recordButton.setEnabled(false);
            playButton.setEnabled(false);
            forwardButton.setEnabled(false);
            return;
        }

        // Update states for all buttons
        rewindButton.setEnabled(hasActiveSession && SessionManager.getInstance().canMoveBack());
        forwardButton.setEnabled(SessionManager.getInstance().canMoveForward());

        // Enable pause button when the session is running
        pauseButton.setEnabled(hasActiveSession && session.isRunning());

        playButton.setEnabled(hasActiveSession && !session.isRunning());
        stopButton.setEnabled(hasActiveSession && session.isRunning());
        recordButton.setEnabled(hasActiveSession);
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(action -> {
            // Skip if this panel is the sender
            if (action.getSender() == TransportPanel.this) {
                return;
            }

            if (Objects.nonNull(action.getCommand())) {
                String cmd = action.getCommand();

                // Track session navigation
                if (isSessionNavigationCommand(cmd)) {
                    isRecording = false;
                    updateRecordButtonAppearance();
                    lastSessionNavTime = System.currentTimeMillis();
                    lastCommand = cmd;
                }
                // Special handling for value changes - wait 1 second after startup/navigation
                else if (isValueChangeCommand(cmd)) {
                    // Existing implementation...
                }

                // Switch for specific state handling commands
                switch (cmd) {
                    case Commands.TRANSPORT_STATE_CHANGED:
                        if (action.getData() instanceof Boolean playing) {
                            SwingUtilities.invokeLater(() -> {
                                playButton.setEnabled(!playing);
                                stopButton.setEnabled(playing);
                                pauseButton.setEnabled(playing);
                            });
                        }
                        break;
                    // Other cases...
                }

                // Keep track of last command processed
                lastCommand = cmd;

                updatePlayButtonAppearance();
            }
        }, new String[]{
                Commands.TRANSPORT_STATE_CHANGED,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.TRANSPORT_RECORD_START,
                Commands.TRANSPORT_RECORD_STOP,
                Commands.SESSION_CREATED,
                Commands.SESSION_SELECTED,
                Commands.SESSION_LOADED,
                // Value change commands
                Commands.PLAYER_ADDED,
                Commands.PLAYER_DELETED,
                Commands.RULE_ADDED,
                Commands.RULE_UPDATED,
                Commands.RULE_DELETED,
                Commands.RULE_ADDED_TO_PLAYER,
                Commands.RULE_REMOVED_FROM_PLAYER,
                Commands.NEW_VALUE_LEVEL,
                Commands.NEW_VALUE_NOTE,
                Commands.NEW_VALUE_SWING,
                Commands.NEW_VALUE_PROBABILITY,
                Commands.NEW_VALUE_VELOCITY_MIN,
                Commands.NEW_VALUE_VELOCITY_MAX,
                Commands.NEW_VALUE_RANDOM,
                Commands.NEW_VALUE_PAN,
                Commands.NEW_VALUE_SPARSE,
                Commands.SESSION_UPDATED,
                Commands.PRESET_CHANGED,
                Commands.UPDATE_TEMPO,
                Commands.UPDATE_TIME_SIGNATURE,
                Commands.TIMING_PARAMETERS_CHANGED,
                Commands.TRANSPOSE_UP,
                Commands.TRANSPOSE_DOWN
        });
    }

    /**
     * Check if a command is related to session navigation
     */
    private boolean isSessionNavigationCommand(String cmd) {
        return cmd.equals(Commands.TRANSPORT_REWIND) ||
                cmd.equals(Commands.TRANSPORT_FORWARD) ||
                cmd.equals(Commands.SESSION_CREATED) ||
                cmd.equals(Commands.SESSION_SELECTED) ||
                cmd.equals(Commands.SESSION_LOADED) ||
                cmd.equals(Commands.SESSION_REQUEST);
    }

    /**
     * Check if a command is related to value changes that should trigger recording
     */
    private boolean isValueChangeCommand(String cmd) {
        // Return true for any command that should trigger recording

        // Player modification commands
        return switch (cmd) {
            case Commands.PLAYER_ADDED, Commands.PLAYER_DELETED -> true;


            // Rule modification commands
            case Commands.RULE_ADDED, Commands.RULE_UPDATED, Commands.RULE_DELETED, Commands.RULE_ADDED_TO_PLAYER,
                 Commands.RULE_REMOVED_FROM_PLAYER -> true;


            // Value change commands
            case Commands.NEW_VALUE_LEVEL, Commands.NEW_VALUE_NOTE, Commands.NEW_VALUE_SWING,
                 Commands.NEW_VALUE_PROBABILITY, Commands.NEW_VALUE_VELOCITY_MIN, Commands.NEW_VALUE_VELOCITY_MAX,
                 Commands.NEW_VALUE_RANDOM, Commands.NEW_VALUE_PAN, Commands.NEW_VALUE_SPARSE -> true;


            // Only enable recording for actual session updates, not initial loading
            case Commands.SESSION_UPDATED -> true;


            // Other parameter changes
            case Commands.PRESET_CHANGED, Commands.UPDATE_TEMPO, Commands.UPDATE_TIME_SIGNATURE,
                 Commands.TIMING_PARAMETERS_CHANGED, Commands.TRANSPOSE_UP, Commands.TRANSPOSE_DOWN -> true;
            default -> false;
        };

    }
}
