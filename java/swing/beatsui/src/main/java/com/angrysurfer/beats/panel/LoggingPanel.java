package com.angrysurfer.beats.panel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A panel that displays log messages from both the application's Logger
 * and any System.out/System.err output.
 */
public class LoggingPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(LoggingPanel.class);

    // TODO: add this to dynamically change levels: 
    // ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.angrysurfer")).setLevel(Level.DEBUG);
    private final Style infoStyle;
    private final Style debugStyle;
    private final Style warningStyle;
    private final Style errorStyle;
    private final Style commandStyle;
    // Log buffering
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    // UI Components
    private JTextPane logTextPane;
    // Styling
    private final StyledDocument document;
    private JScrollPane scrollPane;
    private JComboBox<String> logLevelCombo;
    private JCheckBox autoScrollCheck;
    private JButton clearButton;
    private JButton copyButton;
    private JButton saveButton;
    private JCheckBox showTimestampsCheck;
    private JTextField filterField;
    private Thread logProcessorThread;
    private volatile boolean running = true;

    // State
    private String logLevel = "INFO";
    private boolean autoScroll = true;
    private boolean showTimestamps = true;

    /**
     * Constructor
     */
    public LoggingPanel() {
        setLayout(new BorderLayout());

        // Setup document and styles
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);

        logTextPane.setBackground(Color.WHITE);
        document = logTextPane.getStyledDocument();

        // Create styles
        infoStyle = logTextPane.addStyle("INFO", null);
        StyleConstants.setForeground(infoStyle, new Color(0, 100, 0));

        debugStyle = logTextPane.addStyle("DEBUG", null);
        StyleConstants.setForeground(debugStyle, new Color(0, 0, 150));

        warningStyle = logTextPane.addStyle("WARN", null);
        StyleConstants.setForeground(warningStyle, new Color(180, 100, 0));
        StyleConstants.setBold(warningStyle, true);

        errorStyle = logTextPane.addStyle("ERROR", null);
        StyleConstants.setForeground(errorStyle, new Color(180, 0, 0));
        StyleConstants.setBold(errorStyle, true);

        commandStyle = logTextPane.addStyle("COMMAND", null);
        StyleConstants.setForeground(commandStyle, new Color(100, 0, 120));
        StyleConstants.setItalic(commandStyle, true);

        // Initialize UI
        initializeUI();

        // Set up logger intercept
        setupLoggerRedirect();

        // Start log processor thread
        startLogProcessor();

        // Add sample log entries to show it's working
        logger.info("Logging panel initialized");
        logger.debug("Debug logging is enabled");

        // Register with CommandBus to monitor events
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action == null || action.getCommand() == null) {
                    return;
                }

                // Log the command
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                String source = action.getSender() != null ? action.getSender().getClass().getSimpleName() : "unknown";
                String dataInfo = action.getData() != null ?
                        "[" + action.getData().getClass().getSimpleName() + "]" : "";

                String message = String.format("CMD: %s from %s %s",
                        action.getCommand(), source, dataInfo);

                addLogEntry(new LogEntry("COMMAND", message, timestamp));
            }
        }, new String[]{"*"});
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        // Main log display
        logTextPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logTextPane.setEditable(false);
        
        // Force word wrapping - KEY FIX
        // 1. Create a new JTextPane with tracking viewport width
        JTextPane wrappedTextPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true; // This forces the component to use viewport width
            }
        };
        
        // Copy all properties and content from the existing text pane
        wrappedTextPane.setDocument(logTextPane.getDocument());
        wrappedTextPane.setEditable(false);
        wrappedTextPane.setFont(logTextPane.getFont());
        wrappedTextPane.setBackground(logTextPane.getBackground());
        
        // Replace the reference
        logTextPane = wrappedTextPane;

        // Scroll pane - critical for word wrapping
        scrollPane = new JScrollPane(logTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Force NEVER for horizontal scrollbar to ensure wrapping
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Add mouse wheel listener for smooth scrolling
        logTextPane.addMouseWheelListener(e -> {
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            int notches = e.getWheelRotation() * 3;
            int scrollAmount = notches * verticalBar.getUnitIncrement();

            // Calculate new position
            int newValue = verticalBar.getValue() + scrollAmount;

            // Ensure value is within bounds
            if (newValue < verticalBar.getMinimum()) {
                newValue = verticalBar.getMinimum();
            } else if (newValue > verticalBar.getMaximum() - verticalBar.getVisibleAmount()) {
                newValue = verticalBar.getMaximum() - verticalBar.getVisibleAmount();
            }

            // Set the scrollbar position and disable auto-scroll if scrolling up
            if (notches < 0) {
                autoScroll = false;
                autoScrollCheck.setSelected(false);
            }

            verticalBar.setValue(newValue);
        });

        // Controls Panel
        JPanel controlsPanel = new JPanel(new BorderLayout(5, 0));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Left controls - filter and level
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Log level selector
        leftControls.add(new JLabel("Level:"));
        logLevelCombo = new JComboBox<>(new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR"});
        logLevelCombo.setSelectedItem("DEBUG");
        logLevelCombo.addActionListener(e -> {
            logLevel = (String) logLevelCombo.getSelectedItem();
            logger.info("Log level set to {}", logLevel);
        });
        leftControls.add(logLevelCombo);

        // Filter field
        leftControls.add(new JLabel("Filter:"));
        filterField = new JTextField(15);
        filterField.addActionListener(e -> applyFilter());
        leftControls.add(filterField);

        JButton filterButton = new JButton("Apply");
        filterButton.addActionListener(e -> applyFilter());
        leftControls.add(filterButton);

        // Show timestamps option
        showTimestampsCheck = new JCheckBox("Show Timestamps", showTimestamps);
        showTimestampsCheck.addActionListener(e -> {
            showTimestamps = showTimestampsCheck.isSelected();
            refreshDisplay();
        });
        leftControls.add(showTimestampsCheck);

        // Right controls - actions
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        // Auto-scroll option
        autoScrollCheck = new JCheckBox("Auto-scroll", autoScroll);
        autoScrollCheck.addActionListener(e -> autoScroll = autoScrollCheck.isSelected());
        rightControls.add(autoScrollCheck);

        // Clear button
        clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearLog());
        rightControls.add(clearButton);

        // Copy button
        copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> copyToClipboard());
        rightControls.add(copyButton);

        // Save button
        saveButton = new JButton("Save...");
        saveButton.addActionListener(e -> saveLogToFile());
        rightControls.add(saveButton);

        // Add controls to the panel
        controlsPanel.add(leftControls, BorderLayout.WEST);
        controlsPanel.add(rightControls, BorderLayout.EAST);

        // Add everything to the main panel
        add(controlsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Set up redirects to capture log output
     */
    private void setupLoggerRedirect() {
        // Intercept System.out
        System.setOut(new PrintStream(new LoggingOutputStream("INFO"), true));

        // Intercept System.err
        System.setErr(new PrintStream(new LoggingOutputStream("ERROR"), true));
    }

    /**
     * Start the log processor thread
     */
    private void startLogProcessor() {
        logProcessorThread = new Thread(() -> {
            try {
                while (running) {
                    LogEntry entry = logQueue.take();
                    SwingUtilities.invokeLater(() -> appendLog(entry));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        logProcessorThread.setName("LogProcessor");
        logProcessorThread.setDaemon(true);
        logProcessorThread.start();
    }

    /**
     * Add a log entry to the queue
     */
    private void addLogEntry(LogEntry entry) {
        // Filter by log level
        if (shouldShowLogLevel(entry.level)) {
            try {
                logQueue.put(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Determine if a log level should be shown based on the current filter
     */
    private boolean shouldShowLogLevel(String entryLevel) {
        String[] levels = {"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "COMMAND"};
        int selectedIndex = -1;
        int entryIndex = -1;

        // Find indices
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equals(logLevel)) {
                selectedIndex = i;
            }
            if (levels[i].equals(entryLevel)) {
                entryIndex = i;
            }
        }

        // Special case for COMMAND
        if (entryLevel.equals("COMMAND")) {
            return true; // Always show commands
        }

        // Return true if the entry level is at or above the selected level
        return entryIndex >= selectedIndex;
    }

    /**
     * Append a log entry to the document
     */
    private void appendLog(LogEntry entry) {
        try {
            // Build the log line
            StringBuilder logLine = new StringBuilder();

            // Add timestamp if enabled
            if (showTimestamps) {
                logLine.append("[").append(entry.timestamp).append("] ");
            }

            // Add level
            logLine.append("[").append(entry.level).append("] ");

            // Add message
            logLine.append(entry.message);

            // Add newline
            logLine.append("\n");

            // Apply filter if needed
            String filter = filterField.getText().trim();
            if (!filter.isEmpty() && !logLine.toString().toLowerCase().contains(filter.toLowerCase())) {
                return;
            }

            // Select style based on level
            Style style;
            switch (entry.level) {
                case "DEBUG":
                    style = debugStyle;
                    break;
                case "WARN":
                    style = warningStyle;
                    break;
                case "ERROR":
                    style = errorStyle;
                    break;
                case "COMMAND":
                    style = commandStyle;
                    break;
                default:
                    style = infoStyle;
                    break;
            }

            // Append text with style
            document.insertString(document.getLength(), logLine.toString(), style);

            // Auto-scroll if enabled
            if (autoScroll) {
                logTextPane.setCaretPosition(document.getLength());
            }
        } catch (BadLocationException e) {
            logger.error("Error appending log entry to document", e);
        }
    }

    /**
     * Apply the current filter
     */
    private void applyFilter() {
        refreshDisplay();
    }

    /**
     * Refresh the entire display
     */
    private void refreshDisplay() {
        // Implement if needed - would rebuild display from saved log entries
        logger.info("Display refresh requested with filter: {}", filterField.getText());
    }

    /**
     * Clear the log display
     */
    private void clearLog() {
        try {
            document.remove(0, document.getLength());
            logger.info("Log cleared");
        } catch (BadLocationException e) {
            logger.error("Error clearing log document", e);
        }
    }

    /**
     * Copy log content to clipboard
     */
    private void copyToClipboard() {
        String text = logTextPane.getText();
        if (text != null && !text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(text), null);
            logger.info("Log copied to clipboard");
        }
    }

    /**
     * Save log to a file
     */
    private void saveLogToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Log File");

        // Default filename with timestamp
        String defaultFilename = "beats_log_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        fileChooser.setSelectedFile(new java.io.File(defaultFilename));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();

                // Add .txt extension if not present
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new java.io.File(file.getAbsolutePath() + ".txt");
                }

                // Write to file
                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write(logTextPane.getText());
                writer.close();

                logger.info("Log saved to file: {}", file.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving log: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Error saving log: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        running = false;
        if (logProcessorThread != null) {
            logProcessorThread.interrupt();
        }
    }

    /**
     * LogEntry class to represent a log message
     */
    private static class LogEntry {
        final String level;
        final String message;
        final String timestamp;

        public LogEntry(String level, String message, String timestamp) {
            this.level = level;
            this.message = message;
            this.timestamp = timestamp;
        }
    }

    /**
     * OutputStream implementation that redirects to our logging system
     */
    private class LoggingOutputStream extends OutputStream {
        private final StringBuilder buffer = new StringBuilder();
        private final String level;

        public LoggingOutputStream(String level) {
            this.level = level;
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) b;
            if (c == '\n') {
                // End of line, process the buffer
                String message = buffer.toString();
                if (!message.trim().isEmpty()) {
                    String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                    addLogEntry(new LogEntry(level, message, timestamp));
                }
                buffer.setLength(0);
            } else {
                buffer.append(c);
            }
        }
    }
}