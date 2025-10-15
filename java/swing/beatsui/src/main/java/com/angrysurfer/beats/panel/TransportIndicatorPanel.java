package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.LEDIndicator;
import com.angrysurfer.beats.widget.VuMeter;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.sequencer.TimingUpdate;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class TransportIndicatorPanel extends JPanel implements IBusListener {

    // Constants for UI sizing and formatting
    private static final int SECTION_SPACING = 4;
    private static final int FIELD_HEIGHT = 20;
    private static final int SMALL_FIELD_WIDTH = 50;
    private static final int MEDIUM_FIELD_WIDTH = 100;
    private static final Color SECTION_BORDER_COLOR = new Color(180, 180, 180);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // Session information
    private JLabel sessionIdLabel;
    private JTextField sessionIdField;
    private JLabel bpmLabel;
    private JTextField bpmField;
    private JLabel playerCountLabel;
    private JTextField playerCountField;

    // Transport section
    private LEDIndicator playingLed;
    private LEDIndicator recordingLed;
    // private JTextField transportStateField;
    private JLabel timeSignatureLabel;
    private JTextField timeSignatureField;

    // Player information
    private JLabel playerLabel;
    private JTextField playerNameField;
    private JLabel channelLabel;
    private JTextField channelField;
    private JLabel instrumentLabel;
    private JTextField instrumentField;

    // Performance monitors
    private JLabel cpuLabel;
    private JProgressBar cpuUsageBar;
    private JLabel memoryLabel;
    private JProgressBar memoryUsageBar;

    // Timing display
    private JLabel positionLabel;
    private JTextField positionField;

    // Level meters and status indicators
    private VuMeter leftMeter;
    private VuMeter rightMeter;

    // System message area
    private JLabel messageLabel;
    private JTextField messageField;

    // Data fields

    private int tickCount = 0;
    private int beatCount = 0;
    private int barCount = 0;
    private int partCount = 0;
    private float tempo = 120.0f;
    private boolean isPlaying = false;
    private boolean isRecording = false;
    private String timeSignature = "4/4";
    private Player currentPlayer;
    private Timer performanceMonitorTimer;
    private Random random = new Random(); // For demo level meter movement
    private static final Logger logger = LoggerFactory.getLogger(TransportIndicatorPanel.class);

    public TransportIndicatorPanel() {
        super();
        setup();
        registerForEvents();
        startPerformanceMonitoring();
        requestInitialData();
    }

    private void registerForEvents() {
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this, new String[]{
                Commands.PLAYER_UPDATE_EVENT,
                Commands.TIMING_UPDATE,
                Commands.TEMPO_CHANGE,
                Commands.TIME_SIGNATURE_CHANGE,
                Commands.TIMING_RESET,
                Commands.TRANSPORT_START,
                Commands.TRANSPORT_STOP,
                Commands.TRANSPORT_RECORD,
                Commands.TRANSPORT_PAUSE,
                Commands.SESSION_LOADED,
                Commands.SESSION_CHANGED
        });
    }

    private void requestInitialData() {
        SwingUtilities.invokeLater(() -> {
            CommandBus.getInstance().publish(Commands.SESSION_REQUEST, this);
            CommandBus.getInstance().publish(Commands.TRANSPORT_STATE_REQUEST, this);
            CommandBus.getInstance().publish(Commands.ACTIVE_PLAYER_REQUEST, this);
        });
    }

    private void setup() {
        // Global panel setup
        setLayout(new FlowLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.add(createTransportSection());
        add(mainPanel, BorderLayout.CENTER);
    }


    private JPanel createTransportSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Transport state indicator
        JPanel ledPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        playingLed = new LEDIndicator(Color.GREEN, "PLAY");
        playingLed.setOffColor(Color.RED);
        recordingLed = new LEDIndicator(Color.RED, "REC");
        recordingLed.setOffColor(Color.RED);
        ledPanel.add(playingLed);
        ledPanel.add(recordingLed);
        panel.add(ledPanel);

        // Transport state text
        // transportStateField = createStatusField(SMALL_FIELD_WIDTH);
        // transportStateField.setText("Stopped");
        // panel.add(transportStateField);

        // Time signature
        timeSignatureLabel = new JLabel("Time:");
        panel.add(timeSignatureLabel);

        timeSignatureField = createStatusField(SMALL_FIELD_WIDTH);
        timeSignatureField.setText("4/4");
        panel.add(timeSignatureField);

        // Position display
        positionLabel = new JLabel("Pos:");
        panel.add(positionLabel);

        positionField = createStatusField(MEDIUM_FIELD_WIDTH);
        positionField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        updateTimeDisplay();
        panel.add(positionField);

        return panel;
    }

    private JTextField createStatusField(int width) {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(UIHelper.FIELD_BACKGROUND);
        field.setForeground(UIHelper.FIELD_FOREGROUND);

        if (width > 0) {
            Dimension size = new Dimension(width, UIHelper.CONTROL_HEIGHT);
            field.setPreferredSize(size);
            field.setMinimumSize(size);
        }

        return field;
    }

    private void startPerformanceMonitoring() {
        performanceMonitorTimer = new Timer(1000, e -> {
            updatePerformanceMetrics();
            updateLevelMeters();
        });
        performanceMonitorTimer.start();
    }

    private void updatePerformanceMetrics() {
        // Get CPU usage (example implementation)
        long cpuUsage = getSystemCpuUsage();
        cpuUsageBar.setValue((int) cpuUsage);
        cpuUsageBar.setString(cpuUsage + "%");

        // Get memory usage
        long memoryUsage = getSystemMemoryUsage();
        memoryUsageBar.setValue((int) memoryUsage);
        memoryUsageBar.setString(memoryUsage + "%");
    }

    private void updateLevelMeters() {
        // For demo/testing, use random values - replace with actual audio levels
        if (isPlaying) {
            int leftLevel = Math.max(0, Math.min(100, random.nextInt(60) + (isRecording ? 40 : 20)));
            int rightLevel = Math.max(0, Math.min(100, random.nextInt(60) + (isRecording ? 40 : 20)));

            leftMeter.setLevel(leftLevel);
            rightMeter.setLevel(rightLevel);
        } else {
            leftMeter.setLevel(0);
            rightMeter.setLevel(0);
        }
    }

    private long getSystemCpuUsage() {
        // Mock implementation - replace with actual JMX or other system monitoring
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return Math.round(osBean.getCpuLoad() * 100.0);
        } catch (Exception e) {
            // Fallback to random values if the above doesn't work
            return isPlaying ? Math.min(90, 30 + random.nextInt(20)) : 10 + random.nextInt(10);
        }
    }

    private long getSystemMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (usedMemory * 100) / maxMemory;
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null)
            return;

        try {
            switch (action.getCommand()) {
                case Commands.PLAYER_RULE_UPDATE_EVENT -> {
                    if (action.getData() instanceof PlayerUpdateEvent event &&
                            currentPlayer != null &&
                            event.getPlayer().getId().equals(currentPlayer.getId())) {
                        updatePlayerInfo(event.getPlayer());
                    }
                }
                case Commands.TIMING_UPDATE -> {
                    if (action.getData() instanceof TimingUpdate update) {
                        handleTimingUpdate(update);
                    }
                }
                case Commands.TEMPO_CHANGE -> {
                    if (action.getData() instanceof Number newTempo) {
                        tempo = newTempo.floatValue();
                        bpmField.setText(String.format("%.1f", tempo));
                    }
                }
                case Commands.TIME_SIGNATURE_CHANGE -> {
                    if (action.getData() instanceof String newTimeSignature) {
                        timeSignature = newTimeSignature;
                        timeSignatureField.setText(timeSignature);
                    }
                }
                case Commands.TIMING_RESET -> {
                    resetTimingCounters();
                }
                case Commands.TRANSPORT_START -> {
                    isPlaying = true;
                    isRecording = false;
                    updateTransportState("Playing");
                }
                case Commands.TRANSPORT_STOP -> {
                    isPlaying = false;
                    isRecording = false;
                    updateTransportState("Stopped");
                }
                case Commands.TRANSPORT_RECORD -> {
                    isPlaying = true;
                    isRecording = true;
                    updateTransportState("Recording");
                }
                case Commands.TRANSPORT_PAUSE -> {
                    isPlaying = false;
                    updateTransportState("Paused");
                }
                default -> {
                    // No action needed for other commands
                }
            }
        } catch (Exception e) {
            logger.error("Error in TransportIndicatorPanel.onAction: {}", e.getMessage(), e);
        }
    }

    private void handleTimingUpdate(TimingUpdate update) {
        // Update timing values from the TimingUpdate record
        if (update.tick() != null) {
            tickCount = update.tick().intValue();
        }
        if (update.beat() != null) {
            beatCount = update.beat().intValue();
        }
        if (update.bar() != null) {
            barCount = update.bar().intValue();
        }
        if (update.part() != null) {
            partCount = update.part().intValue();
        }

        // Update the time display with all values
        updateTimeDisplay();
    }

    private void updateSessionInfo(Session session) {
        if (session != null) {
            sessionIdField.setText(String.valueOf(session.getId()));
            playerCountField.setText(String.valueOf(session.getPlayers().size()));

            // Update tempo if available
            if (session.getTempoInBPM() != null) {
                tempo = session.getTempoInBPM();
                bpmField.setText(String.format("%.1f", tempo));
            }
        } else {
            clearSessionInfo();
        }
    }

    private void updatePlayerInfo(Player player) {
        if (player != null) {
            currentPlayer = player;
            playerNameField.setText(player.getName());
            channelField.setText(player.getChannel() != null ? String.valueOf(player.getChannel()) : "");

            if (player.getInstrument() != null) {
                updateInstrumentInfo(player.getInstrument());
            } else {
                instrumentField.setText("None");
            }
        } else {
            clearPlayerInfo();
        }
    }

    private void updateInstrumentInfo(InstrumentWrapper instrument) {
        if (instrument != null) {
            String instName = instrument.getName();

            // If available, add preset information
            if (instrument.getPreset() != null) {
                instName += " (" + instrument.getPreset() + ")";
            }

            instrumentField.setText(instName);
        } else {
            instrumentField.setText("None");
        }
    }

    private void updateTransportState(String state) {
        // transportStateField.setText(state);

        // Update LED indicators
        playingLed.setOn(isPlaying);
        recordingLed.setOn(isRecording);

        // Additional visual feedback
        // transportStateField.setForeground(isRecording ? Color.RED : (isPlaying ? new Color(0, 150, 0) : Color.BLACK));
    }

    private void clearSessionInfo() {
        sessionIdField.setText("");
        playerCountField.setText("");
        bpmField.setText("120");
    }

    private void clearPlayerInfo() {
        currentPlayer = null;
        playerNameField.setText("");
        channelField.setText("");
        instrumentField.setText("");
    }

    private void resetTimingCounters() {
        tickCount = 0;
        beatCount = 0;
        barCount = 0;
        partCount = 0;
        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        String formattedTime = String.format("%02d:%02d:%02d:%02d", partCount, barCount, beatCount, tickCount);
        positionField.setText(formattedTime);
    }

    public void setMessage(String text) {
        messageField.setText(text);
    }

    /**
     * Must be called when application is closing to prevent memory leaks
     */
    public void cleanup() {
        if (performanceMonitorTimer != null) {
            performanceMonitorTimer.stop();
        }
    }
}
