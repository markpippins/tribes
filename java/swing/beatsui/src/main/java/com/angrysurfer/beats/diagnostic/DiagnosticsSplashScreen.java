package com.angrysurfer.beats.diagnostic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Splash screen with progress reporting for diagnostics
 */
public class DiagnosticsSplashScreen extends JWindow {
    private final JProgressBar progressBar;
    private final JLabel messageLabel;
    private final JLabel titleLabel;
    private int maxProgress = 100;
    
    /**
     * Create a splash screen with title and initial message
     */
    public DiagnosticsSplashScreen(String title, String initialMessage) {
        // Set up window
        setSize(400, 200);
        setLocationRelativeTo(null);
        
        // Create panel with border
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        // contentPane.setBackground(new Color(248, 248, 248));
        
        // Title label
        titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Message label
        messageLabel = new JLabel(initialMessage);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        // Progress bar
        progressBar = new JProgressBar(0, maxProgress);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        
        // Layout
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(messageLabel, BorderLayout.NORTH);
        centerPanel.add(progressBar, BorderLayout.SOUTH);
        
        contentPane.add(titleLabel, BorderLayout.NORTH);
        contentPane.add(centerPanel, BorderLayout.CENTER);
        
        setContentPane(contentPane);
    }
    
    /**
     * Set the maximum progress value
     * @param max The maximum value
     */
    public void setMaxProgress(int max) {
        maxProgress = max;
        progressBar.setMaximum(max);
    }
    
    /**
     * Update progress and message
     * @param progress The current progress value
     * @param message The current status message
     */
    public void setProgress(int progress, String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
            messageLabel.setText(message);
        });
    }
    
    /**
     * Update just the progress
     * @param progress The current progress value
     */
    public void setProgress(int progress) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
    }
    
    /**
     * Update just the message
     * @param message The current status message
     */
    public void setMessage(String message) {
        SwingUtilities.invokeLater(() -> messageLabel.setText(message));
    }
 
    
}
