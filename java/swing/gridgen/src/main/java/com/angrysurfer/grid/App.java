package com.angrysurfer.grid;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.grid.panel.GridPanel;
import com.formdev.flatlaf.FlatLightLaf;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting grid visualizer...");

            // Setup Look and Feel
            setupLookAndFeel();

            // Create and show GUI on EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    createAndShowGUI();
                    logger.info("Grid visualizer started successfully");
                } catch (Exception e) {
                    handleInitializationFailure("Failed to create application window", e);
                }
            });
        } catch (Exception e) {
            handleInitializationFailure("Fatal error during application startup", e);
        }
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            logger.info("Set default Look and Feel: FlatLightLaf");
        } catch (Exception e) {
            logger.error("Error setting look and feel: {}", e.getMessage());
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Grid Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create the grid panel which will automatically start the visualizer
        GridPanel gridPanel = new GridPanel();
        
        frame.setContentPane(gridPanel);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
        
        logger.info("Grid visualizer window displayed");
    }

    private static void handleInitializationFailure(String errorMessage, Exception e) {
        logger.error("Critical initialization error: {}", errorMessage);
        logger.error("Exception caught during initialization", e);

        SwingUtilities.invokeLater(() -> {
            String fullMessage = String.format("""
                    Failed to initialize grid visualizer: %s
                    
                    Error details: %s
                    
                    The application will now exit.""", errorMessage, e.getMessage());

            JOptionPane.showMessageDialog(null, fullMessage, "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }
}