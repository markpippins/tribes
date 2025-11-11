package com.angrysurfer.beats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Splash screen displayed during application startup
 */
public class SplashScreen extends JWindow {
    private static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);

    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    // For tracking progress
    private int totalTasks = 0;
    private int completedTasks = 0;

    public SplashScreen() {
        // Set window size
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Create content panel with gradient background
        JPanel content = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Create dark gradient background
                int w = getWidth();
                int h = getHeight();
                GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 30),
                        0, h, new Color(50, 50, 80));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, w, h);
                g2d.dispose();
            }
        };
        content.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setContentPane(content);

        // Try to load logo
        BufferedImage logo = loadLogo();
        if (logo != null) {
            LogoPanel logoPanel = new LogoPanel(logo);
            logoPanel.setPreferredSize(new Dimension(400, 200));
            content.add(logoPanel, BorderLayout.NORTH);
        } else {
            // Use text title if logo not available
            JLabel titleLabel = new JLabel("Beat Generator", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
            titleLabel.setForeground(Color.WHITE);
            content.add(titleLabel, BorderLayout.NORTH);
        }

        // Version label
        JLabel versionLabel = new JLabel("Version 1.0.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(200, 200, 255));
        content.add(versionLabel, BorderLayout.CENTER);

        // Status panel (south)
        JPanel statusPanel = new JPanel(new BorderLayout(0, 10));
        statusPanel.setOpaque(false);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(80, 180, 200));
        progressBar.setBackground(new Color(40, 40, 60));
        progressBar.setBorderPainted(true);

        // Status label
        statusLabel = new JLabel("Starting application...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);

        // Add components to status panel
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.SOUTH);
        content.add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Configure the expected number of tasks for progress tracking
     *
     * @param count Number of expected tasks
     */
    public void setTaskCount(int count) {
        this.totalTasks = Math.max(1, count); // Ensure at least 1
        this.completedTasks = 0;
        updateProgress();
    }

    /**
     * Mark a task as completed and update progress
     *
     * @param taskName Name of the completed task
     */
    public void completeTask(String taskName) {
        completedTasks++;
        setStatus(taskName);
        updateProgress();
    }

    /**
     * Update the status message
     *
     * @param status New status message to display
     */
    public void setStatus(final String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            logger.info("Splash screen status: " + status);
        });
    }

    /**
     * Update the progress bar based on completed tasks
     */
    private void updateProgress() {
        if (totalTasks > 0) {
            final int progress = (completedTasks * 100) / totalTasks;
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
            });
        }
    }

    /**
     * Manually set progress percentage
     *
     * @param percent Progress percentage (0-100)
     */
    public void setProgress(int percent) {
        final int validPercent = Math.max(0, Math.min(100, percent));
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(validPercent);
            progressBar.setString(validPercent + "%");
        });
    }

    /**
     * Load the application logo
     */
    private BufferedImage loadLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/beat-generator-logo.png");
            if (is != null) {
                return ImageIO.read(is);
            }
            logger.warn("Logo file not found, using text title instead");
        } catch (Exception e) {
            logger.error("Error loading logo: " + e.getMessage());
        }
        return null;
    }

    /**
     * Panel to display the logo image
     */
    private static class LogoPanel extends JPanel {
        private final BufferedImage logo;

        public LogoPanel(BufferedImage logo) {
            this.logo = logo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (logo != null) {
                // Draw logo centered
                int x = (getWidth() - logo.getWidth()) / 2;
                int y = (getHeight() - logo.getHeight()) / 2;
                g.drawImage(logo, x, y, this);
            }
        }
    }
}
