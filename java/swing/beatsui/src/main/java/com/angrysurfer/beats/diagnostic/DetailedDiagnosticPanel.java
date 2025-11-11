package com.angrysurfer.beats.diagnostic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

/**
 * Panel for displaying detailed diagnostic information with copy and save functionality
 */
public class DetailedDiagnosticPanel extends JPanel {
    private final JTextArea logArea;
    private final JScrollPane scrollPane;
    private final JButton copyButton;
    private final JButton saveButton;
    private final JButton closeButton;
    
    /**
     * Constructor
     */
    public DetailedDiagnosticPanel() {
        this(null);
    }
    
    /**
     * Constructor with initial log text
     * @param logText The initial text for the log area
     */
    public DetailedDiagnosticPanel(String logText) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create the log area with monospaced font for better readability
        logArea = new JTextArea(25, 80);
        logArea.setBackground(Color.WHITE);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        
        // Set initial text if provided
        if (logText != null) {
            logArea.setText(logText);
            logArea.setCaretPosition(0); // Scroll to top
        }
        
        // Add keyboard shortcuts
        logArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+C to copy
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    copyToClipboard();
                }
                // Ctrl+S to save
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                    saveToFile();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        // Create scroll pane
        scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        // Copy button
        copyButton = new JButton("Copy to Clipboard");
        copyButton.setToolTipText("Copy the entire log to clipboard (Ctrl+C)");
        copyButton.addActionListener(e -> copyToClipboard());
        
        // Save button
        saveButton = new JButton("Save to File");
        saveButton.setToolTipText("Save the log to a text file (Ctrl+S)");
        saveButton.addActionListener(e -> saveToFile());
        
        // Close button
        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            // Find the parent window and close it
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        });
        
        buttonPanel.add(copyButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Set the text to display in the log area
     * @param text The text to display
     */
    public void setLogText(String text) {
        logArea.setText(text);
        logArea.setCaretPosition(0); // Scroll to top
    }
    
    /**
     * Append text to the log area
     * @param text The text to append
     */
    public void appendLogText(String text) {
        logArea.append(text);
        logArea.setCaretPosition(logArea.getText().length()); // Scroll to bottom
    }
    
    /**
     * Get the current log text
     * @return The current log text
     */
    public String getLogText() {
        return logArea.getText();
    }
    
    /**
     * Set a callback for the close button
     * @param onClose The callback to execute when the close button is clicked
     */
    public void setOnClose(Runnable onClose) {
        closeButton.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            }
            // Find the parent window and close it
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        });
    }
    
    /**
     * Copy the log text to the system clipboard
     */
    private void copyToClipboard() {
        logArea.selectAll();
        logArea.copy();
        logArea.setCaretPosition(0);
    }
    
    /**
     * Save the log text to a file
     */
    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Diagnostic Log");
        fileChooser.setSelectedFile(new java.io.File("diagnostic_log.txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                java.io.FileWriter writer = new java.io.FileWriter(file);
                writer.write(logArea.getText());
                writer.close();
                JOptionPane.showMessageDialog(this, 
                    "Log saved successfully to:\n" + file.getAbsolutePath(),
                    "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving log: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Create a dialog with the diagnostic panel
     * @param parent The parent component
     * @param title The dialog title
     * @param logText The log text to display
     * @return The created dialog
     */
    public static JDialog createDialog(Component parent, String title, String logText) {
        DetailedDiagnosticPanel panel = new DetailedDiagnosticPanel(logText);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.MODELESS);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        
        // Set close action
        panel.setOnClose(() -> dialog.dispose());
        
        return dialog;
    }
    
    /**
     * Test main method
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Diagnostic Panel Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Create sample log text
            StringBuilder sampleLog = new StringBuilder();
            for (int i = 1; i <= 50; i++) {
                sampleLog.append("Line ").append(i).append(": Sample diagnostic log entry\n");
            }
            
            DetailedDiagnosticPanel panel = new DetailedDiagnosticPanel(sampleLog.toString());
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
