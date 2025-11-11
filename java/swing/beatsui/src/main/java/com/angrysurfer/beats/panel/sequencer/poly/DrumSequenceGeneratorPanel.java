package com.angrysurfer.beats.panel.sequencer.poly;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.sequencer.DrumSequencer;

/**
 * Panel for generating random patterns in the drum sequencer
 */
public class DrumSequenceGeneratorPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceGeneratorPanel.class);

    // UI components
    private JComboBox<String> densityCombo;
    private JButton generateButton;

    // Reference to the sequencer
    private final DrumSequencer sequencer;

    /**
     * Create a new DrumSequenceGeneratorPanel
     * 
     * @param sequencer The drum sequencer
     */    public DrumSequenceGeneratorPanel(DrumSequencer sequencer) {
        this.sequencer = sequencer;
        UIHelper.setWidgetPanelBorder(this, "Generate");

        // REDUCED: from 5,2 to 2,1
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 1));
        
        // Add mouse wheel listener to the entire panel
        addMouseWheelListener(this::handleMouseWheelEvent);

        initializeComponents();
    }

    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Create density combo without a label
        String[] densityOptions = { "25%", "50%", "75%", "100%" };
        densityCombo = new JComboBox<>(densityOptions);
        densityCombo.setSelectedIndex(1); // Default to 50%

        // REDUCED: from LARGE_CONTROL_WIDTH to MEDIUM_CONTROL_WIDTH + 10
        densityCombo.setPreferredSize(new Dimension(UIHelper.MEDIUM_CONTROL_WIDTH + 10, UIHelper.CONTROL_HEIGHT));
        densityCombo.setToolTipText("Set pattern density");

        // Generate button with dice icon
        generateButton = new JButton("🎲");
        generateButton.setToolTipText("Generate a random pattern");
        generateButton.setPreferredSize(new Dimension(24
                , 24));
        generateButton.setMargin(new Insets(2, 2, 2, 2));
        generateButton.addActionListener(e -> {
            // Get selected density from the combo
            int density = (densityCombo.getSelectedIndex() + 1) * 25;
            logger.info("Generating pattern with density: {}%", density);

            // Generate pattern in the sequencer
            sequencer.generatePattern(density);

            // Publish event to refresh UI
            CommandBus.getInstance().publish(
                    Commands.DRUM_GRID_REFRESH_REQUESTED,
                    this,
                    null);
        });

        // Latch toggle button (moved from sequence parameters panel)
        JToggleButton latchToggleButton = new JToggleButton("L", false);
        latchToggleButton.setToolTipText("Generate new pattern each cycle");
        latchToggleButton.setPreferredSize(new Dimension(24, 24));
        latchToggleButton.addActionListener(e -> {
            // sequencer.setLatchEnabled(latchToggleButton.isSelected());
            logger.info("Latch mode set to: {}", latchToggleButton.isSelected());
        });
        latchToggleButton.setEnabled(false);

        // Add components to panel
        add(generateButton);
        add(densityCombo);
        add(latchToggleButton);
    }

    /**
     * Set the density value
     * 
     * @param densityPercent Density percentage (25, 50, 75, 100)
     */
    public void setDensity(int densityPercent) {
        // Convert percentage to index (0-3)
        int index = (densityPercent / 25) - 1;

        // Ensure index is within bounds
        if (index >= 0 && index < densityCombo.getItemCount()) {
            densityCombo.setSelectedIndex(index);
        }
    }

    /**
     * Get the current density percentage value
     * 
     * @return The density percentage (25, 50, 75, 100)
     */
    public int getDensity() {
        return (densityCombo.getSelectedIndex() + 1) * 25;
    }    /**
     * Handles mouse wheel events for the panel's components
     *
     * @param e The mouse wheel event
     */
    private void handleMouseWheelEvent(MouseWheelEvent e) {
        // Determine scroll direction (-1 for up, 1 for down)
        int scrollDirection = e.getWheelRotation() > 0 ? -1 : 1;
        
        // Handle density combo box directly
        int currentIndex = densityCombo.getSelectedIndex();
        int newIndex = currentIndex + scrollDirection;
        
        // Ensure within bounds
        newIndex = Math.max(0, Math.min(newIndex, densityCombo.getItemCount() - 1));
        
        // Update if changed
        if (newIndex != currentIndex) {
            densityCombo.setSelectedIndex(newIndex);
            // The action listener will handle the update
        }
    }
}
